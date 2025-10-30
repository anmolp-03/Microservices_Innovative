const { MongoClient, ObjectId } = require('mongodb');
const amqp = require('amqplib');
const dns = require('dns');
dns.setDefaultResultOrder('ipv4first');

class BillService {
    constructor() {
    this.mongoUri = process.env.MONGODB_URI || 'mongodb://mongodb:27017/restaurant?replicaSet=rs0';
        this.rabbitMqUrl = process.env.RABBITMQ_URL || 'amqp://guest:guest@rabbitmq:5672';
        this.channel = null;
        this.client = null;
        this.db = null;
    }

    async connect() {
        try {
            console.log('Connecting to MongoDB and RabbitMQ for bill service');
            // Connect to MongoDB
            this.client = new MongoClient(this.mongoUri, { serverSelectionTimeoutMS: 5000 });
            await this.client.connect();
            this.db = this.client.db('restaurant');
            console.log('Connected to MongoDB');

            // Connect to RabbitMQ
            const connection = await amqp.connect(this.rabbitMqUrl, {
                // Force IPv4 resolution because Docker DNS may advertise IPv6-only host entries
                lookup: (hostname, options, callback) => dns.lookup(hostname, { family: 4 }, callback)
            });
            this.channel = await connection.createChannel();
            console.log('Connected to RabbitMQ and created channel');
            
            // Setup exchanges and queues
            await this.channel.assertExchange('bill.events', 'direct', { durable: true });
            await this.channel.assertQueue('bill.generated', { durable: true });
            await this.channel.bindQueue('bill.generated', 'bill.events', 'bill.generated');
            console.log('bill.events exchange and queue configured');

            // Listen for order events from the command side
            await this.channel.assertExchange('order.events', 'direct', { durable: false });
            await this.channel.assertQueue('order.created', { durable: false });
            await this.channel.bindQueue('order.created', 'order.events', 'order.created');
            this.channel.consume('order.created', msg => this.handleOrderCreated(msg));
            console.log('Subscribed to order.created events');

            // Start change stream
            this.startChangeStream();

            console.log('Bill service connected to MongoDB and RabbitMQ, listening for order events');
        } catch (error) {
            console.error('Error during initialization:', error);
            throw error;
        }
    }

    async startChangeStream() {
        const collection = this.db.collection('bills');
        const changeStream = collection.watch();
        
        changeStream.on('change', async change => {
            if (change.operationType === 'insert') {
                const bill = change.fullDocument;
                const event = {
                    id: bill._id.toString(),
                    orderId: bill.orderId,
                    totalAmount: bill.totalAmount,
                    tax: bill.tax,
                    finalAmount: bill.finalAmount,
                    generatedAt: bill.generatedAt
                };
                
                await this.channel.publish(
                    'bill.events',
                    'bill.generated',
                    Buffer.from(JSON.stringify(event))
                );
            }
        });
    }

    async handleOrderCreated(msg) {
        if (!msg) return;
        
        try {
            const order = JSON.parse(msg.content.toString());
            console.log(`Processing order.created event for order ${order.id}`);
            await this.generateBill(order);
            this.channel.ack(msg);
            console.log(`Generated bill for order ${order.id}`);
        } catch (error) {
            console.error('Error handling order created:', error);
            this.channel.nack(msg);
        }
    }

    async generateBill(order) {
        const bill = {
            orderId: order.id,
            totalAmount: this.calculateTotal(order.items),
            tax: this.calculateTax(order.items),
            status: 'PENDING',
            generatedAt: new Date(),
            updatedAt: new Date()
        };
        
        bill.finalAmount = bill.totalAmount + bill.tax;
        
        await this.db.collection('bills').insertOne(bill);
        return bill;
    }

    calculateTotal(items) {
        return items.reduce((total, item) => {
            const price = Number(item.price);
            const quantity = Number(item.quantity);
            if (Number.isNaN(price) || Number.isNaN(quantity)) {
                console.warn('Skipping order item with invalid price or quantity', item);
                return total;
            }
            return total + (price * quantity);
        }, 0);
    }

    calculateTax(items) {
        const total = this.calculateTotal(items);
        return total * 0.1; // 10% tax
    }

    async getBill(billId) {
        try {
            const objectId = new ObjectId(billId);
            return await this.db.collection('bills').findOne({ _id: objectId });
        } catch (error) {
            console.error('Invalid bill id supplied', error);
            return null;
        }
    }

    async getBillByOrder(orderId) {
        return await this.db.collection('bills').findOne({ orderId });
    }
}

module.exports = BillService;
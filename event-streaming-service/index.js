const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const amqp = require('amqplib');
const cors = require('cors');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

// Serve static files
app.use(express.static(path.join(__dirname, 'public')));
app.use(cors());

const RABBITMQ_URL = process.env.RABBITMQ_URL || 'amqp://guest:guest@rabbitmq:5672';
const PORT = process.env.PORT || 3100;

// Store connected clients
const clients = new Set();

// Connect to RabbitMQ and set up exchanges/queues
async function setupRabbitMQ() {
  try {
    const connection = await amqp.connect(RABBITMQ_URL);
    const channel = await connection.createChannel();

    // Create exchanges for different services
    await channel.assertExchange('logs.menu', 'fanout', { durable: false });
    await channel.assertExchange('logs.order', 'fanout', { durable: false });
    await channel.assertExchange('logs.bill', 'fanout', { durable: false });
    await channel.assertExchange('logs.review', 'fanout', { durable: false });

    // Create and bind queues for each service
    const services = ['menu', 'order', 'bill', 'review'];
    
    for (const service of services) {
      const { queue } = await channel.assertQueue('', { exclusive: true });
      await channel.bindQueue(queue, `logs.${service}`, '');
      
      // Consume messages from each queue
      channel.consume(queue, (msg) => {
        if (msg) {
          const logMessage = {
            service,
            timestamp: new Date().toISOString(),
            message: msg.content.toString(),
          };
          
          // Broadcast to all connected clients
          io.emit('log_message', logMessage);
          channel.ack(msg);
        }
      });
    }

    console.log('Connected to RabbitMQ, listening for logs...');
    return channel;
  } catch (error) {
    console.error('Error connecting to RabbitMQ:', error);
    setTimeout(setupRabbitMQ, 5000);
  }
}

// Socket.IO connection handling
io.on('connection', (socket) => {
  console.log('Client connected:', socket.id);
  clients.add(socket);

  socket.on('disconnect', () => {
    console.log('Client disconnected:', socket.id);
    clients.delete(socket);
  });
});

// Start the server
async function startServer() {
  await setupRabbitMQ();
  server.listen(PORT, () => {
    console.log(`Event streaming service listening on port ${PORT}`);
  });
}

startServer().catch(console.error);
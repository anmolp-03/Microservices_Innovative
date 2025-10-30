const express = require('express')
const BillService = require('./bill_service')

const app = express()
app.use(express.json())

const service = new BillService()

service.connect()
  .then(() => console.log('Bill service connected to MongoDB and RabbitMQ'))
  .catch(err => {
    console.error('Bill service failed to start:', err)
    process.exit(1)
  })

app.get('/health', (_req, res) => {
  res.json({ status: 'ok' })
})

app.get('/bills/order/:orderId', async (req, res) => {
  try {
    const bill = await service.getBillByOrder(req.params.orderId)
    if (!bill) {
      return res.status(404).json({ error: 'Bill not found for order' })
    }
    res.json(bill)
  } catch (error) {
    console.error('Failed to load bill by order', error)
    res.status(500).json({ error: 'Failed to load bill' })
  }
})

app.get('/bills/:billId', async (req, res) => {
  try {
    const bill = await service.getBill(req.params.billId)
    if (!bill) {
      return res.status(404).json({ error: 'Bill not found' })
    }
    res.json(bill)
  } catch (error) {
    console.error('Failed to load bill', error)
    res.status(500).json({ error: 'Failed to load bill' })
  }
})

app.listen(3000, () => console.log('bill-service listening on 3000'))

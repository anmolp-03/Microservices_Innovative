import json
import os
import threading
import time
from datetime import datetime
from typing import Any, Dict, List, Optional

import pika
from bson import ObjectId
from pymongo import MongoClient


class OrderService:
    def __init__(self) -> None:
        mongo_uri = os.getenv('MONGODB_URI', 'mongodb://mongodb:27017/restaurant?replicaSet=rs0')
        self.client = MongoClient(mongo_uri)
        self.db = self.client.restaurant

        self.rabbitmq_url = os.getenv('RABBITMQ_URL', 'amqp://guest:guest@rabbitmq:5672')
        self.rabbitmq_conn: Optional[pika.BlockingConnection] = None
        self.channel: Optional[Any] = None

        self._ensure_channel()
        self.start_change_stream()

    def start_change_stream(self) -> None:
        def watch_changes() -> None:
            pipeline = [{'$match': {'operationType': {'$in': ['insert', 'update']}}}]
            while True:
                try:
                    with self.db.orders.watch(pipeline, full_document='updateLookup') as stream:
                        for change in stream:
                            self.handle_change(change)
                except Exception as exc:
                    print(f"Order change stream error: {exc}")
                    time.sleep(1)

        thread = threading.Thread(target=watch_changes, daemon=True)
        thread.start()

    def handle_change(self, change: Dict[str, Any]) -> None:
        operation = change.get('operationType')

        try:
            if operation == 'insert':
                document = change['fullDocument']
                event = {
                    'id': str(document['_id']),
                    'customerId': document.get('customerId'),
                    'items': document.get('items'),
                    'status': document.get('status'),
                    'createdAt': document.get('createdAt').isoformat() if document.get('createdAt') else None
                }
                self._publish('order.created', event)
            elif operation == 'update':
                document = change['fullDocument']
                event = {
                    'id': str(document['_id']),
                    'status': document.get('status'),
                    'updatedAt': document.get('updatedAt').isoformat() if document.get('updatedAt') else None
                }
                self._publish('order.updated', event)
        except Exception as exc:
            print(f"Failed to handle order change event: {exc}")

    def _ensure_channel(self) -> None:
        if self.rabbitmq_conn and self.rabbitmq_conn.is_open and self.channel and getattr(self.channel, 'is_open', False):
            return

        if self.rabbitmq_conn:
            try:
                self.rabbitmq_conn.close()
            except Exception:
                pass

        self.rabbitmq_conn = pika.BlockingConnection(pika.URLParameters(self.rabbitmq_url))
        self.channel = self.rabbitmq_conn.channel()

        self.channel.exchange_declare(exchange='order.events', exchange_type='direct')
        self.channel.queue_declare(queue='order.created')
        self.channel.queue_declare(queue='order.updated')
        self.channel.queue_bind(exchange='order.events', queue='order.created', routing_key='order.created')
        self.channel.queue_bind(exchange='order.events', queue='order.updated', routing_key='order.updated')

    def _publish(self, routing_key: str, payload: Dict[str, Any]) -> None:
        self._ensure_channel()
        assert self.channel is not None
        message = json.dumps(payload)
        self.channel.basic_publish(exchange='order.events', routing_key=routing_key, body=message)
        print(f"Published {routing_key} event for order {payload.get('id')}")

    def create_order(self, customer_id: str, items: List[Dict[str, Any]]) -> str:
        order = {
            'customerId': customer_id,
            'items': items,
            'status': 'PENDING',
            'createdAt': datetime.utcnow(),
            'updatedAt': datetime.utcnow()
        }
        result = self.db.orders.insert_one(order)
        return str(result.inserted_id)

    def update_order_status(self, order_id: str, status: str) -> bool:
        result = self.db.orders.update_one(
            {'_id': ObjectId(order_id)},
            {
                '$set': {
                    'status': status,
                    'updatedAt': datetime.utcnow()
                }
            }
        )
        return result.modified_count > 0

    def get_order(self, order_id: str) -> Optional[Dict[str, Any]]:
        order = self.db.orders.find_one({'_id': ObjectId(order_id)})
        if order:
            order['id'] = str(order.pop('_id'))
            return order
        return None

    def get_orders_by_customer(self, customer_id: str) -> List[Dict[str, Any]]:
        orders = list(self.db.orders.find({'customerId': customer_id}))
        for order in orders:
            order['id'] = str(order.pop('_id'))
        return orders
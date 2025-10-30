from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, ConfigDict
import httpx
import os
from typing import List

from order_service import OrderService


app = FastAPI()
order_service = OrderService()

MENU_SERVICE_URL = os.getenv('MENU_SERVICE_URL', 'http://menu:8080')


class OrderItemRequest(BaseModel):
    menu_id: str = Field(alias='menuId')
    quantity: int = Field(gt=0, description='Quantity must be positive')

    model_config = ConfigDict(populate_by_name=True)


class OrderRequest(BaseModel):
    customer_id: str = Field(alias='customerId')
    items: List[OrderItemRequest]

    model_config = ConfigDict(populate_by_name=True)


async def fetch_menu_items() -> dict[str, dict]:
    """Retrieve menu items and index by id."""
    async with httpx.AsyncClient() as client:
        response = await client.get(f"{MENU_SERVICE_URL}/menu")
        if response.status_code != 200:
            raise HTTPException(status_code=502, detail='Menu service unavailable')
        items = response.json()
        return {item['id']: item for item in items}


@app.post('/orders')
async def create_order(request: OrderRequest):
    if not request.items:
        raise HTTPException(status_code=400, detail='At least one order item is required')

    menu_index = await fetch_menu_items()

    order_items = []
    for item in request.items:
        menu_item = menu_index.get(item.menu_id)
        if not menu_item:
            raise HTTPException(status_code=400, detail=f'Menu item {item.menu_id} not found')

        order_items.append({
            'menuId': menu_item['id'],
            'name': menu_item['name'],
            'price': menu_item['price'],
            'quantity': item.quantity
        })

    order_id = order_service.create_order(request.customer_id, order_items)
    return {'order_id': order_id}


@app.get('/orders/{order_id}')
def get_order(order_id: str):
    order = order_service.get_order(order_id)
    if not order:
        raise HTTPException(status_code=404, detail='Order not found')
    return order


@app.get('/customers/{customer_id}/orders')
def get_orders_by_customer(customer_id: str):
    return order_service.get_orders_by_customer(customer_id)

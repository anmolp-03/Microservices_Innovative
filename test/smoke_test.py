#!/usr/bin/env python3
"""
Simple smoke test for the restaurant microservices.
No external dependencies — uses urllib from the stdlib.

Usage: python test/smoke_test.py
"""
import json
import sys
import urllib.request
import urllib.error

HOSTS = {
    'menu': 'http://localhost:8080/menu',
    'order': 'http://localhost:8000/orders',
    'bill': 'http://localhost:3000/bills',
    'review': 'http://localhost:4000/reviews',
}

HEADERS = {'Content-Type': 'application/json'}


def http_post(url, payload):
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(url, data=data, headers=HEADERS, method='POST')
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            status = resp.getcode()
            body = resp.read()
            text = body.decode('utf-8') if body else ''
            return status, text
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')
    except Exception as e:
        print(f'ERROR: request to {url} failed: {e}')
        sys.exit(2)


def http_get(url):
    req = urllib.request.Request(url, headers=HEADERS, method='GET')
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            status = resp.getcode()
            body = resp.read()
            text = body.decode('utf-8') if body else ''
            return status, text
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode('utf-8')
    except Exception as e:
        print(f'ERROR: request to {url} failed: {e}')
        sys.exit(2)


def pretty_print(prefix, text):
    try:
        obj = json.loads(text)
        print(prefix, json.dumps(obj, indent=2))
    except Exception:
        print(prefix, text)


def main():
    print('1) GET menu')
    code, body = http_get(HOSTS['menu'])
    if code != 200:
        print(f'GET /menu returned {code}')
        print(body)
        sys.exit(3)
    menu = json.loads(body)
    pretty_print('Menu:', body)

    # choose menu id
    menu_id = None
    if isinstance(menu, list) and len(menu) > 0:
        for m in menu:
            if m.get('id') == 2:
                menu_id = 2
                break
        if not menu_id:
            menu_id = menu[0].get('id')
    elif isinstance(menu, dict):
        menu_id = menu.get('id')

    if not menu_id:
        print('No menu id found to place order')
        sys.exit(4)

    print(f'Using menu_id = {menu_id}')

    print('\n2) POST /orders')
    order_payload = {'customer': 'smoke-py', 'items': [{'menu_id': menu_id, 'qty': 1}]}
    code, body = http_post(HOSTS['order'], order_payload)
    if code not in (200, 201):
        print(f'Create order failed: {code}')
        print(body)
        sys.exit(5)
    order_resp = json.loads(body)
    pretty_print('Order created:', body)
    order_id = order_resp.get('order_id')
    if not order_id:
        print('order_id missing in response')
        sys.exit(6)

    print(f'\n3) GET /orders/{order_id}')
    code, body = http_get(f"http://localhost:8000/orders/{order_id}")
    if code != 200:
        print(f'Get order failed: {code}')
        print(body)
        sys.exit(7)
    pretty_print('Order fetched:', body)

    print(f'\n4) POST /bills for order {order_id}')
    code, body = http_post(HOSTS['bill'], {'order_id': order_id})
    if code not in (200, 201):
        print(f'Bill request failed: {code}')
        print(body)
        sys.exit(8)
    pretty_print('Bill response:', body)

    print('\n5) POST /reviews')
    code, body = http_post(HOSTS['review'], {'customer': 'smoke-py', 'rating': 5, 'comment': 'smoke test'})
    if code not in (200, 201):
        print(f'Review post failed: {code}')
        print(body)
        sys.exit(9)
    print(f'Review posted: HTTP {code}')

    print('\nSMOKE TESTS PASSED')
    sys.exit(0)


if __name__ == '__main__':
    main()

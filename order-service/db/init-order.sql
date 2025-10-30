-- init-order.sql: create orders and order_items tables
CREATE TABLE IF NOT EXISTS orders (
  id SERIAL PRIMARY KEY,
  customer TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
  id SERIAL PRIMARY KEY,
  order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
  menu_id INTEGER NOT NULL,
  qty INTEGER NOT NULL
);

-- init-bill.sql: create bills table
CREATE TABLE IF NOT EXISTS bills (
  id SERIAL PRIMARY KEY,
  order_id INTEGER NOT NULL,
  total DOUBLE PRECISION NOT NULL
);

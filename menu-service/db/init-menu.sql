-- init-menu.sql: create table and seed sample menu items
CREATE TABLE IF NOT EXISTS menu_item (
  id SERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  price DOUBLE PRECISION NOT NULL
);

INSERT INTO menu_item (name, price) VALUES
  ('Margherita Pizza', 7.99)
  ON CONFLICT DO NOTHING;

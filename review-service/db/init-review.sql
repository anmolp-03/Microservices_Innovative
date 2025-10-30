-- init-review.sql: create reviews table
CREATE TABLE IF NOT EXISTS reviews (
  id SERIAL PRIMARY KEY,
  customer TEXT NOT NULL,
  rating INTEGER NOT NULL,
  comment TEXT
);

CREATE TABLE IF NOT EXISTS restaurant_service.reviews
(
    id           INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    order_id     INTEGER REFERENCES restaurant_service.orders (id) ON DELETE CASCADE,
    rating       SMALLINT CHECK (rating BETWEEN 1 AND 10),
    visitor_name VARCHAR(64) NOT NULL,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_reviews_rating ON restaurant_service.reviews (rating);
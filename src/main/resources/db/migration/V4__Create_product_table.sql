-- Таблица для позиций меню
CREATE TABLE IF NOT EXISTS restaurant_service.products
(
    id           INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name         TEXT           NOT NULL,
    description  TEXT,
    type_id      INTEGER        REFERENCES restaurant_service.product_types (id) ON DELETE SET NULL,
    price        DECIMAL(10, 2) NOT NULL,
    cooking_time TIME
);

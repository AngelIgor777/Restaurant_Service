-- Таблица для типов продуктов
CREATE TABLE IF NOT EXISTS restaurant_service.product_types
(
    id   INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE
);

INSERT INTO restaurant_service.product_types (name) VALUES
                                                        ('Напитки'),
                                                        ('Закуски'),
                                                        ('Основные блюда'),
                                                        ('Десерты');
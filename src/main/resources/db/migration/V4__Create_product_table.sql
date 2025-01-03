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

INSERT INTO restaurant_service.products (name, description, type_id, price, cooking_time)
VALUES ('Кола', 'Газированный напиток', 1, 17.50, NULL),
       ('Чай', 'Чёрный или зелёный', 1, 20.00, '00:02:00'),
       ('Суп', 'Куриный суп', 3, 35.00, '00:20:00'),
       ('Торт', 'Шоколадный торт', 4, '55.75', '00:15:00'),
       ('Цезарь', 'Салат с курицей и соусом', 5, 45.00, '00:10:00'),       -- добавлен салат "Цезарь"
       ('Стейк', 'Говяжий стейк, жареный на гриле', 6, 250.00, '00:30:00'); -- добавлено блюдо на гриле "Стейк"

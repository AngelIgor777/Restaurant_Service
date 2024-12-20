CREATE TABLE IF NOT EXISTS restaurant_service.tables
(
    id     INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    number INT NOT NULL,
    CONSTRAINT unique_table_number UNIQUE (number)
);
INSERT INTO restaurant_service.tables (number)
VALUES (1),
       (2),
       (3),
       (4);
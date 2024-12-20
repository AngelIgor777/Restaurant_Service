
CREATE TABLE IF NOT EXISTS restaurant_service.ingredients
(
    id          INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name        VARCHAR(128) NOT NULL UNIQUE,
    description TEXT,
    unit        VARCHAR(50)  NOT NULL CHECK (unit IN ('kg', 'liter', 'piece'))
);

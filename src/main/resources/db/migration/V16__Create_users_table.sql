CREATE TABLE IF NOT EXISTS restaurant_service.users
(
    id         INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chat_id    BIGINT REFERENCES restaurant_service.telegram_user (chat_id) ON DELETE CASCADE UNIQUE NOT NULL
);


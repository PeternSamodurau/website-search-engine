-- Удаляем таблицу, если она существует, для полной очистки перед созданием
DROP TABLE IF EXISTS contacts CASCADE;

-- Создаем таблицу для хранения контактов
CREATE TABLE contacts (
    id UUID PRIMARY KEY,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL
);
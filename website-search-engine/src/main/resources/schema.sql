-- Удаляем таблицы в обратном порядке зависимостей, чтобы избежать ошибок
DROP TABLE IF EXISTS "index" CASCADE;
DROP TABLE IF EXISTS lemma CASCADE;
DROP TABLE IF EXISTS page CASCADE;
DROP TABLE IF EXISTS site CASCADE;

-- Удаляем пользовательский тип ENUM, если он существует
DROP TYPE IF EXISTS status_enum;

-- Создаем пользовательский тип ENUM для статусов сайта
CREATE TYPE status_enum AS ENUM ('INDEXING', 'INDEXED', 'FAILED');

-- Создаем таблицу site
CREATE TABLE site
(
    id          SERIAL PRIMARY KEY,
    status      status_enum NOT NULL,
    status_time TIMESTAMP   NOT NULL,
    last_error  TEXT,
    url         VARCHAR(255) NOT NULL,
    name        VARCHAR(255) NOT NULL
);

-- Создаем таблицу page
CREATE TABLE page
(
    id      SERIAL PRIMARY KEY,
    site_id INT          NOT NULL,
    path    VARCHAR(255) NOT NULL,
    code    INT          NOT NULL,
    content TEXT         NOT NULL,
    FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE
);
-- Добавляем индекс на path для быстрого поиска
CREATE INDEX path_index ON page (path, site_id);


-- Создаем таблицу lemma
CREATE TABLE lemma
(
    id        SERIAL PRIMARY KEY,
    site_id   INT          NOT NULL,
    lemma     VARCHAR(255) NOT NULL,
    frequency INT          NOT NULL,
    FOREIGN KEY (site_id) REFERENCES site (id) ON DELETE CASCADE,
    UNIQUE (site_id, lemma)
);

-- Создаем таблицу index (имя в кавычках, т.к. это ключевое слово)
CREATE TABLE "index"
(
    id       SERIAL PRIMARY KEY,
    page_id  INT  NOT NULL,
    lemma_id INT  NOT NULL,
    "rank"   REAL NOT NULL, -- rank тоже ключевое слово
    FOREIGN KEY (page_id) REFERENCES page (id) ON DELETE CASCADE,
    FOREIGN KEY (lemma_id) REFERENCES lemma (id) ON DELETE CASCADE
);

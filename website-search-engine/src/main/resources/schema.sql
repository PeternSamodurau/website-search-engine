-- Удаляем таблицы в обратном порядке зависимостей
DROP TABLE IF EXISTS `index`;
DROP TABLE IF EXISTS `lemma`;
DROP TABLE IF EXISTS `page`;
DROP TABLE IF EXISTS `site`;


-- Создаем таблицу site
CREATE TABLE `site`
(
    `id`          INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `status`      VARCHAR(255) NOT NULL,
    `status_time` DATETIME     NOT NULL,
    `last_error`  TEXT,
    `url`         VARCHAR(255) NOT NULL,
    `name`        VARCHAR(255) NOT NULL
);

-- Создаем таблицу page
CREATE TABLE `page`
(
    `id`      INT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `site_id` INT      NOT NULL,
    `path`    VARCHAR(512) NOT NULL,
    `code`    INT      NOT NULL,
    `content` MEDIUMTEXT NOT NULL,
    FOREIGN KEY (`site_id`) REFERENCES `site` (`id`) ON DELETE CASCADE
);
-- Добавляем индекс на path
CREATE INDEX `path_index` ON `page` (`path`, `site_id`);


-- Создаем таблицу lemma
CREATE TABLE `lemma`
(
    `id`        INT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `site_id`   INT          NOT NULL,
    `lemma`     VARCHAR(255) NOT NULL,
    `frequency` INT          NOT NULL,
    FOREIGN KEY (`site_id`) REFERENCES `site` (`id`) ON DELETE CASCADE,
    CONSTRAINT `uk_lemma_site` UNIQUE (`site_id`, `lemma`)
);

-- Создаем таблицу index
CREATE TABLE `index`
(
    `id`       INT   NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `page_id`  INT   NOT NULL,
    `lemma_id` INT   NOT NULL,
    `rank`     FLOAT NOT NULL,
    FOREIGN KEY (`page_id`) REFERENCES `page` (`id`) ON DELETE CASCADE,
    FOREIGN KEY (`lemma_id`) REFERENCES `lemma` (`id`) ON DELETE CASCADE
);
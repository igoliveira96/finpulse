-- FinPulse local seed, schema v1. All values are illustrative and match MockMarketData.
-- Rebuild the packaged SQLite file: python3 tools/build_seed_database.py
PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;
PRAGMA user_version = 1;

CREATE TABLE IF NOT EXISTS `investors` (`id` INTEGER NOT NULL, `first_name` TEXT NOT NULL, `initials` TEXT NOT NULL, PRIMARY KEY(`id`));
CREATE TABLE IF NOT EXISTS `portfolios` (`investor_id` INTEGER NOT NULL, `balance_in_cents` INTEGER NOT NULL, `monthly_gain_in_cents` INTEGER NOT NULL, `monthly_change_percent` REAL NOT NULL, PRIMARY KEY(`investor_id`), FOREIGN KEY(`investor_id`) REFERENCES `investors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE TABLE IF NOT EXISTS `portfolio_history` (`investor_id` INTEGER NOT NULL, `sample_index` INTEGER NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`investor_id`, `sample_index`), FOREIGN KEY(`investor_id`) REFERENCES `portfolios`(`investor_id`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE TABLE IF NOT EXISTS `stocks` (`symbol` TEXT NOT NULL, `company` TEXT NOT NULL, `price_in_cents` INTEGER NOT NULL, `daily_change_percent` REAL NOT NULL, `display_order` INTEGER NOT NULL, PRIMARY KEY(`symbol`));
CREATE TABLE IF NOT EXISTS `stock_history` (`stock_symbol` TEXT NOT NULL, `sample_index` INTEGER NOT NULL, `value` REAL NOT NULL, PRIMARY KEY(`stock_symbol`, `sample_index`), FOREIGN KEY(`stock_symbol`) REFERENCES `stocks`(`symbol`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE TABLE IF NOT EXISTS `favorites` (`investor_id` INTEGER NOT NULL, `stock_symbol` TEXT NOT NULL, PRIMARY KEY(`investor_id`, `stock_symbol`), FOREIGN KEY(`investor_id`) REFERENCES `investors`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`stock_symbol`) REFERENCES `stocks`(`symbol`) ON UPDATE NO ACTION ON DELETE CASCADE );
CREATE INDEX IF NOT EXISTS `index_favorites_stock_symbol` ON `favorites` (`stock_symbol`);

INSERT INTO investors (id, first_name, initials) VALUES (1, 'Igor', 'IG');
INSERT INTO portfolios (investor_id, balance_in_cents, monthly_gain_in_cents, monthly_change_percent) VALUES (1, 35000000, 842000, 2.46);

INSERT INTO portfolio_history (investor_id, sample_index, value) VALUES
    (1, 0, 18.0),
    (1, 1, 21.0),
    (1, 2, 20.0),
    (1, 3, 24.0),
    (1, 4, 29.0),
    (1, 5, 28.0),
    (1, 6, 32.0),
    (1, 7, 30.0),
    (1, 8, 35.0),
    (1, 9, 34.0),
    (1, 10, 39.0),
    (1, 11, 42.0),
    (1, 12, 47.0),
    (1, 13, 43.0),
    (1, 14, 40.0),
    (1, 15, 45.0),
    (1, 16, 48.0),
    (1, 17, 51.0),
    (1, 18, 56.0),
    (1, 19, 54.0),
    (1, 20, 60.0),
    (1, 21, 63.0),
    (1, 22, 61.0),
    (1, 23, 68.0),
    (1, 24, 70.0),
    (1, 25, 78.0);

INSERT INTO stocks (symbol, company, price_in_cents, daily_change_percent, display_order) VALUES
    ('PETR4', 'Petrobras', 3842, 1.24, 0),
    ('VALE3', 'Vale', 6280, -0.85, 1),
    ('ITUB4', 'Itaú Unibanco', 3416, 0.62, 2),
    ('WEGE3', 'WEG', 5230, 2.18, 3),
    ('BBAS3', 'Banco do Brasil', 2745, 0.94, 4),
    ('BBDC4', 'Bradesco', 1482, -0.47, 5);

INSERT INTO stock_history (stock_symbol, sample_index, value) VALUES
    ('PETR4', 0, 18.0),
    ('PETR4', 1, 23.0),
    ('PETR4', 2, 20.0),
    ('PETR4', 3, 28.0),
    ('PETR4', 4, 25.0),
    ('PETR4', 5, 30.0),
    ('PETR4', 6, 26.0),
    ('PETR4', 7, 32.0),
    ('PETR4', 8, 36.0),
    ('PETR4', 9, 34.0),
    ('PETR4', 10, 40.0),
    ('VALE3', 0, 40.0),
    ('VALE3', 1, 35.0),
    ('VALE3', 2, 38.0),
    ('VALE3', 3, 27.0),
    ('VALE3', 4, 31.0),
    ('VALE3', 5, 24.0),
    ('VALE3', 6, 28.0),
    ('VALE3', 7, 26.0),
    ('VALE3', 8, 30.0),
    ('VALE3', 9, 25.0),
    ('VALE3', 10, 29.0),
    ('ITUB4', 0, 20.0),
    ('ITUB4', 1, 25.0),
    ('ITUB4', 2, 23.0),
    ('ITUB4', 3, 29.0),
    ('ITUB4', 4, 26.0),
    ('ITUB4', 5, 32.0),
    ('ITUB4', 6, 30.0),
    ('ITUB4', 7, 36.0),
    ('ITUB4', 8, 35.0),
    ('ITUB4', 9, 39.0),
    ('ITUB4', 10, 42.0),
    ('WEGE3', 0, 14.0),
    ('WEGE3', 1, 21.0),
    ('WEGE3', 2, 18.0),
    ('WEGE3', 3, 26.0),
    ('WEGE3', 4, 24.0),
    ('WEGE3', 5, 31.0),
    ('WEGE3', 6, 29.0),
    ('WEGE3', 7, 35.0),
    ('WEGE3', 8, 32.0),
    ('WEGE3', 9, 40.0),
    ('WEGE3', 10, 46.0),
    ('BBAS3', 0, 18.0),
    ('BBAS3', 1, 24.0),
    ('BBAS3', 2, 22.0),
    ('BBAS3', 3, 29.0),
    ('BBAS3', 4, 26.0),
    ('BBAS3', 5, 31.0),
    ('BBAS3', 6, 30.0),
    ('BBAS3', 7, 35.0),
    ('BBAS3', 8, 33.0),
    ('BBAS3', 9, 39.0),
    ('BBAS3', 10, 41.0),
    ('BBDC4', 0, 45.0),
    ('BBDC4', 1, 38.0),
    ('BBDC4', 2, 42.0),
    ('BBDC4', 3, 31.0),
    ('BBDC4', 4, 34.0),
    ('BBDC4', 5, 29.0),
    ('BBDC4', 6, 32.0),
    ('BBDC4', 7, 23.0),
    ('BBDC4', 8, 27.0),
    ('BBDC4', 9, 21.0),
    ('BBDC4', 10, 18.0);

INSERT INTO favorites (investor_id, stock_symbol) VALUES (1, 'PETR4'), (1, 'VALE3'), (1, 'ITUB4');

COMMIT;

-- Los clientes pasan a ser una lista propia: el pedido apunta al cliente en lugar de guardar su nombre.
-- Los nombres que ya tenian los pedidos se convierten en clientes (sin repetir). Se puede repetir sin riesgo.
-- Despues hay que recargar 02_procedures.sql.
-- Las bases nuevas no la necesitan: 01_schema.sql ya incluye este cambio.

CREATE TABLE IF NOT EXISTS customer (
         customer_id INT AUTO_INCREMENT PRIMARY KEY,
         name VARCHAR(150) UNIQUE NOT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

ALTER TABLE customer_order
    ADD COLUMN IF NOT EXISTS customer_id INT DEFAULT NULL AFTER order_number;

-- Solo si la columna antigua sigue ahi (en una segunda pasada ya no existe)
SET @old_column = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_order' AND COLUMN_NAME = 'customer_name');

SET @sql = IF(@old_column > 0,
    'INSERT IGNORE INTO customer (name) SELECT DISTINCT TRIM(customer_name) FROM customer_order WHERE TRIM(customer_name) <> ''''',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@old_column > 0,
    'UPDATE customer_order co JOIN customer c ON c.name = TRIM(co.customer_name) SET co.customer_id = c.customer_id',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE customer_order DROP COLUMN IF EXISTS customer_name;

SET @fk = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'customer_order' AND CONSTRAINT_NAME = 'fk_order_customer');
SET @sql = IF(@fk = 0,
    'ALTER TABLE customer_order ADD CONSTRAINT fk_order_customer FOREIGN KEY (customer_id) REFERENCES customer (customer_id)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

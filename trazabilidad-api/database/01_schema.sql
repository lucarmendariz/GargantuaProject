-- =================================================================
-- 1. LIMPIEZA DE TABLAS (Para que al ejecutar el update_db.sh se limpie la base de datos por completo)
-- =================================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS lot_history;
DROP TABLE IF EXISTS product_lot;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS app_user;
SET FOREIGN_KEY_CHECKS = 1;

-- =================================================================
-- 2. CREACIÓN DE TABLAS MAESTRAS
-- =================================================================

-- Tabla de Productos
CREATE TABLE product (
         product_id INT AUTO_INCREMENT PRIMARY KEY,
         sku VARCHAR(50) UNIQUE NOT NULL,
         name VARCHAR(100) NOT NULL,
         description TEXT DEFAULT NULL,
         is_active TINYINT(1) DEFAULT 1 NOT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Tabla de Lotes
CREATE TABLE product_lot (
         lot_id INT AUTO_INCREMENT PRIMARY KEY,
         product_id INT NOT NULL,
         lot_number VARCHAR(100) UNIQUE NOT NULL,
         quantity DECIMAL(12,4) NOT NULL DEFAULT 0.0000,
         measure_unit VARCHAR(20) NOT NULL DEFAULT 'UNITS',
         expiration_date DATE DEFAULT NULL,
         supplier_reference VARCHAR(100) DEFAULT NULL,
         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
         CONSTRAINT fk_lot_product FOREIGN KEY (product_id) REFERENCES product (product_id)
) ENGINE=InnoDB;

-- Tabla de Historial (Log de movimientos y captura OCR)
CREATE TABLE lot_history (
         history_id INT AUTO_INCREMENT PRIMARY KEY,
         lot_id INT NOT NULL,
         action_type VARCHAR(50) NOT NULL,
         operator VARCHAR(100) NOT NULL,
         action_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
         quantity_affected DECIMAL(12,4) DEFAULT NULL,
         ocr_raw_text TEXT DEFAULT NULL,
         observations TEXT DEFAULT NULL,
         CONSTRAINT fk_history_lot FOREIGN KEY (lot_id) REFERENCES product_lot (lot_id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Tabla de Usuarios del Sistema
CREATE TABLE app_user (
          user_id INT AUTO_INCREMENT PRIMARY KEY,
          username VARCHAR(50) UNIQUE NOT NULL,
          password_hash VARCHAR(255) NOT NULL, -- Guardaremos las contraseñas cifradas
          name VARCHAR(100) NOT NULL,
          role VARCHAR(50) NOT NULL DEFAULT 'OPERATOR', -- OPERATOR, ADMIN, QUALITY
          is_active TINYINT(1) DEFAULT 1 NOT NULL,
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
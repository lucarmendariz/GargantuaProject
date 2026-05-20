DELIMITER ;;

-- 1. Obtener el estado actual completo de un lote por su código de barras/OCR
CREATE OR REPLACE PROCEDURE `ProductLot_GetCurrentStatus`(IN _lotNumber VARCHAR(100))
BEGIN
    IF (SELECT lot_id FROM product_lot WHERE lot_number = _lotNumber) IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El lote no existe.', MYSQL_ERRNO = 101;
    END IF;

    SELECT
        p.product_id,
        p.sku AS product_sku,
        p.name AS product_name,
        pl.lot_id,
        pl.lot_number,
        pl.quantity AS initial_quantity,
        pl.measure_unit,
        pl.expiration_date,
        lh.action_type AS current_state,
        lh.action_timestamp AS last_update,
        lh.operator AS last_operator,
        lh.ocr_raw_text,
        lh.observations AS last_observations
    FROM product_lot pl
             JOIN product p ON pl.product_id = p.product_id
             JOIN lot_history lh ON lh.lot_id = pl.lot_id
    WHERE pl.lot_number = _lotNumber
    ORDER BY lh.history_id DESC
    LIMIT 1;
END;;

-- 2. Obtener todos los productos activos
CREATE OR REPLACE PROCEDURE `Product_FindAll`()
BEGIN
    SELECT * FROM product WHERE is_active = 1;
END;;

-- 3. Obtener un producto por su ID único
CREATE OR REPLACE PROCEDURE `Product_FindById`(IN _productId INT)
BEGIN
    SELECT * FROM product WHERE product_id = _productId;
END;;

-- 4. Obtener todos los lotes asociados a un producto específico
CREATE OR REPLACE PROCEDURE `ProductLot_FindByProductId`(IN _productId INT)
BEGIN
    SELECT * FROM product_lot WHERE product_id = _productId;
END;;

-- 5. Obtener el historial cronológico completo de un lote
CREATE OR REPLACE PROCEDURE `LotHistory_FindByLotId`(IN _lotId INT)
BEGIN
    SELECT * FROM lot_history WHERE lot_id = _lotId ORDER BY history_id DESC;
END;;

-- 6. Insertar un nuevo movimiento/escaneo en el historial
CREATE OR REPLACE PROCEDURE `LotHistory_Insert`(
    IN _lotId INT,
    IN _actionType VARCHAR(50),
    IN _operator VARCHAR(100),
    IN _quantityAffected DECIMAL(12,4),
    IN _ocrRawText TEXT,
    IN _observations TEXT
)
BEGIN
    INSERT INTO lot_history (lot_id, action_type, operator, quantity_affected, ocr_raw_text, observations)
    VALUES (_lotId, _actionType, _operator, _quantityAffected, _ocrRawText, _observations);
END;;

DELIMITER ;
DELIMITER ;;

-- =================================================================
-- BLOQUE 1: CRUD DE PRODUCTOS (product)
-- =================================================================

-- 1.1 CREATE: Crear un nuevo producto
CREATE OR REPLACE PROCEDURE `Product_Create`(
    IN _sku VARCHAR(50),
    IN _name VARCHAR(100),
    IN _description TEXT
)
BEGIN
    IF (SELECT COUNT(*) FROM product WHERE sku = _sku) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Ya existe un producto con ese SKU.', MYSQL_ERRNO = 102;
    END IF;

    INSERT INTO product (sku, name, description, is_active)
    VALUES (_sku, _name, _description, 1);

    SELECT LAST_INSERT_ID() AS new_product_id;
END;;

-- 1.2 READ (ALL): Listar todo el catálogo para administración
CREATE OR REPLACE PROCEDURE `Product_GetAll`()
BEGIN
    SELECT * FROM product ORDER BY name ASC;
END;;

-- 1.3 READ (ACTIVE): Listar solo productos activos (para la app de día a día)
CREATE OR REPLACE PROCEDURE `Product_FindAll`()
BEGIN
    SELECT * FROM product WHERE is_active = 1 ORDER BY name ASC;
END;;

-- 1.4 READ (BY ID): Buscar un producto específico
CREATE OR REPLACE PROCEDURE `Product_FindById`(
    IN _productId INT
)
BEGIN
    SELECT * FROM product WHERE product_id = _productId;
END;;

-- 1.5 UPDATE: Modificar datos del producto
CREATE OR REPLACE PROCEDURE `Product_Update`(
    IN _productId INT,
    IN _sku VARCHAR(50),
    IN _name VARCHAR(100),
    IN _description TEXT,
    IN _isActive TINYINT(1)
)
BEGIN
    IF (SELECT COUNT(*) FROM product WHERE product_id = _productId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El producto no existe.', MYSQL_ERRNO = 103;
    END IF;

    UPDATE product
    SET sku = _sku, name = _name, description = _description, is_active = _isActive
    WHERE product_id = _productId;
END;;

-- 1.6 DELETE: Baja lógica de un producto
CREATE OR REPLACE PROCEDURE `Product_Delete`(
    IN _productId INT
)
BEGIN
    IF (SELECT COUNT(*) FROM product WHERE product_id = _productId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El producto no existe.', MYSQL_ERRNO = 103;
    END IF;

    UPDATE product SET is_active = 0 WHERE product_id = _productId;
END;;


-- =================================================================
-- BLOQUE 2: CRUD DE LOTES DE PRODUCTO (product_lot)
-- =================================================================

-- 2.1 CREATE: Registrar la entrada de un nuevo lote físico
CREATE OR REPLACE PROCEDURE `ProductLot_Create`(
    IN _productId INT,
    IN _lotNumber VARCHAR(100),
    IN _quantity DECIMAL(12,4),
    IN _measureUnit VARCHAR(20),
    IN _expirationDate DATE,
    IN _supplierReference VARCHAR(100)
)
BEGIN
    -- Validar que el producto maestro exista
    IF (SELECT COUNT(*) FROM product WHERE product_id = _productId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El ID de producto asociado no existe.', MYSQL_ERRNO = 104;
    END IF;

    -- Validar que el número de lote sea único
    IF (SELECT COUNT(*) FROM product_lot WHERE lot_number = _lotNumber) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Ya existe un lote físico registrado con ese código.', MYSQL_ERRNO = 105;
    END IF;

    INSERT INTO product_lot (product_id, lot_number, quantity, measure_unit, expiration_date, supplier_reference)
    VALUES (_productId, _lotNumber, _quantity, _measureUnit, _expirationDate, _supplierReference);

    SELECT LAST_INSERT_ID() AS new_lot_id;
END;;

-- 2.2 READ (ALL): Ver todos los lotes en el sistema
CREATE OR REPLACE PROCEDURE `ProductLot_GetAll`()
BEGIN
    SELECT * FROM product_lot ORDER BY created_at DESC;
END;;

-- 2.3 READ (BY PRODUCT ID): Ver los lotes de un producto específico
CREATE OR REPLACE PROCEDURE `ProductLot_FindByProductId`(
    IN _productId INT
)
BEGIN
    SELECT * FROM product_lot WHERE product_id = _productId ORDER BY expiration_date ASC;
END;;

-- 2.4 READ (BY ID): Buscar un lote por su ID interno
CREATE OR REPLACE PROCEDURE `ProductLot_FindById`(
    IN _lotId INT
)
BEGIN
    SELECT * FROM product_lot WHERE lot_id = _lotId;
END;;

-- 2.5 UPDATE: Modificar stock o datos de un lote
CREATE OR REPLACE PROCEDURE `ProductLot_Update`(
    IN _lotId INT,
    IN _quantity DECIMAL(12,4),
    IN _measureUnit VARCHAR(20),
    IN _expirationDate DATE,
    IN _supplierReference VARCHAR(100)
)
BEGIN
    IF (SELECT COUNT(*) FROM product_lot WHERE lot_id = _lotId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El lote que intentas actualizar no existe.', MYSQL_ERRNO = 106;
    END IF;

    UPDATE product_lot
    SET quantity = _quantity,
        measure_unit = _measureUnit,
        expiration_date = _expirationDate,
        supplier_reference = _supplierReference
    WHERE lot_id = _lotId;
END;;

-- 2.6 DELETE: Eliminar lote (Elimina historial en cascada automáticamente por clave foránea)
CREATE OR REPLACE PROCEDURE `ProductLot_Delete`(
    IN _lotId INT
)
BEGIN
    IF (SELECT COUNT(*) FROM product_lot WHERE lot_id = _lotId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El lote que intentas eliminar no existe.', MYSQL_ERRNO = 106;
    END IF;

    DELETE FROM product_lot WHERE lot_id = _lotId;
END;;


-- =================================================================
-- BLOQUE 3: HISTORIAL Y CONTROL DE TRAZABILIDAD (OCR / AUDITORÍA)
-- =================================================================

-- 3.1 CREATE: Insertar un nuevo evento/movimiento detectado por el OCR
CREATE OR REPLACE PROCEDURE `LotHistory_Insert`(
    IN _lotId INT,
    IN _actionType VARCHAR(50),
    IN _operator VARCHAR(100),
    IN _quantityAffected DECIMAL(12,4),
    IN _ocrRawText TEXT,
    IN _observations TEXT
)
BEGIN
    IF (SELECT COUNT(*) FROM product_lot WHERE lot_id = _lotId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: No se puede registrar historial para un lote inexistente.', MYSQL_ERRNO = 106;
    END IF;

    INSERT INTO lot_history (lot_id, action_type, operator, quantity_affected, ocr_raw_text, observations)
    VALUES (_lotId, _actionType, _operator, _quantityAffected, _ocrRawText, _observations);
END;;

-- 3.2 READ (BY LOT): Consultar toda la línea de tiempo de un lote (desde su origen)
CREATE OR REPLACE PROCEDURE `LotHistory_FindByLotId`(
    IN _lotId INT
)
BEGIN
    SELECT * FROM lot_history WHERE lot_id = _lotId ORDER BY action_timestamp DESC;
END;;

-- 3.3 TRACEABILITY ENGINE: El motor principal del OCR
-- Une los tres modelos en la estructura DTO comprimida que lee la cámara
CREATE OR REPLACE PROCEDURE `ProductLot_GetCurrentStatus`(
    IN _lotNumber VARCHAR(100)
)
BEGIN
    -- Comprobar si el lote existe antes de realizar los cruces de tablas
    IF (SELECT lot_id FROM product_lot WHERE lot_number = _lotNumber) IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El número de lote escaneado no existe en el sistema.', MYSQL_ERRNO = 101;
    END IF;

    SELECT
        p.product_id,
        p.sku AS product_sku,
        p.name AS product_name,
        pl.lot_id,
        pl.lot_number,
        pl.quantity AS initial_quantity,
        pl.measure_unit,
        pl.expiration_date,
        lh.action_type AS current_state,
        lh.action_timestamp AS last_update,
        lh.operator AS last_operator,
        lh.ocr_raw_text,
        lh.observations AS last_observations
    FROM product_lot pl
             JOIN product p ON pl.product_id = p.product_id
             JOIN lot_history lh ON lh.lot_id = pl.lot_id
    WHERE pl.lot_number = _lotNumber
    ORDER BY lh.history_id DESC
    LIMIT 1;
END;;

DELIMITER ;
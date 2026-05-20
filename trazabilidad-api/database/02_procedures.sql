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
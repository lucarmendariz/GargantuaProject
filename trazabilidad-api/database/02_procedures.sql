DELIMITER ;;

-- Nombre actual de un usuario. Los movimientos guardan el nombre de ese momento y el usuario
-- (para su foto): la API pasa el usuario en _operator y el nombre se busca aqui.
CREATE OR REPLACE FUNCTION `Operator_Name`(_username VARCHAR(50)) RETURNS VARCHAR(100)
    READS SQL DATA
BEGIN
    RETURN COALESCE((SELECT name FROM app_user WHERE username = _username), _username);
END;;

-- Cantidad del lote en pedidos activos: lo reservado en los abiertos y lo consumido en los
-- preparados o entregados. Al cerrar o cancelar el pedido deja de contar.
CREATE OR REPLACE FUNCTION `Lot_InUse`(_lotId INT) RETURNS DECIMAL(12,4)
    READS SQL DATA
BEGIN
    RETURN (SELECT COALESCE(SUM(IF(co.status = 'ABIERTO', ol.reserved_quantity, ol.consumed_quantity)), 0)
            FROM order_line ol JOIN customer_order co ON co.order_id = ol.order_id
            WHERE ol.lot_id = _lotId AND co.status IN ('ABIERTO', 'PREPARADO', 'ENTREGADO'));
END;;

-- =================================================================
-- BLOQUE 1: CRUD DE PRODUCTOS (product)
-- =================================================================

-- 1.1 CREATE: Crear un nuevo producto (MODIFICADO CON PARÁMETRO OUT)
CREATE OR REPLACE PROCEDURE `Product_Create`(
    IN _sku VARCHAR(50),
    IN _name VARCHAR(100),
    IN _description TEXT,
    IN _gtin VARCHAR(14), -- Codigo de barras comercial (EAN/GTIN), opcional
    OUT _newProductId INT
)
BEGIN
    IF (SELECT COUNT(*) FROM product WHERE sku = _sku) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Ya existe un producto con ese SKU.', MYSQL_ERRNO = 102;
    END IF;
    IF _gtin IS NOT NULL AND (SELECT COUNT(*) FROM product WHERE gtin = _gtin) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Ese codigo de barras ya es de otro producto.', MYSQL_ERRNO = 107;
    END IF;

    INSERT INTO product (sku, gtin, name, description, is_active)
    VALUES (_sku, _gtin, _name, _description, 1);

    SET _newProductId = LAST_INSERT_ID();
END;;

-- 1.2 READ (ALL): Listar todo el catálogo para administración
CREATE OR REPLACE PROCEDURE `Product_GetAll`()
BEGIN
    SELECT product_id, sku, gtin, name, description, is_active, UNIX_TIMESTAMP(created_at) AS created_at, UNIX_TIMESTAMP(updated_at) AS updated_at FROM product ORDER BY name ASC;
END;;

-- 1.3 READ (ACTIVE): Listar solo productos activos (para la app de día a día)
CREATE OR REPLACE PROCEDURE `Product_FindAll`()
BEGIN
    SELECT product_id, sku, gtin, name, description, is_active, UNIX_TIMESTAMP(created_at) AS created_at, UNIX_TIMESTAMP(updated_at) AS updated_at FROM product WHERE is_active = 1 ORDER BY name ASC;
END;;

-- 1.4 READ (BY ID): Buscar un producto específico
CREATE OR REPLACE PROCEDURE `Product_FindById`(
    IN _productId INT
)
BEGIN
    SELECT product_id, sku, gtin, name, description, is_active, UNIX_TIMESTAMP(created_at) AS created_at, UNIX_TIMESTAMP(updated_at) AS updated_at FROM product WHERE product_id = _productId;
END;;

-- 1.5 UPDATE: Modificar datos del producto
CREATE OR REPLACE PROCEDURE `Product_Update`(
    IN _productId INT,
    IN _sku VARCHAR(50),
    IN _name VARCHAR(100),
    IN _description TEXT,
    IN _isActive TINYINT(1),
    IN _gtin VARCHAR(14)
)
BEGIN
    IF (SELECT COUNT(*) FROM product WHERE product_id = _productId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El producto no existe.', MYSQL_ERRNO = 103;
    END IF;
    IF _gtin IS NOT NULL AND (SELECT COUNT(*) FROM product WHERE gtin = _gtin AND product_id <> _productId) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: Ese codigo de barras ya es de otro producto.', MYSQL_ERRNO = 107;
    END IF;

    UPDATE product
    SET sku = _sku, gtin = _gtin, name = _name, description = _description, is_active = _isActive
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

-- 2.1 CREATE: Registrar la entrada de un nuevo lote físico (MODIFICADO CON PARÁMETRO OUT)
CREATE OR REPLACE PROCEDURE `ProductLot_Create`(
    IN _productId INT,
    IN _lotNumber VARCHAR(100),
    IN _quantity DECIMAL(12,4),
    IN _measureUnit VARCHAR(20),
    IN _expirationDate BIGINT,
    IN _supplierReference VARCHAR(100),
    OUT _newLotId INT
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

    SET _newLotId = LAST_INSERT_ID();
END;;

-- 2.2 READ (ALL): Ver todos los lotes en el sistema
CREATE OR REPLACE PROCEDURE `ProductLot_GetAll`()
BEGIN
    SELECT lot_id, product_id, lot_number, quantity, reserved_quantity, consumed_quantity, quantity - consumed_quantity AS current_stock, quantity - consumed_quantity - reserved_quantity AS available_quantity, Lot_InUse(lot_id) AS in_use_quantity, measure_unit, expiration_date, supplier_reference, UNIX_TIMESTAMP(created_at) AS created_at, UNIX_TIMESTAMP(updated_at) AS updated_at FROM product_lot ORDER BY created_at DESC;
END;;

-- 2.3 READ (BY PRODUCT ID): Ver los lotes de un producto específico
CREATE OR REPLACE PROCEDURE `ProductLot_FindByProductId`(
    IN _productId INT
)
BEGIN
    SELECT lot_id, product_id, lot_number, quantity, reserved_quantity, consumed_quantity, quantity - consumed_quantity AS current_stock, quantity - consumed_quantity - reserved_quantity AS available_quantity, Lot_InUse(lot_id) AS in_use_quantity, measure_unit, expiration_date, supplier_reference, UNIX_TIMESTAMP(created_at) AS created_at, UNIX_TIMESTAMP(updated_at) AS updated_at FROM product_lot WHERE product_id = _productId ORDER BY expiration_date ASC;
END;;

-- 2.4 READ (BY ID): Buscar un lote por su ID interno
CREATE OR REPLACE PROCEDURE `ProductLot_FindById`(
    IN _lotId INT
)
BEGIN
    SELECT lot_id, product_id, lot_number, quantity, reserved_quantity, consumed_quantity, quantity - consumed_quantity AS current_stock, quantity - consumed_quantity - reserved_quantity AS available_quantity, Lot_InUse(lot_id) AS in_use_quantity, measure_unit, expiration_date, supplier_reference, UNIX_TIMESTAMP(created_at) AS created_at, UNIX_TIMESTAMP(updated_at) AS updated_at FROM product_lot WHERE lot_id = _lotId;
END;;

-- 2.5 UPDATE: Modificar stock o datos de un lote
CREATE OR REPLACE PROCEDURE `ProductLot_Update`(
    IN _lotId INT,
    IN _quantity DECIMAL(12,4),
    IN _measureUnit VARCHAR(20),
    IN _expirationDate BIGINT,
    IN _supplierReference VARCHAR(100)
)
BEGIN
    DECLARE v_used DECIMAL(12,4) DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_used = NULL;
    -- Si algo falla, no queda nada a medias y se libera el bloqueo
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- La fila queda bloqueada hasta el COMMIT: una reserva a la vez no puede dejar el disponible en negativo
    START TRANSACTION;
    SELECT consumed_quantity + reserved_quantity INTO v_used FROM product_lot WHERE lot_id = _lotId FOR UPDATE;

    IF v_used IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El lote que intentas actualizar no existe.', MYSQL_ERRNO = 106;
    END IF;
    IF v_used > _quantity THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: La cantidad no puede ser menor que lo ya reservado o consumido.', MYSQL_ERRNO = 122;
    END IF;

    UPDATE product_lot
    SET quantity = _quantity,
        measure_unit = _measureUnit,
        expiration_date = _expirationDate,
        supplier_reference = _supplierReference
    WHERE lot_id = _lotId;
    COMMIT;
END;;

-- 2.6 DELETE: Eliminar lote (Elimina historial en cascada automáticamente por clave foránea)
CREATE OR REPLACE PROCEDURE `ProductLot_Delete`(
    IN _lotId INT
)
BEGIN
    IF (SELECT COUNT(*) FROM product_lot WHERE lot_id = _lotId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El lote que intentas eliminar no existe.', MYSQL_ERRNO = 106;
    END IF;

    IF (SELECT COUNT(*) FROM order_line WHERE lot_id = _lotId) > 0 OR (SELECT COUNT(*) FROM order_event WHERE lot_id = _lotId) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Error: El lote se ha usado en pedidos y debe conservarse por trazabilidad.', MYSQL_ERRNO = 121;
    END IF;

    DELETE FROM product_lot WHERE lot_id = _lotId;
END;;


-- =================================================================
-- BLOQUE 3: HISTORIAL Y CONTROL DE TRAZABILIDAD (OCR / AUDITORÍA)
-- =================================================================

-- 3.1 CREATE: Insertar un nuevo evento/movimiento detectado por el OCR (MODIFICADO CON PARÁMETRO OUT)
CREATE OR REPLACE PROCEDURE `LotHistory_Insert`(
    IN _lotId INT,
    IN _actionType VARCHAR(50),
    IN _operator VARCHAR(100),
    IN _quantityAffected DECIMAL(12,4),
    IN _ocrRawText TEXT,
    IN _observations TEXT,
    OUT _historyId INT
)
BEGIN
    DECLARE v_lot INT DEFAULT NULL;
    DECLARE v_state VARCHAR(50) DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_state = NULL;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- El lote queda bloqueado hasta el COMMIT: un bloqueo y otro movimiento a la vez no se cruzan
    START TRANSACTION;
    SELECT lot_id INTO v_lot FROM product_lot WHERE lot_id = _lotId FOR UPDATE;
    IF v_lot IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No existe ningún lote con ese ID.', MYSQL_ERRNO = 106;
    END IF;
    -- VENDIDO ya no se registra: las ventas sin lote no descuentan materia prima y las de lotes van por pedidos
    IF _actionType IS NULL OR _actionType NOT IN ('RECEPCIONADO', 'EN_PRODUCCION', 'BLOQUEADO', 'DESBLOQUEADO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Movimiento no válido.', MYSQL_ERRNO = 125;
    END IF;
    IF _quantityAffected < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cantidad no puede ser negativa.', MYSQL_ERRNO = 125;
    END IF;

    -- Estado del lote: su ultimo movimiento manual (los de pedidos no cuentan), como en Lot_AssertUsable
    SELECT action_type INTO v_state FROM lot_history
    WHERE lot_id = _lotId AND order_id IS NULL
    ORDER BY history_id DESC LIMIT 1;

    -- Un bloqueo de calidad solo se levanta desbloqueando a proposito, no con cualquier movimiento
    IF v_state = 'BLOQUEADO' AND _actionType NOT IN ('BLOQUEADO', 'DESBLOQUEADO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El lote está bloqueado por calidad: hay que desbloquearlo antes de registrar otro movimiento.', MYSQL_ERRNO = 112;
    END IF;
    IF _actionType = 'DESBLOQUEADO' AND (v_state IS NULL OR v_state <> 'BLOQUEADO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El lote no está bloqueado.', MYSQL_ERRNO = 125;
    END IF;

    -- Usar en produccion (cocina, venta sin lote) gasta materia prima: se descuenta del stock como un consumo.
    -- No puede tocar lo reservado para pedidos: la comprobacion y el UPDATE son una sola sentencia.
    IF _actionType = 'EN_PRODUCCION' THEN
        IF _quantityAffected IS NULL OR _quantityAffected <= 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Indica la cantidad usada en producción.', MYSQL_ERRNO = 125;
        END IF;
        UPDATE product_lot
        SET consumed_quantity = consumed_quantity + _quantityAffected
        WHERE lot_id = _lotId AND quantity - consumed_quantity - reserved_quantity >= _quantityAffected;
        IF ROW_COUNT() = 0 THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No hay stock disponible suficiente en el lote (lo reservado para pedidos no se puede usar).', MYSQL_ERRNO = 116;
        END IF;
    END IF;

    INSERT INTO lot_history (lot_id, action_type, operator, operator_username, quantity_affected, ocr_raw_text, observations)
    VALUES (_lotId, _actionType, Operator_Name(_operator), _operator, _quantityAffected, _ocrRawText, _observations);

    SET _historyId = LAST_INSERT_ID();
    COMMIT;
END;;

-- 3.2 READ (BY LOT): Consultar toda la línea de tiempo de un lote (desde su origen)
CREATE OR REPLACE PROCEDURE `LotHistory_FindByLotId`(
    IN _lotId INT
)
BEGIN
    SELECT lh.history_id, lh.lot_id, lh.action_type, lh.operator, 
           -- Usuario para la foto; en movimientos antiguos (sin usuario) se busca por el nombre
           COALESCE(lh.operator_username, (SELECT u.username FROM app_user u WHERE u.name = lh.operator LIMIT 1)) AS operator_username,
           UNIX_TIMESTAMP(lh.action_timestamp) AS action_timestamp, lh.quantity_affected, lh.ocr_raw_text, lh.observations, co.order_number
    FROM lot_history lh
             LEFT JOIN customer_order co ON co.order_id = lh.order_id
    WHERE lh.lot_id = _lotId
    ORDER BY lh.action_timestamp DESC, lh.history_id DESC;
END;;

-- 3.3 TRACEABILITY ENGINE: El motor principal del OCR
CREATE OR REPLACE PROCEDURE `ProductLot_GetCurrentStatus`(
    IN _lotNumber VARCHAR(100)
)
BEGIN
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
        pl.reserved_quantity,
        pl.consumed_quantity,
        pl.quantity - pl.consumed_quantity AS current_stock,
        pl.quantity - pl.consumed_quantity - pl.reserved_quantity AS available_quantity,
        pl.measure_unit,
        pl.expiration_date,
        lh.action_type AS current_state,
        UNIX_TIMESTAMP(lh.action_timestamp) AS last_update,
        lh.operator AS last_operator,
        lh.ocr_raw_text,
        lh.observations AS last_observations
    FROM product_lot pl
             JOIN product p ON pl.product_id = p.product_id
             -- El estado es el ultimo movimiento manual: reservar o consumir no borra un BLOQUEADO
             LEFT JOIN lot_history lh ON lh.history_id = (
                 SELECT MAX(history_id) FROM lot_history WHERE lot_id = pl.lot_id AND order_id IS NULL
             )
    WHERE pl.lot_number = _lotNumber
    LIMIT 1;
END;;

DELIMITER ;

DELIMITER ;;

-- Buscar un usuario por su username para validar el login
CREATE OR REPLACE PROCEDURE `AppUser_FindByUsername`(
    IN _username VARCHAR(50)
)
BEGIN
    SELECT * FROM app_user WHERE username = _username AND is_active = 1;
END;;

-- =================================================================
-- BLOQUE 5: MODULOS CONTRATADOS, PERMISOS Y USUARIOS
-- =================================================================

-- Lo que puede hacer un usuario activo en cada modulo (vacio si no existe o esta desactivado).
-- Modulo apagado: NINGUNO para todos. ADMIN y SUPERADMIN: GESTIONAR en los activos.
CREATE OR REPLACE PROCEDURE `Access_GetForUser`(IN _username VARCHAR(50))
BEGIN
    SELECT u.role, m.module_code, m.name,
           CASE WHEN m.is_enabled = 0 THEN 'NINGUNO'
                WHEN u.role IN ('ADMIN', 'SUPERADMIN') THEN 'GESTIONAR'
                ELSE COALESCE(rp.access, 'NINGUNO') END AS access
    FROM app_user u
             JOIN app_module m
             LEFT JOIN role_permission rp ON rp.role = u.role AND rp.module_code = m.module_code
    WHERE u.username = _username AND u.is_active = 1
    ORDER BY m.sort_order;
END;;

CREATE OR REPLACE PROCEDURE `Module_GetAll`()
BEGIN
    SELECT module_code, name, is_enabled FROM app_module ORDER BY sort_order;
END;;

CREATE OR REPLACE PROCEDURE `Module_SetEnabled`(IN _moduleCode VARCHAR(30), IN _enabled TINYINT(1))
BEGIN
    IF (SELECT COUNT(*) FROM app_module WHERE module_code = _moduleCode) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El módulo no existe.', MYSQL_ERRNO = 130;
    END IF;
    UPDATE app_module SET is_enabled = _enabled WHERE module_code = _moduleCode;
END;;

CREATE OR REPLACE PROCEDURE `RolePermission_GetAll`()
BEGIN
    SELECT rp.role, rp.module_code, rp.access
    FROM role_permission rp JOIN app_module m ON m.module_code = rp.module_code
    ORDER BY rp.role, m.sort_order;
END;;

-- Solo se configuran ENCARGADO y OPERARIO: ADMIN y SUPERADMIN siempre gestionan todo
CREATE OR REPLACE PROCEDURE `RolePermission_Set`(IN _role VARCHAR(50), IN _moduleCode VARCHAR(30), IN _access VARCHAR(20))
BEGIN
    IF _role NOT IN ('ENCARGADO', 'OPERARIO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se configuran los permisos de ENCARGADO y OPERARIO.', MYSQL_ERRNO = 131;
    END IF;
    IF _access NOT IN ('NINGUNO', 'VER', 'GESTIONAR') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El permiso debe ser NINGUNO, VER o GESTIONAR.', MYSQL_ERRNO = 131;
    END IF;
    IF (SELECT COUNT(*) FROM app_module WHERE module_code = _moduleCode) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El módulo no existe.', MYSQL_ERRNO = 130;
    END IF;
    INSERT INTO role_permission (role, module_code, access) VALUES (_role, _moduleCode, _access)
    ON DUPLICATE KEY UPDATE access = _access;
END;;

-- Usuarios del cliente (sin la contraseña). El SUPERADMIN no aparece: no es del cliente.
CREATE OR REPLACE PROCEDURE `AppUser_GetAll`()
BEGIN
    SELECT user_id, username, name, role, is_active, UNIX_TIMESTAMP(created_at) AS created_at
    FROM app_user WHERE role <> 'SUPERADMIN' ORDER BY is_active DESC, name;
END;;

CREATE OR REPLACE PROCEDURE `AppUser_Create`(
    IN _username VARCHAR(50),
    IN _passwordHash VARCHAR(255),
    IN _name VARCHAR(100),
    IN _role VARCHAR(50),
    OUT _newUserId INT
)
BEGIN
    IF _role NOT IN ('ADMIN', 'ENCARGADO', 'OPERARIO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El rol debe ser ADMIN, ENCARGADO u OPERARIO.', MYSQL_ERRNO = 131;
    END IF;
    IF (SELECT COUNT(*) FROM app_user WHERE username = _username) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un usuario con ese nombre de usuario.', MYSQL_ERRNO = 132;
    END IF;
    INSERT INTO app_user (username, password_hash, name, role) VALUES (_username, _passwordHash, _name, _role);
    SET _newUserId = LAST_INSERT_ID();
END;;

-- Foto de cualquier usuario (tambien desactivado: sus movimientos antiguos la siguen mostrando)
CREATE OR REPLACE PROCEDURE `AppUser_GetAvatar`(IN _username VARCHAR(50))
BEGIN
    SELECT avatar FROM app_user WHERE username = _username;
END;;

-- Perfil del propio usuario (sin la contraseña)
CREATE OR REPLACE PROCEDURE `AppUser_GetProfile`(IN _username VARCHAR(50))
BEGIN
    SELECT username, name, role, avatar, UNIX_TIMESTAMP(created_at) AS created_at
    FROM app_user WHERE username = _username AND is_active = 1;
END;;

-- El propio usuario cambia su nombre y su foto (la API ya los ha validado; foto NULL = sin foto)
CREATE OR REPLACE PROCEDURE `AppUser_UpdateProfile`(IN _username VARCHAR(50), IN _name VARCHAR(100), IN _avatar MEDIUMTEXT)
BEGIN
    UPDATE app_user SET name = _name, avatar = _avatar WHERE username = _username AND is_active = 1;
END;;

-- Cambia la contraseña del propio usuario (la API ya ha comprobado la actual)
CREATE OR REPLACE PROCEDURE `AppUser_SetPassword`(IN _username VARCHAR(50), IN _passwordHash VARCHAR(255))
BEGIN
    UPDATE app_user SET password_hash = _passwordHash WHERE username = _username AND is_active = 1;
END;;

-- Cambia rol y estado. Los usuarios SUPERADMIN no se tocan desde el panel del cliente.
CREATE OR REPLACE PROCEDURE `AppUser_Update`(IN _userId INT, IN _role VARCHAR(50), IN _active TINYINT(1))
BEGIN
    IF (SELECT COUNT(*) FROM app_user WHERE user_id = _userId AND role <> 'SUPERADMIN') = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El usuario no existe.', MYSQL_ERRNO = 133;
    END IF;
    IF _role NOT IN ('ADMIN', 'ENCARGADO', 'OPERARIO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El rol debe ser ADMIN, ENCARGADO u OPERARIO.', MYSQL_ERRNO = 131;
    END IF;
    UPDATE app_user SET role = _role, is_active = _active WHERE user_id = _userId;
END;;

DELIMITER ;

DELIMITER ;;

-- =================================================================
-- BLOQUE 4: PEDIDOS
-- Reservar baja el disponible; el stock solo baja al cerrar el pedido.
-- =================================================================

-- Bloquea el pedido durante la transaccion y comprueba que siga abierto
CREATE OR REPLACE PROCEDURE `Order_LockOpen`(IN _orderId INT)
BEGIN
    DECLARE v_status VARCHAR(20) DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_status = NULL;

    SELECT status INTO v_status FROM customer_order WHERE order_id = _orderId FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido no existe.', MYSQL_ERRNO = 113;
    END IF;
    IF v_status <> 'ABIERTO' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido ya no esta abierto.', MYSQL_ERRNO = 111;
    END IF;
END;;

-- Un lote BLOQUEADO (por ejemplo, por calidad) no se puede usar
CREATE OR REPLACE PROCEDURE `Lot_AssertUsable`(IN _lotId INT)
BEGIN
    DECLARE v_state VARCHAR(50) DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_state = NULL;

    SELECT action_type INTO v_state FROM lot_history
    WHERE lot_id = _lotId AND order_id IS NULL
    ORDER BY history_id DESC LIMIT 1;

    IF v_state = 'BLOQUEADO' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El lote esta bloqueado y no se puede usar.', MYSQL_ERRNO = 112;
    END IF;
END;;

-- Clientes: se eligen de esta lista al crear un pedido
CREATE OR REPLACE PROCEDURE `Customer_GetAll`()
BEGIN
    SELECT c.customer_id, c.name, UNIX_TIMESTAMP(c.created_at) AS created_at,
           (SELECT COUNT(*) FROM customer_order co WHERE co.customer_id = c.customer_id) AS order_count
    FROM customer c
    ORDER BY c.name;
END;;

CREATE OR REPLACE PROCEDURE `Customer_Create`(
    IN _name VARCHAR(150),
    OUT _customerId INT
)
BEGIN
    IF (SELECT COUNT(*) FROM customer WHERE name = _name) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un cliente con ese nombre.', MYSQL_ERRNO = 126;
    END IF;

    INSERT INTO customer (name) VALUES (_name);
    SET _customerId = LAST_INSERT_ID();
END;;

CREATE OR REPLACE PROCEDURE `Order_Create`(
    IN _orderNumber VARCHAR(100),
    IN _customerId INT,
    IN _scheduledDate BIGINT,
    IN _notes TEXT,
    IN _operator VARCHAR(100),
    OUT _orderId INT
)
BEGIN
    IF (SELECT COUNT(*) FROM customer_order WHERE order_number = _orderNumber) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Ya existe un pedido con ese numero.', MYSQL_ERRNO = 110;
    END IF;
    IF _customerId IS NOT NULL AND (SELECT COUNT(*) FROM customer WHERE customer_id = _customerId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El cliente no existe.', MYSQL_ERRNO = 127;
    END IF;

    INSERT INTO customer_order (order_number, customer_id, scheduled_date, notes, created_by)
    VALUES (_orderNumber, _customerId, _scheduledDate, _notes, Operator_Name(_operator));
    SET _orderId = LAST_INSERT_ID();

    INSERT INTO order_event (order_id, event_type, operator, operator_username, notes)
    VALUES (_orderId, 'CREADO', Operator_Name(_operator), _operator, _notes);
END;;

-- Todos los filtros son opcionales. _status admite varios estados separados por comas
-- (ABIERTO,PREPARADO). El rango de fechas incluye _from y excluye _to.
CREATE OR REPLACE PROCEDURE `Order_GetAll`(
    IN _status VARCHAR(100),
    IN _from BIGINT,
    IN _to BIGINT
)
BEGIN
    SELECT co.order_id, co.order_number, cu.name AS customer_name, co.scheduled_date, co.status, co.notes,
           co.created_by, co.closed_by, UNIX_TIMESTAMP(co.created_at) AS created_at, UNIX_TIMESTAMP(co.closed_at) AS closed_at,
           co.prepared_by, UNIX_TIMESTAMP(co.prepared_at) AS prepared_at,
           co.delivered_by, UNIX_TIMESTAMP(co.delivered_at) AS delivered_at,
           (SELECT COUNT(*) FROM order_line ol WHERE ol.order_id = co.order_id) AS line_count
    FROM customer_order co
             LEFT JOIN customer cu ON cu.customer_id = co.customer_id
    WHERE (_status IS NULL OR FIND_IN_SET(co.status, _status) > 0)
      AND (_from IS NULL OR co.scheduled_date >= _from)
      AND (_to IS NULL OR co.scheduled_date < _to)
    ORDER BY co.scheduled_date DESC, co.order_id DESC;
END;;

CREATE OR REPLACE PROCEDURE `Order_FindById`(IN _orderId INT)
BEGIN
    SELECT co.order_id, co.order_number, cu.name AS customer_name, co.scheduled_date, co.status, co.notes,
           co.created_by, co.closed_by, UNIX_TIMESTAMP(co.created_at) AS created_at, UNIX_TIMESTAMP(co.closed_at) AS closed_at,
           co.prepared_by, UNIX_TIMESTAMP(co.prepared_at) AS prepared_at,
           co.delivered_by, UNIX_TIMESTAMP(co.delivered_at) AS delivered_at,
           (SELECT COUNT(*) FROM order_line ol WHERE ol.order_id = co.order_id) AS line_count
    FROM customer_order co
             LEFT JOIN customer cu ON cu.customer_id = co.customer_id
    WHERE co.order_id = _orderId;
END;;

CREATE OR REPLACE PROCEDURE `OrderLine_FindByOrder`(IN _orderId INT)
BEGIN
    SELECT ol.line_id, ol.order_id, ol.lot_id, pl.lot_number, p.name AS product_name, p.sku AS product_sku,
           pl.measure_unit, ol.reserved_quantity, ol.consumed_quantity
    FROM order_line ol
             JOIN product_lot pl ON pl.lot_id = ol.lot_id
             JOIN product p ON p.product_id = pl.product_id
    WHERE ol.order_id = _orderId
    ORDER BY ol.line_id;
END;;

CREATE OR REPLACE PROCEDURE `OrderEvent_FindByOrder`(IN _orderId INT)
BEGIN
    SELECT oe.event_id, oe.order_id, oe.lot_id, pl.lot_number, pl.measure_unit, oe.event_type, oe.quantity, oe.operator, COALESCE(oe.operator_username, (SELECT u.username FROM app_user u WHERE u.name = oe.operator LIMIT 1)) AS operator_username,
           oe.notes,
           UNIX_TIMESTAMP(oe.event_timestamp) AS event_timestamp
    FROM order_event oe
             LEFT JOIN product_lot pl ON pl.lot_id = oe.lot_id
    WHERE oe.order_id = _orderId
    ORDER BY oe.event_id DESC;
END;;

-- Reserva: la comprobacion y el UPDATE son una sola sentencia para evitar reservar de mas
CREATE OR REPLACE PROCEDURE `OrderLine_Add`(
    IN _orderId INT,
    IN _lotId INT,
    IN _quantity DECIMAL(12,4),
    IN _operator VARCHAR(100),
    OUT _lineId INT
)
BEGIN
    DECLARE v_lot INT DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_lot = NULL;

    CALL Order_LockOpen(_orderId);

    IF _quantity IS NULL OR _quantity <= 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cantidad debe ser mayor que cero.', MYSQL_ERRNO = 114;
    END IF;

    -- Se bloquea el lote desde el principio para que dos reservas no se interbloqueen
    SELECT lot_id INTO v_lot FROM product_lot WHERE lot_id = _lotId FOR UPDATE;
    IF v_lot IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El lote no existe.', MYSQL_ERRNO = 106;
    END IF;
    IF (SELECT COUNT(*) FROM order_line WHERE order_id = _orderId AND lot_id = _lotId) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El lote ya esta en este pedido.', MYSQL_ERRNO = 115;
    END IF;

    CALL Lot_AssertUsable(_lotId);

    UPDATE product_lot
    SET reserved_quantity = reserved_quantity + _quantity
    WHERE lot_id = _lotId AND quantity - consumed_quantity - reserved_quantity >= _quantity;

    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No hay stock disponible suficiente en el lote.', MYSQL_ERRNO = 116;
    END IF;

    INSERT INTO order_line (order_id, lot_id, reserved_quantity) VALUES (_orderId, _lotId, _quantity);
    SET _lineId = LAST_INSERT_ID();

    INSERT INTO order_event (order_id, lot_id, event_type, quantity, operator, operator_username)
    VALUES (_orderId, _lotId, 'RESERVADO', _quantity, Operator_Name(_operator), _operator);
    INSERT INTO lot_history (lot_id, action_type, operator, operator_username, quantity_affected, order_id)
    VALUES (_lotId, 'RESERVADO', Operator_Name(_operator), _operator, _quantity, _orderId);
END;;

CREATE OR REPLACE PROCEDURE `OrderLine_Remove`(
    IN _orderId INT,
    IN _lineId INT,
    IN _operator VARCHAR(100)
)
BEGIN
    DECLARE v_lot INT DEFAULT NULL;
    DECLARE v_reserved DECIMAL(12,4);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_lot = NULL;

    CALL Order_LockOpen(_orderId);

    SELECT lot_id, reserved_quantity INTO v_lot, v_reserved
    FROM order_line WHERE line_id = _lineId AND order_id = _orderId FOR UPDATE;

    IF v_lot IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La linea no pertenece a este pedido.', MYSQL_ERRNO = 117;
    END IF;

    UPDATE product_lot SET reserved_quantity = reserved_quantity - v_reserved WHERE lot_id = v_lot;
    DELETE FROM order_line WHERE line_id = _lineId;

    INSERT INTO order_event (order_id, lot_id, event_type, quantity, operator, operator_username, notes)
    VALUES (_orderId, v_lot, 'LIBERADO', v_reserved, Operator_Name(_operator), _operator, 'Lote retirado del pedido');
    INSERT INTO lot_history (lot_id, action_type, operator, operator_username, quantity_affected, observations, order_id)
    VALUES (v_lot, 'LIBERADO', Operator_Name(_operator), _operator, v_reserved, 'Lote retirado del pedido', _orderId);
END;;

-- Libera lo reservado y resta del stock lo realmente usado, que puede ser mas o menos
CREATE OR REPLACE PROCEDURE `OrderLine_Consume`(
    IN _orderId INT,
    IN _lineId INT,
    IN _consumed DECIMAL(12,4),
    IN _operator VARCHAR(100)
)
BEGIN
    DECLARE v_lot INT DEFAULT NULL;
    DECLARE v_reserved DECIMAL(12,4);
    DECLARE v_previous DECIMAL(12,4);
    DECLARE v_note VARCHAR(100);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_lot = NULL;

    CALL Order_LockOpen(_orderId);

    SELECT lot_id, reserved_quantity, consumed_quantity INTO v_lot, v_reserved, v_previous
    FROM order_line WHERE line_id = _lineId AND order_id = _orderId FOR UPDATE;

    IF v_lot IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La linea no pertenece a este pedido.', MYSQL_ERRNO = 117;
    END IF;
    IF v_previous IS NOT NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Esta linea ya tiene su consumo registrado.', MYSQL_ERRNO = 118;
    END IF;
    IF _consumed IS NULL OR _consumed < 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La cantidad consumida no puede ser negativa.', MYSQL_ERRNO = 114;
    END IF;
    IF _consumed > 0 THEN
        CALL Lot_AssertUsable(v_lot);
    END IF;

    UPDATE product_lot
    SET reserved_quantity = reserved_quantity - v_reserved,
        consumed_quantity = consumed_quantity + _consumed
    WHERE lot_id = v_lot AND quantity - consumed_quantity - reserved_quantity + v_reserved >= _consumed;

    IF ROW_COUNT() = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No hay stock disponible suficiente en el lote.', MYSQL_ERRNO = 116;
    END IF;

    UPDATE order_line SET consumed_quantity = _consumed WHERE line_id = _lineId;

    SET v_note = CONCAT('Reservado: ', v_reserved + 0);
    INSERT INTO order_event (order_id, lot_id, event_type, quantity, operator, operator_username, notes)
    VALUES (_orderId, v_lot, 'CONSUMIDO', _consumed, Operator_Name(_operator), _operator, v_note);
    INSERT INTO lot_history (lot_id, action_type, operator, operator_username, quantity_affected, observations, order_id)
    VALUES (v_lot, 'CONSUMIDO', Operator_Name(_operator), _operator, _consumed, v_note, _orderId);
END;;

-- Ciclo de un pedido: ABIERTO -> PREPARADO -> ENTREGADO -> CERRADO (automatico).
-- Desde ABIERTO tambien se puede CANCELAR.

-- Antes el consumo cerraba el pedido; ahora lo deja PREPARADO
DROP PROCEDURE IF EXISTS `Order_Close`;;

-- Preparar: con el consumo real de todas las lineas ya registrado
CREATE OR REPLACE PROCEDURE `Order_Prepare`(
    IN _orderId INT,
    IN _operator VARCHAR(100)
)
BEGIN
    CALL Order_LockOpen(_orderId);

    IF (SELECT COUNT(*) FROM order_line WHERE order_id = _orderId) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido no tiene lotes asignados.', MYSQL_ERRNO = 120;
    END IF;
    IF (SELECT COUNT(*) FROM order_line WHERE order_id = _orderId AND consumed_quantity IS NULL) > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Falta indicar el consumo real de alguna linea.', MYSQL_ERRNO = 119;
    END IF;

    UPDATE customer_order SET status = 'PREPARADO', prepared_by = Operator_Name(_operator), prepared_at = NOW() WHERE order_id = _orderId;

    INSERT INTO order_event (order_id, event_type, operator, operator_username) VALUES (_orderId, 'PREPARADO', Operator_Name(_operator), _operator);
END;;

-- Entregar: solo un pedido preparado
CREATE OR REPLACE PROCEDURE `Order_Deliver`(
    IN _orderId INT,
    IN _operator VARCHAR(100)
)
BEGIN
    DECLARE v_status VARCHAR(20) DEFAULT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_status = NULL;

    SELECT status INTO v_status FROM customer_order WHERE order_id = _orderId FOR UPDATE;

    IF v_status IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'El pedido no existe.', MYSQL_ERRNO = 113;
    END IF;
    IF v_status <> 'PREPARADO' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Solo se puede entregar un pedido preparado.', MYSQL_ERRNO = 123;
    END IF;

    UPDATE customer_order SET status = 'ENTREGADO', delivered_by = Operator_Name(_operator), delivered_at = NOW() WHERE order_id = _orderId;

    INSERT INTO order_event (order_id, event_type, operator, operator_username) VALUES (_orderId, 'ENTREGADO', Operator_Name(_operator), _operator);
END;;

-- Cierre automatico: archiva los pedidos entregados hace al menos _hours horas
CREATE OR REPLACE PROCEDURE `Order_CloseDelivered`(
    IN _hours INT,
    OUT _closed INT
)
BEGIN
    DECLARE v_now TIMESTAMP DEFAULT NOW();

    UPDATE customer_order
    SET status = 'CERRADO', closed_by = 'Sistema', closed_at = v_now
    WHERE status = 'ENTREGADO' AND delivered_at <= v_now - INTERVAL _hours HOUR;
    SET _closed = ROW_COUNT();

    -- El cierre tambien queda en el historial de cada lote del pedido
    INSERT INTO lot_history (lot_id, action_type, operator, quantity_affected, observations, order_id)
    SELECT ol.lot_id, 'CERRADO', 'Sistema', ol.consumed_quantity, 'Pedido cerrado', ol.order_id
    FROM order_line ol JOIN customer_order co ON co.order_id = ol.order_id
    WHERE co.status = 'CERRADO' AND co.closed_by = 'Sistema' AND co.closed_at = v_now;

    INSERT INTO order_event (order_id, event_type, operator, notes)
    SELECT order_id, 'CERRADO', 'Sistema', CONCAT('Cierre automatico ', _hours, ' h despues de la entrega')
    FROM customer_order
    WHERE status = 'CERRADO' AND closed_by = 'Sistema' AND closed_at = v_now;
END;;

CREATE OR REPLACE PROCEDURE `Order_Cancel`(
    IN _orderId INT,
    IN _operator VARCHAR(100)
)
BEGIN
    CALL Order_LockOpen(_orderId);

    UPDATE product_lot pl
        JOIN order_line ol ON ol.lot_id = pl.lot_id
    SET pl.reserved_quantity = pl.reserved_quantity - ol.reserved_quantity
    WHERE ol.order_id = _orderId;

    INSERT INTO order_event (order_id, lot_id, event_type, quantity, operator, operator_username, notes)
    SELECT _orderId, lot_id, 'LIBERADO', reserved_quantity, Operator_Name(_operator), _operator, 'Pedido cancelado'
    FROM order_line WHERE order_id = _orderId;

    INSERT INTO lot_history (lot_id, action_type, operator, operator_username, quantity_affected, observations, order_id)
    SELECT lot_id, 'LIBERADO', Operator_Name(_operator), _operator, reserved_quantity, 'Pedido cancelado', _orderId
    FROM order_line WHERE order_id = _orderId;

    UPDATE customer_order SET status = 'CANCELADO', closed_by = Operator_Name(_operator), closed_at = NOW() WHERE order_id = _orderId;

    INSERT INTO order_event (order_id, event_type, operator, operator_username) VALUES (_orderId, 'CANCELADO', Operator_Name(_operator), _operator);
END;;

-- Lotes en uso: reservados en pedidos abiertos, opcionalmente de un rango de dias
CREATE OR REPLACE PROCEDURE `OrderLine_GetInUse`(
    IN _from BIGINT,
    IN _to BIGINT
)
BEGIN
    SELECT ol.line_id, ol.order_id, co.order_number, cu.name AS customer_name, ol.lot_id, pl.lot_number, p.name AS product_name,
           p.sku AS product_sku, pl.measure_unit, ol.reserved_quantity,
           pl.quantity - pl.consumed_quantity AS current_stock,
           pl.quantity - pl.consumed_quantity - pl.reserved_quantity AS available_quantity,
           pl.expiration_date,
           UNIX_TIMESTAMP(ol.created_at) AS reserved_at
    FROM order_line ol
             JOIN customer_order co ON co.order_id = ol.order_id
             LEFT JOIN customer cu ON cu.customer_id = co.customer_id
             JOIN product_lot pl ON pl.lot_id = ol.lot_id
             JOIN product p ON p.product_id = pl.product_id
    WHERE co.status = 'ABIERTO'
      AND (_from IS NULL OR co.scheduled_date >= _from)
      AND (_to IS NULL OR co.scheduled_date < _to)
    ORDER BY p.name, pl.lot_number, co.order_number;
END;;

-- Pedidos en los que se ha usado un lote
CREATE OR REPLACE PROCEDURE `Lot_GetOrders`(IN _lotId INT)
BEGIN
    SELECT co.order_id, co.order_number, cu.name AS customer_name, co.status AS order_status, co.scheduled_date,
           ol.reserved_quantity, ol.consumed_quantity
    FROM order_line ol
             JOIN customer_order co ON co.order_id = ol.order_id
             LEFT JOIN customer cu ON cu.customer_id = co.customer_id
    WHERE ol.lot_id = _lotId
    ORDER BY co.scheduled_date DESC, co.order_id DESC;
END;;

DELIMITER ;

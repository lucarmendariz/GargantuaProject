-- =================================================================
-- 04. DATOS DE PRUEBA PARA LA DEMO
-- =================================================================
-- Rellena todas las pantallas: usuarios de cada rol, productos (uno inactivo),
-- lotes caducados, a punto de caducar y bloqueados, historial de planta y
-- pedidos en todos los estados: abiertos, preparados, entregados, cerrados
-- (automaticamente) y cancelados.
--
-- Las fechas se calculan desde HOY, asi que el Panel Central siempre tiene
-- pedidos del dia. Los pedidos se crean con los mismos procedimientos que usa
-- la API, para que reservas, consumos e historiales cuadren.
--
-- Se ejecuta despues de 01-03 y se puede repetir: cada bloque se carga una sola vez.
-- Si la base ya tenia la demo anterior, solo añade los pedidos de los estados nuevos.
--   docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < 04_demo_data.sql
--
-- Usuarios (contraseña de todos: moncorp2026, solo para desarrollo):
--   luca  ADMIN      (ya viene en 03_seeds.sql)
--   ane   ENCARGADO
--   mikel OPERARIO

DELIMITER ;;

CREATE OR REPLACE PROCEDURE `Demo_Load`()
demo: BEGIN
    -- Mediodia UTC: cae en el mismo dia en la hora de España
    DECLARE v_today BIGINT DEFAULT UNIX_TIMESTAMP(CURDATE()) + 43200;
    DECLARE d BIGINT DEFAULT 86400;

    DECLARE p_hue, p_pat, p_ceb, p_sal, p_ace, p_torc, p_tors, p_caj, p_bac INT;
    DECLARE l_hue_a, l_hue_b, l_hue_old, l_pat_a, l_pat_b, l_ceb_a, l_sal_a, l_ace_a,
            l_torc_a, l_tors_a, l_caj_a, l_bac_old INT;
    DECLARE o1, o2, o3, o4, o5, o6, o7, o8, o9 INT;
    DECLARE ln_a, ln_b, v_closed INT;
    DECLARE v_loaded INT DEFAULT 0;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET p_ace = NULL;
    -- Todo o nada: si algo falla no quedan datos a medias
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- =============================================================
    -- BLOQUE 1: catalogo, lotes, historial y pedidos basicos
    -- =============================================================
    catalogo: BEGIN
    IF (SELECT COUNT(*) FROM product WHERE sku = 'GAR-HUE-001') > 0 THEN
        LEAVE catalogo;
    END IF;
    SET v_loaded = v_loaded + 1;

    -- -------------------------------------------------------------
    -- USUARIOS (hash bcrypt de 'moncorp2026', el mismo que 03_seeds.sql)
    -- -------------------------------------------------------------
    INSERT IGNORE INTO app_user (username, password_hash, name, role, is_active)
    VALUES ('ane', '$2a$10$A8ZBNb2G/0/Y4Lv1Ygw8iejFWQ.M5A3UPrxl5I2clCaCyyvhSL8s6', 'Ane Etxeberria', 'ENCARGADO', 1),
           ('mikel', '$2a$10$A8ZBNb2G/0/Y4Lv1Ygw8iejFWQ.M5A3UPrxl5I2clCaCyyvhSL8s6', 'Mikel Goñi', 'OPERARIO', 1);

    -- -------------------------------------------------------------
    -- PRODUCTOS: materias primas, producto terminado y embalaje
    -- -------------------------------------------------------------
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-HUE-001', 'Huevo campero L', 'Huevo de gallina criada en suelo, categoría A. Alérgeno: huevo', 1);
    SET p_hue = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-PAT-002', 'Patata agria', 'Patata de Álava para freír, calibre 40-70 mm', 1);
    SET p_pat = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-CEB-003', 'Cebolla dulce', 'Cebolla dulce de Zalla pelada y en juliana', 1);
    SET p_ceb = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-SAL-004', 'Sal marina fina', 'Sal marina de Añana en sacos de 25 kg', 1);
    SET p_sal = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-TOR-010', 'Tortilla de patata con cebolla', 'Tortilla de 1 kg, jugosa. Alérgenos: huevo', 1);
    SET p_torc = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-TOR-011', 'Tortilla de patata sin cebolla', 'Tortilla de 1 kg, cuajada. Alérgenos: huevo', 1);
    SET p_tors = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-CAJ-020', 'Caja de cartón para tortilla', 'Caja individual con ventana, apta para uso alimentario', 1);
    SET p_caj = LAST_INSERT_ID();
    INSERT INTO product (sku, name, description, is_active) VALUES
        ('GAR-TOR-099', 'Tortilla de bacalao (descatalogada)', 'Producto retirado del catálogo en 2026', 0);
    SET p_bac = LAST_INSERT_ID();

    -- El aceite ya existe en 03_seeds.sql; si no, se crea
    SELECT product_id INTO p_ace FROM product WHERE sku = 'MON-OIL-003';
    IF p_ace IS NULL THEN
        INSERT INTO product (sku, name, description, is_active) VALUES
            ('MON-OIL-003', 'Aceite de Oliva Virgen Extra', 'Garrafa de 5 litros de extracción en frío', 1);
        SET p_ace = LAST_INSERT_ID();
    END IF;

    -- -------------------------------------------------------------
    -- LOTES: vigentes, a punto de caducar, caducados y sin caducidad
    -- -------------------------------------------------------------
    CALL ProductLot_Create(p_hue, 'LOT-HUE-2611-A', 720, 'UNITS', v_today + 12 * d, 'ALB-GRANJA-1102', l_hue_a);
    CALL ProductLot_Create(p_hue, 'LOT-HUE-2610-B', 360, 'UNITS', v_today + 2 * d, 'ALB-GRANJA-1088', l_hue_b);
    CALL ProductLot_Create(p_hue, 'LOT-HUE-2610-OLD', 120, 'UNITS', v_today - 3 * d, 'ALB-GRANJA-1050', l_hue_old);
    CALL ProductLot_Create(p_pat, 'LOT-PAT-2611-A', 500, 'KILOS', v_today + 60 * d, 'ALB-ARABA-551', l_pat_a);
    CALL ProductLot_Create(p_pat, 'LOT-PAT-2611-B', 250, 'KILOS', v_today + 45 * d, 'ALB-ARABA-552', l_pat_b);
    CALL ProductLot_Create(p_ceb, 'LOT-CEB-2611-A', 150, 'KILOS', v_today + 30 * d, 'ALB-ZALLA-077', l_ceb_a);
    CALL ProductLot_Create(p_sal, 'LOT-SAL-2609-A', 50, 'KILOS', v_today + 365 * d, 'ALB-AÑANA-310', l_sal_a);
    CALL ProductLot_Create(p_ace, 'LOT-ACE-2610-A', 200, 'KILOS', v_today + 180 * d, 'ALB-JAEN-904', l_ace_a);
    CALL ProductLot_Create(p_torc, 'LOT-TORC-2611-01', 80, 'UNITS', v_today + 4 * d, 'PROD-INTERNA-0611', l_torc_a);
    CALL ProductLot_Create(p_tors, 'LOT-TORS-2611-01', 60, 'UNITS', v_today + 4 * d, 'PROD-INTERNA-0612', l_tors_a);
    CALL ProductLot_Create(p_caj, 'LOT-CAJ-2608-A', 400, 'BOXES', NULL, 'ALB-CARTON-220', l_caj_a);
    CALL ProductLot_Create(p_bac, 'LOT-BAC-2605-OLD', 10, 'UNITS', v_today - 20 * d, 'PROD-INTERNA-0499', l_bac_old);

    -- -------------------------------------------------------------
    -- HISTORIAL DE PLANTA (movimientos manuales y lecturas OCR)
    -- -------------------------------------------------------------
    INSERT INTO lot_history (lot_id, action_type, operator, action_timestamp, quantity_affected, ocr_raw_text, observations) VALUES
        (l_hue_a,   'RECEPCIONADO',  'Mikel Goñi',     NOW() - INTERVAL 3 DAY,              720, 'OCR_RAW: GAR-HUE-001 || LOTE: 2611-A', 'Entrada conforme, temperatura del camión 6 ºC'),
        (l_hue_a,   'EN_PRODUCCION', 'Mikel Goñi',     NOW() - INTERVAL 1 DAY,              180, 'OCR_RAW: MATCH LOTE 2611-A', 'Cascado para la tanda de mañana'),
        (l_hue_b,   'RECEPCIONADO',  'Ane Etxeberria', NOW() - INTERVAL 10 DAY,             360, 'OCR_RAW: GAR-HUE-001 || LOTE: 2610-B', 'Caduca pronto: usar primero'),
        (l_hue_old, 'RECEPCIONADO',  'Ane Etxeberria', NOW() - INTERVAL 20 DAY,             120, 'OCR_RAW: GAR-HUE-001 || LOTE: 2610-OLD', NULL),
        (l_pat_a,   'RECEPCIONADO',  'Mikel Goñi',     NOW() - INTERVAL 5 DAY,              500, 'OCR_RAW: GAR-PAT-002 L:2611-A', 'Palés en cámara de 8 ºC'),
        (l_pat_a,   'EN_PRODUCCION', 'Mikel Goñi',     NOW() - INTERVAL 1 DAY + INTERVAL 2 HOUR, 120, 'OCR_RAW: MATCH LOTE PAT 2611-A', 'Pelado y corte en cubos'),
        (l_pat_b,   'RECEPCIONADO',  'Mikel Goñi',     NOW() - INTERVAL 5 DAY,              250, 'OCR_RAW: GAR-PAT-002 L:2611-B', NULL),
        (l_pat_b,   'BLOQUEADO',     'Ane Etxeberria', NOW() - INTERVAL 4 DAY,              250, NULL, 'Control de calidad: patatas brotadas, pendiente de devolución al proveedor'),
        (l_ceb_a,   'RECEPCIONADO',  'Mikel Goñi',     NOW() - INTERVAL 4 DAY,              150, 'OCR_RAW: GAR-CEB-003 L:2611-A', NULL),
        (l_ceb_a,   'EN_PRODUCCION', 'Mikel Goñi',     NOW() - INTERVAL 1 DAY + INTERVAL 3 HOUR, 25, NULL, 'Pochado lento 40 min'),
        (l_sal_a,   'RECEPCIONADO',  'Ane Etxeberria', NOW() - INTERVAL 60 DAY,             50, 'OCR_RAW: GAR-SAL-004', NULL),
        (l_ace_a,   'RECEPCIONADO',  'Ane Etxeberria', NOW() - INTERVAL 30 DAY,             200, 'OCR_RAW: MON-OIL-003 L:2610-A', NULL),
        (l_torc_a,  'RECEPCIONADO',  'Mikel Goñi',     NOW() - INTERVAL 1 DAY + INTERVAL 6 HOUR, 80, 'OCR_RAW: GAR-TOR-010 L:2611-01', 'Tanda de 80 tortillas con cebolla'),
        (l_tors_a,  'RECEPCIONADO',  'Mikel Goñi',     NOW() - INTERVAL 1 DAY + INTERVAL 7 HOUR, 60, 'OCR_RAW: GAR-TOR-011 L:2611-01', 'Tanda de 60 tortillas sin cebolla'),
        (l_caj_a,   'RECEPCIONADO',  'Ane Etxeberria', NOW() - INTERVAL 90 DAY,             400, NULL, NULL),
        (l_bac_old, 'EN_PRODUCCION', 'Mikel Goñi',     NOW() - INTERVAL 25 DAY,             10, NULL, 'Última tanda antes de descatalogar');

    -- Lo usado en produccion descuenta stock, igual que lo hace LotHistory_Insert desde la app
    UPDATE product_lot pl
        JOIN (SELECT lot_id, SUM(quantity_affected) AS used FROM lot_history
              WHERE action_type = 'EN_PRODUCCION' AND lot_id IN (l_hue_a, l_pat_a, l_ceb_a, l_bac_old)
              GROUP BY lot_id) h ON h.lot_id = pl.lot_id
    SET pl.consumed_quantity = pl.consumed_quantity + h.used;

    -- -------------------------------------------------------------
    -- PEDIDOS
    -- -------------------------------------------------------------
    -- Clientes (se pueden repetir: ya existentes no se duplican)
    INSERT IGNORE INTO customer (name) VALUES
        ('Bar Txoko Zaharra'),
        ('Comedor Escolar Ikastola'),
        ('Catering Itsasoa'),
        ('Restaurante Arrantzale'),
        ('Sociedad Gastronómica Lagunak'),
        ('Hotel Gargantua'),
        ('Asador Mendibil'),
        ('Cafetería Bidebarrieta'),
        ('Pintxos Gure Txokoa');

    -- Hoy, abierto: materias primas para producir
    CALL Order_Create('PED-DEMO-001', (SELECT customer_id FROM customer WHERE name = 'Bar Txoko Zaharra'), v_today, 'Entregar antes de las 11:00', 'ane', o1);
    CALL OrderLine_Add(o1, l_hue_a, 120, 'ane', ln_a);
    CALL OrderLine_Add(o1, l_pat_a, 80, 'ane', ln_a);
    CALL OrderLine_Add(o1, l_ceb_a, 20, 'ane', ln_a);
    CALL OrderLine_Add(o1, l_ace_a, 15, 'ane', ln_a);

    -- Hoy, abierto: producto terminado
    CALL Order_Create('PED-DEMO-002', (SELECT customer_id FROM customer WHERE name = 'Comedor Escolar Ikastola'), v_today, NULL, 'luca', o2);
    CALL OrderLine_Add(o2, l_torc_a, 30, 'luca', ln_a);
    CALL OrderLine_Add(o2, l_tors_a, 20, 'luca', ln_a);
    CALL OrderLine_Add(o2, l_caj_a, 50, 'luca', ln_a);

    -- Mañana, abierto: usa el lote de huevos que caduca pronto
    CALL Order_Create('PED-DEMO-003', (SELECT customer_id FROM customer WHERE name = 'Catering Itsasoa'), v_today + d, 'Evento de 120 personas', 'ane', o3);
    CALL OrderLine_Add(o3, l_hue_b, 100, 'ane', ln_a);
    CALL OrderLine_Add(o3, l_sal_a, 2, 'ane', ln_a);

    -- Hace cuatro días, ya cerrado: preparado (con consumo distinto de lo reservado),
    -- entregado hace 3 días y cerrado automáticamente a las 48 h
    CALL Order_Create('PED-DEMO-004', (SELECT customer_id FROM customer WHERE name = 'Restaurante Arrantzale'), v_today - 4 * d, NULL, 'ane', o4);
    CALL OrderLine_Add(o4, l_hue_a, 90, 'ane', ln_a);
    CALL OrderLine_Add(o4, l_pat_a, 60, 'ane', ln_b);
    CALL OrderLine_Consume(o4, ln_a, 96, 'mikel');
    CALL OrderLine_Consume(o4, ln_b, 55, 'mikel');
    CALL Order_Prepare(o4, 'mikel');
    CALL Order_Deliver(o4, 'mikel');
    UPDATE customer_order SET prepared_at = NOW() - INTERVAL 4 DAY, delivered_at = NOW() - INTERVAL 3 DAY WHERE order_id = o4;
    UPDATE order_event SET event_timestamp = NOW() - INTERVAL 3 DAY WHERE order_id = o4 AND event_type = 'ENTREGADO';
    UPDATE order_event SET event_timestamp = NOW() - INTERVAL 4 DAY WHERE order_id = o4 AND event_type <> 'ENTREGADO';

    -- Hace dos días, cancelado: se liberó lo reservado
    CALL Order_Create('PED-DEMO-005', (SELECT customer_id FROM customer WHERE name = 'Sociedad Gastronómica Lagunak'), v_today - 2 * d, 'El cliente anuló la cena', 'luca', o5);
    CALL OrderLine_Add(o5, l_torc_a, 15, 'luca', ln_a);
    CALL Order_Cancel(o5, 'luca');

    -- Próxima semana, abierto y todavía sin lotes
    CALL Order_Create('PED-DEMO-006', (SELECT customer_id FROM customer WHERE name = 'Hotel Gargantua'), v_today + 7 * d, 'Pendiente de confirmar cantidades', 'ane', o6);
    END catalogo;

    -- =============================================================
    -- BLOQUE 2: pedidos preparados, entregados y cerrados automaticamente
    -- =============================================================
    estados: BEGIN
    IF (SELECT COUNT(*) FROM customer_order WHERE order_number = 'PED-DEMO-007') > 0 THEN
        LEAVE estados;
    END IF;
    SET v_loaded = v_loaded + 1;

    -- Los lotes se buscan por codigo: sirve tambien si el bloque 1 se cargo antes
    SELECT lot_id INTO l_hue_a FROM product_lot WHERE lot_number = 'LOT-HUE-2611-A';
    SELECT lot_id INTO l_ceb_a FROM product_lot WHERE lot_number = 'LOT-CEB-2611-A';
    SELECT lot_id INTO l_tors_a FROM product_lot WHERE lot_number = 'LOT-TORS-2611-01';
    SELECT lot_id INTO l_caj_a FROM product_lot WHERE lot_number = 'LOT-CAJ-2608-A';
    SELECT lot_id INTO l_sal_a FROM product_lot WHERE lot_number = 'LOT-SAL-2609-A';

    -- Hoy, PREPARADO: listo para salir hacia el cliente
    CALL Order_Create('PED-DEMO-007', (SELECT customer_id FROM customer WHERE name = 'Asador Mendibil'), v_today, 'Recogen en planta a las 13:00', 'ane', o7);
    CALL OrderLine_Add(o7, l_hue_a, 36, 'ane', ln_a);
    CALL OrderLine_Add(o7, l_ceb_a, 6, 'ane', ln_b);
    CALL OrderLine_Consume(o7, ln_a, 36, 'mikel');
    CALL OrderLine_Consume(o7, ln_b, 5.5, 'mikel');
    CALL Order_Prepare(o7, 'mikel');

    -- Ayer, ENTREGADO hace 20 horas: se cerrara solo dentro de 28 horas
    CALL Order_Create('PED-DEMO-008', (SELECT customer_id FROM customer WHERE name = 'Cafetería Bidebarrieta'), v_today - d, NULL, 'luca', o8);
    CALL OrderLine_Add(o8, l_tors_a, 12, 'luca', ln_a);
    CALL OrderLine_Add(o8, l_caj_a, 12, 'luca', ln_b);
    CALL OrderLine_Consume(o8, ln_a, 12, 'mikel');
    CALL OrderLine_Consume(o8, ln_b, 12, 'mikel');
    CALL Order_Prepare(o8, 'mikel');
    CALL Order_Deliver(o8, 'mikel');
    UPDATE customer_order SET prepared_at = NOW() - INTERVAL 22 HOUR, delivered_at = NOW() - INTERVAL 20 HOUR WHERE order_id = o8;
    UPDATE order_event SET event_timestamp = NOW() - INTERVAL 20 HOUR WHERE order_id = o8 AND event_type = 'ENTREGADO';
    UPDATE order_event SET event_timestamp = NOW() - INTERVAL 22 HOUR WHERE order_id = o8 AND event_type <> 'ENTREGADO';

    -- Hace una semana, ya CERRADO: entregado hace 6 dias
    CALL Order_Create('PED-DEMO-009', (SELECT customer_id FROM customer WHERE name = 'Pintxos Gure Txokoa'), v_today - 7 * d, 'Pedido semanal', 'ane', o9);
    CALL OrderLine_Add(o9, l_hue_a, 48, 'ane', ln_a);
    CALL OrderLine_Add(o9, l_sal_a, 1, 'ane', ln_b);
    CALL OrderLine_Consume(o9, ln_a, 48, 'mikel');
    CALL OrderLine_Consume(o9, ln_b, 0.8, 'mikel');
    CALL Order_Prepare(o9, 'mikel');
    CALL Order_Deliver(o9, 'mikel');
    UPDATE customer_order SET prepared_at = NOW() - INTERVAL 7 DAY, delivered_at = NOW() - INTERVAL 6 DAY WHERE order_id = o9;
    UPDATE order_event SET event_timestamp = NOW() - INTERVAL 6 DAY WHERE order_id = o9 AND event_type = 'ENTREGADO';
    UPDATE order_event SET event_timestamp = NOW() - INTERVAL 7 DAY WHERE order_id = o9 AND event_type <> 'ENTREGADO';
    END estados;

    -- =============================================================
    -- BLOQUE 3: codigos de barras (GTIN) de los productos, para que al escanear la
    -- etiqueta GS1 de un lote se reconozca el producto. Solo si aun no tienen;
    -- IGNORE evita el error si ese codigo ya se asigno a mano a otro producto.
    -- =============================================================
    UPDATE IGNORE product
    SET gtin = CASE sku
        WHEN 'GAR-HUE-001' THEN '08412345678905'
        WHEN 'GAR-PAT-002' THEN '08412345000016'
        WHEN 'GAR-CEB-003' THEN '08412345000023'
        WHEN 'GAR-SAL-004' THEN '08412345000030'
        WHEN 'GAR-TOR-010' THEN '08412345000108'
        WHEN 'GAR-TOR-011' THEN '08412345000115'
        WHEN 'GAR-CAJ-020' THEN '08412345000207'
        WHEN 'MON-OIL-003' THEN '08480000000014'
    END
    WHERE gtin IS NULL
      AND sku IN ('GAR-HUE-001', 'GAR-PAT-002', 'GAR-CEB-003', 'GAR-SAL-004', 'GAR-TOR-010', 'GAR-TOR-011', 'GAR-CAJ-020', 'MON-OIL-003');
    SET v_loaded = v_loaded + ROW_COUNT();

    -- Los pedidos entregados hace mas de 48 h (PED-DEMO-004 y 009) se cierran como lo haria la API,
    -- con la fecha en que se habrian cerrado: 48 h despues de la entrega
    CALL Order_CloseDelivered(48, v_closed);
    UPDATE order_event oe
        JOIN customer_order co ON co.order_id = oe.order_id
    SET oe.event_timestamp = co.delivered_at + INTERVAL 48 HOUR
    WHERE co.order_number IN ('PED-DEMO-004', 'PED-DEMO-009') AND co.closed_by = 'Sistema' AND oe.event_type = 'CERRADO';
    UPDATE customer_order SET closed_at = delivered_at + INTERVAL 48 HOUR
    WHERE order_number IN ('PED-DEMO-004', 'PED-DEMO-009') AND closed_by = 'Sistema';

    COMMIT;
    IF v_loaded = 0 THEN
        SELECT 'Los datos de demo ya estaban cargados: no se ha cambiado nada.' AS aviso;
    ELSE
        SELECT 'Datos de demo cargados.' AS aviso;
    END IF;
END;;

DELIMITER ;

CALL Demo_Load();
DROP PROCEDURE IF EXISTS Demo_Load;

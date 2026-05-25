-- =================================================================
-- 1. SEED: PRODUCTOS DE MONCORP
-- =================================================================
INSERT INTO product (product_id, sku, name, description, is_active)
VALUES (1, 'MON-CHE-001', 'Queso Cabrales Selección MonCorp', 'Queso azul artesanal madurado en cueva', 1),
       (2, 'MON-WIN-002', 'Vino Tinto Reserva MonCorp', 'Vino tinto de barrica, cosecha seleccionada', 1),
       (3, 'MON-OIL-003', 'Aceite de Oliva Virgen Extra', 'Garrafa de 5 litros de extracción en frío', 1);

-- =================================================================
-- 2. SEED: LOTES ASOCIADOS (Los objetivos de las pruebas de la cámara)
-- =================================================================
INSERT INTO product_lot (lot_id, product_id, lot_number, quantity, measure_unit, expiration_date, supplier_reference)
VALUES (1, 1, 'LOT-2026-05-19A', 50.0000, 'UNITS', 1779282053, 'ALB-7761'),
       (2, 1, 'LOT-2026-04-10B', 120.0000, 'UNITS', 1779282053, 'ALB-7400'),
       (3, 2, 'LOT-2026-VINO-01', 300.0000, 'BOXES', 1779282053, 'ALB-8812');

-- =================================================================
-- 3. SEED: HISTORIAL CRONOLÓGICO DE ACCIONES
-- =================================================================

-- Historial para el Lote 1 (Caso de éxito completo)
INSERT INTO lot_history (lot_id, action_type, operator, action_timestamp, quantity_affected, ocr_raw_text, observations)
VALUES (1, 'RECEPCIONADO', 'Luca', '2026-05-19 09:00:00', 50.0000, 'OCR_RAW: MON-CHE-001 || LOTE: 05-19A || CAD: 31-12-2026', 'Entrada conforme del camión de reparto'),
       (1, 'EN_PRODUCCION', 'Alex', '2026-05-19 11:30:00', 50.0000, 'OCR_RAW: MATCH LOTE 05-19A', 'Lote enviado a la mesa de etiquetado final'),
       (1, 'VENDIDO', 'Luca', '2026-05-19 14:00:00', 10.0000, 'OCR_RAW: LOT-2026-05-19A', 'Venta parcial. Albarán de salida de cliente #4521');

-- Historial para el Lote 2 (Simulación de lote retenido/bloqueado)
INSERT INTO lot_history (lot_id, action_type, operator, action_timestamp, quantity_affected, ocr_raw_text, observations)
VALUES (2, 'RECEPCIONADO', 'Alex', '2026-04-10 08:15:00', 120.0000, 'OCR_RAW: PROD_001_B_10', 'Recepción de stock de seguridad'),
       (2, 'BLOQUEADO', 'Calidad_Juan', '2026-04-11 10:00:00', 120.0000, NULL, 'Retirado preventivamente por rotura de cadena de frío en transporte');

-- Historial para el Lote 3 (Stock parado en almacén)
INSERT INTO lot_history (lot_id, action_type, operator, action_timestamp, quantity_affected, ocr_raw_text, observations)
VALUES (3, 'RECEPCIONADOOO', 'Juanillo', '2026-05-01 12:00:00', 300.0000, 'OCR_RAW: MON-WIN-002 L:VINO-01', 'Palés ubicados en bodega central');

-- =================================================================
-- 4. SEED: USUARIOS DE LA APLICAION
-- =================================================================
-- Usuario de prueba: username 'luca', password 'moncorp2026'
INSERT INTO app_user (username, password_hash, name, role, is_active)
VALUES ('luca', 'moncorp2026', 'Luca Armendariz', 'ADMIN', 1);
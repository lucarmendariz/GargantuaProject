-- =================================================================
-- 00. CREACIÓN DE LA BASE DE DATOS Y USUARIO DE APLICACIÓN
-- =================================================================

-- 1. Crear la base de datos si no existe con codificación moderna
CREATE DATABASE IF NOT EXISTS traceability_lean
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- 2. Crear el usuario exclusivo para la app de Java (si no existe)
-- Reemplaza 'MonCorpPass2026!' por la contraseña que tú quieras
CREATE USER IF NOT EXISTS 'trazabilidad_app'@'localhost'
    IDENTIFIED BY 'MonCorpPass2026!';

-- 3. Darle permisos absolutos al usuario PERO SOLO en su base de datos
GRANT ALL PRIVILEGES ON traceability_lean.* TO 'trazabilidad_app'@'localhost';

-- 4. Aplicar los cambios de permisos inmediatamente
FLUSH PRIVILEGES;
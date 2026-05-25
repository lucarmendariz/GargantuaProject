#!/bin/bash

set -e

# Configuración única unificada
DB_USER="trazabilidad_app"
DB_PASS="MonCorpPass2026!"
DB_NAME="traceability_lean"

echo "================================================="
echo "🧨 SCRIPT 2: RESET COMPLETO + LIMPIEZA DE DATOS"
echo "================================================="

# 1. Parar contenedor y BORRAR el volumen de datos (Evita duplicados)
echo "🧹 Eliminando contenedor y base de datos antigua..."
docker compose down -v

# 2. Levantar Docker desde cero (Limpio de fábrica)
echo "🐳 Creando contenedor MariaDB nuevo..."
docker compose up -d

# 3. Esperar a MariaDB (Entramos como root/root que viene por defecto)
echo "⏳ Esperando a que MariaDB inicie sus servicios..."
until docker exec trazabilidad_db mariadb -uroot -proot -e "SELECT 1" >/dev/null 2>&1
do
  sleep 2
done

echo "✅ MariaDB lista para recibir scripts"

# 4. COMANDO MAESTRO (ROOT) - Para crear el entorno e inicializar
ROOT_CMD="docker exec -i trazabilidad_db mariadb -uroot -proot"

echo "🔐 [ROOT] Creando usuario '$DB_USER' y base de datos '$DB_NAME'..."
$ROOT_CMD < 00_init_user_db.sql

# 5. COMANDO DE APLICACIÓN - Para rellenar las tablas sin duplicados
APP_CMD="docker exec -i trazabilidad_db mariadb -u$DB_USER -p$DB_PASS $DB_NAME"

echo "🏗️ [APP] Ejecutando estructura del esquema (01_schema.sql)..."
$APP_CMD < 01_schema.sql

echo "⚙️ [APP] Cargando procedimientos almacenados (02_procedures.sql)..."
$APP_CMD < 02_procedures.sql

echo "🌱 [APP] Insertando semillas limpias (03_seeds.sql)..."
$APP_CMD < 03_seeds.sql

echo "================================================="
echo "🚀 TODO LISTO: BASE DE DATOS TOTALMENTE RECREADA"
echo "================================================="
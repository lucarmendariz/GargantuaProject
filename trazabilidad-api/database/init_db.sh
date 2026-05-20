#!/bin/bash

# =================================================================
# CONFIGURACIÓN MAESTRA: ENTRAR COMO ROOT (Una sola vez)
# =================================================================
DB_USER="root"
DB_PASS=""  # Deja vacío si usas MariaDB de Homebrew sin clave, o pon tu clave de root

cd "$(dirname "$0")"

echo "================================================="
echo "🏗️  SCRIPT DE INICIALIZACIÓN PRIMARIA (ROOT)"
echo "================================================="

DB_CMD="mariadb -h localhost -P 3306 -u $DB_USER"
if [ ! -z "$DB_PASS" ]; then DB_CMD="$DB_CMD -p$DB_PASS"; fi

echo "🔐 [1/4] Creando Base de Datos y Usuario 'trazabilidad_app'..."
$DB_CMD < 00_init_user_db.sql
if [ $? -ne 0 ]; then echo "❌ Error en 00_init_user_db.sql"; exit 1; fi

echo "🏗️  [2/4] Creando estructura de tablas inicial..."
$DB_CMD traceability_lean < 01_schema.sql
if [ $? -ne 0 ]; then echo "❌ Error en 01_schema.sql"; exit 1; fi

echo "⚙️  [3/4] Cargando procedimientos almacenados..."
$DB_CMD traceability_lean < 02_procedures.sql
if [ $? -ne 0 ]; then echo "❌ Error en 02_procedures.sql"; exit 1; fi

echo "🌱 [4/4] Insertando datos maestros de MonCorp (Seeds)..."
$DB_CMD traceability_lean < 03_seeds.sql
if [ $? -ne 0 ]; then echo "❌ Error en 03_seeds.sql"; exit 1; fi

echo "================================================="
echo "🚀 ¡ENTORNO CREADO! Ya puedes usar update_db.sh"
echo "================================================="
#!/bin/bash

# CONFIGURACIÓN (Para crear el usuario y la BD necesitamos entrar como ROOT obligatoriamente)
DB_USER="root"
DB_PASS="MonCorpPass2026"
DB_NAME="traceability_lean"
DB_HOST="localhost"
DB_PORT="3306"

cd "$(dirname "$0")"

echo "================================================="
echo "🔄 ACTUALIZADOR AUTOMÁTICO DE BASE DE DATOS"
echo "================================================="

# Validar que los 4 archivos existen
if [ ! -f "00_init_user_db.sql" ] || [ ! -f "01_schema.sql" ] || [ ! -f "02_procedures.sql" ] || [ ! -f "03_seeds.sql" ]; then
    echo "❌ Error: Falta alguno de los archivos SQL (00, 01, 02 o 03) en esta carpeta."
    exit 1
fi

DB_CMD="mariadb -h $DB_HOST -P $DB_PORT -u $DB_USER"
if [ ! -z "$DB_PASS" ]; then DB_CMD="$DB_CMD -p$DB_PASS"; fi

echo "🔐 0/4 Inicializando Base de datos y Usuario..."
$DB_CMD < 00_init_user_db.sql
if [ $? -ne 0 ]; then echo "❌ Error en 00_init_user_db.sql"; exit 1; fi

echo "🏗️  1/4 Creando tablas y limpiando datos viejos..."
$DB_CMD $DB_NAME < 01_schema.sql
if [ $? -ne 0 ]; then echo "❌ Error en 01_schema.sql"; exit 1; fi

echo "⚙️  2/4 Cargando procedimientos almacenados..."
$DB_CMD $DB_NAME < 02_procedures.sql
if [ $? -ne 0 ]; then echo "❌ Error en 02_procedures.sql"; exit 1; fi

echo "🌱 3/4 Insertando datos de prueba (Seeds)..."
$DB_CMD $DB_NAME < 03_seeds.sql
if [ $? -ne 0 ]; then echo "❌ Error en 03_seeds.sql"; exit 1; fi

echo "================================================="
echo "🚀 ¡BINGO! Entorno e infraestructura creados con éxito."
echo "================================================="
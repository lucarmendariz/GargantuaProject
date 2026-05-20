#!/bin/bash

# =================================================================
# CONFIGURACIÓN SEGURA: USUARIO RESTRINGIDO DE LA APP
# =================================================================
DB_USER="trazabilidad_app"
DB_PASS="MonCorpPass2026!"  # La clave que definimos en el paso 00
DB_NAME="traceability_lean"
DB_HOST="localhost"
DB_PORT="3306"

cd "$(dirname "$0")"

echo "================================================="
echo "🔄 ACTUALIZADOR DE BASE DE DATOS (MODO APLICACIÓN)"
echo "================================================="

# Validar que existan los 3 archivos de trabajo diario
if [ ! -f "01_schema.sql" ] || [ ! -f "02_procedures.sql" ] || [ ! -f "03_seeds.sql" ]; then
    echo "❌ Error: Falta alguno de los archivos SQL (01, 02 o 03) en esta carpeta."
    exit 1
fi

# Comando base usando el usuario de la app
DB_CMD="mariadb -h $DB_HOST -P $DB_PORT -u $DB_USER -p$DB_PASS"

echo "🏗️  [1/3] Limpiando y recreando tablas..."
$DB_CMD $DB_NAME < 01_schema.sql
if [ $? -ne 0 ]; then echo "❌ Error en 01_schema.sql"; exit 1; fi

echo "⚙️  [2/3] Actualizando procedimientos almacenados..."
$DB_CMD $DB_NAME < 02_procedures.sql
if [ $? -ne 0 ]; then echo "❌ Error en 02_procedures.sql"; exit 1; fi

echo "🌱 [3/3] Sincronizando datos de prueba (Seeds)..."
$DB_CMD $DB_NAME < 03_seeds.sql
if [ $? -ne 0 ]; then echo "❌ Error en 03_seeds.sql"; exit 1; fi

echo "================================================="
echo "🚀 Sincronización completada de forma segura."
echo "================================================="
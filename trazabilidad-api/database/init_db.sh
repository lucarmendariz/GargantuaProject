#!/bin/bash

set -e

echo "================================================="
echo "🐳 SCRIPT 1: LEVANTANDO ENTORNO DOCKER MARIADB"
echo "================================================="

# 1. Levantar Docker (Si ya estaba levantado, no hace nada)
echo "🚀 Levantando contenedor..."
docker compose up -d

# 2. Esperar a que MariaDB esté lista (Usa las credenciales maestras del docker-compose)
echo "⏳ Esperando a que el motor de MariaDB responda..."
until docker exec trazabilidad_db mariadb -uroot -proot -e "SELECT 1" >/dev/null 2>&1
do
  sleep 2
done

echo "================================================="
echo "✅ DOCKER LISTO - BASE DE DATOS EN MARCHA"
echo "================================================="
# GargantuaProject

> Aplicación web de trazabilidad para plantas de alimentación: lotes e inventario, pedidos que reservan y consumen stock, movimientos de producción con escáner de etiquetas, y paneles para activar módulos contratados y repartir permisos por rol.

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-orange)

Este repositorio contiene el backend y el frontend en el mismo lugar, pero **se desarrollan por separado**: cada parte tiene su carpeta, su entorno de desarrollo y su forma de arrancar.

## Estructura del repositorio

```
GargantuaProject/
├── trazabilidad-api/               Backend (API REST)
│   └── database/                   Scripts SQL y update_db.sh
├── GargantAppFront/
│   └── gargantapp-front/           Frontend (Vue)
└── produccion/                     Instalación en un cliente: Docker, instalar.sh y backup.sh
```

| Parte | Carpeta | Tecnología | IDE |
|-------|---------|------------|-----|
| Backend | `trazabilidad-api` | Java 17, Spring Boot 3.3.5, Gradle, MariaDB (JDBC) | IntelliJ IDEA |
| Frontend | `GargantAppFront/gargantapp-front` | Vue | Visual Studio Code |

## Requisitos

- **Git**
- **JDK 17**
- **Node.js LTS** (incluye npm)
- **MariaDB** (instalada en local o con Docker)
- **IntelliJ IDEA** para el backend y **Visual Studio Code** para el frontend
- En Windows, **Git Bash** para ejecutar el script `update_db.sh`

## Puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/lucarmendariz/GargantuaProject.git
cd GargantuaProject
```

### 2. Secretos y base de datos

Copia la plantilla y sustituye cada valor (puedes generarlos con `openssl rand -hex 32`):

```bash
cd trazabilidad-api/database
cp .env.example .env
```

`.env` no se sube al repositorio. Después crea la base de datos con sus tablas y datos de prueba:

```bash
bash update_db.sh
```

> `update_db.sh` **borra la base de datos** y la crea de nuevo. Para arrancarla sin perder datos usa `bash init_db.sh`.
>
> En Windows, ejecuta los scripts desde **Git Bash**. Si dan errores de `\r`, convierte el archivo a finales de línea LF.

Usuario de prueba: `luca` / `moncorp2026`. Es público, así que no lo uses fuera de desarrollo.

**Datos de demo (opcional).** `04_demo_data.sql` rellena todas las pantallas: un usuario de cada rol (`ane` ENCARGADO y `mikel` OPERARIO, misma contraseña), productos de la planta de tortillas, lotes vigentes, a punto de caducar, caducados y bloqueados, historial de planta y pedidos abiertos, cerrados y cancelados con fechas relativas a hoy. Se puede repetir sin duplicar nada. Desde `trazabilidad-api/database`:

```bash
set -a; source ./.env; set +a
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < 04_demo_data.sql
```

### Datos de prueba automáticos en desarrollo (perfil `dev`)

Con el perfil `dev` la API **reinicia la base de datos con los datos de prueba cada vez que arranca**: borra todas las tablas y ejecuta `01_schema.sql`, `02_procedures.sql`, `03_seeds.sql` y `04_demo_data.sql`. Además vigila esos cuatro scripts y, **si guardas un cambio en alguno, repite el reinicio** sin parar la API (si el script tiene un error, se ve en el log y la API sigue en marcha). Así siempre se trabaja con los mismos datos y con el esquema al día, sin aplicar migraciones a mano.

- `./gradlew bootRun` usa el perfil `dev` con el reinicio encendido (`app.dev.reset-db=false` en `application-dev.properties` lo apaga). Desde IntelliJ, añade `SPRING_PROFILES_ACTIVE=dev` en las variables de entorno de la configuración de ejecución y pon como *Working directory* la carpeta `trazabilidad-api`.
- `docker compose up -d` también usa `dev`, pero **con el reinicio apagado**, para que instalar la app en un cliente con Docker nunca borre sus datos. En tu equipo de desarrollo, enciéndelo con un `.env` en la raíz del repositorio (no se sube a git):

  ```bash
  echo DEV_RESET_DB=true > .env
  ```
- Por seguridad solo reinicia bases de datos locales (`localhost`, `127.0.0.1` o el contenedor `trazabilidad_db` del compose de desarrollo). **No uses el perfil `dev` en producción.**

### Todo con Docker (sin instalar JDK ni Node)

Desde la raíz del repositorio, `docker compose up -d` arranca el grupo de contenedores **`gargantapp-dev`**: la base de datos (`trazabilidad_db`, en `localhost:3307`, con las contraseñas de `trazabilidad-api/database/.env`), la API (`http://localhost:8080`) y el frontend (`https://localhost:5173`, con un certificado local: el navegador avisa la primera vez). El frontend llama a la API a través de `/api`, que Vite reenvía al contenedor de la API. Para usar HTTP sin certificado: `HTTPS=false docker compose up -d`.

### Escanear etiquetas con la cámara del móvil

En *Inventario → Añadir lote*, en la *Cámara OCR* del Panel Central y en *Producción Planta* hay un botón **Escanear con la cámara**. Lee códigos de barras y QR (Code128, EAN, QR, DataMatrix y GS1 con lote `(10)`, caducidad `(17)` y cantidad `(37)`) y, si la etiqueta no tiene código, reconoce el texto por OCR (`LOTE: ...`, `CAD: dd/mm/aaaa`, `ALB-...`). Los campos se rellenan solos y se revisan antes de guardar. El producto se reconoce por su código de barras (EAN/GTIN, campo del producto en *Inventario*) o por el SKU escrito en la etiqueta; si el código aún no es de ningún producto, al dar de alta el lote se guarda en el producto elegido y la próxima vez se reconoce solo.

Para usarlo desde un móvil (Safari solo deja usar la cámara en HTTPS, y el iPhone no acepta el certificado provisional de Vite):

1. En el PC, desde `GargantAppFront/gargantapp-front`, crea el certificado con la IP del PC y reinicia el front:

   ```bash
   bash scripts/generar-certificado.sh
   docker compose restart front
   ```

   Crea una autoridad de certificación propia (CA) y un certificado para `localhost` y las IP del PC, en `certs/` (no se sube al repositorio). Si cambia la IP, vuelve a ejecutarlo: la CA se mantiene.
2. El móvil y el PC tienen que estar en la misma WiFi. En el iPhone, abre en Safari `https://<IP del PC>:5173/gargantua-ca.crt`, acepta el aviso (*Mostrar detalles → visitar este sitio web*) y permite descargar el perfil.
3. Instálalo en *Ajustes → General → VPN y gestión de dispositivos → Gargantua Desarrollo CA → Instalar*.
4. Actívalo en *Ajustes → General → Información → Ajustes de confianza de certificados → Gargantua Desarrollo CA*.
5. Abre `https://<IP del PC>:5173`: ya no sale ningún aviso. La primera vez que escanees, permite el acceso a la cámara.

El OCR descarga el idioma la primera vez que se usa, así que el móvil necesita internet en ese momento.

### 3. Backend

1. Abre la carpeta `trazabilidad-api` en **IntelliJ IDEA** (no la raíz del repositorio).
2. Activa el procesamiento de anotaciones para Lombok: *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing*.
3. Arranca la aplicación desde IntelliJ o por terminal. La configuración se lee de `database/.env`, con `trazabilidad-api` como directorio de trabajo:

   ```bash
   cd trazabilidad-api
   ./gradlew bootRun
   ```

   En Windows: `gradlew.bat bootRun`. La API queda disponible en `http://localhost:8080`.

La API exige un token JWT, que se obtiene en `POST /api/auth/login` y caduca a las 8 horas. Los permisos se leen de la base de datos en cada petición: desactivar a un usuario, cambiarle el rol o apagar un módulo surte efecto al momento.

### Módulos contratados y permisos

Cada cliente tiene su propia instalación. Hay cuatro roles:

| Rol | Qué hace |
|-----|----------|
| `SUPERADMIN` | El proveedor de la app. En **Plataforma** activa los módulos que el cliente tiene contratados. También entra en Administración para dar soporte. |
| `ADMIN` | El administrador del cliente. Gestiona todos los módulos contratados y, en **Administración**, decide qué puede hacer cada rol y da de alta, cambia de rol o desactiva a sus usuarios. |
| `ENCARGADO`, `OPERARIO` | Ven los módulos que les permita el `ADMIN`, con uno de estos niveles: **Sin acceso**, **Ver** (consultar y operar en planta: preparar y entregar pedidos, registrar movimientos) o **Gestionar** (además crear, editar y borrar). |

Módulos: `PEDIDOS`, `INVENTARIO`, `LOTES`, `HISTORIAL` y `PRODUCCION`. Un módulo no contratado no lo ve nadie, ni el `ADMIN`, y la API rechaza cualquier acceso a él. La trazabilidad de un lote la consulta cualquier usuario activo.

Permisos por defecto (los de antes de existir el panel): `ENCARGADO` gestiona Pedidos, Inventario y Lotes; `OPERARIO` ve Pedidos y Producción.

Usuario de prueba del proveedor (solo en la base de datos de desarrollo, `03_seeds.sql`): `superadmin` / `moncorp2026`.

Las instalaciones nuevas en clientes crean `superadmin` con tu contraseña desde `produccion/instalar.sh` (ver «Instalar en un cliente»). Cada usuario cambia su nombre, su foto y su contraseña en **Mi perfil** (pulsando su nombre en la cabecera del Panel Central).

**Instalaciones antiguas actualizadas con la migración `004`.** La migración crea `superadmin` desactivado, porque la contraseña de prueba es pública. Genera el hash de una contraseña propia y actívalo, desde `trazabilidad-api/database`:

```bash
# Hash BCrypt de la contraseña (htpasswd viene con Apache; también sirve cualquier generador BCrypt)
htpasswd -nbBC 10 "" 'TU-CONTRASEÑA' | tr -d ':\n' | sed 's/$2y/$2a/'
set -a; source ./.env; set +a
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean \
  -e "UPDATE app_user SET password_hash='<HASH>', is_active=1 WHERE username='superadmin';"
```

> `update_db.sh` y `03_seeds.sql` cargan usuarios con contraseñas públicas (`luca`, `superadmin`): úsalos solo en desarrollo.

### 4. Frontend

1. Abre la carpeta `GargantAppFront/gargantapp-front` en **Visual Studio Code**.
2. Instala las dependencias y arranca el servidor de desarrollo:

   ```bash
   cd GargantAppFront/gargantapp-front
   npm install
   npm run dev
   ```

   [Confirma el comando en la sección `scripts` de `package.json`: puede ser `npm run dev` o `npm run serve`.]

3. Abre la dirección que muestre la terminal (normalmente `http://localhost:5173`).

> El frontend necesita que el backend esté en marcha. Si el navegador muestra errores de CORS, el backend debe permitir el origen del frontend.

## Instalar en un cliente

Cada cliente tiene su propia instalación: un servidor (en la nube o en la planta) con Docker. Todo lo necesario está en `produccion/`:

| Fichero | Para qué |
|---------|----------|
| `docker-compose.yml` | Base de datos, API (compilada, **sin** el perfil `dev`: nunca borra datos) y web (Caddy: sirve la app, reenvía `/api` y saca el HTTPS) |
| `instalar.sh` | Primera instalación y actualizaciones |
| `backup.sh` | Copia de seguridad de la base de datos |
| `.env.example` | Configuración del cliente (`instalar.sh` crea `.env` a partir de ella) |

**1. Elegir dónde.** Lo más sencillo es un servidor pequeño en la nube y un dominio (`trazabilidad.cliente.com`) apuntando a él, con los puertos 80 y 443 abiertos: Caddy saca el certificado HTTPS de Let's Encrypt solo y la cámara funciona en cualquier móvil. Sin internet en la planta, usa un equipo de su red y su IP como dominio: Caddy usa su propia CA y cada móvil tiene que confiar en ella (el certificado raíz queda en el volumen `caddy_data`, en `pki/authorities/local/root.crt`).

**2. Instalar.** En el servidor, con Docker y su plugin `compose`:

```bash
git clone https://github.com/lucarmendariz/GargantuaProject.git
cd GargantuaProject/produccion
./instalar.sh
```

Pregunta la dirección de la app, genera las contraseñas internas en `produccion/.env` (**guarda una copia de ese fichero**: sin él no se abre la base de datos), construye y arranca todo, y con la base de datos vacía pide:
- tu contraseña de proveedor (usuario `superadmin`),
- el usuario, el nombre y la contraseña del administrador del cliente.

La base de datos empieza vacía: solo módulos, permisos por defecto y esos dos usuarios. Después, entra como `superadmin` en **Plataforma** y deja activos los módulos contratados; el administrador del cliente da de alta a su gente en **Administración**.

**3. Copias de seguridad.** `instalar.sh` programa `backup.sh` a diario a las 03:00 (con `crontab`). Cada copia queda en `produccion/copias/` (se guardan `DIAS_COPIAS` días). Para guardar también una copia **fuera del servidor**, configura [rclone](https://rclone.org/) con un destino (otro servidor, S3, Google Drive…) y ponlo en `DESTINO_REMOTO` del `.env`. Restaurar una copia (sustituye los datos actuales):

```bash
cd produccion && set -a && source ./.env && set +a
gunzip -c copias/gargantapp-AAAAMMDD-HHMM.sql.gz | docker compose exec -T -e MYSQL_PWD="$DB_ROOT_PASSWORD" db mariadb -uroot traceability_lean
```

**4. Actualizar a una versión nueva.**

```bash
cd GargantuaProject && git pull
cd produccion && ./backup.sh
# La API vieja no puede funcionar con la base de datos nueva: se para mientras se migra
docker compose stop api
set -a && source ./.env && set +a
# Solo las migraciones nuevas que este cliente aún no tenga (p. ej. 005 y 006), en orden, y siempre 02_procedures.sql al final
for f in migrations/005_perfil_usuario.sql migrations/006_operario_usuario.sql 02_procedures.sql; do
  docker compose exec -T -e MYSQL_PWD="$DB_ROOT_PASSWORD" db mariadb -uroot traceability_lean < ../trazabilidad-api/database/$f
done
./instalar.sh
```

Si algo sale mal, restaura la copia que acaba de hacer `backup.sh` (ver «Copias de seguridad»).

**Probar la instalación en tu equipo** (con el entorno de desarrollo arrancado, en otros puertos): `DOMINIO=:80` en `produccion/.env` y `PUERTO_HTTP=8088 ./instalar.sh`; la app queda en `http://localhost:8088`.

## Pedidos y stock

Un pedido tiene una fecha programada y una línea por cada lote que usa. Pasa por estos estados:

`ABIERTO` → `PREPARADO` → `ENTREGADO` → `CERRADO` (automático). Desde `ABIERTO` también se puede `CANCELAR`.

1. **Reservar** (abierto): al asignar un lote baja su cantidad *disponible*, pero no su stock.
2. **Preparar:** se indica lo realmente consumido en cada línea. Eso se resta del stock y se libera lo reservado. O se registran todas las líneas o no cambia nada. Desde aquí los lotes del pedido ya no se pueden cambiar.
3. **Entregar:** se marca cuando el pedido sale hacia el cliente.
4. **Cerrar (automático):** a las 48 h de la entrega la API pasa el pedido a `CERRADO` (`app.orders.auto-close-hours`). Los cerrados se consultan en la pestaña *Pedidos cerrados* del módulo Pedidos, con buscador.
5. **Cancelar o retirar un lote** (solo abierto): se libera lo reservado sin tocar el stock.

`stock = entrada - consumido` y `disponible = stock - reservado`. Un lote cuyo último movimiento manual es `BLOQUEADO` no se puede reservar ni consumir. Los lotes caducados se señalan, pero no se bloquean.

| Permiso en Pedidos | Puede |
|--------------------|-------|
| Gestionar | Crear, asignar y retirar lotes, preparar, entregar y cancelar |
| Ver | Consultar, preparar y entregar |

**Actualizar una base de datos existente sin perder datos**, desde `trazabilidad-api/database`:

```bash
set -a; source ./.env; set +a
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/001_pedidos.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/002_estados_pedido.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/003_gtin_producto.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/004_modulos_permisos.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/005_perfil_usuario.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/006_operario_usuario.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < migrations/007_clientes.sql
docker exec -i -e MYSQL_PWD="$DB_PASSWORD" trazabilidad_db mariadb -utrazabilidad_app traceability_lean < 02_procedures.sql
```

## Flujo de trabajo

- Trabaja cada parte en su propio IDE y abre solo su carpeta.
- Haz commits que afecten, siempre que sea posible, a una sola parte (backend o frontend) para que el historial sea fácil de leer.
- No subas contraseñas ni claves. Los valores reales de `application.properties` y los archivos `.env` deben quedar fuera del repositorio.

## Problemas frecuentes

- **Puerto ocupado:** cambia `server.port` en el backend o el puerto del servidor de desarrollo del frontend.
- **Error de conexión a la base de datos o `Could not resolve placeholder`:** comprueba que MariaDB está arrancada, que existe `database/.env` y que el directorio de trabajo del backend es `trazabilidad-api`.
- **Versión de Java incorrecta:** comprueba con `java -version` que tienes la 17.
- **`mariadb: command not found` en `update_db.sh`:** instala el cliente de MariaDB o ejecuta los `.sql` dentro del contenedor de Docker.

## Tests

```bash
# Backend: necesita Docker, levanta su propia MariaDB temporal
cd trazabilidad-api && ./gradlew test

# Frontend
cd GargantAppFront/gargantapp-front && npm test
```


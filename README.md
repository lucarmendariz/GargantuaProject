# GargantuaProject

> ⚠️ COMPLETAR: una frase que resuma el proyecto. Ejemplo: *Aplicación web para el seguimiento y la trazabilidad de [productos / lotes / equipos / pedidos] a lo largo de su ciclo de vida.*

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-orange)

# Gargantua

[Descripción breve del proyecto: qué hace la aplicación y para quién.]

Este repositorio contiene el backend y el frontend en el mismo lugar, pero **se desarrollan por separado**: cada parte tiene su carpeta, su entorno de desarrollo y su forma de arrancar.

## Estructura del repositorio

```
GargantuaProject/
├── trazabilidad-api/               Backend (API REST)
│   └── database/                   Scripts SQL y update_db.sh
└── GargantAppFront/
    └── gargantapp-front/           Frontend (Vue)
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

### 2. Base de datos

Con Docker:

```bash
docker run --name mariadb-gargantua -e MARIADB_ROOT_PASSWORD=<tu_clave> -e MARIADB_DATABASE=<nombre_bd> -p 3306:3306 -d mariadb:lts
```

Después, carga o actualiza las tablas con los scripts de `trazabilidad-api/database`:

```bash
cd trazabilidad-api/database
bash update_db.sh
```

> En Windows, ejecuta el script desde **Git Bash**, no desde PowerShell. Si da errores de `\r`, convierte el archivo a finales de línea LF.

### 3. Backend

1. Abre la carpeta `trazabilidad-api` en **IntelliJ IDEA** (no la raíz del repositorio).
2. Activa el procesamiento de anotaciones para Lombok: *Settings → Build, Execution, Deployment → Compiler → Annotation Processors → Enable annotation processing*.
3. Configura la conexión en `src/main/resources/application.properties`:

   ```properties
   spring.datasource.url=jdbc:mariadb://localhost:3306/<nombre_bd>
   spring.datasource.username=<usuario>
   spring.datasource.password=<contraseña>
   ```

4. Arranca la aplicación desde IntelliJ o por terminal:

   ```bash
   cd trazabilidad-api
   ./gradlew bootRun
   ```

   En Windows: `gradlew.bat bootRun`. La API queda disponible en `http://localhost:8080`.

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

## Flujo de trabajo

- Trabaja cada parte en su propio IDE y abre solo su carpeta.
- Haz commits que afecten, siempre que sea posible, a una sola parte (backend o frontend) para que el historial sea fácil de leer.
- No subas contraseñas ni claves. Los valores reales de `application.properties` y los archivos `.env` deben quedar fuera del repositorio.

## Problemas frecuentes

- **Puerto ocupado:** cambia `server.port` en el backend o el puerto del servidor de desarrollo del frontend.
- **Error de conexión a la base de datos:** comprueba que MariaDB está arrancada y que URL, usuario, contraseña y nombre de la base coinciden con `application.properties`.
- **Versión de Java incorrecta:** comprueba con `java -version` que tienes la 17.
- **`mariadb: command not found` en `update_db.sh`:** instala el cliente de MariaDB o ejecuta los `.sql` dentro del contenedor de Docker.

## Índice

- [Descripción](#descripción)
- [Funcionalidades](#funcionalidades)
- [Tecnologías](#tecnologías)
- [Estructura del repositorio](#estructura-del-repositorio)
- [Requisitos previos](#requisitos-previos)
- [Instalación y puesta en marcha](#instalación-y-puesta-en-marcha)
- [Variables de entorno](#variables-de-entorno)
- [Documentación de la API](#documentación-de-la-api)
- [Tests](#tests)
- [Hoja de ruta](#hoja-de-ruta)
- [Autoría](#autoría)

## Descripción

⚠️ COMPLETAR: explica en 3-5 líneas **qué problema resuelve**, **quién lo usa** y **qué se traza** (por ejemplo, qué entidades se siguen y qué eventos se registran).

El proyecto se divide en dos partes independientes:

- **`trazabilidad-api`**: API que gestiona los datos y la lógica de negocio.
- **`GargantAppFront/gargantapp-front`**: aplicación web que consume la API.

## Funcionalidades

⚠️ COMPLETAR: deja solo lo que **ya está implementado** y mueve el resto a la hoja de ruta.

- [ ] Autenticación y gestión de usuarios
- [ ] Roles y permisos
- [ ] Registro de [entidades principales]
- [ ] Historial de movimientos / eventos de cada elemento
- [ ] Búsqueda y filtrado
- [ ] Informes o exportación de datos

## Tecnologías

| Capa | Tecnología |
|------|------------|
| API | ⚠️ COMPLETAR (lenguaje y framework) |
| Base de datos | ⚠️ COMPLETAR |
| Frontend | ⚠️ COMPLETAR (framework, versión) |
| Autenticación | ⚠️ COMPLETAR (token, sesión, API Key…) |
| Otros | ⚠️ COMPLETAR (Docker, tests, linter…) |

## Estructura del repositorio

```
GargantuaProject/
├── GargantAppFront/
│   └── gargantapp-front/   # Frontend
└── trazabilidad-api/       # Backend / API
```

## Requisitos previos

⚠️ COMPLETAR con versiones concretas, por ejemplo:

- Node.js ≥ XX
- ⚠️ Runtime de la API (PHP / Python / Java / Node…) ≥ XX
- ⚠️ Base de datos (MySQL / PostgreSQL…) ≥ XX
- Git

## Instalación y puesta en marcha

### 1. Clonar el repositorio

```bash
git clone https://github.com/lucarmendariz/GargantuaProject.git
cd GargantuaProject
```

### 2. Arrancar la API

```bash
cd trazabilidad-api
# ⚠️ COMPLETAR: instalar dependencias
# ⚠️ COMPLETAR: copiar .env.example a .env y configurarlo
# ⚠️ COMPLETAR: crear la base de datos y ejecutar migraciones / importar el script SQL
# ⚠️ COMPLETAR: comando para arrancar el servidor
```

La API quedará disponible en `http://localhost:⚠️PUERTO`.

### 3. Arrancar el frontend

```bash
cd GargantAppFront/gargantapp-front
npm install
npm run dev
```

La aplicación quedará disponible en `http://localhost:⚠️PUERTO`.

## Variables de entorno

Nunca subas archivos `.env` con datos reales. Copia la plantilla y rellénala:

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| ⚠️ `DB_HOST` | Servidor de la base de datos | `localhost` |
| ⚠️ `DB_NAME` | Nombre de la base de datos | `gargantua` |
| ⚠️ `DB_USER` | Usuario de la base de datos | `root` |
| ⚠️ `DB_PASSWORD` | Contraseña | *(no subir al repo)* |
| ⚠️ `API_URL` (frontend) | URL base de la API | `http://localhost:8000/api` |

## Documentación de la API

⚠️ COMPLETAR con tus endpoints reales. Plantilla:

| Método | Ruta | Descripción | Auth |
|--------|------|-------------|------|
| POST | `/api/login` | Iniciar sesión | No |
| GET | `/api/⚠️recurso` | Listar elementos | Sí |
| GET | `/api/⚠️recurso/{id}` | Detalle de un elemento | Sí |
| POST | `/api/⚠️recurso` | Crear elemento | Sí |
| PUT | `/api/⚠️recurso/{id}` | Modificar elemento | Sí |
| DELETE | `/api/⚠️recurso/{id}` | Eliminar elemento | Admin |

## Tests

```bash
# ⚠️ COMPLETAR: comando para lanzar los tests de la API
# ⚠️ COMPLETAR: comando para lanzar los tests del frontend (si existen)
```

## Hoja de ruta

- [ ] Completar README con capturas de pantalla
- [ ] Añadir Docker para levantar todo con un solo comando
- [ ] Añadir tests automáticos
- [ ] Documentar la API (OpenAPI / Swagger)
- [ ] ⚠️ COMPLETAR: siguientes funcionalidades

## Autoría

Proyecto desarrollado por **⚠️ tu nombre** como ⚠️ proyecto de ciclo / trabajo personal.

⚠️ COMPLETAR: licencia (por ejemplo, MIT) o indicar que es de uso académico.

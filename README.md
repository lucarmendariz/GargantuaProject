# GargantuaProject

> ⚠️ COMPLETAR: una frase que resuma el proyecto. Ejemplo: *Aplicación web para el seguimiento y la trazabilidad de [productos / lotes / equipos / pedidos] a lo largo de su ciclo de vida.*

![Estado](https://img.shields.io/badge/estado-en%20desarrollo-orange)

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

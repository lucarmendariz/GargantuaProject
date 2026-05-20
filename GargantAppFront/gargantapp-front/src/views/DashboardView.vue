<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';

const user = ref(null);
const lotInput = ref('');
const lotData = ref(null);
const errorMsg = ref('');
const router = useRouter();

// Variables para el calendario dinámico
const currentDayName = ref('');
const currentDayNumber = ref('');
const currentMonthYear = ref('');

onMounted(() => {
  // 1. Verificar sesión
  const savedUser = localStorage.getItem('user');
  if (!savedUser) {
    router.push('/');
  } else {
    user.value = JSON.parse(savedUser);
  }

  // 2. Lógica del calendario nativo
  const hoy = new Date();

  // Extraemos el día de la semana (ej: Miércoles) y capitalizamos la primera letra
  const weekday = hoy.toLocaleDateString('es-ES', { weekday: 'long' });
  currentDayName.value = weekday.charAt(0).toUpperCase() + weekday.slice(1);

  // Número del día (ej: 20)
  currentDayNumber.value = hoy.getDate();

  // Mes y Año (ej: Mayo, 2026)
  const monthYear = hoy.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });
  currentMonthYear.value = monthYear.charAt(0).toUpperCase() + monthYear.slice(1);
});

// Funciones de navegación para la botonera
const navigateTo = (path) => {
  // De momento te redirigirá a estas rutas. Cuando creemos las vistas, el router las pillará automáticamente.
  router.push(path);
};

const searchLot = async () => {
  try {
    errorMsg.value = '';
    lotData.value = null;
    lotData.value = await apiClient.get(`/trazabilidad/lotes/${lotInput.value}/status`);
  } catch (err) {
    errorMsg.value = 'El lote escaneado no existe o no tiene historial registrado.';
  }
};

const logout = () => {
  localStorage.removeItem('user');
  router.push('/');
};
</script>

<template>
  <div v-if="user" class="dashboard">
    <header>
      <div class="brand-title">
        <h1>🏭 Panel Central</h1>
        <p>Planta Gargantúa Tortillas</p>
      </div>
      <div class="user-badge">
        <span>👷‍♂️ <strong>{{ user.name }}</strong></span>
        <button @click="logout" class="btn-logout">Salir</button>
      </div>
    </header>

    <main class="dashboard-grid">

      <section class="control-panel">

        <div class="calendar-widget">
          <div class="calendar-header">{{ currentDayName }}</div>
          <div class="calendar-body">
            <span class="day-number">{{ currentDayNumber }}</span>
            <span class="month-year">{{ currentMonthYear }}</span>
          </div>
        </div>

        <div class="operations-menu">
          <button @click="navigateTo('/inventario')" class="menu-btn btn-inventario">
            <span class="icon">📊</span>
            <span class="text">Inventario Stock</span>
          </button>

          <button @click="navigateTo('/historial-global')" class="menu-btn btn-historial">
            <span class="icon">📜</span>
            <span class="text">Historial Visibilizar</span>
          </button>

          <button @click="navigateTo('/produccion')" class="menu-btn btn-produccion">
            <span class="icon">🏗️</span>
            <span class="text">Producción Planta</span>
          </button>
        </div>

      </section>

      <section class="search-section">
        <h3>Simulador de Cámara OCR de Planta</h3>
        <div class="search-bar">
          <input v-model="lotInput" placeholder="Introduce o escanea un número de lote..." @keyup.enter="searchLot" />
          <button @click="searchLot">Escanear Lote</button>
        </div>
        <p v-if="errorMsg" class="error-text">{{ errorMsg }}</p>
      </section>

      <section v-if="lotData" class="result-card">
        <h3>📋 Información de Trazabilidad del Lote</h3>
        <div class="table-responsive">
          <table class="info-table">
            <tbody>
              <tr>
                <th>Artículo Maestro:</th>
                <td>{{ lotData.productName }} (SKU: {{ lotData.productSku }})</td>
              </tr>
              <tr>
                <th>Código de Lote:</th>
                <td><strong>{{ lotData.lotNumber }}</strong></td>
              </tr>
              <tr>
                <th>Cantidad Inicial:</th>
                <td>{{ lotData.initialQuantity }} {{ lotData.measureUnit }}</td>
              </tr>
              <tr>
                <th>Estado Actual:</th>
                <td><span class="status-badge">{{ lotData.currentState }}</span></td>
              </tr>
              <tr>
                <th>Última Actualización:</th>
                <td>{{ lotData.lastUpdate }}</td>
              </tr>
              <tr>
                <th>Último Operario:</th>
                <td>{{ lotData.lastOperator }}</td>
              </tr>
              <tr>
                <th>Lectura OCR en Bruto:</th>
                <td><code>{{ lotData.ocrRawText }}</code></td>
              </tr>
              <tr>
                <th>Observaciones de Calidad:</th>
                <td>{{ lotData.lastObservations }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

    </main>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  height: 100%;
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--gargantua-gold);
  padding: 15px 25px;
  color: var(--gargantua-dark);
}

.brand-title h1 {
  margin: 0;
  font-size: 1.4rem;
}

.brand-title p {
  margin: 0;
  font-size: 0.8rem;
  opacity: 0.85;
  font-weight: bold;
}

.user-badge {
  display: flex;
  align-items: center;
  gap: 15px;
}

.btn-logout {
  background: var(--gargantua-danger);
  color: var(--gargantua-white);
  border: none;
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-weight: bold;
  -webkit-appearance: none;
  appearance: none;
}

.dashboard-grid {
  padding: 25px;
  display: flex;
  flex-direction: column;
  gap: 25px;
}

/* PANEL SUPERIOR: Organización en fila/grid */
.control-panel {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

/* WIDGET CALENDARIO TRADICIONAL */
.calendar-widget {
  background: var(--gargantua-white);
  border: 2px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  width: 150px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  text-align: center;
  box-shadow: 0 4px 10px rgba(62, 39, 18, 0.05);
  flex-shrink: 0;
}

.calendar-header {
  background: var(--gargantua-orange);
  color: var(--gargantua-white);
  padding: 6px;
  font-weight: bold;
  font-size: 0.9rem;
  letter-spacing: 0.5px;
}

.calendar-body {
  background: var(--gargantua-white);
  padding: 10px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex-grow: 1;
}

.day-number {
  font-size: 2.2rem;
  font-weight: 900;
  color: var(--gargantua-dark);
  line-height: 1;
  margin-bottom: 2px;
}

.month-year {
  font-size: 0.75rem;
  color: var(--gargantua-gray);
  font-weight: bold;
}

/* BOTONERA DE OPERACIONES */
.operations-menu {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  flex-grow: 1;
}

.menu-btn {
  background: var(--gargantua-white);
  border: 2px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  padding: 15px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: var(--transition);
  -webkit-appearance: none;
  appearance: none;
}

.menu-btn .icon {
  font-size: 1.8rem;
}

.menu-btn .text {
  font-weight: bold;
  color: var(--gargantua-dark);
  font-size: 0.9rem;
  text-align: center;
}

/* Efectos Hover personalizados para la fábrica */
.menu-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(230, 126, 34, 0.12);
  border-color: var(--gargantua-gold);
  background-color: #fffdf9;
}

/* SECCIÓN BUSCADOR / OCR */
.search-section {
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  padding: 20px;
  border-radius: var(--radius-md);
}

.search-section h3 {
  margin-top: 0;
  color: var(--gargantua-dark);
  margin-bottom: 12px;
}

.search-bar {
  display: flex;
  gap: 12px;
}

.search-bar input {
  flex: 1;
  padding: 12px;
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-sm);
  font-size: 16px;
  background-color: var(--gargantua-cream);
  color: var(--gargantua-dark);
  -webkit-appearance: none;
  appearance: none;
}

.search-bar button {
  padding: 12px 20px;
  background: var(--gargantua-orange);
  color: var(--gargantua-white);
  border: none;
  border-radius: var(--radius-sm);
  font-weight: bold;
  cursor: pointer;
  transition: var(--transition);
  -webkit-appearance: none;
  appearance: none;
}

.search-bar button:hover {
  background: var(--gargantua-dark);
}

.error-text {
  color: var(--gargantua-danger);
  font-weight: bold;
  margin-top: 10px;
  margin-bottom: 0;
}

/* SECCIÓN RESULTADOS */
.result-card {
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  padding: 20px;
  border-radius: var(--radius-md);
}

.result-card h3 {
  margin-top: 0;
  color: var(--gargantua-dark);
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table th,
.info-table td {
  text-align: left;
  padding: 12px;
  border-bottom: 1px solid var(--gargantua-border);
  font-size: 0.95rem;
}

.info-table th {
  background: var(--gargantua-cream);
  width: 30%;
  color: var(--gargantua-dark);
}

.status-badge {
  background: var(--gargantua-gold);
  color: var(--gargantua-dark);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: bold;
}

code {
  background: var(--gargantua-cream);
  padding: 3px 6px;
  border-radius: var(--radius-sm);
  color: var(--gargantua-orange);
  font-family: monospace;
  border: 1px solid var(--gargantua-border);
}

/* RESPONSIVIDAD TOTAL (MÓVIL) */
@media (max-width: 768px) {
  header {
    flex-direction: column;
    gap: 10px;
    text-align: center;
    padding: 15px;
  }

  .control-panel {
    flex-direction: column;
    align-items: center;
  }

  .calendar-widget {
    width: 100%;
    max-width: 220px;
  }

  .operations-menu {
    grid-template-columns: 1fr;
    width: 100%;
    gap: 10px;
  }

  .menu-btn {
    flex-direction: row;
    justify-content: flex-start;
    padding: 12px 20px;
    gap: 15px;
  }

  .menu-btn .icon {
    font-size: 1.4rem;
  }

  .search-bar {
    flex-direction: column;
  }

  .info-table th {
    width: 40%;
    font-size: 0.85rem;
  }

  .info-table td {
    font-size: 0.85rem;
  }
}
</style>
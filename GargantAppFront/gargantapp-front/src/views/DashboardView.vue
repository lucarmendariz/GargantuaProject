<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';

const user = ref(null);
const lotInput = ref('');
const lotData = ref(null);
const errorMsg = ref('');
const router = useRouter();

// --- NUEVO ESTADO PARA EL CALENDARIO INTERACTIVO ---
const currentDate = ref(new Date());
const selectedDate = ref(new Date());
const weekDays = ['Do', 'Lu', 'Ma', 'Mi', 'Ju', 'Vi', 'Sá'];
const months = [
  'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
  'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'
];

onMounted(() => {
  // Verificar sesión
  const savedUser = localStorage.getItem('user');
  if (!savedUser) {
    router.push('/');
  } else {
    user.value = JSON.parse(savedUser);
  }
});

// --- LÓGICA DEL MOTOR DEL CALENDARIO ---
const currentYear = computed(() => currentDate.value.getFullYear());
const currentMonthName = computed(() => months[currentDate.value.getMonth()]);

// Genera la matriz de días que se pintará en el grid del calendario
const calendarDays = computed(() => {
  const year = currentDate.value.getFullYear();
  const month = currentDate.value.getMonth();

  // Primer día del mes actual y cuántos días tiene
  const firstDayIndex = new Date(year, month, 1).getDay();
  const totalDays = new Date(year, month + 1, 0).getDate();

  // Días del mes anterior (para rellenar los huecos iniciales de la cuadrícula)
  const prevTotalDays = new Date(year, month, 0).getDate();

  const daysArray = [];

  // Huecos del mes anterior
  for (let i = firstDayIndex - 1; i >= 0; i--) {
    daysArray.push({
      day: prevTotalDays - i,
      isCurrentMonth: false,
      date: new Date(year, month - 1, prevTotalDays - i)
    });
  }

  // Días del mes actual
  for (let i = 1; i <= totalDays; i++) {
    daysArray.push({
      day: i,
      isCurrentMonth: true,
      date: new Date(year, month, i)
    });
  }

  // Huecos del mes siguiente para completar la última semana completa si es necesario
  const remainingCells = 42 - daysArray.length; // Estándar de 6 filas (7x6 = 42)
  for (let i = 1; i <= remainingCells; i++) {
    daysArray.push({
      day: i,
      isCurrentMonth: false,
      date: new Date(year, month + 1, i)
    });
  }

  return daysArray;
});

// Navegación del calendario
const prevMonth = () => {
  currentDate.value = new Date(currentYear.value, currentDate.value.getMonth() - 1, 1);
};

const nextMonth = () => {
  currentDate.value = new Date(currentYear.value, currentDate.value.getMonth() + 1, 1);
};

const selectDay = (dayObj) => {
  selectedDate.value = dayObj.date;
  // Si seleccionas un día del mes anterior o siguiente, movemos el calendario a ese mes
  if (!dayObj.isCurrentMonth) {
    currentDate.value = new Date(dayObj.date.getFullYear(), dayObj.date.getMonth(), 1);
  }
};

// Comprobar si un día de la cuadrícula es "Hoy"
const isToday = (date) => {
  const today = new Date();
  return date.getDate() === today.getDate() &&
         date.getMonth() === today.getMonth() &&
         date.getFullYear() === today.getFullYear();
};

// Comprobar si un día es el seleccionado por el operario
const isSelected = (date) => {
  return date.getDate() === selectedDate.value.getDate() &&
         date.getMonth() === selectedDate.value.getMonth() &&
         date.getFullYear() === selectedDate.value.getFullYear();
};

// --- RESTO DE FUNCIONES ---
const navigateTo = (path) => {
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

        <div class="full-calendar-widget">
          <div class="cal-nav">
            <button @click="prevMonth" class="cal-nav-btn">◀</button>
            <span class="cal-title"><strong>{{ currentMonthName }}</strong> {{ currentYear }}</span>
            <button @click="nextMonth" class="cal-nav-btn">▶</button>
          </div>
          
          <div class="cal-weekdays">
            <span v-for="day in weekDays" :key="day" class="weekday-cell">{{ day }}</span>
          </div>
          
          <div class="cal-days-grid">
            <button 
              v-for="(dayObj, idx) in calendarDays" 
              :key="idx"
              @click="selectDay(dayObj)"
              class="day-cell"
              :class="{
                'outside-month': !dayObj.isCurrentMonth,
                'today-cell': isToday(dayObj.date),
                'selected-cell': isSelected(dayObj.date)
              }"
            >
              {{ dayObj.day }}
            </button>
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

/* PANEL SUPERIOR */
.control-panel {
  display: flex;
  gap: 20px;
  align-items: stretch;
}

/* 🎨 NUEVOS ESTILOS PARA EL CALENDARIO COMPLETO MATRICIAL */
.full-calendar-widget {
  background: var(--gargantua-white);
  border: 2px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  width: 280px;
  display: flex;
  flex-direction: column;
  padding: 12px;
  box-shadow: 0 4px 10px rgba(62, 39, 18, 0.05);
  flex-shrink: 0;
  user-select: none;
}

.cal-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.cal-nav-btn {
  background: var(--gargantua-cream);
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  padding: 4px 10px;
  font-size: 0.8rem;
  font-weight: bold;
  transition: var(--transition);
}

.cal-nav-btn:hover {
  background: var(--gargantua-gold);
}

.cal-title {
  color: var(--gargantua-dark);
  font-size: 0.95rem;
}

.cal-weekdays {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  text-align: center;
  font-weight: bold;
  font-size: 0.75rem;
  color: var(--gargantua-orange);
  margin-bottom: 6px;
}

.cal-days-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-cell {
  background: var(--gargantua-cream);
  border: 1px solid transparent;
  border-radius: var(--radius-sm);
  padding: 6px 0;
  font-size: 0.85rem;
  font-weight: bold;
  color: var(--gargantua-dark);
  cursor: pointer;
  text-align: center;
  transition: var(--transition);
  -webkit-appearance: none;
  appearance: none;
}

.day-cell:hover {
  background: var(--gargantua-gold);
}

.outside-month {
  color: #b9bec5;
  background: transparent;
}

.today-cell {
  border: 2px solid var(--gargantua-orange) !important;
}

.selected-cell {
  background: var(--gargantua-dark) !important;
  color: var(--gargantua-gold) !important;
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

/* RESPONSIVIDAD TOTAL */
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

  .full-calendar-widget {
    width: 100%;
    max-width: 320px;
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
<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { unitLabel } from '../utils/units';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { can, clearSession, getUser, hasRole } from '../utils/session';
import { dayRange, formatDayLong, formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { loadLayout, moveItem, resetLayout, saveLayout } from '../utils/dashboardLayout';
import ModuleIcon from '../components/ModuleIcon.vue';
import UserAvatar from '../components/UserAvatar.vue';
import CameraScanner from '../components/CameraScanner.vue';
import { parseLabel } from '../utils/labelParser';

// Accesos del panel. El calendario es otro bloque más y también se puede mover.
// Cada módulo se ve si el usuario tiene acceso (module) o el rol indicado (roles, para los paneles).
const MODULES = {
  pedidos: { label: 'Pedidos', path: '/pedidos', module: 'PEDIDOS' },
  inventario: { label: 'Inventario Stock', path: '/inventario', module: 'INVENTARIO' },
  lotes: { label: 'Gestión de Lotes', path: '/lotes', module: 'LOTES' },
  historial: { label: 'Historial Visibilizar', path: '/historial-global', module: 'HISTORIAL' },
  produccion: { label: 'Producción Planta', path: '/produccion', module: 'PRODUCCION' },
  administracion: { label: 'Administración', path: '/administracion', roles: ['ADMIN', 'SUPERADMIN'] },
  plataforma: { label: 'Plataforma', path: '/plataforma', roles: ['SUPERADMIN'] },
};
const blockLabel = (id) => (id === 'calendario' ? 'Calendario' : MODULES[id].label);
const isVisible = (id) => id === 'calendario'
  || (MODULES[id].module ? can(MODULES[id].module) : hasRole(...MODULES[id].roles));
const seesOrders = can('PEDIDOS');

// El guardián del router ya garantiza que hay sesión
const user = getUser();
// La foto no viaja en la sesión (puede pesar): se pide aparte para la cabecera
const avatar = ref(null);
onMounted(async () => {
  try {
    avatar.value = (await apiClient.get('/me/perfil')).avatar;
  } catch {
    // Sin foto: se ven las iniciales
  }
});
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

// --- ORDEN PERSONALIZABLE DE LOS BLOQUES ---
const layoutOwner = () => user?.username || user?.name || 'anonimo';
const layout = ref(loadLayout(layoutOwner()).filter(isVisible));
const editingLayout = ref(false);
const dragIndex = ref(null);
const dropIndex = ref(null);
const layoutStatus = ref('');

const applyLayout = (next, message) => {
  layout.value = next;
  saveLayout(layoutOwner(), next);
  layoutStatus.value = message;
};

const moveBlock = (from, to) => {
  if (to < 0 || to >= layout.value.length || from === to) return;
  const id = layout.value[from];
  applyLayout(moveItem(layout.value, from, to), `${blockLabel(id)} movido a la posición ${to + 1} de ${layout.value.length}.`);
};

const onDragStart = (index, event) => {
  dragIndex.value = index;
  event.dataTransfer.effectAllowed = 'move';
  // Firefox necesita algún dato para empezar a arrastrar
  event.dataTransfer.setData('text/plain', layout.value[index]);
};

const onDragOver = (index) => {
  if (dragIndex.value !== null) dropIndex.value = index;
};

const onDrop = (index) => {
  if (dragIndex.value !== null) moveBlock(dragIndex.value, index);
  onDragEnd();
};

const onDragEnd = () => {
  dragIndex.value = null;
  dropIndex.value = null;
};

const restoreLayout = () => {
  layout.value = resetLayout(layoutOwner()).filter(isVisible);
  layoutStatus.value = 'Orden original restablecido.';
};

const toggleEditing = () => {
  editingLayout.value = !editingLayout.value;
  layoutStatus.value = '';
};

const openModule = (id) => {
  // Mientras se ordena, pulsar un módulo no navega
  if (!editingLayout.value) router.push(MODULES[id].path);
};

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

// Navegación del calendario: step -1 = mes anterior, +1 = mes siguiente
const shiftMonth = (step) => {
  currentDate.value = new Date(currentYear.value, currentDate.value.getMonth() + step, 1);
};

const selectDay = (dayObj) => {
  selectedDate.value = dayObj.date;
  // Si seleccionas un día del mes anterior o siguiente, movemos el calendario a ese mes
  if (!dayObj.isCurrentMonth) {
    currentDate.value = new Date(dayObj.date.getFullYear(), dayObj.date.getMonth(), 1);
  }
};

const sameDay = (a, b) => a.toDateString() === b.toDateString();

// --- PEDIDOS Y PRODUCTOS EN USO DEL DIA SELECCIONADO ---
const dayOrders = ref([]);
const inUseLines = ref([]);
const dayLoading = ref(true);
const dayError = ref('');
const selectedDayLabel = computed(() => formatDayLong(selectedDate.value));
let dayRequest = 0;

const loadDay = async () => {
  // Si se pulsan varios días seguidos, solo cuenta la última respuesta
  const request = ++dayRequest;
  dayLoading.value = true;
  dayError.value = '';
  const { from, to } = dayRange(selectedDate.value);
  try {
    const [orders, lines] = await Promise.all([
      apiClient.get(`/pedidos?desde=${from}&hasta=${to}`),
      apiClient.get(`/pedidos/en-uso?desde=${from}&hasta=${to}`)
    ]);
    if (request !== dayRequest) return;
    dayOrders.value = orders;
    inUseLines.value = lines;
  } catch (error) {
    const message = await errorMessage(error, 'No se pudieron cargar los pedidos del día.');
    if (request !== dayRequest) return;
    dayOrders.value = [];
    inUseLines.value = [];
    dayError.value = message;
  } finally {
    if (request === dayRequest) dayLoading.value = false;
  }
};

// Sin acceso a Pedidos no se piden los del día (la API respondería 403)
if (seesOrders) {
  onMounted(loadDay);
  watch(selectedDate, loadDay);
}

// --- RESTO DE FUNCIONES ---
const searchLot = async () => {
  try {
    errorMsg.value = '';
    lotData.value = null;
    lotData.value = await apiClient.get(`/trazabilidad/lotes/${encodeURIComponent(lotInput.value.trim())}/status`);
  } catch (err) {
    errorMsg.value = err?.status === 404
      ? 'El lote escaneado no existe o no tiene historial registrado.'
      : await errorMessage(err, 'No se pudo consultar el lote.');
  }
};

// Cámara real: lee el código o el texto de la etiqueta y busca el lote
const scannerOpen = ref(false);
const onScan = ({ text, source }) => {
  scannerOpen.value = false;
  const label = parseLabel(text, { source });
  if (!label.lotNumber) {
    lotData.value = null;
    errorMsg.value = `No se ha encontrado un número de lote en lo leído: "${label.raw.slice(0, 80)}"`;
    return;
  }
  lotInput.value = label.lotNumber;
  searchLot();
};

const logout = () => {
  clearSession();
  router.push('/');
};
</script>

<template>
  <div v-if="user" class="content">
    <header class="card-header">
      <div class="brand-title">
        <h1>Panel Central</h1>
        <p>Planta Gargantúa Tortillas</p>
      </div>
      <div class="user-badge">
        <button @click="router.push('/cuenta')" class="profile-link" title="Mi perfil" aria-label="Mi perfil">
          <UserAvatar :src="avatar" :name="user.name" :size="34" />
          <strong>{{ user.name }}</strong>
        </button>
        <button @click="logout" class="btn-logout">Salir</button>
      </div>
    </header>

    <main class="card-body">

      <section class="control-panel" aria-label="Accesos del panel">
        <div class="layout-toolbar">
          <p v-if="editingLayout" class="item-meta layout-help">
            Arrastra los bloques o usa las flechas para ordenarlos. El orden se guarda para tu usuario.
          </p>
          <div class="layout-actions">
            <button v-if="editingLayout" @click="restoreLayout" class="btn-secondary">Restablecer orden</button>
            <button @click="toggleEditing" :class="editingLayout ? '' : 'btn-secondary'" :aria-pressed="editingLayout">
              {{ editingLayout ? 'Listo' : 'Personalizar panel' }}
            </button>
          </div>
        </div>
        <p class="visually-hidden" role="status" aria-live="polite">{{ layoutStatus }}</p>

        <div class="dashboard-grid" :class="{ editing: editingLayout }">
          <div
            v-for="(id, index) in layout"
            :key="id"
            class="dashboard-block"
            :class="[`block-${id}`, { dragging: dragIndex === index, 'drop-target': dropIndex === index && dragIndex !== index }]"
            :draggable="editingLayout"
            @dragstart="onDragStart(index, $event)"
            @dragover.prevent="onDragOver(index)"
            @dragleave="dropIndex = null"
            @drop.prevent="onDrop(index)"
            @dragend="onDragEnd"
          >
            <div v-if="editingLayout" class="block-controls">
              <span class="drag-handle" aria-hidden="true">⠿</span>
              <ModuleIcon :name="id" :size="16" class="block-icon" />
              <span class="block-name">{{ blockLabel(id) }}</span>
              <button @click="moveBlock(index, index - 1)" :disabled="index === 0" class="move-btn"
                :aria-label="`Mover ${blockLabel(id)} antes`" title="Mover antes">←</button>
              <button @click="moveBlock(index, index + 1)" :disabled="index === layout.length - 1" class="move-btn"
                :aria-label="`Mover ${blockLabel(id)} después`" title="Mover después">→</button>
            </div>

            <div v-if="id === 'calendario'" class="full-calendar-widget panel">
              <div class="cal-nav">
                <button @click="shiftMonth(-1)" class="cal-nav-btn">Anterior</button>
                <span class="cal-title"><strong>{{ currentMonthName }}</strong> {{ currentYear }}</span>
                <button @click="shiftMonth(1)" class="cal-nav-btn">Siguiente</button>
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
                    'today-cell': sameDay(dayObj.date, new Date()),
                    'selected-cell': sameDay(dayObj.date, selectedDate)
                  }"
                >
                  {{ dayObj.day }}
                </button>
              </div>
            </div>

            <button v-else @click="openModule(id)" class="menu-btn" :class="`btn-${id}`" :tabindex="editingLayout ? -1 : 0">
              <ModuleIcon :name="id" class="menu-icon" />
              <span class="text">{{ MODULES[id].label }}</span>
            </button>
          </div>
        </div>
      </section>

      <section v-if="seesOrders" class="panel day-section">
        <h3>Pedidos del {{ selectedDayLabel }}</h3>
        <p v-if="dayLoading" class="empty-state">Cargando pedidos...</p>
        <p v-else-if="dayError" class="msg-error">{{ dayError }}</p>
        <p v-else-if="dayOrders.length === 0" class="empty-state">No hay pedidos programados este día.</p>
        <div v-else class="item-list">
          <RouterLink v-for="order in dayOrders" :key="order.orderId" :to="`/pedidos/${order.orderId}`" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ order.orderNumber }}</span>
              <span class="status" :class="`status-${order.status}`">{{ order.status }}</span>
            </div>
            <span class="item-meta">{{ order.customerName || 'Sin cliente' }} · {{ order.lineCount }} lote(s)</span>
          </RouterLink>
        </div>
      </section>

      <section v-if="seesOrders && !dayError" class="panel day-section">
        <h3>Productos en uso</h3>
        <p class="item-meta section-help">Lotes reservados en los pedidos abiertos de este día.</p>
        <p v-if="dayLoading" class="empty-state">Cargando productos...</p>
        <p v-else-if="inUseLines.length === 0" class="empty-state">No hay productos en uso este día.</p>
        <div v-else class="item-list">
          <div v-for="line in inUseLines" :key="line.lineId" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ line.productName }}</span>
              <span v-if="isExpired(line.expirationDate)" class="status status-CADUCADO">CADUCADO</span>
            </div>
            <RouterLink :to="`/lotes/${line.lotId}/trazabilidad`">Lote {{ line.lotNumber }}</RouterLink>
            <RouterLink :to="`/pedidos/${line.orderId}`">Pedido {{ line.orderNumber }}</RouterLink>
            <span class="item-meta">En uso: {{ line.reservedQuantity }} {{ unitLabel(line.measureUnit) }}</span>
            <span class="item-meta">Stock del lote: {{ line.currentStock }} · Disponible: {{ line.availableQuantity }}</span>
            <span class="item-meta">Reservado: {{ formatEpochDateTime(line.reservedAt) }} · Caducidad: {{ formatEpochDate(line.expirationDate) }}</span>
          </div>
        </div>
      </section>

      <section class="search-section panel">
        <h3>Cámara OCR de Planta</h3>
        <div class="input-row">
          <input v-model="lotInput" aria-label="Número de lote" placeholder="Introduce o escanea un número de lote..." @keyup.enter="searchLot" />
          <button @click="searchLot">Buscar Lote</button>
        </div>
        <button @click="scannerOpen = true" class="btn-secondary scan-camera-btn">Escanear con la cámara</button>
        <p v-if="errorMsg" class="msg-error">{{ errorMsg }}</p>
        <CameraScanner v-if="scannerOpen" title="Escanear lote" @result="onScan" @close="scannerOpen = false" />
      </section>

      <section v-if="lotData" class="result-card panel">
        <h3>Información de Trazabilidad del Lote</h3>
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
                <td>{{ lotData.initialQuantity }} {{ unitLabel(lotData.measureUnit) }}</td>
              </tr>
              <tr>
                <th>Stock Actual:</th>
                <td>{{ lotData.currentStock }} {{ unitLabel(lotData.measureUnit) }}</td>
              </tr>
              <tr>
                <th>Disponible:</th>
                <td>{{ lotData.availableQuantity }} {{ unitLabel(lotData.measureUnit) }} (reservado: {{ lotData.reservedQuantity }} {{ unitLabel(lotData.measureUnit) }})</td>
              </tr>
              <tr>
                <th>Estado Actual:</th>
                <td><span class="status-badge">{{ lotData.currentState || 'Sin movimientos' }}</span></td>
              </tr>
              <tr>
                <th>Última Actualización:</th>
                <td>{{ formatEpochDateTime(lotData.lastUpdate) }}</td>
              </tr>
              <tr>
                <th>Último Operario:</th>
                <td>{{ lotData.lastOperator || 'N/A' }}</td>
              </tr>
              <tr>
                <th>Lectura OCR en Bruto:</th>
                <td><code v-if="lotData.ocrRawText">{{ lotData.ocrRawText }}</code><template v-else>N/A</template></td>
              </tr>
              <tr>
                <th>Observaciones de Calidad:</th>
                <td>{{ lotData.lastObservations || 'N/A' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <RouterLink class="trace-link" :to="`/lotes/${lotData.lotId}/trazabilidad`">Ver trazabilidad completa del lote</RouterLink>
      </section>

    </main>
  </div>
</template>

<style scoped>
.brand-title p {
  color: var(--gargantua-gold);
  font-size: 0.85rem;
  font-weight: 600;
}

.user-badge {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 16px;
  min-width: 0;
}

/* El nombre del usuario conectado abre Mi perfil */
.profile-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: var(--control-height);
  padding: 4px 12px 4px 4px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 999px;
  color: inherit;
}

.profile-link:hover,
.profile-link:focus-visible {
  background: rgba(255, 255, 255, 0.1);
  border-color: var(--gargantua-gold);
}

.btn-logout {
  background: transparent;
  border-color: var(--gargantua-gold);
  color: var(--gargantua-cream);
}

.btn-logout:hover {
  background: var(--gargantua-gold);
  color: var(--gargantua-dark);
}

.control-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Barra para personalizar el orden */
.layout-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 8px 16px;
}

.layout-help {
  flex: 1 1 260px;
  margin: 0;
}

.layout-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}

/* Cuadricula de bloques: el calendario ocupa dos columnas. "dense" rellena los
   huecos que deja al moverlo, para que el panel no quede con espacios vacios. */
.dashboard-grid {
  display: grid;
  gap: 12px;
  grid-auto-flow: row dense;
  grid-auto-rows: minmax(72px, auto);
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.dashboard-block > .menu-btn,
.dashboard-block > .panel {
  flex: 1;
}

.block-calendario {
  grid-column: 1 / -1;
}

@media (min-width: 860px) {
  .dashboard-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .block-calendario {
    grid-column: span 2;
    grid-row: span 3;
  }
}

/* Modo personalizar */
.dashboard-grid.editing .dashboard-block {
  padding: 6px;
  border: 2px dashed var(--gargantua-border);
  border-radius: var(--radius-md);
  cursor: grab;
}

.dashboard-grid.editing .menu-btn {
  pointer-events: none;
}

.dashboard-block.dragging {
  opacity: 0.4;
}

.dashboard-block.drop-target {
  border-color: var(--gargantua-orange) !important;
  background: var(--gargantua-cream);
}

.block-controls {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.drag-handle {
  color: var(--gargantua-gray);
  font-size: 1.1rem;
  line-height: 1;
}

.block-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  color: var(--gargantua-gray);
  font-size: 0.8rem;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.move-btn {
  min-height: 32px;
  padding: 2px 10px;
  background: var(--gargantua-white);
  border-color: var(--gargantua-border);
  color: var(--gargantua-dark);
}

.move-btn:hover:not(:disabled) {
  background: var(--gargantua-cream);
  border-color: var(--gargantua-orange);
}

.move-btn:disabled {
  cursor: not-allowed;
  opacity: 0.4;
}

/* Calendario */
.full-calendar-widget {
  user-select: none;
}

.cal-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
}

.cal-nav-btn {
  padding: 4px 10px;
  background: var(--gargantua-cream);
  border-color: var(--gargantua-border);
  color: var(--gargantua-dark);
  font-size: 0.85rem;
}

.cal-nav-btn:hover {
  background: var(--gargantua-gold);
}

.cal-title {
  color: var(--gargantua-dark);
  text-align: center;
}

/* En las pantallas mas pequenas el mes pasa encima de los botones */
@media (max-width: 340px) {
  .cal-nav {
    flex-wrap: wrap;
  }

  .cal-title {
    flex: 1 1 100%;
    order: -1;
  }
}

.cal-weekdays,
.cal-days-grid {
  display: grid;
  gap: 4px;
  grid-template-columns: repeat(7, minmax(0, 1fr));
}

.cal-weekdays {
  margin-bottom: 6px;
  color: var(--gargantua-orange);
  font-size: 0.8rem;
  font-weight: 700;
  text-align: center;
}

.day-cell {
  min-height: 40px;
  padding: 0;
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
  font-size: 0.875rem;
}

.day-cell:hover {
  background: var(--gargantua-gold);
}

.outside-month {
  background: transparent;
  color: #8a7d6e;
  font-weight: 400;
}

.today-cell {
  border: 2px solid var(--gargantua-orange);
}

.selected-cell,
.selected-cell:hover {
  background: var(--gargantua-dark);
  color: var(--gargantua-gold);
}

/* Accesos a las secciones: icono encima del nombre */
.menu-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-height: 72px;
  padding: 16px;
  background: var(--gargantua-white);
  border-color: var(--gargantua-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  color: var(--gargantua-dark);
}

.menu-btn:hover {
  background: var(--gargantua-white);
  border-color: var(--gargantua-orange);
  color: var(--gargantua-orange-dark);
}

.menu-icon {
  color: var(--gargantua-orange);
  transition: transform var(--transition);
}

.menu-btn:hover .menu-icon {
  color: var(--gargantua-orange-dark);
  transform: translateY(-2px);
}

.block-icon {
  color: var(--gargantua-orange);
}

@media (prefers-reduced-motion: reduce) {
  .menu-icon {
    transition: none;
  }
}

.day-section h3 {
  margin-bottom: 12px;
}

.day-section h3::first-letter {
  text-transform: uppercase;
}

.section-help {
  margin-top: -8px;
  margin-bottom: 12px;
}

.trace-link {
  display: inline-block;
  margin-top: 14px;
}

.scan-camera-btn {
  margin-top: 10px;
}

.search-section h3,
.result-card h3 {
  margin-bottom: 12px;
}

.msg-error {
  margin-top: 12px;
}

/* Datos del lote */
.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table th,
.info-table td {
  padding: 12px;
  border-bottom: 1px solid var(--gargantua-border);
  font-size: 0.95rem;
  overflow-wrap: anywhere;
  text-align: left;
}

.info-table tr:last-child th,
.info-table tr:last-child td {
  border-bottom: 0;
}

.info-table th {
  width: 30%;
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
  font-weight: 600;
}

.status-badge {
  padding: 3px 10px;
  background: var(--gargantua-gold);
  border-radius: var(--radius-sm);
  color: var(--gargantua-dark);
  font-size: 0.8rem;
  font-weight: 700;
}

.info-table code {
  color: var(--gargantua-orange-dark);
}

/* En el movil, cada dato ocupa su propia linea */
@media (max-width: 560px) {
  .info-table,
  .info-table tbody,
  .info-table tr,
  .info-table th,
  .info-table td {
    display: block;
    width: 100%;
  }

  .info-table tr {
    padding: 10px 0;
    border-bottom: 1px solid var(--gargantua-border);
  }

  .info-table tr:last-child {
    border-bottom: 0;
  }

  .info-table th,
  .info-table td {
    padding: 0;
    border-bottom: 0;
  }

  .info-table th {
    background: transparent;
    color: var(--gargantua-gray);
    font-size: 0.85rem;
  }
}
</style>

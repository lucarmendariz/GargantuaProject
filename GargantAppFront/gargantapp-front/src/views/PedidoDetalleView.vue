<script setup>
import { computed, onMounted, ref } from 'vue';
import { unitLabel } from '../utils/units';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { askConfirm } from '../utils/confirm';
import EventTimeline from '../components/EventTimeline.vue';
import TabMenu from '../components/TabMenu.vue';
import { useAction } from '../composables/useAction';
import { useTabRoute } from '../composables/useTabRoute';
import { formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { can } from '../utils/session';
import { eventToTimeline } from '../utils/timeline';

const props = defineProps({
  id: { type: String, required: true }
});

const router = useRouter();
// Crear, asignar lotes y cancelar: permiso GESTIONAR en Pedidos
const canManage = computed(() => can('PEDIDOS', 'GESTIONAR'));

const detail = ref(null);
const loadError = ref('');
const { busy, actionError, actionSuccess, run } = useAction(() => load());

const lots = ref([]);
const products = ref([]);
const newLotId = ref('');
const newQuantity = ref('');
const consumption = ref({});

const order = computed(() => detail.value?.order);
const isOpen = computed(() => order.value?.status === 'ABIERTO');
const isPrepared = computed(() => order.value?.status === 'PREPARADO');

// Menú del pedido: una acción por pestaña. Asignar lotes solo en pedidos abiertos y para ADMIN o ENCARGADO.
const tabs = computed(() => [
  { id: 'lotes', label: 'Lotes y estado' },
  ...(isOpen.value && canManage.value ? [{ id: 'asignar', label: 'Asignar lote' }] : []),
  { id: 'historial', label: 'Historial' },
]);
const activeTab = useTabRoute(tabs);
const isDelivered = computed(() => order.value?.status === 'ENTREGADO');
const isClosed = computed(() => order.value?.status === 'CERRADO');
const timelineItems = computed(() => (detail.value?.events || []).map(eventToTimeline));

const productName = (productId) => products.value.find((product) => product.productId === productId)?.name || '';

// Solo se ofrecen los lotes con stock disponible que aún no están en el pedido
const lotOptions = computed(() => {
  const used = new Set((detail.value?.lines || []).map((line) => line.lotId));
  return lots.value.filter((lot) => lot.availableQuantity > 0 && !used.has(lot.lotId));
});

const load = async () => {
  loadError.value = '';
  try {
    detail.value = await apiClient.get(`/pedidos/${props.id}`);
    // Por defecto, lo consumido es lo reservado
    consumption.value = Object.fromEntries(detail.value.lines.map((line) => [line.lineId, line.reservedQuantity]));
    if (isOpen.value && canManage.value) {
      const [allLots, allProducts] = await Promise.all([apiClient.get('/lotes'), apiClient.get('/productos/admin')]);
      lots.value = allLots;
      products.value = allProducts;
    }
  } catch (error) {
    detail.value = null;
    loadError.value = await errorMessage(error, 'No se pudo cargar el pedido.');
  }
};

const addLine = () => run(async () => {
  await apiClient.post(`/pedidos/${props.id}/lineas`, {
    lotId: Number(newLotId.value),
    quantity: Number(newQuantity.value)
  });
  newLotId.value = '';
  newQuantity.value = '';
}, 'Cantidad reservada en el lote.', 'No se pudo reservar el lote.');

const removeLine = async (line) => {
  if (!(await askConfirm({ title: `¿Retirar el lote ${line.lotNumber} del pedido?`, message: "Se liberará lo reservado.", confirmText: "Retirar del pedido", danger: true }))) return;
  run(() => apiClient.delete(`/pedidos/${props.id}/lineas/${line.lineId}`), 'Lote retirado del pedido.', 'No se pudo retirar el lote.');
};

const prepareOrder = async () => {
  if (!(await askConfirm({ title: "¿Marcar el pedido como preparado?", message: "Se restará del stock lo consumido y ya no se podrán cambiar los lotes.", confirmText: "Marcar como preparado" }))) return;
  run(() => apiClient.post(`/pedidos/${props.id}/preparar`, {
    lines: detail.value.lines.map((line) => ({
      lineId: line.lineId,
      consumedQuantity: Number(consumption.value[line.lineId])
    }))
  }), 'Pedido preparado y stock actualizado.', 'No se pudo marcar el pedido como preparado.');
};

const deliverOrder = async () => {
  if (!(await askConfirm({ title: "¿Marcar el pedido como entregado al cliente?", message: "Se cerrará automáticamente pasados dos días.", confirmText: "Marcar como entregado" }))) return;
  run(() => apiClient.post(`/pedidos/${props.id}/entregar`), 'Pedido entregado.', 'No se pudo marcar el pedido como entregado.');
};

// Vuelve a la lista de la que se vino (pedidos o pedidos cerrados)
const goBack = () => {
  if (window.history.state?.back) {
    router.back();
  } else {
    router.push(isClosed.value ? '/pedidos?tab=cerrados' : '/pedidos');
  }
};

const cancelOrder = async () => {
  if (!(await askConfirm({ title: "¿Cancelar el pedido?", message: "Se liberará todo lo reservado y no se podrá reabrir.", confirmText: "Cancelar el pedido", danger: true }))) return;
  run(() => apiClient.post(`/pedidos/${props.id}/cancelar`), 'Pedido cancelado.', 'No se pudo cancelar el pedido.');
};

onMounted(load);
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>{{ order ? `Pedido ${order.orderNumber}` : 'Pedido' }}</h2>
      <button @click="goBack" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p v-if="loadError" class="msg-error">{{ loadError }}</p>

      <template v-if="order">
        <section class="panel">
          <div class="item-head">
            <h3>Datos del pedido</h3>
            <span class="status" :class="`status-${order.status}`">{{ order.status }}</span>
          </div>
          <dl class="facts">
            <div><dt>Cliente</dt><dd>{{ order.customerName || 'Sin cliente' }}</dd></div>
            <div><dt>Fecha programada</dt><dd>{{ formatEpochDate(order.scheduledDate) }}</dd></div>
            <div><dt>Creado por</dt><dd>{{ order.createdBy }}</dd></div>
            <div><dt>Creado el</dt><dd>{{ formatEpochDateTime(order.createdAt) }}</dd></div>
            <div v-if="order.preparedAt"><dt>Preparado por</dt><dd>{{ order.preparedBy }}</dd></div>
            <div v-if="order.preparedAt"><dt>Preparado el</dt><dd>{{ formatEpochDateTime(order.preparedAt) }}</dd></div>
            <div v-if="order.deliveredAt"><dt>Entregado por</dt><dd>{{ order.deliveredBy }}</dd></div>
            <div v-if="order.deliveredAt"><dt>Entregado el</dt><dd>{{ formatEpochDateTime(order.deliveredAt) }}</dd></div>
            <div v-if="order.closedAt"><dt>{{ order.status === 'CANCELADO' ? 'Cancelado por' : 'Cerrado por' }}</dt><dd>{{ order.closedBy === 'Sistema' ? 'Cierre automático' : order.closedBy }}</dd></div>
            <div v-if="order.closedAt"><dt>{{ order.status === 'CANCELADO' ? 'Cancelado el' : 'Cerrado el' }}</dt><dd>{{ formatEpochDateTime(order.closedAt) }}</dd></div>
            <div v-if="order.notes" class="fact-wide"><dt>Observaciones</dt><dd>{{ order.notes }}</dd></div>
          </dl>
        </section>

        <TabMenu v-model="activeTab" :tabs="tabs" label="Acciones del pedido" />

        <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
        <p v-if="actionError" class="msg-error" role="alert">{{ actionError }}</p>

        <section v-if="activeTab === 'asignar'" id="panel-asignar" role="tabpanel" aria-labelledby="tab-asignar" class="panel">
          <h3>Asignar lote</h3>
          <form @submit.prevent="addLine" class="line-form">
            <div class="form-group">
              <label for="line-lot">Lote (*):</label>
              <select id="line-lot" v-model="newLotId" required>
                <option value="" disabled>Selecciona un lote...</option>
                <option v-for="lot in lotOptions" :key="lot.lotId" :value="lot.lotId">
                  {{ lot.lotNumber }} · {{ productName(lot.productId) }} · disponible {{ lot.availableQuantity }} {{ unitLabel(lot.measureUnit) }}{{ isExpired(lot.expirationDate) ? ' · CADUCADO' : '' }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label for="line-quantity">Cantidad prevista (*):</label>
              <input id="line-quantity" v-model="newQuantity" type="number" min="0.0001" step="0.0001" required />
            </div>
            <button type="submit" :disabled="busy">Reservar</button>
          </form>
        </section>

        <section v-if="activeTab === 'lotes'" id="panel-lotes" role="tabpanel" aria-labelledby="tab-lotes" class="panel">
          <h3>Lotes del pedido</h3>
          <p v-if="detail.lines.length === 0" class="empty-state">Este pedido aún no tiene lotes asignados.</p>
          <div v-else class="item-list">
            <div v-for="line in detail.lines" :key="line.lineId" class="item-card">
              <span class="item-title">{{ line.productName }}</span>
              <RouterLink :to="`/lotes/${line.lotId}/trazabilidad`">{{ line.lotNumber }}</RouterLink>
              <span class="item-meta">Reservado: {{ line.reservedQuantity }} {{ unitLabel(line.measureUnit) }}</span>
              <span v-if="line.consumedQuantity !== null" class="item-meta">Consumido: {{ line.consumedQuantity }} {{ unitLabel(line.measureUnit) }}</span>
              <div v-if="isOpen" class="form-group line-consumed">
                <label :for="`consumed-${line.lineId}`">Consumo real ({{ unitLabel(line.measureUnit) }}):</label>
                <input :id="`consumed-${line.lineId}`" v-model="consumption[line.lineId]" type="number" min="0" step="0.0001" required />
              </div>
              <button v-if="isOpen && canManage" @click="removeLine(line)" :disabled="busy" class="btn-secondary">Retirar del pedido</button>
            </div>
          </div>

          <div v-if="isOpen" class="order-actions">
            <button @click="prepareOrder" :disabled="busy || detail.lines.length === 0">Marcar como preparado y restar stock</button>
            <button v-if="canManage" @click="cancelOrder" :disabled="busy" class="btn-danger">Cancelar pedido</button>
          </div>
          <div v-else-if="isPrepared" class="order-actions">
            <button @click="deliverOrder" :disabled="busy">Marcar como entregado</button>
          </div>
          <p v-else-if="isDelivered && order.autoCloseAt" class="item-meta order-actions">
            Entregado al cliente. Se cerrará automáticamente el {{ formatEpochDateTime(order.autoCloseAt) }}
            y pasará a <RouterLink to="/pedidos?tab=cerrados">Pedidos cerrados</RouterLink>.
          </p>
        </section>

        <section v-if="activeTab === 'historial'" id="panel-historial" role="tabpanel" aria-labelledby="tab-historial" class="panel">
          <h3>Historial del pedido</h3>
          <EventTimeline class="history" :items="timelineItems" />
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 14px;
}

.facts {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  margin: 14px 0 0;
}

.fact-wide {
  grid-column: 1 / -1;
}

dt {
  color: var(--gargantua-gray);
  font-size: 0.825rem;
  font-weight: 600;
}

dd {
  margin: 0;
  overflow-wrap: anywhere;
}

.line-form {
  display: grid;
  align-items: end;
  gap: 12px;
  grid-template-columns: minmax(0, 1fr);
}

@media (min-width: 720px) {
  .line-form {
    grid-template-columns: minmax(0, 3fr) minmax(0, 1fr) auto;
  }
}

.line-consumed {
  margin-top: 6px;
}

.order-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--gargantua-border);
}

@media (max-width: 480px) {
  .order-actions button {
    width: 100%;
  }
}
</style>

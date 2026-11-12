<script setup>
import { computed, onMounted, ref } from 'vue';
import { unitLabel } from '../utils/units';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import EventTimeline from '../components/EventTimeline.vue';
import { formatEpochDate, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { historyToTimeline } from '../utils/timeline';

const props = defineProps({
  id: { type: String, required: true }
});

const router = useRouter();
const data = ref(null);
const loadError = ref('');

const lot = computed(() => data.value?.lot);
// Cantidades del lote: lo que entró se reparte en consumido + reservado + disponible
const num = (value) => Number(value) || 0;
const fmt = (value) => num(value).toLocaleString('es-ES', { maximumFractionDigits: 4 });
const unit = computed(() => unitLabel(lot.value?.measureUnit));
const parts = computed(() => {
  const total = num(lot.value?.quantity) || 1;
  return [
    { key: 'consumed', label: 'Consumido', value: lot.value?.consumedQuantity, help: 'Ya gastado: en pedidos preparados y en producción.' },
    { key: 'reserved', label: 'Reservado', value: lot.value?.reservedQuantity, help: 'Apartado para pedidos abiertos: sigue en almacén, pero no se puede usar.' },
    { key: 'available', label: 'Disponible', value: lot.value?.availableQuantity, help: 'Libre para usar en pedidos o en producción.' },
  ].map((part) => ({ ...part, percent: Math.max(0, (num(part.value) / total) * 100) }));
});
const barLabel = computed(() => parts.value.map((part) => `${part.label} ${fmt(part.value)} ${unit.value}`).join(', '));

const timelineItems = computed(() => (data.value?.history || []).map(historyToTimeline));

const goBack = () => {
  if (window.history.state?.back) {
    router.back();
  } else {
    router.push('/dashboard');
  }
};

onMounted(async () => {
  try {
    data.value = await apiClient.get(`/lotes/${props.id}/trazabilidad`);
  } catch (error) {
    loadError.value = await errorMessage(error, 'No se pudo cargar la trazabilidad del lote.');
  }
});
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>{{ lot ? `Trazabilidad de ${lot.lotNumber}` : 'Trazabilidad del lote' }}</h2>
      <button @click="goBack" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p v-if="loadError" class="msg-error">{{ loadError }}</p>

      <template v-if="lot">
        <section class="panel">
          <div class="item-head lot-head">
            <h3>{{ data.product?.name }} ({{ data.product?.sku }})</h3>
            <span v-if="isExpired(lot.expirationDate)" class="status status-CADUCADO">CADUCADO</span>
          </div>
          <p class="item-meta lot-meta">
            Caducidad: {{ formatEpochDate(lot.expirationDate) }}
            <template v-if="lot.supplierReference"> · Proveedor: {{ lot.supplierReference }}</template>
          </p>
          <div class="qty">
            <p class="qty-headline">
              <span class="qty-big">{{ fmt(lot.availableQuantity) }} {{ unit }}</span>
              <span>disponibles de {{ fmt(lot.quantity) }} {{ unit }} que entraron</span>
            </p>

            <div class="qty-bar" role="img" :aria-label="`Reparto de la entrada: ${barLabel}`">
              <span v-for="part in parts" :key="part.key" :class="`qty-seg qty-${part.key}`" :style="{ width: `${part.percent}%` }"></span>
            </div>

            <ul class="qty-legend">
              <li v-for="part in parts" :key="part.key">
                <span :class="`qty-swatch qty-${part.key}`" aria-hidden="true"></span>
                <span class="qty-name">{{ part.label }}</span>
                <span class="qty-value">{{ fmt(part.value) }} {{ unit }}</span>
                <span class="qty-help">{{ part.help }}</span>
              </li>
            </ul>

            <div class="qty-extra">
              <p>
                <strong>Stock actual: {{ fmt(lot.currentStock) }} {{ unit }}</strong>
                <span>Lo que queda físicamente en almacén (reservado + disponible).</span>
              </p>
              <p>
                <strong>En pedidos sin cerrar: {{ fmt(lot.inUseQuantity) }} {{ unit }}</strong>
                <span>Reservado en pedidos abiertos más lo consumido en pedidos preparados o entregados.</span>
              </p>
            </div>
          </div>
        </section>

        <section class="panel">
          <h3>Pedidos en los que se usa</h3>
          <p v-if="data.orders.length === 0" class="empty-state">Este lote no se ha usado en ningún pedido.</p>
          <div v-else class="item-list">
            <RouterLink v-for="usage in data.orders" :key="usage.orderId" :to="`/pedidos/${usage.orderId}`" class="item-card">
              <div class="item-head">
                <span class="item-title">{{ usage.orderNumber }}</span>
                <span class="status" :class="`status-${usage.orderStatus}`">{{ usage.orderStatus }}</span>
              </div>
              <span class="item-meta">{{ usage.customerName || 'Sin cliente' }} · {{ formatEpochDate(usage.scheduledDate) }}</span>
              <span class="item-meta">Reservado: {{ usage.reservedQuantity }} {{ unitLabel(lot.measureUnit) }}</span>
              <span v-if="usage.consumedQuantity !== null" class="item-meta">Consumido: {{ usage.consumedQuantity }} {{ unitLabel(lot.measureUnit) }}</span>
            </RouterLink>
          </div>
        </section>

        <section class="panel">
          <h3>Historial del lote</h3>
          <p v-if="timelineItems.length === 0" class="empty-state">Este lote aún no tiene movimientos.</p>
          <EventTimeline v-else :items="timelineItems" />
        </section>
      </template>
    </div>
  </div>
</template>

<style scoped>
.qty {
  display: grid;
  gap: 14px;
}

.qty-headline {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 10px;
  margin: 0;
  color: var(--gargantua-gray);
}

.qty-big {
  color: var(--gargantua-success);
  font-size: 1.8rem;
  font-weight: 700;
}

.qty-bar {
  display: flex;
  height: 18px;
  overflow: hidden;
  background: var(--gargantua-border);
  border-radius: 999px;
}

.qty-seg {
  height: 100%;
}

.qty-consumed {
  background: #9c8f80;
}

.qty-reserved {
  background: var(--gargantua-gold);
}

.qty-available {
  background: var(--gargantua-success);
}

.qty-legend {
  display: grid;
  gap: 10px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  margin: 0;
  padding: 0;
  list-style: none;
}

.qty-legend li {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 2px 8px;
  padding: 10px 12px;
  background: var(--gargantua-cream);
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-sm);
}

.qty-swatch {
  width: 12px;
  height: 12px;
  border-radius: 3px;
}

.qty-name {
  color: var(--gargantua-gray);
  font-size: 0.85rem;
  font-weight: 600;
}

.qty-value,
.qty-help {
  grid-column: 2;
}

.qty-value {
  color: var(--gargantua-dark);
  font-size: 1.15rem;
  font-weight: 700;
}

.qty-help {
  color: var(--gargantua-gray);
  font-size: 0.8rem;
}

.qty-extra {
  display: grid;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--gargantua-border);
}

.qty-extra p {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 8px;
  margin: 0;
}

.qty-extra span {
  color: var(--gargantua-gray);
  font-size: 0.85rem;
}

.panel > h3 {
  margin-bottom: 14px;
}

.lot-meta {
  margin: 6px 0 14px;
}
</style>

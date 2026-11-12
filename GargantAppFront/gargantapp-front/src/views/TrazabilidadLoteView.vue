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
          <div class="stats">
            <div class="stat"><span class="stat-label">Entrada</span><span class="stat-value">{{ lot.quantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
            <div class="stat"><span class="stat-label">Consumido</span><span class="stat-value">{{ lot.consumedQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
            <div class="stat"><span class="stat-label">Stock actual</span><span class="stat-value">{{ lot.currentStock }} {{ unitLabel(lot.measureUnit) }}</span></div>
            <div class="stat"><span class="stat-label">Reservado</span><span class="stat-value">{{ lot.reservedQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
            <div class="stat"><span class="stat-label">En uso</span><span class="stat-value">{{ lot.inUseQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
            <div class="stat"><span class="stat-label">Disponible</span><span class="stat-value">{{ lot.availableQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
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
.panel > h3 {
  margin-bottom: 14px;
}

.lot-meta {
  margin: 6px 0 14px;
}
</style>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { askConfirm } from '../utils/confirm';
import { useAction } from '../composables/useAction';
import { dateInputToEpoch, epochToDateInput, formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { matchesSearch } from '../utils/search';
import { MEASURE_UNITS, unitLabel } from '../utils/units';
import { can } from '../utils/session';

const router = useRouter();
// Editar y eliminar lotes: permiso GESTIONAR en Lotes
const canManage = can('LOTES', 'GESTIONAR');

const products = ref([]);
const lots = ref([]);
const loading = ref(true);
const listError = ref('');
const search = ref('');

const { busy, actionError, actionSuccess, run } = useAction(() => loadLots());

// --- ESTADO LOCAL PARA CONTROLAR LA EDICIÓN DE UN LOTE ---
const editingLotId = ref(null);
const editForm = ref({ quantity: '', measureUnit: 'UNITS', expirationDate: '', supplierReference: '' });

const productLabel = (productId) => {
  const product = products.value.find((prod) => prod.productId === productId);
  return product ? `${product.name} (${product.sku})` : `Producto #${productId}`;
};

// Busca en el número de lote, el producto (nombre y SKU) y la referencia del proveedor
const filteredLots = computed(() => lots.value.filter((lot) =>
  matchesSearch(search.value, [lot.lotNumber, productLabel(lot.productId), lot.supplierReference])));

const loadLots = async () => {
  loading.value = true;
  listError.value = '';
  try {
    lots.value = await apiClient.get('/lotes');
  } catch (error) {
    lots.value = [];
    listError.value = await errorMessage(error, 'No se pudieron cargar los lotes.');
  } finally {
    loading.value = false;
  }
};

const startEdit = (lot) => {
  actionError.value = '';
  actionSuccess.value = '';
  editingLotId.value = lot.lotId;
  editForm.value = {
    quantity: lot.quantity,
    measureUnit: lot.measureUnit,
    expirationDate: epochToDateInput(lot.expirationDate),
    supplierReference: lot.supplierReference || '',
  };
};

const cancelEdit = () => {
  editingLotId.value = null;
};

const saveEdit = (lot) => run(async () => {
  await apiClient.put(`/lotes/${lot.lotId}`, {
    quantity: Number(editForm.value.quantity),
    measureUnit: editForm.value.measureUnit,
    expirationDate: dateInputToEpoch(editForm.value.expirationDate),
    supplierReference: editForm.value.supplierReference || null,
  });
  editingLotId.value = null;
}, `Lote ${lot.lotNumber} actualizado correctamente.`, 'No se pudo actualizar el lote.');

const deleteLot = async (lot) => {
  const ok = await askConfirm({
    title: `¿Dar de baja el lote ${lot.lotNumber}?`,
    message: 'Se elimina definitivamente, junto con su historial de movimientos. Esta acción no se puede deshacer.',
    confirmText: 'Dar de baja el lote',
    danger: true,
  });
  if (!ok) return;
  run(() => apiClient.delete(`/lotes/${lot.lotId}`), `Lote ${lot.lotNumber} eliminado.`, 'No se pudo eliminar el lote.');
};

onMounted(async () => {
  try {
    products.value = await apiClient.get('/productos/admin');
  } catch (error) {
    console.error('Error al cargar los productos', error);
  }
  await loadLots();
});

</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Gestión de Lotes</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <section class="panel filters">
        <div class="form-group">
          <label for="lot-search">Buscar lote:</label>
          <input id="lot-search" v-model="search" type="search" placeholder="Número de lote, producto, SKU o proveedor..." />
        </div>
      </section>

      <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
      <p v-if="actionError" class="msg-error" role="alert">{{ actionError }}</p>

      <section class="panel">
        <h3>Lotes registrados</h3>
        <p v-if="loading" class="empty-state">Cargando lotes...</p>
        <p v-else-if="listError" class="msg-error">{{ listError }}</p>
        <p v-else-if="lots.length === 0" class="empty-state">Todavía no hay lotes registrados.</p>
        <p v-else-if="filteredLots.length === 0" class="empty-state">Ningún lote coincide con la búsqueda.</p>
        <div v-else class="item-list">
          <div v-for="lot in filteredLots" :key="lot.lotId" class="item-card">
            <div class="item-head">
              <span class="item-title">{{ lot.lotNumber }} <code>#{{ lot.lotId }}</code></span>
              <span v-if="isExpired(lot.expirationDate)" class="status status-CADUCADO">CADUCADO</span>
            </div>
            <span class="item-meta">{{ productLabel(lot.productId) }}</span>

            <template v-if="editingLotId !== lot.lotId">
              <div class="stats lot-stats">
                <div class="stat"><span class="stat-label">Entrada</span><span class="stat-value">{{ lot.quantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
                <div class="stat"><span class="stat-label">Stock</span><span class="stat-value">{{ lot.currentStock }} {{ unitLabel(lot.measureUnit) }}</span></div>
                <div class="stat"><span class="stat-label">Reservado</span><span class="stat-value">{{ lot.reservedQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
                <div class="stat"><span class="stat-label">En uso</span><span class="stat-value">{{ lot.inUseQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
                <div class="stat"><span class="stat-label">Disponible</span><span class="stat-value">{{ lot.availableQuantity }} {{ unitLabel(lot.measureUnit) }}</span></div>
              </div>
              <span class="item-meta">
                Caducidad: {{ formatEpochDate(lot.expirationDate) }}
                · Proveedor: {{ lot.supplierReference || 'N/A' }}
              </span>
              <span class="item-meta">Alta: {{ formatEpochDateTime(lot.createdAt) }} · Última modificación: {{ formatEpochDateTime(lot.updatedAt) }}</span>
              <div class="lot-actions">
                <RouterLink :to="`/lotes/${lot.lotId}/trazabilidad`">Ver trazabilidad</RouterLink>
                <template v-if="canManage">
                  <button @click="startEdit(lot)" :disabled="busy" class="btn-secondary">Editar</button>
                  <button @click="deleteLot(lot)" :disabled="busy" class="btn-danger">Eliminar</button>
                </template>
              </div>
            </template>

            <form v-else @submit.prevent="saveEdit(lot)" class="edit-form">
              <div class="form-group">
                <label :for="`lot-qty-${lot.lotId}`">Cantidad de entrada (*):</label>
                <input :id="`lot-qty-${lot.lotId}`" v-model="editForm.quantity" type="number" min="0" step="0.0001" required />
              </div>
              <div class="form-group">
                <label :for="`lot-unit-${lot.lotId}`">Unidad medida:</label>
                <select :id="`lot-unit-${lot.lotId}`" v-model="editForm.measureUnit">
                  <option v-for="unit in MEASURE_UNITS" :key="unit.value" :value="unit.value">{{ unit.label }}</option>
                </select>
              </div>
              <div class="form-group">
                <label :for="`lot-exp-${lot.lotId}`">Fecha caducidad:</label>
                <input :id="`lot-exp-${lot.lotId}`" v-model="editForm.expirationDate" type="date" />
              </div>
              <div class="form-group">
                <label :for="`lot-ref-${lot.lotId}`">Referencia albarán / proveedor:</label>
                <input :id="`lot-ref-${lot.lotId}`" v-model="editForm.supplierReference" type="text" />
              </div>
              <p class="item-meta form-wide">
                La cantidad no puede ser menor que lo ya reservado ({{ lot.reservedQuantity }}) más lo consumido ({{ lot.consumedQuantity }}).
              </p>
              <div class="lot-actions form-wide">
                <button type="submit" :disabled="busy">Guardar</button>
                <button type="button" @click="cancelEdit" class="btn-secondary">Cancelar</button>
              </div>
            </form>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.panel > h3 {
  margin-bottom: 14px;
}

.filters {
  display: grid;
  align-items: end;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
}

.lot-stats {
  margin: 8px 0;
}

.lot-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.edit-form {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 200px), 1fr));
  margin-top: 8px;
}

.form-wide {
  grid-column: 1 / -1;
}

@media (max-width: 480px) {
  .lot-actions button {
    width: 100%;
  }
}
</style>

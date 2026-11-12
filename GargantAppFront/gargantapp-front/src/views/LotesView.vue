<script setup>
import { onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import { askConfirm } from '../utils/confirm';
import TabMenu from '../components/TabMenu.vue';
import { useAction } from '../composables/useAction';
import { useTabRoute } from '../composables/useTabRoute';
import { dateInputToEpoch, epochToDateInput, formatEpochDate, formatEpochDateTime, isExpired } from '../utils/date';
import { errorMessage } from '../utils/errors';
import { MEASURE_UNITS, unitLabel } from '../utils/units';
import { can } from '../utils/session';

const router = useRouter();
// Editar y eliminar lotes: permiso GESTIONAR en Lotes
const canManage = can('LOTES', 'GESTIONAR');

// Menú del módulo: una acción por pestaña
const tabs = [
  { id: 'registrados', label: 'Lotes registrados' },
  { id: 'buscar', label: 'Buscar lote por ID' },
];
const activeTab = useTabRoute(tabs);
const searchDone = ref(false);

const products = ref([]);
const lots = ref([]);
const loading = ref(true);
const listError = ref('');
const productFilter = ref('');
const lotIdInput = ref('');

const { busy, actionError, actionSuccess, run } = useAction(() => refresh());

// --- ESTADO LOCAL PARA CONTROLAR LA EDICIÓN DE UN LOTE ---
const editingLotId = ref(null);
const editForm = ref({ quantity: '', measureUnit: 'UNITS', expirationDate: '', supplierReference: '' });

const productLabel = (productId) => {
  const product = products.value.find((prod) => prod.productId === productId);
  return product ? `${product.name} (${product.sku})` : `Producto #${productId}`;
};

// Sin filtro se piden todos los lotes; con filtro, solo los del producto elegido
const loadLots = async () => {
  loading.value = true;
  listError.value = '';
  try {
    const endpoint = productFilter.value ? `/lotes/producto/${productFilter.value}` : '/lotes';
    lots.value = await apiClient.get(endpoint);
  } catch (error) {
    lots.value = [];
    listError.value = await errorMessage(error, 'No se pudieron cargar los lotes.');
  } finally {
    loading.value = false;
  }
};

// Búsqueda directa por el ID interno del lote
const searchById = async () => {
  if (!lotIdInput.value) return;
  loading.value = true;
  listError.value = '';
  searchDone.value = true;
  try {
    lots.value = [await apiClient.get(`/lotes/${lotIdInput.value}`)];
  } catch (error) {
    lots.value = [];
    listError.value = error?.status === 404
      ? 'No existe ningún lote con este ID.'
      : await errorMessage(error, 'No se pudo buscar el lote.');
  } finally {
    loading.value = false;
  }
};

// Vuelve a leer lo que se está viendo tras editar o eliminar: el listado o el lote buscado
const refresh = async () => {
  if (activeTab.value !== 'buscar') {
    await loadLots();
    return;
  }
  try {
    lots.value = [await apiClient.get(`/lotes/${lotIdInput.value}`)];
  } catch {
    // El lote buscado se acaba de eliminar
    lots.value = [];
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
  if (activeTab.value === 'registrados') {
    await loadLots();
  } else {
    loading.value = false;
  }
});

watch(productFilter, loadLots);

// Cada pestaña empieza limpia: el listado se vuelve a cargar y la búsqueda espera un ID
watch(activeTab, (tab) => {
  editingLotId.value = null;
  actionError.value = '';
  actionSuccess.value = '';
  listError.value = '';
  if (tab === 'registrados') {
    loadLots();
  } else {
    lots.value = [];
    lotIdInput.value = '';
    searchDone.value = false;
  }
});
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Gestión de Lotes</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <TabMenu v-model="activeTab" :tabs="tabs" label="Acciones de lotes" />

      <div :id="`panel-${activeTab}`" role="tabpanel" :aria-labelledby="`tab-${activeTab}`" class="tab-panel">
        <section v-if="activeTab === 'registrados'" class="panel filters">
          <div class="form-group">
            <label for="lot-product-filter">Producto:</label>
            <select id="lot-product-filter" v-model="productFilter">
              <option value="">Todos los productos</option>
              <option v-for="prod in products" :key="prod.productId" :value="prod.productId">
                {{ prod.name }} ({{ prod.sku }})
              </option>
            </select>
          </div>
        </section>

        <section v-else class="panel filters">
          <div class="form-group">
            <label for="lot-id-search">ID interno del lote:</label>
            <div class="input-row">
              <input id="lot-id-search" v-model="lotIdInput" type="number" min="1" placeholder="Ej: 1" @keyup.enter="searchById" />
              <button @click="searchById">Buscar</button>
            </div>
          </div>
        </section>

        <p v-if="actionSuccess" class="msg-success" role="status">{{ actionSuccess }}</p>
        <p v-if="actionError" class="msg-error" role="alert">{{ actionError }}</p>

        <section v-if="activeTab === 'registrados' || searchDone" class="panel">
          <h3>{{ activeTab === 'registrados' ? 'Lotes registrados' : 'Resultado de la búsqueda' }}</h3>
          <p v-if="loading" class="empty-state">Cargando lotes...</p>
          <p v-else-if="listError" class="msg-error">{{ listError }}</p>
          <p v-else-if="lots.length === 0" class="empty-state">
            {{ activeTab === 'registrados' ? 'No hay lotes con este filtro.' : 'El lote ya no existe.' }}
          </p>
          <div v-else class="item-list">
            <div v-for="lot in lots" :key="lot.lotId" class="item-card">
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

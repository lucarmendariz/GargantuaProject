<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useInventory } from '../composables/useInventory';
import ProductForm from '../components/ProductForm.vue';
import LotForm from '../components/LotForm.vue';

const router = useRouter();
const {
  products, loading, searchQuery, filteredProducts,
  initInventory, loadProducts, loadProductLots, getProductLots, deleteProduct, updateProduct
} = useInventory();

// --- ESTADO LOCAL PARA CONTROLAR LA EDICIÓN EN FILA ---
const editingProductId = ref(null);
const editForm = ref({ productId: null, sku: '', name: '', description: '', active: 1 });

onMounted(() => {
  initInventory();
});

// Activar el modo edición clonando los datos del producto
const startEdit = (prod) => {
  editingProductId.value = prod.productId;
  editForm.value = { ...prod };
};

// Cancelar edición limpia el puntero
const cancelEdit = () => {
  editingProductId.value = null;
};

// Guardar los cambios modificados en caliente
const saveProductEdit = async () => {
  await updateProduct(editForm.value);
  editingProductId.value = null;
};

// Manejo seguro del botón borrar
const handleConfirmDelete = (id, name) => {
  if (confirm(`¿Seguro que deseas retirar el producto maestro "${name}" de la circulación de la planta?`)) {
    deleteProduct(id);
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>📊 Panel de Inventario y Lotes</h2>
      <button @click="router.push('/dashboard')" class="btn-back">⬅️ Volver</button>
    </header>

    <div class="card-body">
      <div class="forms-container">
        <ProductForm @success="loadProducts" />
        <LotForm :products="products" @success="loadProductLots" />
      </div>

      <hr class="section-divider" />

      <div class="search-box">
        <input v-model="searchQuery" type="text"
          placeholder="🔍 Buscar por SKU, nombre del artículo o descripción técnica..." class="search-input" />
      </div>

      <h3 class="section-title">📋 Catálogo Actual de Artículos Activos</h3>
      <div v-if="loading" class="loader">Consultando base de datos de planta...</div>

      <div v-else class="table-responsive">
        <table class="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Código SKU</th>
              <th>Nombre del Artículo</th>
              <th>Descripción Técnica</th>
              <th>Lotes Físicos Asociados</th>
              <th>Estado</th>
              <th class="text-center">Acciones Planta</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="prod in filteredProducts" :key="prod.productId" :class="{ 'row-inactive': !prod.active }">
              <td><code>#{{ prod.productId }}</code></td>
              <td><span class="sku-tag">{{ prod.sku }}</span></td>

              <template v-if="editingProductId !== prod.productId">
                <td><strong>{{ prod.name }}</strong></td>
                <td>{{ prod.description || 'Sin descripción técnica registrada' }}</td>
              </template>
              <template v-else>
                <td><input v-model="editForm.name" class="inline-input" type="text" /></td>
                <td><input v-model="editForm.description" class="inline-input" type="text" /></td>
              </template>

              <td>
                <div v-if="getProductLots(prod.productId).length > 0" class="lots-container">
                  <span v-for="lot in getProductLots(prod.productId)" :key="lot.lotNumber" class="lot-pill"
                    :title="`Expiración: ${lot.expirationDate ? new Date(lot.expirationDate).toLocaleDateString() : 'N/A'}`">
                    <strong>{{ lot.lotNumber }}</strong>: {{ lot.quantity }} {{ lot.measureUnit }}
                  </span>
                </div>
                <span v-else class="no-lots">Sin lotes activos</span>
              </td>

              <td>
                <template v-if="editingProductId !== prod.productId">
                  <span :class="prod.active ? 'badge-active' : 'badge-inactive'">
                    {{ prod.active ? 'Activo' : 'Inactivo' }}
                  </span>
                </template>
                <template v-else>
                  <select v-model="editForm.active">
                    <option :value="true">Activo</option>
                    <option :value="false">Inactivo</option>
                  </select>
                </template>
              </td>

              <td class="text-center">
                <div v-if="editingProductId !== prod.productId" class="actions-wrapper">
                  <button @click="startEdit(prod)" class="btn-action btn-edit" title="Editar Metadatos">✏️</button>
                  <button v-if="prod.active" @click="handleConfirmDelete(prod.productId, prod.name)"
                    class="btn-action btn-delete" title="Dar de baja">
                    🗑️
                  </button>
                </div>
                <div v-else class="actions-wrapper">
                  <button @click="saveProductEdit" class="btn-action btn-save" title="Guardar">💾</button>
                  <button @click="cancelEdit" class="btn-action btn-cancel" title="Cancelar">❌</button>
                </div>
              </td>
            </tr>
            <tr v-if="filteredProducts.length === 0">
              <td colspan="7" class="text-center">No se encontraron productos que coincidan con la búsqueda.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.card-header {
  background: var(--gargantua-gold);
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--gargantua-dark);
}

.card-header h2 {
  margin: 0;
  font-size: 1.3rem;
}

.btn-back {
  background: var(--gargantua-dark);
  color: var(--gargantua-white);
  border: none;
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-weight: bold;
}

.card-body {
  padding: 25px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.forms-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
}

:deep(.form-section) {
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  padding: 20px;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 10px rgba(62, 39, 18, 0.02);
}

:deep(.form-section h3) {
  margin-top: 0;
  color: var(--gargantua-dark);
  margin-bottom: 15px;
  border-bottom: 2px solid var(--gargantua-cream);
  padding-bottom: 5px;
}

:deep(.inner-form) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.form-grid-inputs) {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

:deep(.form-group) {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

:deep(label) {
  font-weight: bold;
  color: var(--gargantua-dark);
  font-size: 0.85rem;
}

:deep(input),
:deep(select),
:deep(textarea) {
  padding: 10px;
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-sm);
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
  font-size: 0.95rem;
  width: 100%;
  box-sizing: border-box;
}

:deep(.btn-submit) {
  padding: 11px;
  color: white;
  border: none;
  border-radius: var(--radius-sm);
  font-weight: bold;
  cursor: pointer;
  margin-top: 5px;
}

:deep(.btn-prod) {
  background: var(--gargantua-dark);
  color: var(--gargantua-gold);
}

:deep(.btn-lot) {
  background: var(--gargantua-orange);
}

:deep(.msg-success) {
  color: var(--gargantua-success);
  font-weight: bold;
  font-size: 0.85rem;
}

:deep(.msg-error) {
  color: var(--gargantua-danger);
  font-weight: bold;
  font-size: 0.85rem;
}

.section-divider {
  border: 0;
  border-top: 2px solid var(--gargantua-border);
  margin: 10px 0;
}

.section-title {
  margin: 0;
  color: var(--gargantua-dark);
}

.search-input {
  background: var(--gargantua-white);
  border: 2px solid var(--gargantua-border);
  padding: 12px 15px;
  font-size: 1rem;
  border-radius: var(--radius-md);
  width: 100%;
  box-sizing: border-box;
}

.loader {
  text-align: center;
  padding: 30px;
  font-weight: bold;
  color: var(--gargantua-orange);
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
  background: var(--gargantua-white);
  border-radius: var(--radius-md);
  border: 1px solid var(--gargantua-border);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--gargantua-border);
  font-size: 0.9rem;
}

.data-table th {
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
}

.sku-tag {
  background: var(--gargantua-cream);
  padding: 3px 6px;
  border-radius: 4px;
  border: 1px dashed var(--gargantua-orange);
  font-family: monospace;
  color: var(--gargantua-orange);
  font-weight: bold;
}

.badge-active {
  background: var(--gargantua-success);
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
}

/* 🎨 NUEVOS ESTILOS PARA LAS GESTIONES DE EDICIÓN Y BAJAS */
.row-inactive {
  background-color: #f8fafc;
  opacity: 0.75;
}

.badge-inactive {
  background: var(--gargantua-danger);
  color: white;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: bold;
}

.inline-input {
  padding: 4px 8px !important;
  background: white !important;
  border: 1px solid var(--gargantua-orange) !important;
  font-size: 0.85rem !important;
}

.inline-select {
  padding: 4px !important;
  background: white !important;
  border: 1px solid var(--gargantua-orange) !important;
  font-size: 0.85rem !important;
  height: auto !important;
}

.actions-wrapper {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.btn-action {
  border: 1px solid var(--gargantua-border);
  background: white;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: 0.9rem;
  transition: var(--transition);
}

.btn-action:hover {
  background: var(--gargantua-cream);
  transform: scale(1.05);
}

.btn-delete:hover {
  background: #fee2e2;
  border-color: var(--gargantua-danger);
}

.btn-save:hover {
  background: #dcfce7;
  border-color: var(--gargantua-success);
}

.lots-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 300px;
}

.lot-pill {
  background: var(--gargantua-cream);
  border: 1px solid var(--gargantua-border);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-family: monospace;
}

.no-lots {
  color: #94a3b8;
  font-size: 0.85rem;
  font-style: italic;
}

.text-center {
  text-align: center;
  color: #64748b;
  padding: 20px !important;
}

@media (max-width: 850px) {
  .forms-container {
    grid-template-columns: 1fr;
  }

  :deep(.form-grid-inputs) {
    grid-template-columns: 1fr;
  }
}
</style>
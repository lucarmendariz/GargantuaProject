<script setup>
import { onMounted, ref } from 'vue';
import { unitLabel } from '../utils/units';
import { useRouter } from 'vue-router';
import { useInventory } from '../composables/useInventory';
import ProductForm from '../components/ProductForm.vue';
import LotForm from '../components/LotForm.vue';
import TabMenu from '../components/TabMenu.vue';
import { useTabRoute } from '../composables/useTabRoute';
import { askConfirm } from '../utils/confirm';
import { formatEpochDate } from '../utils/date';
import { can } from '../utils/session';

const router = useRouter();

// Dar de alta, editar y dar de baja: permiso GESTIONAR en Inventario
const canManage = can('INVENTARIO', 'GESTIONAR');

// Menú del módulo: una acción por pestaña
const tabs = [
  { id: 'lotes', label: 'Visualizar lotes' },
  ...(canManage ? [
    { id: 'activo', label: 'Añadir definición de activo' },
    { id: 'lote', label: 'Añadir lote' },
  ] : []),
];
const activeTab = useTabRoute(tabs);
const {
  products, loading, searchQuery, filteredProducts,
  initInventory, loadProducts, loadProductLots, getProductLots, deleteProduct, updateProduct
} = useInventory();

// --- ESTADO LOCAL PARA CONTROLAR LA EDICIÓN EN FILA ---
const editingProductId = ref(null);
const editForm = ref({ productId: null, sku: '', gtin: '', name: '', description: '', active: 1 });

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
  // Si falla (p. ej. código de barras repetido) la fila sigue en edición para corregirlo
  if (await updateProduct({ ...editForm.value, gtin: editForm.value.gtin || null })) {
    editingProductId.value = null;
  }
};

// Al dar de alta un lote puede haberse guardado el código de barras del producto
const onLotCreated = () => {
  loadProductLots();
  loadProducts();
};

// Manejo seguro del botón borrar
const handleConfirmDelete = async (id, name) => {
  const ok = await askConfirm({
    title: `¿Dar de baja el producto "${name}"?`,
    message: 'Deja de estar disponible en la planta. Sus lotes y su historial se conservan, y se puede reactivar editándolo.',
    confirmText: 'Dar de baja',
    danger: true,
  });
  if (ok) deleteProduct(id);
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Panel de Inventario y Lotes</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <TabMenu v-model="activeTab" :tabs="tabs" label="Acciones de inventario" />

      <div v-if="activeTab === 'activo'" id="panel-activo" role="tabpanel" aria-labelledby="tab-activo" class="tab-panel form-tab">
        <ProductForm @success="loadProducts" />
      </div>

      <div v-if="activeTab === 'lote'" id="panel-lote" role="tabpanel" aria-labelledby="tab-lote" class="tab-panel form-tab">
        <LotForm :products="products" @success="onLotCreated" />
      </div>

      <div v-if="activeTab === 'lotes'" id="panel-lotes" role="tabpanel" aria-labelledby="tab-lotes" class="tab-panel">
        <div class="search-box">
          <input v-model="searchQuery" type="text"
            placeholder="Buscar por SKU, nombre del artículo o descripción técnica..." class="search-input" />
        </div>

        <h3 class="section-title">Catálogo Actual de Artículos Activos</h3>
        <div v-if="loading" class="loader">Consultando base de datos de planta...</div>

        <div v-else class="table-responsive">
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Código SKU</th>
                <th>Nombre del Artículo</th>
                <th>Descripción Técnica</th>
                <th>Lotes (disponible / stock)</th>
                <th>Estado</th>
                <th>Acciones Planta</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prod in filteredProducts" :key="prod.productId" :class="{ 'row-inactive': !prod.active }">
                <td data-label="ID"><code>#{{ prod.productId }}</code></td>
                <td data-label="Código SKU">
                  <div class="code-cell">
                    <span class="sku-tag">{{ prod.sku }}</span>
                    <input v-if="editingProductId === prod.productId" v-model="editForm.gtin" class="inline-input"
                      type="text" inputmode="numeric" aria-label="Código de barras (EAN/GTIN)" placeholder="Código de barras" />
                    <span v-else-if="prod.gtin" class="gtin-tag" title="Código de barras (GTIN)">EAN {{ prod.gtin }}</span>
                  </div>
                </td>

                <template v-if="editingProductId !== prod.productId">
                  <td data-label="Artículo"><strong>{{ prod.name }}</strong></td>
                  <td data-label="Descripción">{{ prod.description || 'Sin descripción técnica registrada' }}</td>
                </template>
                <template v-else>
                  <td data-label="Artículo"><input v-model="editForm.name" class="inline-input" type="text" /></td>
                  <td data-label="Descripción"><input v-model="editForm.description" class="inline-input" type="text" /></td>
                </template>

                <td data-label="Lotes">
                  <div v-if="getProductLots(prod.productId).length > 0" class="lots-container">
                    <RouterLink v-for="lot in getProductLots(prod.productId)" :key="lot.lotNumber" class="lot-pill"
                      :to="`/lotes/${lot.lotId}/trazabilidad`"
                      :title="`Expiración: ${formatEpochDate(lot.expirationDate)}. Ver trazabilidad`">
                      <strong>{{ lot.lotNumber }}</strong>: {{ lot.availableQuantity }} / {{ lot.currentStock }} {{ unitLabel(lot.measureUnit) }}
                    </RouterLink>
                  </div>
                  <span v-else class="no-lots">Sin lotes activos</span>
                </td>

                <td data-label="Estado">
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

                <td class="cell-actions" data-label="Acciones">
                  <span v-if="!canManage" class="no-lots">Solo consulta</span>
                  <div v-else-if="editingProductId !== prod.productId" class="actions-wrapper">
                    <button @click="startEdit(prod)" class="btn-action btn-edit" title="Editar Metadatos">Editar</button>
                    <button v-if="prod.active" @click="handleConfirmDelete(prod.productId, prod.name)"
                      class="btn-action btn-delete" title="Dar de baja">
                      Dar de baja
                    </button>
                  </div>
                  <div v-else class="actions-wrapper">
                    <button @click="saveProductEdit" class="btn-action btn-save" title="Guardar">Guardar</button>
                    <button @click="cancelEdit" class="btn-action btn-cancel" title="Cancelar">Cancelar</button>
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
  </div>
</template>

<style scoped>
/* Los formularios de alta, solos en su pestaña, no ocupan todo el ancho */
.form-tab {
  max-width: 760px;
}

:deep(.form-section h3) {
  margin-bottom: 16px;
}

:deep(.inner-form) {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

:deep(.form-grid-inputs) {
  display: grid;
  gap: 14px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 150px), 1fr));
}

.section-title {
  margin-bottom: -4px;
}

.loader {
  padding: 32px;
  color: var(--gargantua-gray);
  font-weight: 600;
  text-align: center;
}

.table-responsive {
  overflow-x: auto;
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th,
.data-table td {
  padding: 12px 14px;
  border-bottom: 1px solid var(--gargantua-border);
  font-size: 0.925rem;
  text-align: left;
  vertical-align: top;
}

.data-table tbody tr:last-child td {
  border-bottom: 0;
}

.data-table th {
  background: var(--gargantua-cream);
  color: var(--gargantua-dark);
  font-weight: 600;
  white-space: nowrap;
}

.code-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.gtin-tag {
  color: var(--gargantua-gray);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 0.8rem;
  white-space: nowrap;
}

.sku-tag {
  color: var(--gargantua-orange-dark);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight: 600;
  white-space: nowrap;
}

.badge-active,
.badge-inactive {
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  font-weight: 700;
}

.badge-active {
  background: var(--gargantua-success-bg);
  color: var(--gargantua-success);
}

.badge-inactive {
  background: var(--gargantua-danger-bg);
  color: var(--gargantua-danger);
}

.row-inactive td {
  background: var(--gargantua-cream);
  color: var(--gargantua-gray);
}

.actions-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.btn-action {
  min-height: 36px;
  padding: 4px 10px;
  background: var(--gargantua-white);
  border-color: var(--gargantua-border);
  color: var(--gargantua-dark);
  font-size: 0.85rem;
  white-space: nowrap;
}

.btn-action:hover {
  background: var(--gargantua-cream);
  border-color: var(--gargantua-orange);
}

.btn-delete {
  color: var(--gargantua-danger);
}

.btn-delete:hover {
  background: var(--gargantua-danger-bg);
  border-color: var(--gargantua-danger);
}

.btn-save {
  background: var(--gargantua-orange);
  color: var(--gargantua-white);
}

.btn-save:hover {
  background: var(--gargantua-orange-dark);
  border-color: var(--gargantua-orange-dark);
}

.data-table code {
  white-space: nowrap;
}

@media (min-width: 1100px) {
  .actions-wrapper {
    flex-wrap: nowrap;
    width: max-content;
  }
}

.lots-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.lot-pill {
  padding: 2px 8px;
  background: var(--gargantua-cream);
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-sm);
  color: inherit;
  font-size: 0.825rem;
  font-weight: 400;
  overflow-wrap: anywhere;
  text-decoration: none;
}

.lot-pill:hover {
  border-color: var(--gargantua-orange);
}

.no-lots {
  color: var(--gargantua-gray);
  font-size: 0.875rem;
}

.text-center {
  padding: 24px !important;
  color: var(--gargantua-gray);
  text-align: center !important;
}

/* En pantallas estrechas cada producto se muestra como una ficha */
@media (max-width: 1000px) {
  .table-responsive {
    overflow: visible;
    background: transparent;
    border: 0;
    box-shadow: none;
  }

  .data-table,
  .data-table tbody,
  .data-table tr,
  .data-table td {
    display: block;
    width: 100%;
  }

  .data-table thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
  }

  .data-table tr {
    margin-bottom: 12px;
    padding: 6px 14px;
    background: var(--gargantua-white);
    border: 1px solid var(--gargantua-border);
    border-radius: var(--radius-md);
    box-shadow: var(--shadow-sm);
  }

  .data-table tr.row-inactive {
    background: var(--gargantua-cream);
  }

  .data-table td,
  .data-table tbody tr:last-child td {
    display: grid;
    align-items: start;
    gap: 12px;
    grid-template-columns: minmax(84px, 30%) minmax(0, 1fr);
    padding: 10px 0;
    background: transparent;
    border-bottom: 1px solid var(--gargantua-border);
    overflow-wrap: anywhere;
  }

  .data-table td:last-child,
  .data-table tbody tr:last-child td:last-child {
    border-bottom: 0;
  }

  .data-table td > span,
  .data-table td > code {
    justify-self: start;
  }

  .data-table td[data-label]::before {
    content: attr(data-label);
    color: var(--gargantua-gray);
    font-size: 0.85rem;
    font-weight: 600;
  }

  .data-table td.text-center {
    display: block;
  }

  .sku-tag {
    white-space: normal;
  }

  .btn-action {
    min-height: var(--control-height);
    padding-inline: 14px;
  }
}
</style>

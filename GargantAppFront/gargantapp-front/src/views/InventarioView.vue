<script setup>
import { ref, onMounted, computed } from 'vue'; // Importamos computed
import { useRouter } from 'vue-router';
import apiClient from '../api';

const products = ref([]);
const loading = ref(true);
const router = useRouter();
const lots = ref([]);

// --- ESTADO DE BÚSQUEDA ---
const searchQuery = ref('');

// --- ESTADO DEL FORMULARIO: NUEVO PRODUCTO ---
const prodSku = ref('');
const prodName = ref('');
const prodDesc = ref('');
const prodSuccess = ref('');
const prodError = ref('');

// --- ESTADO DEL FORMULARIO: NUEVO LOTE ---
const lotProductId = ref('');
const lotNumber = ref('');
const lotQuantity = ref('');
const lotMeasureUnit = ref('UNITS');
const lotExpirationDate = ref('');
const lotSupplierReference = ref('');
const lotSuccess = ref('');
const lotError = ref('');

// Función central para cargar los productos activos
const loadProducts = async () => {
  try {
    products.value = await apiClient.get('/productos');
  } catch (error) {
    console.error("Error al cargar el inventario", error);
  } finally {
    loading.value = false;
  }
};

const loadProductLots = async () => {
  try {
    lots.value = await apiClient.get('/lotes');
  } catch (error) {
    console.error("Error al cargar los lotes", error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadProducts();
  loadProductLots();
});

// Función auxiliar para obtener los lotes de un producto específico
const getProductLots = (productId) => {
  return lots.value.filter(lot => lot.productId === productId);
};

// Propiedad computada para filtrar el catálogo en tiempo real
const filteredProducts = computed(() => {
  const query = searchQuery.value.toLowerCase().trim();
  if (!query) return products.value;

  return products.value.filter(prod => {
    return (
      prod.sku?.toLowerCase().includes(query) ||
      prod.name?.toLowerCase().includes(query) ||
      prod.description?.toLowerCase().includes(query)
    );
  });
});

// Registrar un producto maestro nuevo
const handleCreateProduct = async () => {
  try {
    prodSuccess.value = '';
    prodError.value = '';

    await apiClient.post('/productos', {
      sku: prodSku.value,
      name: prodName.value,
      description: prodDesc.value
    });

    prodSuccess.value = '🟢 Producto maestro añadido al catálogo.';
    
    prodSku.value = ''; prodName.value = ''; prodDesc.value = '';
    await loadProducts();
  } catch (error) {
    prodError.value = '❌ Error al crear el producto. Comprueba que el SKU no esté duplicado.';
  }
};

// Crear un lote físico para un producto
const handleCreateLot = async () => {
  try {
    lotSuccess.value = '';
    lotError.value = '';

    await apiClient.post('/lotes', {
      productId: parseInt(lotProductId.value),
      lotNumber: lotNumber.value,
      quantity: parseFloat(lotQuantity.value),
      measureUnit: lotMeasureUnit.value,
      expirationDate: lotExpirationDate.value ? new Date(lotExpirationDate.value).getTime() : null,
      supplierReference: lotSupplierReference.value
    });

    lotSuccess.value = '🟢 Lote físico registrado e indexado correctamente.';
    
    // Limpiamos el formulario de lotes
    lotProductId.value = ''; lotNumber.value = ''; lotQuantity.value = '';
    lotExpirationDate.value = ''; lotSupplierReference.value = '';
    
    // Refrescamos los lotes para que aparezcan inmediatamente en la tabla
    await loadProductLots();
  } catch (error) {
    lotError.value = '❌ Error al crear el lote. Verifica que el código de lote sea único.';
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
        
        <section class="form-section">
          <h3>🍎 1. Declarar Producto Maestro</h3>
          <form @submit.prevent="handleCreateProduct" class="inner-form">
            <div class="form-group">
              <label>Código SKU (*):</label>
              <input v-model="prodSku" type="text" required placeholder="Ej: GAR-TOR-01" />
            </div>
            <div class="form-group">
              <label>Nombre Comercial (*):</label>
              <input v-model="prodName" type="text" required placeholder="Ej: Tortilla con Cebolla" />
            </div>
            <div class="form-group">
              <label>Descripción Técnica:</label>
              <textarea v-model="prodDesc" rows="2" placeholder="Ingredientes, alérgenos..."></textarea>
            </div>
            <button type="submit" class="btn-submit btn-prod">Añadir al Catálogo</button>
            <p v-if="prodSuccess" class="msg-success">{{ prodSuccess }}</p>
            <p v-if="prodError" class="msg-error">{{ prodError }}</p>
          </form>
        </section>

        <section class="form-section">
          <h3>📦 2. Registrar Entrada de Lote</h3>
          <form @submit.prevent="handleCreateLot" class="inner-form">
            <div class="form-group">
              <label>Asociar a Producto (*):</label>
              <select v-model="lotProductId" required>
                <option value="" disabled selected>Selecciona un artículo comercial...</option>
                <option v-for="prod in products" :key="prod.productId" :value="prod.productId">
                  {{ prod.name }} ({{ prod.sku }})
                </option>
              </select>
            </div>
            <div class="form-grid-inputs">
              <div class="form-group">
                <label>Número de Lote (*):</label>
                <input v-model="lotNumber" type="text" required placeholder="Ej: LOT-2026-A" />
              </div>
              <div class="form-group">
                <label>Cantidad Inicial (*):</label>
                <input v-model="lotQuantity" type="number" step="0.0001" required placeholder="0.00" />
              </div>
              <div class="form-group">
                <label>Unidad Medida:</label>
                <select v-model="lotMeasureUnit">
                  <option value="UNITS">Unidades (Fichas)</option>
                  <option value="KILOS">Kilogramos (Materia prima)</option>
                  <option value="BOXES">Cajas de empaquetado</option>
                </select>
              </div>
              <div class="form-group">
                <label>Fecha Caducidad:</label>
                <input v-model="lotExpirationDate" type="date" />
              </div>
            </div>
            <div class="form-group">
              <label>Referencia Albarán / Proveedor:</label>
              <input v-model="lotSupplierReference" type="text" placeholder="Ej: ALB-PROV-99" />
            </div>
            <button type="submit" class="btn-submit btn-lot">Dar de Alta Lote</button>
            <p v-if="lotSuccess" class="msg-success">{{ lotSuccess }}</p>
            <p v-if="lotError" class="msg-error">{{ lotError }}</p>
          </form>
        </section>

      </div>

      <hr class="section-divider" />

      <div class="search-box">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="🔍 Buscar por SKU, nombre del artículo o descripción técnica..." 
          class="search-input"
        />
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
            </tr>
          </thead>
          <tbody>
            <tr v-for="prod in filteredProducts" :key="prod.productId">
              <td><code>#{{ prod.productId }}</code></td>
              <td><span class="sku-tag">{{ prod.sku }}</span></td>
              <td><strong>{{ prod.name }}</strong></td>
              <td>{{ prod.description || 'Sin descripción técnica registrada' }}</td>
              
              <td>
                <div v-if="getProductLots(prod.productId).length > 0" class="lots-container">
                  <span 
                    v-for="lot in getProductLots(prod.productId)" 
                    :key="lot.lotNumber" 
                    class="lot-pill"
                    :title="`Expiración: ${lot.expirationDate ? new Date(lot.expirationDate).toLocaleDateString() : 'N/A'}`"
                  >
                    <strong>{{ lot.lotNumber }}</strong>: {{ lot.quantity }} {{ lot.measureUnit }}
                  </span>
                </div>
                <span v-else class="no-lots">Sin lotes activos</span>
              </td>

              <td><span class="badge-active">Activo</span></td>
            </tr>
            <tr v-if="filteredProducts.length === 0">
              <td colspan="6" class="text-center">No se encontraron productos que coincidan con la búsqueda.</td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  </div>
</template>

<style scoped>
.content { display: flex; flex-direction: column; height: 100%; }
.card-header { background: var(--gargantua-gold); padding: 20px; display: flex; justify-content: space-between; align-items: center; color: var(--gargantua-dark); }
.card-header h2 { margin: 0; font-size: 1.3rem; }

.btn-back { 
  background: var(--gargantua-dark); color: var(--gargantua-white); border: none; 
  padding: 8px 14px; border-radius: var(--radius-sm); cursor: pointer; font-weight: bold; 
  -webkit-appearance: none; appearance: none;
}

.card-body { padding: 25px; flex-grow: 1; display: flex; flex-direction: column; gap: 20px; }

/* CONTENEDOR DE FORMULARIOS */
.forms-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 25px;
}

.form-section {
  background: var(--gargantua-white);
  border: 1px solid var(--gargantua-border);
  padding: 20px;
  border-radius: var(--radius-md);
  box-shadow: 0 4px 10px rgba(62,39,18,0.02);
}
.form-section h3 { margin-top: 0; color: var(--gargantua-dark); margin-bottom: 15px; border-bottom: 2px solid var(--gargantua-cream); padding-bottom: 5px; }

.inner-form { display: flex; flex-direction: column; gap: 12px; }
.form-grid-inputs { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

.form-group { display: flex; flex-direction: column; gap: 4px; }
label { font-weight: bold; color: var(--gargantua-dark); font-size: 0.85rem; }
input, select, textarea { 
  padding: 10px; border: 1px solid var(--gargantua-border); border-radius: var(--radius-sm); 
  background: var(--gargantua-cream); color: var(--gargantua-dark); font-size: 0.95rem; 
  -webkit-appearance: none; appearance: none; box-sizing: border-box; width: 100%;
}
input:focus, select:focus, textarea:focus { outline: none; border-color: var(--gargantua-orange); }

.btn-submit { 
  padding: 11px; color: white; border: none; border-radius: var(--radius-sm); 
  font-weight: bold; cursor: pointer; transition: var(--transition); margin-top: 5px;
  -webkit-appearance: none; appearance: none;
}
.btn-prod { background: var(--gargantua-dark); color: var(--gargantua-gold); }
.btn-prod:hover { background: var(--gargantua-orange); color: white; }
.btn-lot { background: var(--gargantua-orange); }
.btn-lot:hover { background: var(--gargantua-dark); }

.msg-success { color: var(--gargantua-success); font-weight: bold; margin: 5px 0 0 0; font-size: 0.85rem; }
.msg-error { color: var(--gargantua-danger); font-weight: bold; margin: 5px 0 0 0; font-size: 0.85rem; }

.section-divider { border: 0; border-top: 2px solid var(--gargantua-border); margin: 10px 0; }
.section-title { margin: 0; color: var(--gargantua-dark); }

/* BARRA DE BÚSQUEDA */
.search-box {
  width: 100%;
}
.search-input {
  background: var(--gargantua-white);
  border: 2px solid var(--gargantua-border);
  padding: 12px 15px;
  font-size: 1rem;
  border-radius: var(--radius-md);
}
.search-input:focus {
  border-color: var(--gargantua-orange);
  background: var(--gargantua-cream);
}

/* TABLA DE ARTÍCULOS Y ESTILOS DE LOTES */
.loader { text-align: center; padding: 30px; font-weight: bold; color: var(--gargantua-orange); }
.table-responsive { width: 100%; overflow-x: auto; background: var(--gargantua-white); border-radius: var(--radius-md); border: 1px solid var(--gargantua-border); }
.data-table { width: 100%; border-collapse: collapse; }
.data-table th, .data-table td { padding: 12px 14px; text-align: left; border-bottom: 1px solid var(--gargantua-border); font-size: 0.9rem; }
.data-table th { background: var(--gargantua-cream); color: var(--gargantua-dark); font-weight: bold; }

.sku-tag { background: var(--gargantua-cream); padding: 3px 6px; border-radius: 4px; border: 1px dashed var(--gargantua-orange); font-family: monospace; font-weight: bold; color: var(--gargantua-orange); }
.badge-active { background: var(--gargantua-success); color: white; padding: 3px 8px; border-radius: 4px; font-size: 0.75rem; font-weight: bold; }

/* Contenedores de etiquetas de lotes */
.lots-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 300px;
}
.lot-pill {
  background: var(--gargantua-cream);
  border: 1px solid var(--gargantua-border);
  color: var(--gargantua-dark);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.8rem;
  font-family: monospace;
  white-space: nowrap;
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

/* RESPONSIVIDAD */
@media (max-width: 850px) {
  .forms-container { grid-template-columns: 1fr; gap: 20px; }
  .form-grid-inputs { grid-template-columns: 1fr; }
  .card-header { padding: 15px; }
  .card-body { padding: 15px; }
  .data-table th, .data-table td { padding: 10px; font-size: 0.8rem; }
}
</style>
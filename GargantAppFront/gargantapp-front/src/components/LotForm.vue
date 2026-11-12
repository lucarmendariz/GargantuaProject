<script setup>
import { computed, ref } from 'vue';
import SearchSelect from './SearchSelect.vue';
import apiClient from '../api';
import CameraScanner from './CameraScanner.vue';
import { dateInputToEpoch } from '../utils/date';
import { normalizeGtin, parseLabel } from '../utils/labelParser';
import { MEASURE_UNITS } from '../utils/units';

const props = defineProps({
  products: { type: Array, required: true }
});
const productOptions = computed(() => props.products.map((prod) => ({ value: prod.productId, label: `${prod.name} (${prod.sku})` })));

const emit = defineEmits(['success']);

const lotProductId = ref('');
const lotNumber = ref('');
const lotQuantity = ref('');
const lotMeasureUnit = ref('UNITS');
const lotExpirationDate = ref('');
const lotSupplierReference = ref('');
const lotSuccess = ref('');
const lotError = ref('');

// --- LECTURA DE LA ETIQUETA CON LA CÁMARA ---
const scannerOpen = ref(false);
const scanSummary = ref('');
const scanWarning = ref('');
const scanRaw = ref('');
const scanInfo = ref('');
// Código de barras del producto leído en la etiqueta: si el producto aún no lo tiene,
// se le guarda al dar de alta el lote para reconocerlo solo la próxima vez
const scannedGtin = ref('');

const openScanner = () => {
  lotSuccess.value = '';
  lotError.value = '';
  scannerOpen.value = true;
};

// Rellena solo los campos que se han reconocido; el usuario revisa y da de alta
const onScan = ({ text, source }) => {
  scannerOpen.value = false;
  const label = parseLabel(text, { products: props.products, source });
  scanRaw.value = label.raw;
  scannedGtin.value = normalizeGtin(label.gtin) || '';
  scanInfo.value = scannedGtin.value && !label.productId
    ? `El código de barras ${scannedGtin.value} aún no está asociado a ningún producto. Elige el producto: al dar de alta el lote se guardará y la próxima vez se reconocerá solo.`
    : '';
  const found = [];

  if (label.productId) {
    lotProductId.value = label.productId;
    found.push(`producto ${label.sku}`);
  } else if (scannedGtin.value) {
    // Código de un producto desconocido: no dejar elegido el de un escaneo anterior
    lotProductId.value = '';
  }
  if (label.lotNumber) {
    lotNumber.value = label.lotNumber;
    found.push(`lote ${label.lotNumber}`);
  }
  if (label.quantity !== undefined) {
    lotQuantity.value = label.quantity;
    found.push(`cantidad ${label.quantity}`);
  }
  if (label.expirationDate) {
    lotExpirationDate.value = label.expirationDate;
    found.push(`caducidad ${label.expirationDate.split('-').reverse().join('/')}`);
  }
  if (label.supplierReference) {
    lotSupplierReference.value = label.supplierReference;
    found.push(`albarán ${label.supplierReference}`);
  }

  if (found.length) {
    scanSummary.value = `Leído (${label.format}): ${found.join(', ')}. Revisa los datos y completa lo que falte antes de dar de alta.`;
    scanWarning.value = label.sku && !label.productId ? `El SKU ${label.sku} no está en el catálogo.` : '';
  } else {
    scanSummary.value = '';
    scanWarning.value = 'No se ha reconocido ningún dato del lote. Revisa el texto leído o inténtalo de nuevo.';
  }
};

// Guarda en el producto elegido el código de barras leído, si todavía no tenía ninguno.
// No bloquea el alta del lote: si falla, solo se avisa.
const rememberBarcode = async () => {
  const product = props.products.find((item) => item.productId === Number(lotProductId.value));
  if (!scannedGtin.value || !product || product.gtin) return '';
  try {
    await apiClient.put(`/productos/${product.productId}`, { ...product, gtin: scannedGtin.value });
    return `Código de barras guardado en "${product.name}": la próxima vez se reconocerá solo.`;
  } catch (error) {
    return error?.status === 409
      ? 'El código de barras no se ha guardado porque ya es de otro producto.'
      : 'No se pudo guardar el código de barras en el producto.';
  }
};

const handleCreateLot = async () => {
  try {
    lotSuccess.value = '';
    lotError.value = '';

    await apiClient.post('/lotes', {
      productId: parseInt(lotProductId.value),
      lotNumber: lotNumber.value,
      quantity: parseFloat(lotQuantity.value),
      measureUnit: lotMeasureUnit.value,
      expirationDate: dateInputToEpoch(lotExpirationDate.value),
      supplierReference: lotSupplierReference.value
    });

    lotSuccess.value = 'Lote físico registrado e indexado correctamente.';
    const barcodeMessage = await rememberBarcode();
    if (barcodeMessage) lotSuccess.value += ` ${barcodeMessage}`;

    lotProductId.value = ''; lotNumber.value = ''; lotQuantity.value = '';
    lotExpirationDate.value = ''; lotSupplierReference.value = '';
    scanSummary.value = ''; scanWarning.value = ''; scanRaw.value = ''; scanInfo.value = ''; scannedGtin.value = '';
    
    emit('success');
  } catch (error) {
    lotError.value = 'Error al crear el lote. Verifica que el código de lote sea único.';
  }
};
</script>

<template>
  <section class="form-section panel">
    <h3>Añadir lote (entrada de mercancía)</h3>
    <form @submit.prevent="handleCreateLot" class="inner-form">
      <button type="button" class="btn-secondary btn-scan" @click="openScanner">
        Escanear etiqueta con la cámara
      </button>
      <div v-if="scanSummary || scanWarning" class="scan-result">
        <p v-if="scanSummary" class="msg-success" role="status">{{ scanSummary }}</p>
        <p v-if="scanWarning" class="msg-error" role="alert">{{ scanWarning }}</p>
        <p v-if="scanInfo" class="item-meta scan-info">{{ scanInfo }}</p>
        <details v-if="scanRaw">
          <summary>Ver texto leído</summary>
          <pre class="scan-raw">{{ scanRaw }}</pre>
        </details>
      </div>

      <div class="form-group">
        <label for="lot-product">Asociar a Producto (*):</label>
        <SearchSelect id="lot-product" v-model="lotProductId" :options="productOptions" required placeholder="Busca un artículo comercial..." />
      </div>
      <div class="form-grid-inputs">
        <div class="form-group">
          <label for="lot-number">Número de Lote (*):</label>
          <input id="lot-number" v-model="lotNumber" type="text" required placeholder="Ej: LOT-2026-A" />
        </div>
        <div class="form-group">
          <label for="lot-quantity">Cantidad Inicial (*):</label>
          <input id="lot-quantity" v-model="lotQuantity" type="number" step="0.0001" required placeholder="0.00" />
        </div>
        <div class="form-group">
          <label for="lot-unit">Unidad Medida:</label>
          <select id="lot-unit" v-model="lotMeasureUnit">
            <option v-for="unit in MEASURE_UNITS" :key="unit.value" :value="unit.value">{{ unit.label }}</option>
          </select>
        </div>
        <div class="form-group">
          <label for="lot-expiration">Fecha Caducidad:</label>
          <input id="lot-expiration" v-model="lotExpirationDate" type="date" />
        </div>
      </div>
      <div class="form-group">
        <label for="lot-supplier">Referencia Albarán / Proveedor:</label>
        <input id="lot-supplier" v-model="lotSupplierReference" type="text" placeholder="Ej: ALB-PROV-99" />
      </div>
      <button type="submit" class="btn-submit btn-lot">Dar de Alta Lote</button>
      <p v-if="lotSuccess" class="msg-success">{{ lotSuccess }}</p>
      <p v-if="lotError" class="msg-error">{{ lotError }}</p>
    </form>

    <CameraScanner v-if="scannerOpen" title="Escanear etiqueta del lote" @result="onScan" @close="scannerOpen = false" />
  </section>
</template>

<style scoped>
.scan-result {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.scan-result p {
  margin: 0;
}

.scan-info {
  padding: 8px 10px;
  background: #dbeafe;
  border-radius: var(--radius-sm);
  color: #1e40af;
}

.scan-raw {
  max-height: 140px;
  margin: 6px 0 0;
  padding: 8px;
  overflow: auto;
  background: var(--gargantua-cream);
  border-radius: var(--radius-sm);
  font-size: 0.8rem;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>

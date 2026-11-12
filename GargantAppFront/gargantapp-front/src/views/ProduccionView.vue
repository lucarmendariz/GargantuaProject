<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';
import CameraScanner from '../components/CameraScanner.vue';
import SearchSelect from '../components/SearchSelect.vue';
import { errorMessage as describeError } from '../utils/errors';
import { parseLabel } from '../utils/labelParser';
import { unitLabel } from '../utils/units';
import { can, getUser } from '../utils/session';

const router = useRouter();
// Levantar un bloqueo de calidad: solo quien gestiona los lotes (la API también lo exige)
const canUnblock = can('LOTES', 'GESTIONAR');

// Lotes para elegir por su número o su producto (no por el ID interno)
const lots = ref([]);
const products = ref([]);
const lotsError = ref('');
const productName = (productId) => products.value.find((product) => product.productId === productId)?.name || '';
const lotOptions = computed(() => lots.value.map((lot) => ({
  value: lot.lotId,
  label: `${lot.lotNumber} · ${productName(lot.productId)} · disponible ${lot.availableQuantity} ${unitLabel(lot.measureUnit)}`,
})));
const selectedLot = computed(() => lots.value.find((lot) => lot.lotId === Number(lotId.value)));

const loadLots = async () => {
  try {
    [lots.value, products.value] = await Promise.all([apiClient.get('/lotes'), apiClient.get('/productos/admin')]);
  } catch (error) {
    lotsError.value = await describeError(error, 'No se pudieron cargar los lotes.');
  }
};
onMounted(loadLots);

// Cámara: lee la etiqueta, busca el lote por su número y lo deja elegido junto con el texto OCR
const scannerOpen = ref(false);
const scannedLot = ref('');
const onScan = async ({ text, source }) => {
  scannerOpen.value = false;
  successMessage.value = '';
  errorMessage.value = '';
  scannedLot.value = '';
  const label = parseLabel(text, { source });
  // El campo es de una línea: se separan las líneas de la etiqueta con " | "
  ocrRawText.value = label.raw.split(/\s*\n\s*/).filter(Boolean).join(' | ');
  if (!label.lotNumber) {
    errorMessage.value = 'No se ha encontrado un número de lote en lo leído. Búscalo en la lista.';
    return;
  }
  try {
    const status = await apiClient.get(`/trazabilidad/lotes/${encodeURIComponent(label.lotNumber)}/status`);
    lotId.value = status.lotId;
    scannedLot.value = `${status.lotNumber} · ${status.productName} (estado: ${status.currentState || 'sin movimientos'})`;
  } catch (error) {
    errorMessage.value = error?.status === 404
      ? `El lote ${label.lotNumber} no existe en el sistema.`
      : 'No se pudo consultar el lote. Inténtalo de nuevo.';
  }
};
const lotId = ref('');
const actionType = ref('EN_PRODUCCION');
const quantityAffected = ref('');
const ocrRawText = ref('');
const observations = ref('');

const successMessage = ref('');
const errorMessage = ref('');
const operatorName = getUser()?.name || 'Operario Anónimo';

const submitProductionLog = async () => {
  try {
    successMessage.value = '';
    errorMessage.value = '';

    // POST nativo a /api/historial/escanear
    await apiClient.post('/historial/escanear', {
      lotId: parseInt(lotId.value),
      actionType: actionType.value,
      quantityAffected: quantityAffected.value ? parseFloat(quantityAffected.value) : null,
      ocrRawText: ocrRawText.value || `MANUAL_ENTRY_PROD_FLOOR_${Date.now()}`,
      observations: observations.value
    });

    successMessage.value = actionType.value === 'EN_PRODUCCION'
      ? `Uso registrado: se han descontado ${quantityAffected.value} ${unitLabel(selectedLot.value?.measureUnit)} del lote ${selectedLot.value?.lotNumber ?? ''}.`
      : 'Movimiento registrado correctamente.';
    // El stock disponible ha cambiado
    loadLots();

    // Limpiar el formulario menos el operario
    lotId.value = ''; quantityAffected.value = ''; ocrRawText.value = ''; observations.value = ''; scannedLot.value = '';
  } catch (error) {
    // Motivo de la API: lote inexistente, bloqueado por calidad, sin permiso para desbloquear...
    errorMessage.value = await describeError(error, 'Error al registrar el movimiento.');
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>Terminal de Producción en Planta</h2>
      <button @click="router.push('/dashboard')" class="btn-back">Volver</button>
    </header>

    <div class="card-body">
      <p class="subtitle">Registra la materia prima que se usa en cocina (se descuenta del stock) o bloquea un lote por calidad.</p>
      
      <form @submit.prevent="submitProductionLog" class="prod-form panel">
        <div class="scan-row">
          <button type="button" class="btn-secondary" @click="scannerOpen = true">Escanear etiqueta con la cámara</button>
          <span v-if="scannedLot" class="item-meta" role="status">Lote leído: {{ scannedLot }}</span>
        </div>
        <div class="form-grid">
          <div class="form-group">
            <label for="prod-lot">Lote (*):</label>
            <SearchSelect id="prod-lot" v-model="lotId" :options="lotOptions" required placeholder="Busca por número de lote o producto..." />
            <span v-if="lotsError" class="msg-error">{{ lotsError }}</span>
          </div>

          <div class="form-group">
            <label for="prod-action">Operación:</label>
            <select id="prod-action" v-model="actionType">
              <option value="EN_PRODUCCION">USAR EN PRODUCCIÓN (descuenta del stock)</option>
              <option value="BLOQUEADO">BLOQUEAR (control de calidad)</option>
              <option v-if="canUnblock" value="DESBLOQUEADO">DESBLOQUEAR (levantar el bloqueo de calidad)</option>
            </select>
          </div>

          <div class="form-group">
            <label for="prod-quantity">
              {{ actionType === 'EN_PRODUCCION' ? 'Cantidad usada (*)' : 'Cantidad afectada' }}{{ selectedLot ? ` (${unitLabel(selectedLot.measureUnit)})` : '' }}:
            </label>
            <input id="prod-quantity" v-model="quantityAffected" type="number" min="0" step="0.0001" placeholder="Ej: 50"
                   :required="actionType === 'EN_PRODUCCION'" />
            <span v-if="actionType === 'EN_PRODUCCION' && selectedLot" class="item-meta">
              Disponible: {{ selectedLot.availableQuantity }} {{ unitLabel(selectedLot.measureUnit) }} (lo reservado para pedidos no se puede usar)
            </span>
          </div>

          <div class="form-group">
            <label for="prod-ocr">Texto de la etiqueta (opcional, por si la cámara no lee bien):</label>
            <input id="prod-ocr" v-model="ocrRawText" type="text" placeholder="Lectura en bruto de la cámara..." />
          </div>
        </div>

        <div class="form-group full-width">
          <label for="prod-notes">Observaciones de Planta:</label>
          <textarea id="prod-notes" v-model="observations" rows="3" placeholder="Detalles de la cochura o lote de huevos/patatas usado..."></textarea>
        </div>

        <div class="footer-actions">
          <span class="operator-tag">Registra: {{ operatorName }}</span>
          <button type="submit" class="btn-submit">Enviar a Base de Datos</button>
        </div>
      </form>

      <p v-if="successMessage" class="msg-success">{{ successMessage }}</p>
      <p v-if="errorMessage" class="msg-error">{{ errorMessage }}</p>
    </div>

    <CameraScanner v-if="scannerOpen" title="Escanear etiqueta del lote" @result="onScan" @close="scannerOpen = false" />
  </div>
</template>

<style scoped>
.scan-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
}

.subtitle {
  color: var(--gargantua-gray);
}

.prod-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 260px), 1fr));
}

.footer-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-top: 16px;
  border-top: 1px solid var(--gargantua-border);
}

.operator-tag {
  color: var(--gargantua-gray);
  font-size: 0.9rem;
  font-weight: 600;
}

@media (max-width: 480px) {
  .btn-submit {
    width: 100%;
  }
}
</style>

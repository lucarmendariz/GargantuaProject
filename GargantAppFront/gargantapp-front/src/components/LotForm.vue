<script setup>
import { ref } from 'vue';
import apiClient from '../api';

defineProps({
  products: { type: Array, required: true }
});

const emit = defineEmits(['success']);

const lotProductId = ref('');
const lotNumber = ref('');
const lotQuantity = ref('');
const lotMeasureUnit = ref('UNITS');
const lotExpirationDate = ref('');
const lotSupplierReference = ref('');
const lotSuccess = ref('');
const lotError = ref('');

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
    
    lotProductId.value = ''; lotNumber.value = ''; lotQuantity.value = '';
    lotExpirationDate.value = ''; lotSupplierReference.value = '';
    
    emit('success');
  } catch (error) {
    lotError.value = '❌ Error al crear el lote. Verifica que el código de lote sea único.';
  }
};
</script>

<template>
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
</template>
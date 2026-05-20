<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';

const router = useRouter();
const lotId = ref('');
const actionType = ref('EN_PRODUCCION');
const quantityAffected = ref('');
const ocrRawText = ref('');
const observations = ref('');

const successMessage = ref('');
const errorMessage = ref('');
const operatorName = ref('Operario Anónimo');

onMounted(() => {
  const savedUser = localStorage.getItem('user');
  if (savedUser) {
    operatorName.value = JSON.parse(savedUser).name;
  }
});

const submitProductionLog = async () => {
  try {
    successMessage.value = '';
    errorMessage.value = '';

    // POST nativo a /api/historial/escanear
    const responseMessage = await apiClient.post('/historial/escanear', {
      lotId: parseInt(lotId.value),
      actionType: actionType.value,
      operator: operatorName.value,
      quantityAffected: quantityAffected.value ? parseFloat(quantityAffected.value) : null,
      ocrRawText: ocrRawText.value || `MANUAL_ENTRY_PROD_FLOOR_${Date.now()}`,
      observations: observations.value
    });

    successMessage.value = '🟢 Movimiento de producción enviado y guardado correctamente.';
    
    // Limpiar el formulario menos el operario
    lotId.value = ''; quantityAffected.value = ''; ocrRawText.value = ''; observations.value = '';
  } catch (error) {
    errorMessage.value = '❌ Error al registrar el movimiento. Verifica que el ID de lote sea válido.';
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>🏗️ Terminal de Producción en Planta</h2>
      <button @click="router.push('/dashboard')" class="btn-back">⬅️ Volver</button>
    </header>

    <div class="card-body">
      <p class="subtitle">Registrar manualmente un cambio de estado en la cadena de elaboración.</p>
      
      <form @submit.prevent="submitProductionLog" class="prod-form">
        <div class="form-grid">
          <div class="form-group">
            <label>ID Interno del Lote (*):</label>
            <input v-model="lotId" type="number" required placeholder="Ej: 1" />
          </div>

          <div class="form-group">
            <label>Operación / Estado:</label>
            <select v-model="actionType">
              <option value="EN_PRODUCCION">⚡ EN PRODUCCIÓN (Cocinado/Giro)</option>
              <option value="BLOQUEADO">⚠️ BLOQUEADO (Control de Calidad)</option>
              <option value="VENDIDO">🛒 VENDIDO (Salida de Almacén)</option>
            </select>
          </div>

          <div class="form-group">
            <label>Cantidad Afectada:</label>
            <input v-model="quantityAffected" type="number" step="0.01" placeholder="Ej: 50.00" />
          </div>

          <div class="form-group">
            <label>Texto del Escáner OCR (Opcional):</label>
            <input v-model="ocrRawText" type="text" placeholder="Lectura en bruto de la cámara..." />
          </div>
        </div>

        <div class="form-group full-width">
          <label>Observaciones de Planta:</label>
          <textarea v-model="observations" rows="3" placeholder="Detalles de la cochura o lote de huevos/patatas usado..."></textarea>
        </div>

        <div class="footer-actions">
          <span class="operator-tag">👷‍♂️ Registra: {{ operatorName }}</span>
          <button type="submit" class="btn-submit">Enviar a Base de Datos</button>
        </div>
      </form>

      <p v-if="successMessage" class="msg-success">{{ successMessage }}</p>
      <p v-if="errorMessage" class="msg-error">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<style scoped>
.content { display: flex; flex-direction: column; height: 100%; }
.card-header { background: var(--gargantua-gold); padding: 20px; display: flex; justify-content: space-between; align-items: center; color: var(--gargantua-dark); }
.card-header h2 { margin: 0; font-size: 1.3rem; }
.btn-back { background: var(--gargantua-dark); color: var(--gargantua-white); border: none; padding: 8px 14px; border-radius: var(--radius-sm); cursor: pointer; font-weight: bold; }
.card-body { padding: 25px; flex-grow: 1; }
.subtitle { color: var(--gargantua-gray); margin-top: 0; margin-bottom: 25px; font-size: 0.95rem; }

.prod-form { display: flex; flex-direction: column; gap: 15px; background: var(--gargantua-white); padding: 20px; border-radius: var(--radius-md); border: 1px solid var(--gargantua-border); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 15px; }
.form-group { display: flex; flex-direction: column; gap: 5px; }
.full-width { grid-column: span 2; }
label { font-weight: bold; color: var(--gargantua-dark); font-size: 0.9rem; }
input, select, textarea { padding: 11px; border: 1px solid var(--gargantua-border); border-radius: var(--radius-sm); background: var(--gargantua-cream); color: var(--gargantua-dark); font-size: 1rem; -webkit-appearance: none; appearance: none; }
input:focus, select:focus, textarea:focus { outline: none; border-color: var(--gargantua-orange); }

.footer-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; border-top: 1px solid var(--gargantua-border); padding-top: 15px; flex-wrap: wrap; gap: 10px; }
.operator-tag { font-weight: bold; color: var(--gargantua-gray); font-size: 0.9rem; }
.btn-submit { background: var(--gargantua-orange); color: white; border: none; padding: 12px 24px; border-radius: var(--radius-sm); font-weight: bold; cursor: pointer; }

.msg-success { color: var(--gargantua-success); font-weight: bold; margin-top: 15px; }
.msg-error { color: var(--gargantua-danger); font-weight: bold; margin-top: 15px; }

@media (max-width: 600px) {
  .form-grid { grid-template-columns: 1fr; }
  .full-width { grid-column: span 1; }
  .footer-actions { flex-direction: column; align-items: stretch; text-align: center; }
}
</style>
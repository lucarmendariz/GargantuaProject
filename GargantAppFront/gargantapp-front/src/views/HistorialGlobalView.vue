<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';

const lotIdInput = ref('');
const historyLogs = ref([]);
const searchDone = ref(false);
const errorMsg = ref('');
const router = useRouter();

const fetchHistory = async () => {
  if (!lotIdInput.value) return;
  try {
    errorMsg.value = '';
    historyLogs.value = await apiClient.get(`/historial/lote/${lotIdInput.value}`);
    searchDone.value = true;
  } catch (error) {
    historyLogs.value = [];
    searchDone.value = true;
    errorMsg.value = 'No se encontraron registros de auditoría para este ID de lote.';
  }
};
</script>

<template>
  <div class="content">
    <header class="card-header">
      <h2>📜 Visibilizar Historial de Auditoría</h2>
      <button @click="router.push('/dashboard')" class="btn-back">⬅️ Volver</button>
    </header>

    <div class="card-body">
      <div class="search-box">
        <input v-model="lotIdInput" type="number" placeholder="Introduce el ID numérico del lote (Ej: 1, 2)..." @keyup.enter="fetchHistory" />
        <button @click="fetchHistory">Ver Auditoría</button>
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div v-if="historyLogs.length > 0" class="timeline">
        <div v-for="log in historyLogs" :key="log.historyId" class="timeline-item">
          <div class="timeline-badge" :class="log.actionType.toLowerCase()">
            📌
          </div>
          <div class="timeline-panel">
            <div class="timeline-heading">
              <span class="action-title">{{ log.actionType }}</span>
              <span class="action-time">⏱️ {{ log.actionTimestamp }}</span>
            </div>
            <div class="timeline-body">
              <p><strong>Operario en planta:</strong> {{ log.operator }}</p>
              <p v-if="log.quantityAffected"><strong>Cantidad afectada:</strong> {{ log.quantityAffected }}</p>
              <p><strong>Observaciones registradas:</strong> {{ log.observations || 'Ninguna' }}</p>
              <div class="ocr-dump">
                <code>{{ log.ocrRawText }}</code>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <p v-else-if="searchDone && !errorMsg" class="info-empty">Este lote existe pero no tiene movimientos registrados.</p>
    </div>
  </div>
</template>

<style scoped>
.content { display: flex; flex-direction: column; height: 100%; }
.card-header { background: var(--gargantua-gold); padding: 20px; display: flex; justify-content: space-between; align-items: center; color: var(--gargantua-dark); }
.card-header h2 { margin: 0; font-size: 1.3rem; }
.btn-back { background: var(--gargantua-dark); color: var(--gargantua-white); border: none; padding: 8px 14px; border-radius: var(--radius-sm); cursor: pointer; font-weight: bold; }
.card-body { padding: 25px; flex-grow: 1; }
.search-box { display: flex; gap: 10px; margin-bottom: 25px; }
.search-box input { flex-grow: 1; padding: 12px; border: 2px solid var(--gargantua-border); border-radius: var(--radius-md); background: var(--gargantua-cream); font-size: 1rem; }
.search-box button { background: var(--gargantua-orange); color: white; border: none; padding: 0 20px; border-radius: var(--radius-md); font-weight: bold; cursor: pointer; }
.error { color: var(--gargantua-danger); font-weight: bold; }
.info-empty { text-align: center; color: var(--gargantua-gray); margin-top: 30px; }

/* Estilos de la Línea de Tiempo Industrial */
.timeline { position: relative; padding: 10px 0; list-style: none; }
.timeline::before { content: " "; position: absolute; top: 0; bottom: 0; left: 20px; width: 3px; background-color: var(--gargantua-border); }
.timeline-item { position: relative; margin-bottom: 20px; display: flex; gap: 20px; }
.timeline-badge { width: 40px; height: 40px; background: var(--gargantua-gold); border-radius: 50%; display: flex; align-items: center; justify-content: center; z-index: 2; flex-shrink: 0; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }
.timeline-panel { background: var(--gargantua-white); border: 1px solid var(--gargantua-border); padding: 15px; border-radius: var(--radius-md); flex-grow: 1; box-shadow: 0 2px 8px rgba(0,0,0,0.02); }
.timeline-heading { display: flex; justify-content: space-between; border-bottom: 1px solid var(--gargantua-border); padding-bottom: 8px; margin-bottom: 8px; flex-wrap: wrap; gap: 5px; }
.action-title { font-weight: bold; color: var(--gargantua-dark); }
.action-time { font-size: 0.8rem; color: var(--gargantua-gray); }
.timeline-body p { margin: 4px 0; font-size: 0.9rem; }
.ocr-dump { margin-top: 8px; background: var(--gargantua-cream); padding: 6px; border-radius: var(--radius-sm); border: 1px dashed var(--gargantua-orange); }
.ocr-dump code { font-family: monospace; font-size: 0.8rem; color: var(--gargantua-dark); }

@media (max-width: 600px) {
  .search-box { flex-direction: column; }
  .timeline-heading { flex-direction: column; align-items: flex-start; }
}
</style>
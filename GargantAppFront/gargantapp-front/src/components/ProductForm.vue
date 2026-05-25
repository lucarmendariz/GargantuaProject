<script setup>
import { ref } from 'vue';
import apiClient from '../api';

const emit = defineEmits(['success']);

const prodSku = ref('');
const prodName = ref('');
const prodDesc = ref('');
const prodSuccess = ref('');
const prodError = ref('');

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
    
    emit('success');
  } catch (error) {
    prodError.value = '❌ Error al crear el producto. Comprueba que el SKU no esté duplicado.';
  }
};
</script>

<template>
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
</template>
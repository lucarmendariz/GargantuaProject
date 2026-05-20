<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import apiClient from '../api';

const username = ref('');
const password = ref('');
const errorMessage = ref('');
const router = useRouter();

const handleLogin = async () => {
  try {
    errorMessage.value = '';
    
    // Llamada POST nativa a /api/auth/login
    const userData = await apiClient.post('/auth/login', {
      username: username.value,
      password: password.value
    });

    // Guardamos la sesión en el navegador
    localStorage.setItem('user', JSON.stringify(userData));
    
    // Saltamos al panel de control
    router.push('/dashboard');
    
  } catch (error) {
    if (error && error.status === 401) {
      errorMessage.value = 'Usuario o contraseña incorrectos.';
    } else {
      errorMessage.value = 'Error al conectar con el servidor de MonCorp.';
    }
  }
};
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <h2>Gargantua App</h2>
      <p>Control de trazabilidad</p>
      
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>Usuario:</label>
          <input v-model="username" type="text" required placeholder="Ej: luca" />
        </div>
        
        <div class="form-group">
          <label>Contraseña:</label>
          <input v-model="password" type="password" required placeholder="••••••••" />
        </div>
        
        <button type="submit">Entrar al Sistema</button>
      </form>
      
      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}

.login-card {
  background: var(--gargantua-white);
  padding: 35px;
  border-radius: var(--radius-md);
  box-shadow: 0 6px 15px rgba(74, 51, 25, 0.08);
  width: 330px;
  border-top: 5px solid var(--gargantua-gold); /* Detalle dorado arriba */
}

h2 {
  margin-bottom: 5px;
  color: var(--gargantua-dark);
  text-align: center;
}

p {
  color: var(--gargantua-gray);
  font-size: 14px;
  margin-bottom: 25px;
  text-align: center;
}

.form-group {
  margin-bottom: 18px;
}

label {
  display: block;
  margin-bottom: 6px;
  font-weight: bold;
  color: var(--gargantua-dark);
}

input {
  width: 100%;
  padding: 11px;
  border: 1px solid var(--gargantua-border);
  border-radius: var(--radius-sm);
  box-sizing: border-box;
  background-color: var(--gargantua-cream);
  color: var(--gargantua-dark);
}

input:focus {
  outline: none;
  border-color: var(--gargantua-orange);
}

button {
  width: 100%;
  padding: 12px;
  background-color: var(--gargantua-gold);
  color: var(--gargantua-dark);
  border: none;
  border-radius: var(--radius-sm);
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
  transition: var(--transition);
}

button:hover {
  background-color: var(--gargantua-orange);
  color: var(--gargantua-white);
}

.error {
  color: var(--gargantua-danger);
  font-weight: bold;
  margin-top: 15px;
  text-align: center;
}
</style>

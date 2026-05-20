const BASE_URL = 'http://localhost:8080/api';

const apiClient = {
  // Para peticiones GET (como buscar un lote)
  async get(endpoint) {
    const response = await fetch(`${BASE_URL}${endpoint}`);
    if (!response.ok) throw response; // Si da error (404, 500, etc) lo lanzamos
    return response.json(); // Si va bien, devolvemos el JSON masticado
  },

  // Para peticiones POST (como el Login)
  async post(endpoint, body) {
    const response = await fetch(`${BASE_URL}${endpoint}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(body)
    });
    if (!response.ok) throw response;
    return response.json();
  }
};

export default apiClient;
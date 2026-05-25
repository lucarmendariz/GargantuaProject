import { ref, computed } from 'vue';
import apiClient from '../api';

export function useInventory() {
  const products = ref([]);
  const lots = ref([]);
  const loading = ref(true);
  const searchQuery = ref('');

  // Carga el catálogo completo usando la ruta de administración para poder ver e interactuar con los inactivos
  const loadProducts = async () => {
    try {
      products.value = await apiClient.get('/productos/admin');
    } catch (error) {
      console.error("Error al cargar el inventario industrial", error);
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

  const initInventory = async () => {
    loading.value = true;
    await Promise.all([loadProducts(), loadProductLots()]);
  };

  const getProductLots = (productId) => {
    return lots.value.filter(lot => lot.productId === productId);
  };

  // Desactivar un producto (Baja lógica)
  const deleteProduct = async (productId) => {
    try {
      await apiClient.delete(`/productos/${productId}`);
      await loadProducts(); // Recargamos catálogo
    } catch (error) {
      alert("Error al dar de baja el producto maestro.");
    }
  };

 // Modificar o Reactivar un producto
const updateProduct = async (productData) => {
  try {
    await apiClient.put(`/productos/${productData.productId}`, productData);
    await loadProducts();
  } catch (error) {
    // 🔍 ESTO NOS VA A CHIVAR EL ERROR REAL EN LA CONSOLA F12:
    console.error("Fallo crítico en el PUT:", error);
    if (error.response) {
      console.error("Datos devueltos por Spring:", error.response.data);
      console.error("Código de estado HTTP:", error.response.status);
    }
    alert(`Error al actualizar la ficha: ${error.message}`);
  }
};

  const filteredProducts = computed(() => {
    const query = searchQuery.value.toLowerCase().trim();
    if (!query) return products.value;

    return products.value.filter(prod => 
      prod.sku?.toLowerCase().includes(query) ||
      prod.name?.toLowerCase().includes(query) ||
      prod.description?.toLowerCase().includes(query)
    );
  });

  return {
    products,
    lots,
    loading,
    searchQuery,
    initInventory,
    loadProducts,
    loadProductLots,
    getProductLots,
    deleteProduct,
    updateProduct,
    filteredProducts
  };
}
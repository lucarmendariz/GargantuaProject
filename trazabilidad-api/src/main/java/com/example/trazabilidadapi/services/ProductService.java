package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.Product;
import com.example.trazabilidadapi.repositories.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    // 1. Obtener los productos activos (Para la app de planta diaria)
    @Transactional(readOnly = true)
    public List<Product> getAllActiveProducts() {
        return repository.findAll();
    }

    // 2. NUEVO - Obtener todo el catálogo (Incluye inactivos para administración)
    @Transactional(readOnly = true)
    public List<Product> getAllProductsForAdmin() {
        return repository.findAllAdmin();
    }

    // 3. Buscar un producto específico por su ID único
    @Transactional(readOnly = true)
    public Product getProductById(int id) {
        return repository.findById(id);
    }

    // 4. Registrar un nuevo producto maestro utilizando el parámetro OUT
    @Transactional(rollbackFor = Exception.class)
    public Integer insertProduct(Product product) throws SQLException {
        return repository.insertProduct(product);
    }

    // 5. NUEVO - Actualizar datos del producto maestro (SKU, Nombre, Descripción, Estado)
    @Transactional(rollbackFor = Exception.class)
    public void updateProduct(Product product) {
        // Podrías añadir validaciones de negocio previas aquí si lo necesitas
        repository.updateProduct(product);
    }

    // 6. NUEVO - Eliminar producto (Baja lógica mediante el procedimiento Product_Delete)
    @Transactional(rollbackFor = Exception.class)
    public void deleteProduct(int id) {
        repository.deleteProduct(id);
    }
}
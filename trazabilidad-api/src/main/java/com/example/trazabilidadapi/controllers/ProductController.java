package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.models.Product;
import com.example.trazabilidadapi.services.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@CrossOrigin(origins = "*") // Permite la comunicación nativa con tu servidor de Vue 3
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // 1. GET /api/productos : Obtener productos ACTIVOS (Usado por tu vista de inventario)
    @GetMapping
    public ResponseEntity<List<Product>> getAllActive() {
        return ResponseEntity.ok(service.getAllActiveProducts());
    }

    // 2. NUEVO -> GET /api/productos/admin : Catálogo completo para el Panel de Administración
    @GetMapping("/admin")
    public ResponseEntity<List<Product>> getAllForAdmin() {
        return ResponseEntity.ok(service.getAllProductsForAdmin());
    }

    // 3. GET /api/productos/{id} : Buscar un artículo por su clave primaria
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable int id) {
        Product product = service.getProductById(id);
        if (product == null) {
            return ResponseEntity.notFound().build(); // Devuelve HTTP 404
        }
        return ResponseEntity.ok(product); // Devuelve HTTP 200
    }

    // 4. POST /api/productos : Declarar un nuevo producto maestro
    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        try {
            Integer newId = service.insertProduct(product);
            return ResponseEntity.status(HttpStatus.CREATED).body(newId); // Devuelve HTTP 201
        } catch (SQLException e) {
            // Si el procedimiento lanza el error 102 (SKU duplicado), enviamos un HTTP 400 Bad Request
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Error al registrar el producto: Comprueba que el código SKU no esté duplicado.");
        }
    }

    // 5. NUEVO -> PUT /api/productos/{id} : Actualizar los metadatos de un producto
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable int id, @RequestBody Product product) {
        // Validación de seguridad para asegurar que modificamos el recurso correcto
        if (product.getProductId() != id) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El ID del cuerpo no coincide con el ID de la ruta.");
        }

        Product existingProduct = service.getProductById(id);
        if (existingProduct == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            service.updateProduct(product);
            return ResponseEntity.ok().body("Producto maestro actualizado correctamente."); // Devuelve HTTP 200
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error crítico al procesar la actualización del producto.");
        }
    }

    // 6. NUEVO -> DELETE /api/productos/{id} : Baja lógica del producto maestro
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable int id) {
        Product existingProduct = service.getProductById(id);
        if (existingProduct == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            service.deleteProduct(id);
            return ResponseEntity.noContent().build(); // Devuelve HTTP 204 (Éxito, sin contenido de retorno)
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error crítico al procesar la baja del producto.");
        }
    }
}
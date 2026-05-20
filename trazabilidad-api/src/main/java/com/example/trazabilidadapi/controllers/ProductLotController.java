package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.models.CurrentLotStatus;
import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.services.ProductLotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@CrossOrigin(origins = "*")
public class ProductLotController {

    private final ProductLotService service;

    public ProductLotController(ProductLotService service) {
        this.service = service;
    }

    // 2.1 CREATE: Crear un nuevo lote
    @PostMapping
    public ResponseEntity<Integer> createLot(@RequestBody ProductLot productLot) {
        try {
            Integer newId = service.insertLot(productLot);
            return ResponseEntity.status(HttpStatus.CREATED).body(newId);
        } catch (SQLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 2.2 READ (ALL): Obtener todos los lotes
    @GetMapping
    public ResponseEntity<List<ProductLot>> getAllLots() {
        return ResponseEntity.ok(service.getAllLots());
    }

    // 2.3 READ (BY PRODUCT ID): Obtener lotes por ID de producto
    @GetMapping("/producto/{productId}")
    public ResponseEntity<List<ProductLot>> getByProductId(@PathVariable int productId) {
        return ResponseEntity.ok(service.getLotsByProduct(productId));
    }

    // 2.4 READ (BY ID): Obtener un lote específico por su ID interno
    @GetMapping("/{lotId}")
    public ResponseEntity<ProductLot> getById(@PathVariable int lotId) {
        ProductLot lot = service.getLotById(lotId);
        if (lot == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(lot);
    }

    // 2.5 UPDATE: Actualizar los datos de un lote existente
    @PutMapping("/{lotId}")
    public ResponseEntity<Void> updateLot(@PathVariable int lotId, @RequestBody ProductLot productLot) {
        // Aseguramos que el ID de la URL se asigne al objeto antes de enviarlo al service
        productLot.setLotId(lotId);
        service.updateLot(productLot);
        return ResponseEntity.ok().build();
    }

    // 2.6 DELETE: Eliminar un lote
    @DeleteMapping("/{lotId}")
    public ResponseEntity<Void> deleteLot(@PathVariable int lotId) {
        service.deleteLot(lotId);
        return ResponseEntity.noContent().build();
    }

    // 3.3 TRACEABILITY ENGINE: Consultar el estado actual y OCR de un lote por su código de barras
    @GetMapping("/estado/{lotNumber}")
    public ResponseEntity<CurrentLotStatus> getCurrentStatus(@PathVariable String lotNumber) {
        CurrentLotStatus status = service.getCurrentLotStatus(lotNumber);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }
}
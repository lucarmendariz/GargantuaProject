package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.services.ProductLotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/lotes")
@CrossOrigin(origins = "*")
public class ProductLotController {

    private final ProductLotService service;

    public ProductLotController(ProductLotService service) {
        this.service = service;
    }

    @GetMapping("/producto/{productId}")
    public ResponseEntity<List<ProductLot>> getByProductId(@PathVariable int productId) {
        return ResponseEntity.ok(service.getLotsByProduct(productId));
    }
}
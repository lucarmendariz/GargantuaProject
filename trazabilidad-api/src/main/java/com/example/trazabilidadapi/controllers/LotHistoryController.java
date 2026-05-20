package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.models.LotHistory;
import com.example.trazabilidadapi.services.LotHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/historial")
@CrossOrigin(origins = "*")
public class LotHistoryController {

    private final LotHistoryService service;

    public LotHistoryController(LotHistoryService service) {
        this.service = service;
    }

    @GetMapping("/lote/{lotId}")
    public ResponseEntity<List<LotHistory>> getByLotId(@PathVariable int lotId) {
        return ResponseEntity.ok(service.getHistoryByLot(lotId));
    }

    @PostMapping("/escanear")
    public ResponseEntity<String> createOcrEntry(@RequestBody LotHistory history) {
        boolean success = service.registerOcrScan(history);
        if (success) {
            return ResponseEntity.status(HttpStatus.CREATED).body("Escaneo del OCR registrado correctamente.");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se pudo guardar el registro del OCR.");
    }
}
package com.example.trazabilidadapi.controllers;

import com.example.trazabilidadapi.models.CurrentLotStatus;
import com.example.trazabilidadapi.services.LotTrazabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/trazabilidad")
@CrossOrigin(origins = "*")
public class LotTrazabilityController {

    private final LotTrazabilityService service;

    public LotTrazabilityController(LotTrazabilityService service) {
        this.service = service;
    }

    @GetMapping("/lotes/{lotNumber}/status")
    public ResponseEntity<CurrentLotStatus> getLotCurrentStatus(@PathVariable String lotNumber) {
        try {
            CurrentLotStatus status = service.getLotCurrentStatus(lotNumber);
            if (status == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno en el servidor", e);
        }
    }
}
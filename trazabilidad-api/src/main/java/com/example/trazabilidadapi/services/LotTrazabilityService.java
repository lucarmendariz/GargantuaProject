package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.CurrentLotStatus;
import com.example.trazabilidadapi.repositories.LotTrazabilityRepository;
import org.springframework.stereotype.Service;

@Service
public class LotTrazabilityService {

    private final LotTrazabilityRepository repository;

    public LotTrazabilityService(LotTrazabilityRepository repository) {
        this.repository = repository;
    }

    public CurrentLotStatus getLotCurrentStatus(String lotNumber) {
        if (lotNumber == null || lotNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("El código de lote escaneado no puede estar vacío.");
        }
        return repository.getLotCurrentStatus(lotNumber);
    }
}
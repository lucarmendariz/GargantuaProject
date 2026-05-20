package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.LotHistory;
import com.example.trazabilidadapi.repositories.LotHistoryRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class LotHistoryService {

    private final LotHistoryRepository repository;

    public LotHistoryService(LotHistoryRepository repository) {
        this.repository = repository;
    }

    public List<LotHistory> getHistoryByLot(int lotId) {
        return repository.findByLotId(lotId);
    }

    public boolean registerOcrScan(LotHistory history) {
        return repository.save(history) > 0;
    }
}
package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.models.CurrentLotStatus;
import com.example.trazabilidadapi.repositories.ProductLotRepository;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;

@Service
public class ProductLotService {

    private final ProductLotRepository repository;

    public ProductLotService(ProductLotRepository repository) {
        this.repository = repository;
    }

    // 2.1 CREATE
    public Integer insertLot(ProductLot productLot) throws SQLException {
        return repository.insertLot(productLot);
    }

    // 2.2 READ (ALL)
    public List<ProductLot> getAllLots() {
        return repository.getAll();
    }

    // 2.3 READ (BY PRODUCT ID)
    public List<ProductLot> getLotsByProduct(int productId) {
        return repository.findByProductId(productId);
    }

    // 2.4 READ (BY ID)
    public ProductLot getLotById(int lotId) {
        return repository.findById(lotId);
    }

    // 2.5 UPDATE
    public void updateLot(ProductLot productLot) {
        repository.updateLot(productLot);
    }

    // 2.6 DELETE
    public void deleteLot(int lotId) {
        repository.deleteLot(lotId);
    }

    // 3.3 TRACEABILITY ENGINE (OCR / STATUS)
    public CurrentLotStatus getCurrentLotStatus(String lotNumber) {
        return repository.getCurrentStatus(lotNumber);
    }
}
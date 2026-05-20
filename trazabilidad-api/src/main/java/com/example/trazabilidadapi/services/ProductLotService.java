package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.repositories.ProductLotRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductLotService {

    private final ProductLotRepository repository;

    public ProductLotService(ProductLotRepository repository) {
        this.repository = repository;
    }

    public List<ProductLot> getLotsByProduct(int productId) {
        return repository.findByProductId(productId);
    }
}
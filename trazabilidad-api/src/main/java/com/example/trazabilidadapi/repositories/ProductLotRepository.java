package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.util.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class ProductLotRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductLotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ProductLot> findByProductId(int productId) {
        List<ProductLot> list = new ArrayList<>();
        try {
            Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (CallableStatement stmt = conn.prepareCall("{call ProductLot_FindByProductId(?)}")) {
                stmt.setInt(1, productId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getProductLotFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar lotes por producto", e);
        }
        return list;
    }
}
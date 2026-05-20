package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.Product;
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
public class ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Product> findAll() {
        List<Product> list = new ArrayList<>();
        try {
            Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (CallableStatement stmt = conn.prepareCall("{call Product_FindAll()}");
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(ResultSetMapper.getProductFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos", e);
        }
        return list;
    }

    public Product findById(int id) {
        try {
            Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (CallableStatement stmt = conn.prepareCall("{call Product_FindById(?)}")) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return ResultSetMapper.getProductFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por ID", e);
        }
        return null;
    }
}
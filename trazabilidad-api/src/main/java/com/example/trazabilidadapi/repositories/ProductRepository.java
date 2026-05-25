package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.Product;
import com.example.trazabilidadapi.util.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 1. Obtener todos los productos ACTIVOS (Para la app del día a día)
    public List<Product> findAll() {
        String sql = "{call Product_FindAll()}";
        return jdbcTemplate.execute((Connection conn) -> {
            List<Product> list = new ArrayList<>();
            try (CallableStatement stmt = conn.prepareCall(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(ResultSetMapper.getProductFromResultSet(rs));
                }
            }
            return list;
        });
    }

    // 2. Obtener TODO el catálogo (Incluye inactivos para el panel de administración)
    public List<Product> findAllAdmin() {
        String sql = "{call Product_GetAll()}";
        return jdbcTemplate.execute((Connection conn) -> {
            List<Product> list = new ArrayList<>();
            try (CallableStatement stmt = conn.prepareCall(sql);
                 ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(ResultSetMapper.getProductFromResultSet(rs));
                }
            }
            return list;
        });
    }

    // 3. Buscar un producto específico por su ID único
    public Product findById(int id) {
        String sql = "{call Product_FindById(?)}";
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return ResultSetMapper.getProductFromResultSet(rs);
                    }
                }
            }
            return null;
        });
    }

    // 4. Insertar un nuevo producto maestro utilizando el parámetro OUT
    public Integer insertProduct(Product product) throws SQLException {
        String sql = "{call Product_Create(?, ?, ?, ?)}";
        return jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setString(1, product.getSku());
                stmt.setString(2, product.getName());
                stmt.setString(3, product.getDescription());
                stmt.registerOutParameter(4, Types.INTEGER);

                stmt.execute();
                return stmt.getInt(4);
            } catch (SQLException e) {
                throw new RuntimeException("Error ejecutando Product_Create en DB", e);
            }
        });
    }

    // 5. NUEVO - Actualizar los datos de un producto existente
    public void updateProduct(Product product) {
        String sql = "{call Product_Update(?, ?, ?, ?, ?)}";
        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, product.getProductId());
                stmt.setString(2, product.getSku());
                stmt.setString(3, product.getName());
                stmt.setString(4, product.getDescription());
                stmt.setInt(5, product.isActive() ? 1 : 0); // Convertimos boolean a TINYINT(1)

                stmt.execute();
            }
            return null;
        });
    }

    // 6. NUEVO - Baja lógica de un producto (Cambia is_active a 0)
    public void deleteProduct(int id) {
        String sql = "{call Product_Delete(?)}";
        jdbcTemplate.execute((Connection conn) -> {
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, id);
                stmt.execute();
            }
            return null;
        });
    }
}
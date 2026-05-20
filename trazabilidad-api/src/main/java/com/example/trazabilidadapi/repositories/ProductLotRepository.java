package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.models.CurrentLotStatus; // DTO personalizado para el estado actual
import com.example.trazabilidadapi.util.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Repository
public class ProductLotRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductLotRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 2.1 CREATE: Registrar un nuevo lote físico
    public Integer insertLot(ProductLot productLot) throws SQLException {
        Integer productLotId = null;
        String sql = "{call ProductLot_Create(?, ?, ?, ?, ?, ?, ?)}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, productLot.getProductId());
            stmt.setString(2, productLot.getLotNumber());
            stmt.setInt(3, productLot.getQuantity());
            stmt.setString(4, productLot.getMeasureUnit());
            stmt.setObject(5, productLot.getExpirationDate(), Types.INTEGER);
            stmt.setString(6, productLot.getSupplierReference());

            // Registrar el parámetro OUT en la posición 7
            stmt.registerOutParameter(7, Types.INTEGER);

            stmt.execute();
            productLotId = stmt.getInt(7);
        }
        return productLotId;
    }

    public List<ProductLot> getAll() {
        List<ProductLot> list = new ArrayList<>();
        String sql = "{call ProductLot_GetAll()}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(ResultSetMapper.getProductLotFromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener todos los lotes", e);
        }
        return list;
    }

    public List<ProductLot> findByProductId(int productId) {
        List<ProductLot> list = new ArrayList<>();
        String sql = "{call ProductLot_FindByProductId(?)}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, productId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(ResultSetMapper.getProductLotFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar lotes por producto", e);
        }
        return list;
    }

    public ProductLot findById(int lotId) {
        String sql = "{call ProductLot_FindById(?)}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, lotId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.getProductLotFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar lote por ID: " + lotId, e);
        }
        return null;
    }

    public void updateLot(ProductLot productLot) {
        String sql = "{call ProductLot_Update(?, ?, ?, ?, ?)}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, productLot.getLotId()); // Se asume que el modelo ProductLot cuenta con getLotId()
            stmt.setInt(2, productLot.getQuantity());
            stmt.setString(3, productLot.getMeasureUnit());
            stmt.setObject(4, productLot.getExpirationDate(), Types.INTEGER);
            stmt.setString(5, productLot.getSupplierReference());

            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar el lote con ID: " + productLot.getLotId(), e);
        }
    }

    public void deleteLot(int lotId) {
        String sql = "{call ProductLot_Delete(?)}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setInt(1, lotId);
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Error al eliminar el lote con ID: " + lotId, e);
        }
    }

    public CurrentLotStatus getCurrentStatus(String lotNumber) {
        String sql = "{call ProductLot_GetCurrentStatus(?)}";

        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {

            stmt.setString(1, lotNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return ResultSetMapper.getCurrentLotStatusFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el motor de trazabilidad para el lote: " + lotNumber, e);
        }
        return null;
    }
}
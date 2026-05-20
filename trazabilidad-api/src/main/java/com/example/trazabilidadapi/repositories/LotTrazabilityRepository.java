package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.CurrentLotStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.example.trazabilidadapi.util.ResultSetMapper;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

@Repository
public class LotTrazabilityRepository {

    private final JdbcTemplate jdbcTemplate;

    public LotTrazabilityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CurrentLotStatus getLotCurrentStatus(String lotNumber) {
        CurrentLotStatus currentLotStatus = null;
        try {
            java.util.Objects.requireNonNull(jdbcTemplate.getDataSource(), "El DataSource no puede ser nulo");
            try (Connection conn = jdbcTemplate.getDataSource().getConnection();
                 CallableStatement stmt = conn.prepareCall("call ProductLot_GetCurrentStatus(?)")) {

                stmt.setString(1, lotNumber);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        currentLotStatus = ResultSetMapper.getLotCurrentStatusFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 101 || "45000".equals(e.getSQLState())) {
                throw new IllegalArgumentException("El número de lote escaneado no existe en el sistema.");
            }
            throw new RuntimeException("Error crítico al consultar la base de datos de MonCorp", e);
        }
        return currentLotStatus;
    }
}
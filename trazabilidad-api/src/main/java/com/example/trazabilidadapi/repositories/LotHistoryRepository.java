package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.LotHistory;
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
public class LotHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public LotHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LotHistory> findByLotId(int lotId) {
        List<LotHistory> list = new ArrayList<>();
        try {
            Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (CallableStatement stmt = conn.prepareCall("{call LotHistory_FindByLotId(?)}")) {
                stmt.setInt(1, lotId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getLotHistoryFromResultSet(rs));
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el historial del lote", e);
        }
        return list;
    }

    public int save(LotHistory history) {
        try {
            Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (CallableStatement stmt = conn.prepareCall("{call LotHistory_Insert(?, ?, ?, ?, ?, ?)}")) {
                stmt.setInt(1, history.getLotId());
                stmt.setString(2, history.getActionType());
                stmt.setString(3, history.getOperator());
                stmt.setBigDecimal(4, history.getQuantityAffected());
                stmt.setString(5, history.getOcrRawText());
                stmt.setString(6, history.getObservations());

                stmt.execute();
                return 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar registro en el historial", e);
        }
    }
}
package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.LotHistory;
import com.example.trazabilidadapi.util.ResultSetMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall("{call LotHistory_FindByLotId(?)}")) {
            stmt.setInt(1, lotId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(ResultSetMapper.getLotHistoryFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al consultar el historial del lote", e);
        }
        return list;
    }

    public int insertLotHistory(LotHistory history) {
        try (Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
             CallableStatement stmt = conn.prepareCall("{call LotHistory_Insert(?, ?, ?, ?, ?, ?, ?)}")) {
            stmt.setInt(1, history.getLotId());
            stmt.setString(2, history.getActionType());
            stmt.setString(3, history.getOperator());
            stmt.setBigDecimal(4, history.getQuantityAffected());
            stmt.setString(5, history.getOcrRawText());
            stmt.setString(6, history.getObservations());
            stmt.registerOutParameter(7, Types.INTEGER);

            stmt.execute();
            return stmt.getInt(7);
        } catch (SQLException e) {
            // Reglas del procedimiento: el mensaje se muestra al operario tal cual
            String message = String.valueOf(e.getMessage()).replaceFirst("^\\(conn=\\d+\\)\\s*", "");
            throw switch (e.getErrorCode()) {
                case 106 -> new ResponseStatusException(HttpStatus.NOT_FOUND, message);
                case 112, 116 -> new ResponseStatusException(HttpStatus.CONFLICT, message);
                case 125 -> new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
                default -> new RuntimeException("Error al guardar registro en el historial", e);
            };
        }
    }
}
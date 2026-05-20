package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.models.AppUser;
import com.example.trazabilidadapi.util.ResultSetMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AppUser findByUsername(String username) {
        try {
            Connection conn = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection();
            try (CallableStatement stmt = conn.prepareCall("{call AppUser_FindByUsername(?)}")) {
                stmt.setString(1, username);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return ResultSetMapper.getAppUserFromResultSet(rs);
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar usuario mediante procedimiento", e);
        }
        return null;
    }
}
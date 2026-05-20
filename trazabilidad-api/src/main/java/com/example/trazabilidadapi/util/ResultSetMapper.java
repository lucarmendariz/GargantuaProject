package com.example.trazabilidadapi.util;

import com.example.trazabilidadapi.models.*;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetMapper {

    public static CurrentLotStatus getLotCurrentStatusFromResultSet(ResultSet rs) throws SQLException {
        return CurrentLotStatus.builder()
                .productId(rs.getInt("product_id"))
                .productSku(rs.getString("product_sku"))
                .productName(rs.getString("product_name"))
                .lotId(rs.getInt("lot_id"))
                .lotNumber(rs.getString("lot_number"))
                .initialQuantity(rs.getBigDecimal("initial_quantity"))
                .measureUnit(rs.getString("measure_unit"))
                .expirationDate(rs.getDate("expiration_date") != null ? rs.getDate("expiration_date").toLocalDate() : null)
                .currentState(rs.getString("current_state"))
                .lastUpdate(rs.getTimestamp("last_update") != null ? rs.getTimestamp("last_update").toLocalDateTime() : null)
                .lastOperator(rs.getString("last_operator"))
                .ocrRawText(rs.getString("ocr_raw_text"))
                .lastObservations(rs.getString("last_observations"))
                .build();
    }

    public static Product getProductFromResultSet(ResultSet rs) throws SQLException {
        return Product.builder()
                .productId(rs.getInt("product_id"))
                .sku(rs.getString("sku"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .isActive(rs.getBoolean("is_active"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();
    }

    public static ProductLot getProductLotFromResultSet(ResultSet rs) throws SQLException {
        return ProductLot.builder()
                .lotId(rs.getInt("lot_id"))
                .productId(rs.getInt("product_id"))
                .lotNumber(rs.getString("lot_number"))
                .quantity(rs.getBigDecimal("quantity"))
                .measureUnit(rs.getString("measure_unit"))
                .expirationDate(rs.getDate("expiration_date") != null ? rs.getDate("expiration_date").toLocalDate() : null)
                .supplierReference(rs.getString("supplier_reference"))
                .createdAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null)
                .build();
    }

    public static LotHistory getLotHistoryFromResultSet(ResultSet rs) throws SQLException {
        return LotHistory.builder()
                .historyId(rs.getInt("history_id"))
                .lotId(rs.getInt("lot_id"))
                .actionType(rs.getString("action_type"))
                .operator(rs.getString("operator"))
                .actionTimestamp(rs.getTimestamp("action_timestamp").toLocalDateTime())
                .quantityAffected(rs.getBigDecimal("quantity_affected"))
                .ocrRawText(rs.getString("ocr_raw_text"))
                .observations(rs.getString("observations"))
                .build();
    }

    public static AppUser getAppUserFromResultSet(ResultSet rs) throws SQLException {
        return AppUser.builder()
                .userId(rs.getInt("user_id"))
                .username(rs.getString("username"))
                .passwordHash(rs.getString("password_hash"))
                .name(rs.getString("name"))
                .role(rs.getString("role"))
                .isActive(rs.getBoolean("is_active"))
                .build();
    }

    public static CurrentLotStatus getCurrentLotStatusFromResultSet(ResultSet rs) throws SQLException {
        return CurrentLotStatus.builder()
                .productId(rs.getInt("product_id"))
                .productSku(rs.getString("product_sku"))
                .productName(rs.getString("product_name"))
                .lotId(rs.getInt("lot_id"))
                .lotNumber(rs.getString("lot_number"))
                .initialQuantity(rs.getBigDecimal("initial_quantity"))
                .measureUnit(rs.getString("measure_unit"))
                // Usamos Long.class para soportar nulos correctamente en tipos numéricos flotantes/enteros
                .expirationDate(rs.getObject("expiration_date", Long.class))
                .currentState(rs.getString("current_state"))
                .lastUpdate(rs.getTimestamp("last_update"))
                .lastOperator(rs.getString("last_operator"))
                .ocrRawText(rs.getString("ocr_raw_text"))
                .lastObservations(rs.getString("last_observations"))
                .build();
    }
}
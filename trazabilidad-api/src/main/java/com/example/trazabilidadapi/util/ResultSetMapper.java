package com.example.trazabilidadapi.util;

import com.example.trazabilidadapi.models.CurrentLotStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)

public class ResultSetMapper {

    public static CurrentLotStatus getLotCurrentStatusFromResultSet(ResultSet resultSet) throws SQLException {
        return CurrentLotStatus.builder()
                .productId(resultSet.getInt("product_id"))
                .productSku(resultSet.getString("product_sku"))
                .productName(resultSet.getString("product_name"))
                .lotId(resultSet.getInt("lot_id"))
                .lotNumber(resultSet.getString("lot_number"))
                .initialQuantity(resultSet.getBigDecimal("initial_quantity"))
                .measureUnit(resultSet.getString("measure_unit"))
                .expirationDate(resultSet.getDate("expiration_date") != null ? resultSet.getDate("expiration_date").toLocalDate() : null)
                .currentState(resultSet.getString("current_state"))
                .lastUpdate(resultSet.getTimestamp("last_update") != null ? resultSet.getTimestamp("last_update").toLocalDateTime() : null)
                .lastOperator(resultSet.getString("last_operator"))
                .ocrRawText(resultSet.getString("ocr_raw_text"))
                .lastObservations(resultSet.getString("last_observations"))
                .build();
    }}

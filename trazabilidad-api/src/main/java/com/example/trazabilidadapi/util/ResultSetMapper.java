package com.example.trazabilidadapi.util;

import com.example.trazabilidadapi.models.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class ResultSetMapper {

    public static CurrentLotStatus getLotCurrentStatusFromResultSet(ResultSet rs) throws SQLException {
        return CurrentLotStatus.builder()
                .productId(rs.getInt("product_id"))
                .productSku(rs.getString("product_sku"))
                .productName(rs.getString("product_name"))
                .lotId(rs.getInt("lot_id"))
                .lotNumber(rs.getString("lot_number"))
                .initialQuantity(rs.getBigDecimal("initial_quantity"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .consumedQuantity(rs.getBigDecimal("consumed_quantity"))
                .currentStock(rs.getBigDecimal("current_stock"))
                .availableQuantity(rs.getBigDecimal("available_quantity"))
                .measureUnit(rs.getString("measure_unit"))
                .expirationDate(rs.getObject("expiration_date", Long.class))
                .currentState(rs.getString("current_state"))
                .lastUpdate(rs.getObject("last_update", Long.class))
                .lastOperator(rs.getString("last_operator"))
                .ocrRawText(rs.getString("ocr_raw_text"))
                .lastObservations(rs.getString("last_observations"))
                .build();
    }

    public static Product getProductFromResultSet(ResultSet rs) throws SQLException {
        return Product.builder()
                .productId(rs.getInt("product_id"))
                .sku(rs.getString("sku"))
                .gtin(rs.getString("gtin"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .isActive(rs.getBoolean("is_active"))
                .createdAt(rs.getObject("created_at", Long.class))
                .updatedAt(rs.getObject("updated_at", Long.class))
                .build();
    }

    public static Customer getCustomerFromResultSet(ResultSet rs) throws SQLException {
        return Customer.builder()
                .customerId(rs.getInt("customer_id"))
                .name(rs.getString("name"))
                .createdAt(rs.getObject("created_at", Long.class))
                .orderCount(rs.getInt("order_count"))
                .build();
    }

    public static ProductLot getProductLotFromResultSet(ResultSet rs) throws SQLException {
        return ProductLot.builder()
                .lotId(rs.getInt("lot_id"))
                .productId(rs.getInt("product_id"))
                .lotNumber(rs.getString("lot_number"))
                .quantity(rs.getBigDecimal("quantity"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .consumedQuantity(rs.getBigDecimal("consumed_quantity"))
                .currentStock(rs.getBigDecimal("current_stock"))
                .availableQuantity(rs.getBigDecimal("available_quantity"))
                .inUseQuantity(rs.getBigDecimal("in_use_quantity"))
                .measureUnit(rs.getString("measure_unit"))
                .expirationDate(rs.getObject("expiration_date", Long.class))
                .supplierReference(rs.getString("supplier_reference"))
                .createdAt(rs.getObject("created_at", Long.class))
                .updatedAt(rs.getObject("updated_at", Long.class))
                .build();
    }

    public static LotHistory getLotHistoryFromResultSet(ResultSet rs) throws SQLException {
        return LotHistory.builder()
                .historyId(rs.getInt("history_id"))
                .lotId(rs.getInt("lot_id"))
                .actionType(rs.getString("action_type"))
                .operator(rs.getString("operator"))
                .operatorUsername(rs.getString("operator_username"))
                .actionTimestamp(rs.getObject("action_timestamp", Long.class))
                .quantityAffected(rs.getBigDecimal("quantity_affected"))
                .ocrRawText(rs.getString("ocr_raw_text"))
                .observations(rs.getString("observations"))
                .orderNumber(rs.getString("order_number"))
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

    public static CustomerOrder getOrderFromResultSet(ResultSet rs) throws SQLException {
        return CustomerOrder.builder()
                .orderId(rs.getInt("order_id"))
                .orderNumber(rs.getString("order_number"))
                .customerName(rs.getString("customer_name"))
                .scheduledDate(rs.getObject("scheduled_date", Long.class))
                .status(rs.getString("status"))
                .notes(rs.getString("notes"))
                .createdBy(rs.getString("created_by"))
                .closedBy(rs.getString("closed_by"))
                .createdAt(rs.getObject("created_at", Long.class))
                .closedAt(rs.getObject("closed_at", Long.class))
                .preparedBy(rs.getString("prepared_by"))
                .preparedAt(rs.getObject("prepared_at", Long.class))
                .deliveredBy(rs.getString("delivered_by"))
                .deliveredAt(rs.getObject("delivered_at", Long.class))
                .lineCount(rs.getInt("line_count"))
                .build();
    }

    public static OrderLine getOrderLineFromResultSet(ResultSet rs) throws SQLException {
        return OrderLine.builder()
                .lineId(rs.getInt("line_id"))
                .orderId(rs.getInt("order_id"))
                .lotId(rs.getInt("lot_id"))
                .lotNumber(rs.getString("lot_number"))
                .productName(rs.getString("product_name"))
                .productSku(rs.getString("product_sku"))
                .measureUnit(rs.getString("measure_unit"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .consumedQuantity(rs.getBigDecimal("consumed_quantity"))
                .build();
    }

    public static OrderEvent getOrderEventFromResultSet(ResultSet rs) throws SQLException {
        return OrderEvent.builder()
                .eventId(rs.getInt("event_id"))
                .orderId(rs.getInt("order_id"))
                .lotId(rs.getObject("lot_id", Integer.class))
                .lotNumber(rs.getString("lot_number"))
                .measureUnit(rs.getString("measure_unit"))
                .eventType(rs.getString("event_type"))
                .quantity(rs.getBigDecimal("quantity"))
                .operator(rs.getString("operator"))
                .operatorUsername(rs.getString("operator_username"))
                .notes(rs.getString("notes"))
                .eventTimestamp(rs.getObject("event_timestamp", Long.class))
                .build();
    }

    public static InUseLine getInUseLineFromResultSet(ResultSet rs) throws SQLException {
        return InUseLine.builder()
                .lineId(rs.getInt("line_id"))
                .orderId(rs.getInt("order_id"))
                .orderNumber(rs.getString("order_number"))
                .customerName(rs.getString("customer_name"))
                .lotId(rs.getInt("lot_id"))
                .lotNumber(rs.getString("lot_number"))
                .productName(rs.getString("product_name"))
                .productSku(rs.getString("product_sku"))
                .measureUnit(rs.getString("measure_unit"))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .currentStock(rs.getBigDecimal("current_stock"))
                .availableQuantity(rs.getBigDecimal("available_quantity"))
                .expirationDate(rs.getObject("expiration_date", Long.class))
                .reservedAt(rs.getObject("reserved_at", Long.class))
                .build();
    }

    public static LotOrderUsage getLotOrderUsageFromResultSet(ResultSet rs) throws SQLException {
        return LotOrderUsage.builder()
                .orderId(rs.getInt("order_id"))
                .orderNumber(rs.getString("order_number"))
                .customerName(rs.getString("customer_name"))
                .orderStatus(rs.getString("order_status"))
                .scheduledDate(rs.getObject("scheduled_date", Long.class))
                .reservedQuantity(rs.getBigDecimal("reserved_quantity"))
                .consumedQuantity(rs.getBigDecimal("consumed_quantity"))
                .build();
    }
}

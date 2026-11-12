package com.example.trazabilidadapi.repositories;

import com.example.trazabilidadapi.exceptions.OrderException;
import com.example.trazabilidadapi.models.CreateOrderRequest;
import com.example.trazabilidadapi.models.Customer;
import com.example.trazabilidadapi.models.CustomerOrder;
import com.example.trazabilidadapi.models.InUseLine;
import com.example.trazabilidadapi.models.LotOrderUsage;
import com.example.trazabilidadapi.models.OrderEvent;
import com.example.trazabilidadapi.models.OrderLine;
import com.example.trazabilidadapi.util.ResultSetMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Usa JdbcTemplate para que todas las llamadas de un servicio compartan su transaccion. */
@Repository
public class OrderRepository {

    private static final int DUPLICATE_KEY = 1062;
    private static final int LOCK_WAIT_TIMEOUT = 1205;
    private static final int DEADLOCK = 1213;

    private final JdbcTemplate jdbcTemplate;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int create(CreateOrderRequest request, String operator) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Order_Create(?, ?, ?, ?, ?, ?)}")) {
                stmt.setString(1, request.getOrderNumber().trim());
                stmt.setObject(2, request.getCustomerId(), Types.INTEGER);
                stmt.setLong(3, request.getScheduledDate());
                stmt.setString(4, request.getNotes());
                stmt.setString(5, operator);
                stmt.registerOutParameter(6, Types.INTEGER);
                stmt.execute();
                return stmt.getInt(6);
            }
        });
    }

    public List<Customer> findCustomers() {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Customer_GetAll()}")) {
                List<Customer> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getCustomerFromResultSet(rs));
                    }
                }
                return list;
            }
        });
    }

    public int createCustomer(String name) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Customer_Create(?, ?)}")) {
                stmt.setString(1, name);
                stmt.registerOutParameter(2, Types.INTEGER);
                stmt.execute();
                return stmt.getInt(2);
            }
        });
    }

    public List<CustomerOrder> findAll(String status, Long from, Long to) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Order_GetAll(?, ?, ?)}")) {
                stmt.setString(1, status);
                stmt.setObject(2, from, Types.BIGINT);
                stmt.setObject(3, to, Types.BIGINT);
                List<CustomerOrder> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getOrderFromResultSet(rs));
                    }
                }
                return list;
            }
        });
    }

    public CustomerOrder findById(int orderId) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Order_FindById(?)}")) {
                stmt.setInt(1, orderId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? ResultSetMapper.getOrderFromResultSet(rs) : null;
                }
            }
        });
    }

    public List<OrderLine> findLines(int orderId) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call OrderLine_FindByOrder(?)}")) {
                stmt.setInt(1, orderId);
                List<OrderLine> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getOrderLineFromResultSet(rs));
                    }
                }
                return list;
            }
        });
    }

    public List<OrderEvent> findEvents(int orderId) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call OrderEvent_FindByOrder(?)}")) {
                stmt.setInt(1, orderId);
                List<OrderEvent> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getOrderEventFromResultSet(rs));
                    }
                }
                return list;
            }
        });
    }

    public List<InUseLine> findInUse(Long from, Long to) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call OrderLine_GetInUse(?, ?)}")) {
                stmt.setObject(1, from, Types.BIGINT);
                stmt.setObject(2, to, Types.BIGINT);
                List<InUseLine> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getInUseLineFromResultSet(rs));
                    }
                }
                return list;
            }
        });
    }

    public List<LotOrderUsage> findOrdersByLot(int lotId) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Lot_GetOrders(?)}")) {
                stmt.setInt(1, lotId);
                List<LotOrderUsage> list = new ArrayList<>();
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(ResultSetMapper.getLotOrderUsageFromResultSet(rs));
                    }
                }
                return list;
            }
        });
    }

    public int addLine(int orderId, int lotId, BigDecimal quantity, String operator) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call OrderLine_Add(?, ?, ?, ?, ?)}")) {
                stmt.setInt(1, orderId);
                stmt.setInt(2, lotId);
                stmt.setBigDecimal(3, quantity);
                stmt.setString(4, operator);
                stmt.registerOutParameter(5, Types.INTEGER);
                stmt.execute();
                return stmt.getInt(5);
            }
        });
    }

    public void removeLine(int orderId, int lineId, String operator) {
        run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call OrderLine_Remove(?, ?, ?)}")) {
                stmt.setInt(1, orderId);
                stmt.setInt(2, lineId);
                stmt.setString(3, operator);
                stmt.execute();
                return null;
            }
        });
    }

    public void consumeLine(int orderId, int lineId, BigDecimal consumed, String operator) {
        run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call OrderLine_Consume(?, ?, ?, ?)}")) {
                stmt.setInt(1, orderId);
                stmt.setInt(2, lineId);
                stmt.setBigDecimal(3, consumed);
                stmt.setString(4, operator);
                stmt.execute();
                return null;
            }
        });
    }

    public void prepare(int orderId, String operator) {
        changeStatus("{call Order_Prepare(?, ?)}", orderId, operator);
    }

    public void deliver(int orderId, String operator) {
        changeStatus("{call Order_Deliver(?, ?)}", orderId, operator);
    }

    /** Pasa a CERRADO los pedidos entregados hace al menos esas horas. Devuelve cuántos. */
    public int closeDelivered(int hours) {
        return run(conn -> {
            try (CallableStatement stmt = conn.prepareCall("{call Order_CloseDelivered(?, ?)}")) {
                stmt.setInt(1, hours);
                stmt.registerOutParameter(2, Types.INTEGER);
                stmt.execute();
                return stmt.getInt(2);
            }
        });
    }

    public void cancel(int orderId, String operator) {
        changeStatus("{call Order_Cancel(?, ?)}", orderId, operator);
    }

    private void changeStatus(String sql, int orderId, String operator) {
        run(conn -> {
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setInt(1, orderId);
                stmt.setString(2, operator);
                stmt.execute();
                return null;
            }
        });
    }

    private <T> T run(ConnectionCallback<T> callback) {
        try {
            return jdbcTemplate.execute(callback);
        } catch (DataAccessException e) {
            if (e.getRootCause() instanceof SQLException sql) {
                OrderException business = toBusinessError(sql.getErrorCode());
                if (business != null) {
                    throw business;
                }
            }
            throw e;
        }
    }

    private OrderException toBusinessError(int code) {
        return switch (code) {
            case 106 -> new OrderException(HttpStatus.NOT_FOUND, "El lote no existe.");
            case 113 -> new OrderException(HttpStatus.NOT_FOUND, "El pedido no existe.");
            case 117 -> new OrderException(HttpStatus.NOT_FOUND, "La línea no pertenece a este pedido.");
            case 114 -> new OrderException(HttpStatus.BAD_REQUEST, "La cantidad indicada no es válida.");
            case 110, DUPLICATE_KEY -> new OrderException(HttpStatus.CONFLICT, "Ya existe un pedido con ese número o ese lote ya está en el pedido.");
            case 111 -> new OrderException(HttpStatus.CONFLICT, "El pedido ya no está abierto.");
            case 112 -> new OrderException(HttpStatus.CONFLICT, "El lote está bloqueado y no se puede usar.");
            case 115 -> new OrderException(HttpStatus.CONFLICT, "El lote ya está en este pedido.");
            case 116 -> new OrderException(HttpStatus.CONFLICT, "No hay stock disponible suficiente en el lote.");
            case 118 -> new OrderException(HttpStatus.CONFLICT, "Esta línea ya tiene su consumo registrado.");
            case 119 -> new OrderException(HttpStatus.CONFLICT, "Falta indicar el consumo real de alguna línea.");
            case 120 -> new OrderException(HttpStatus.CONFLICT, "El pedido no tiene lotes asignados.");
            case 123 -> new OrderException(HttpStatus.CONFLICT, "Solo se puede entregar un pedido preparado.");
            case 126 -> new OrderException(HttpStatus.CONFLICT, "Ya existe un cliente con ese nombre.");
            case 127 -> new OrderException(HttpStatus.BAD_REQUEST, "El cliente no existe.");
            case LOCK_WAIT_TIMEOUT, DEADLOCK -> new OrderException(HttpStatus.CONFLICT, "Otro usuario está modificando estos datos. Inténtalo de nuevo.");
            default -> null;
        };
    }
}

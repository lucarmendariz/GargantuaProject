package com.example.trazabilidadapi.services;

import com.example.trazabilidadapi.exceptions.OrderException;
import com.example.trazabilidadapi.models.CreateOrderRequest;
import com.example.trazabilidadapi.models.Customer;
import com.example.trazabilidadapi.models.CustomerOrder;
import com.example.trazabilidadapi.models.InUseLine;
import com.example.trazabilidadapi.models.LineConsumption;
import com.example.trazabilidadapi.models.LotTraceability;
import com.example.trazabilidadapi.models.OrderDetail;
import com.example.trazabilidadapi.models.PrepareOrderRequest;
import com.example.trazabilidadapi.models.ProductLot;
import com.example.trazabilidadapi.repositories.LotHistoryRepository;
import com.example.trazabilidadapi.repositories.OrderRepository;
import com.example.trazabilidadapi.repositories.ProductLotRepository;
import com.example.trazabilidadapi.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductLotRepository lots;
    private final ProductRepository products;
    private final LotHistoryRepository history;
    private final int autoCloseHours;

    public OrderService(OrderRepository orders, ProductLotRepository lots, ProductRepository products, LotHistoryRepository history,
                        @Value("${app.orders.auto-close-hours:48}") int autoCloseHours) {
        this.autoCloseHours = autoCloseHours;
        this.orders = orders;
        this.lots = lots;
        this.products = products;
        this.history = history;
    }

    @Transactional
    public int create(CreateOrderRequest request, String operator) {
        if (request.getOrderNumber() == null || request.getOrderNumber().isBlank()) {
            throw badRequest("El número de pedido es obligatorio.");
        }
        if (request.getScheduledDate() == null) {
            throw badRequest("La fecha programada es obligatoria.");
        }
        return orders.create(request, operator);
    }

    public List<Customer> customers() {
        return orders.findCustomers();
    }

    @Transactional
    public int createCustomer(String name) {
        if (name == null || name.isBlank()) {
            throw badRequest("El nombre del cliente es obligatorio.");
        }
        if (name.trim().length() > 150) {
            throw badRequest("El nombre del cliente no puede pasar de 150 caracteres.");
        }
        return orders.createCustomer(name.trim());
    }

    public List<CustomerOrder> list(String status, Long from, Long to) {
        List<CustomerOrder> list = orders.findAll(status, from, to);
        list.forEach(this::setAutoCloseAt);
        return list;
    }

    public List<InUseLine> inUse(Long from, Long to) {
        return orders.findInUse(from, to);
    }

    public OrderDetail detail(int orderId) {
        CustomerOrder order = orders.findById(orderId);
        if (order == null) {
            throw new OrderException(HttpStatus.NOT_FOUND, "El pedido no existe.");
        }
        setAutoCloseAt(order);
        return OrderDetail.builder()
                .order(order)
                .lines(orders.findLines(orderId))
                .events(orders.findEvents(orderId))
                .build();
    }

    @Transactional
    public int addLine(int orderId, Integer lotId, BigDecimal quantity, String operator) {
        if (lotId == null) {
            throw badRequest("Hay que indicar el lote.");
        }
        return orders.addLine(orderId, lotId, quantity, operator);
    }

    @Transactional
    public void removeLine(int orderId, int lineId, String operator) {
        orders.removeLine(orderId, lineId, operator);
    }

    /** Registra el consumo real y deja el pedido PREPARADO. O se registran todas las lineas, o no cambia nada. */
    @Transactional
    public void prepare(int orderId, PrepareOrderRequest request, String operator) {
        if (request == null || request.getLines() == null) {
            throw badRequest("Hay que indicar el consumo real de cada línea.");
        }
        for (LineConsumption line : request.getLines()) {
            if (line.getLineId() == null) {
                throw badRequest("Hay que indicar la línea.");
            }
            orders.consumeLine(orderId, line.getLineId(), line.getConsumedQuantity(), operator);
        }
        orders.prepare(orderId, operator);
    }

    @Transactional
    public void deliver(int orderId, String operator) {
        orders.deliver(orderId, operator);
    }

    @Transactional
    public void cancel(int orderId, String operator) {
        orders.cancel(orderId, operator);
    }

    /** Cierra los pedidos entregados hace más del plazo configurado. */
    @Transactional
    public int closeDelivered() {
        return closeDelivered(autoCloseHours);
    }

    @Transactional
    public int closeDelivered(int hours) {
        return orders.closeDelivered(hours);
    }

    private void setAutoCloseAt(CustomerOrder order) {
        if ("ENTREGADO".equals(order.getStatus()) && order.getDeliveredAt() != null) {
            order.setAutoCloseAt(order.getDeliveredAt() + autoCloseHours * 3600L);
        }
    }

    public LotTraceability lotTraceability(int lotId) {
        ProductLot lot = lots.findById(lotId);
        if (lot == null) {
            throw new OrderException(HttpStatus.NOT_FOUND, "El lote no existe.");
        }
        return LotTraceability.builder()
                .lot(lot)
                .product(products.findById(lot.getProductId()))
                .orders(orders.findOrdersByLot(lotId))
                .history(history.findByLotId(lotId))
                .build();
    }

    private OrderException badRequest(String message) {
        return new OrderException(HttpStatus.BAD_REQUEST, message);
    }
}

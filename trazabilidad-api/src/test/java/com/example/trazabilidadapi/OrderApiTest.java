package com.example.trazabilidadapi;

import com.example.trazabilidadapi.services.OrderService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiTest extends IntegrationTestBase {

    @Autowired
    private OrderService orderService;

    private static final AtomicInteger SEQ = new AtomicInteger();
    private static final long DAY = 1800000000L;

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(role)).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder admin(MockHttpServletRequestBuilder request) {
        return as("ADMIN", request);
    }

    private int body(ResultActions result) throws Exception {
        return Integer.parseInt(result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
    }

    private String unique(String prefix) {
        return prefix + "-" + SEQ.incrementAndGet();
    }

    private int lot(double quantity) throws Exception {
        return body(mvc.perform(admin(post("/api/lotes")).content(
                "{\"productId\":1,\"lotNumber\":\"" + unique("LOTE") + "\",\"quantity\":" + quantity + ",\"measureUnit\":\"UNITS\"}")));
    }

    private int order(long day) throws Exception {
        return body(mvc.perform(admin(post("/api/pedidos")).content(
                "{\"orderNumber\":\"" + unique("PED") + "\",\"customerName\":\"Cliente\",\"scheduledDate\":" + day + "}")));
    }

    private int order() throws Exception {
        return order(DAY);
    }

    private ResultActions addLine(int order, int lot, double quantity) throws Exception {
        return mvc.perform(admin(post("/api/pedidos/" + order + "/lineas"))
                .content("{\"lotId\":" + lot + ",\"quantity\":" + quantity + "}"));
    }

    private ResultActions prepare(int order, String lines) throws Exception {
        return mvc.perform(admin(post("/api/pedidos/" + order + "/preparar")).content("{\"lines\":[" + lines + "]}"));
    }

    private String consumption(int line, double quantity) {
        return "{\"lineId\":" + line + ",\"consumedQuantity\":" + quantity + "}";
    }

    private void assertLot(int lot, double stock, double reserved, double available) throws Exception {
        mvc.perform(admin(get("/api/lotes/" + lot)))
                .andExpect(jsonPath("$.currentStock", closeTo(stock, 0.0001)))
                .andExpect(jsonPath("$.reservedQuantity", closeTo(reserved, 0.0001)))
                .andExpect(jsonPath("$.availableQuantity", closeTo(available, 0.0001)));
    }

    @Test
    void reservingLowersAvailableButNotStock() throws Exception {
        int lot = lot(50);
        addLine(order(), lot, 12.5).andExpect(status().isCreated());
        assertLot(lot, 50, 12.5, 37.5);
    }

    @Test
    void preparingSubtractsRealConsumptionAndReleasesRest() throws Exception {
        int lot = lot(50);
        int order = order();
        int line = body(addLine(order, lot, 10));

        prepare(order, consumption(line, 7.25)).andExpect(status().isNoContent());

        assertLot(lot, 42.75, 0, 42.75);
        mvc.perform(admin(get("/api/pedidos/" + order)))
                .andExpect(jsonPath("$.order.status").value("PREPARADO"))
                .andExpect(jsonPath("$.order.preparedBy").value("Usuario Test"))
                .andExpect(jsonPath("$.lines[0].consumedQuantity", closeTo(7.25, 0.0001)));
    }

    @Test
    void preparingCanConsumeMoreThanReservedIfAvailable() throws Exception {
        int lot = lot(20);
        int order = order();
        int line = body(addLine(order, lot, 10));

        prepare(order, consumption(line, 15)).andExpect(status().isNoContent());
        assertLot(lot, 5, 0, 5);
    }

    @Test
    void cannotReserveOrConsumeMoreThanAvailable() throws Exception {
        int lot = lot(10);
        int first = order();
        int firstLine = body(addLine(first, lot, 8));
        addLine(order(), lot, 3).andExpect(status().isConflict());
        assertLot(lot, 10, 8, 2);

        int second = order();
        body(addLine(second, lot, 2));
        prepare(first, consumption(firstLine, 9)).andExpect(status().isConflict());
        assertLot(lot, 10, 10, 0);
    }

    @Test
    void cancelReleasesReservations() throws Exception {
        int lot = lot(10);
        int order = order();
        addLine(order, lot, 4).andExpect(status().isCreated());

        mvc.perform(admin(post("/api/pedidos/" + order + "/cancelar"))).andExpect(status().isNoContent());

        assertLot(lot, 10, 0, 10);
        mvc.perform(admin(get("/api/pedidos/" + order))).andExpect(jsonPath("$.order.status").value("CANCELADO"));
    }

    @Test
    void removingLineReleasesReservation() throws Exception {
        int lot = lot(10);
        int order = order();
        int line = body(addLine(order, lot, 4));

        mvc.perform(admin(delete("/api/pedidos/" + order + "/lineas/" + line))).andExpect(status().isNoContent());

        assertLot(lot, 10, 0, 10);
    }

    @Test
    void prepareIsAllOrNothing() throws Exception {
        int goodLot = lot(10);
        int badLot = lot(5);
        int order = order();
        int goodLine = body(addLine(order, goodLot, 5));
        int badLine = body(addLine(order, badLot, 5));

        prepare(order, consumption(goodLine, 5) + "," + consumption(badLine, 6)).andExpect(status().isConflict());

        assertLot(goodLot, 10, 5, 5);
        assertLot(badLot, 5, 5, 0);
        mvc.perform(admin(get("/api/pedidos/" + order)))
                .andExpect(jsonPath("$.order.status").value("ABIERTO"))
                .andExpect(jsonPath("$.events[?(@.eventType == 'CONSUMIDO')]").doesNotExist());
    }

    @Test
    void prepareRequiresEveryLine() throws Exception {
        int order = order();
        int line = body(addLine(order, lot(5), 1));
        body(addLine(order, lot(5), 1));

        prepare(order, consumption(line, 1)).andExpect(status().isConflict());
    }

    @Test
    void emptyOrderCannotBePrepared() throws Exception {
        prepare(order(), "").andExpect(status().isConflict());
    }

    @Test
    void preparedOrderIsLocked() throws Exception {
        int lot = lot(10);
        int order = order();
        int line = body(addLine(order, lot, 1));
        prepare(order, consumption(line, 1)).andExpect(status().isNoContent());

        addLine(order, lot(5), 1).andExpect(status().isConflict());
        mvc.perform(admin(post("/api/pedidos/" + order + "/cancelar"))).andExpect(status().isConflict());
        mvc.perform(admin(delete("/api/pedidos/" + order + "/lineas/" + line))).andExpect(status().isConflict());
        assertLot(lot, 9, 0, 9);
    }

    @Test
    void blockedLotCannotBeUsed() throws Exception {
        // Lote 2 de los datos de prueba: su ultimo movimiento manual es BLOQUEADO
        addLine(order(), 2, 1).andExpect(status().isConflict());
    }

    private ResultActions movement(String role, int lot, String action, double quantity) throws Exception {
        return mvc.perform(as(role, post("/api/historial/escanear"))
                .content("{\"lotId\":" + lot + ",\"actionType\":\"" + action + "\",\"quantityAffected\":" + quantity + "}"));
    }

    @Test
    void aQualityBlockIsOnlyLiftedByUnblockingOnPurpose() throws Exception {
        int lot = lot(10);
        movement("ADMIN", lot, "BLOQUEADO", 10).andExpect(status().isCreated());

        // Otro movimiento cualquiera ya no levanta el bloqueo: el lote sigue sin poder usarse
        movement("OPERARIO", lot, "EN_PRODUCCION", 1)
                .andExpect(status().isConflict())
                .andExpect(content().string(containsString("bloqueado por calidad")));
        addLine(order(), lot, 1).andExpect(status().isConflict());

        // Desbloquear se hace desde Produccion y exige ademas gestionar los lotes (por defecto, el ADMIN)
        movement("OPERARIO", lot, "DESBLOQUEADO", 0).andExpect(status().isForbidden());
        movement("ADMIN", lot, "DESBLOQUEADO", 0).andExpect(status().isCreated());
        body(addLine(order(), lot, 1));
    }

    @Test
    void productionOnlyAcceptsKnownMovements() throws Exception {
        int lot = lot(10);
        movement("ADMIN", lot, "LO_QUE_SEA", 1).andExpect(status().isBadRequest());
        movement("ADMIN", lot, "EN_PRODUCCION", -500).andExpect(status().isBadRequest());
        movement("ADMIN", lot, "DESBLOQUEADO", 0).andExpect(status().isBadRequest()); // no estaba bloqueado
        movement("ADMIN", 999999, "EN_PRODUCCION", 1).andExpect(status().isNotFound());
    }

    @Test
    void lotBlockedAfterReservingCannotBeConsumedButKeepsItsState() throws Exception {
        int lot = lot(10);
        int order = order();
        int line = body(addLine(order, lot, 2));
        mvc.perform(admin(post("/api/historial/escanear")).content(
                        "{\"lotId\":" + lot + ",\"actionType\":\"BLOQUEADO\",\"quantityAffected\":10}"))
                .andExpect(status().isCreated());

        prepare(order, consumption(line, 2)).andExpect(status().isConflict());

        String number = JsonPath.read(mvc.perform(admin(get("/api/lotes/" + lot))).andReturn().getResponse().getContentAsString(), "$.lotNumber");
        body(addLine(order(), lot(1), 1));
        mvc.perform(admin(get("/api/trazabilidad/lotes/" + number + "/status"))).andExpect(jsonPath("$.currentState").value("BLOQUEADO"));
    }

    @Test
    void duplicatesAreRejected() throws Exception {
        int lot = lot(10);
        int order = order();
        addLine(order, lot, 1).andExpect(status().isCreated());
        addLine(order, lot, 1).andExpect(status().isConflict());

        String number = unique("PED");
        String payload = "{\"orderNumber\":\"" + number + "\",\"scheduledDate\":" + DAY + "}";
        mvc.perform(admin(post("/api/pedidos")).content(payload)).andExpect(status().isCreated());
        mvc.perform(admin(post("/api/pedidos")).content(payload)).andExpect(status().isConflict());
    }

    @Test
    void invalidInputReturnsClearErrors() throws Exception {
        mvc.perform(admin(post("/api/pedidos")).content("{\"orderNumber\":\" \",\"scheduledDate\":1}")).andExpect(status().isBadRequest());
        mvc.perform(admin(post("/api/pedidos")).content("{\"orderNumber\":\"X\"}")).andExpect(status().isBadRequest());
        addLine(order(), lot(5), 0).andExpect(status().isBadRequest());
        addLine(order(), lot(5), -1).andExpect(status().isBadRequest());
        addLine(order(), 999999, 1).andExpect(status().isNotFound());
        addLine(999999, lot(5), 1).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/pedidos/999999"))).andExpect(status().isNotFound());
        mvc.perform(admin(get("/api/lotes/999999/trazabilidad"))).andExpect(status().isNotFound());
    }

    @Test
    void concurrentReservationsNeverOversell() throws Exception {
        int lot = lot(50);
        List<Integer> orders = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            orders.add(order());
        }
        String auth = bearer("ADMIN");
        ExecutorService pool = Executors.newFixedThreadPool(6);
        List<Future<Integer>> results = new ArrayList<>();
        for (int order : orders) {
            Callable<Integer> task = () -> mvc.perform(post("/api/pedidos/" + order + "/lineas")
                            .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lotId\":" + lot + ",\"quantity\":10}"))
                    .andReturn().getResponse().getStatus();
            results.add(pool.submit(task));
        }
        int created = 0;
        for (Future<Integer> result : results) {
            if (result.get() == 201) {
                created++;
            }
        }
        pool.shutdown();

        assertEquals(5, created);
        assertLot(lot, 50, 50, 0);
    }

    @Test
    void preparingOrdersWithLotsInOppositeOrderKeepsStockConsistent() throws Exception {
        int lotA = lot(1000);
        int lotB = lot(1000);
        String auth = bearer("ADMIN");
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            int order = order();
            int first = body(addLine(order, i % 2 == 0 ? lotA : lotB, 1));
            int second = body(addLine(order, i % 2 == 0 ? lotB : lotA, 1));
            String payload = "{\"lines\":[" + consumption(first, 1) + "," + consumption(second, 1) + "]}";
            Callable<Integer> task = () -> mvc.perform(post("/api/pedidos/" + order + "/preparar")
                            .header(HttpHeaders.AUTHORIZATION, auth).contentType(MediaType.APPLICATION_JSON).content(payload))
                    .andReturn().getResponse().getStatus();
            results.add(pool.submit(task));
        }
        int closed = 0;
        for (Future<Integer> result : results) {
            int status = result.get();
            if (status == 204) {
                closed++;
            } else {
                assertEquals(409, status);
            }
        }
        pool.shutdown();

        // Cada pedido cerrado consume 1 de cada lote; los rechazados no dejan nada a medias
        assertLot(lotA, 1000 - closed, 16 - closed, 1000 - 16);
        assertLot(lotB, 1000 - closed, 16 - closed, 1000 - 16);
    }

    @Test
    void dayFilterAndInUseOnlyShowThatDay() throws Exception {
        long day = 1900000000L;
        int open = order(day);
        int prepared = order(day + 3600);
        int otherDay = order(day + 86400);
        int lot = lot(20);
        addLine(open, lot, 1).andExpect(status().isCreated());
        int preparedLine = body(addLine(prepared, lot, 1));
        prepare(prepared, consumption(preparedLine, 1)).andExpect(status().isNoContent());
        addLine(otherDay, lot, 1).andExpect(status().isCreated());

        String range = "?desde=" + day + "&hasta=" + (day + 86400);
        mvc.perform(admin(get("/api/pedidos" + range)))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.orderId == " + open + ")].status").value("ABIERTO"))
                .andExpect(jsonPath("$[?(@.orderId == " + prepared + ")].status").value("PREPARADO"));
        mvc.perform(admin(get("/api/pedidos/en-uso" + range)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderId").value(open))
                .andExpect(jsonPath("$[0].lotId").value(lot))
                .andExpect(jsonPath("$[0].reservedQuantity", closeTo(1, 0.0001)))
                .andExpect(jsonPath("$[0].productName").isNotEmpty());
    }

    @Test
    void traceabilityShowsWholeJourney() throws Exception {
        int lot = lot(10);
        int order = order();
        int line = body(addLine(order, lot, 4));
        prepare(order, consumption(line, 3)).andExpect(status().isNoContent());

        mvc.perform(admin(get("/api/lotes/" + lot + "/trazabilidad")))
                .andExpect(jsonPath("$.lot.currentStock", closeTo(7, 0.0001)))
                .andExpect(jsonPath("$.product.sku").value("MON-CHE-001"))
                .andExpect(jsonPath("$.orders[0].orderId").value(order))
                .andExpect(jsonPath("$.orders[0].orderStatus").value("PREPARADO"))
                .andExpect(jsonPath("$.orders[0].consumedQuantity", closeTo(3, 0.0001)))
                .andExpect(jsonPath("$.history[0].actionType").value("CONSUMIDO"))
                .andExpect(jsonPath("$.history[0].orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.history[1].actionType").value("RESERVADO"));
        mvc.perform(admin(get("/api/pedidos/" + order)))
                .andExpect(jsonPath("$.events[0].eventType").value("PREPARADO"))
                .andExpect(jsonPath("$.events[1].eventType").value("CONSUMIDO"))
                .andExpect(jsonPath("$.events[2].eventType").value("RESERVADO"))
                .andExpect(jsonPath("$.events[3].eventType").value("CREADO"))
                .andExpect(jsonPath("$.events[3].operator").value("Usuario Test"));
    }

    @Test
    void usedLotCannotBeDeletedOrReducedBelowUse() throws Exception {
        int lot = lot(10);
        addLine(order(), lot, 6).andExpect(status().isCreated());

        mvc.perform(admin(delete("/api/lotes/" + lot))).andExpect(status().isConflict());
        mvc.perform(admin(put("/api/lotes/" + lot)).content("{\"quantity\":5,\"measureUnit\":\"UNITS\"}")).andExpect(status().isConflict());
        mvc.perform(admin(put("/api/lotes/" + lot)).content("{\"quantity\":6,\"measureUnit\":\"UNITS\"}")).andExpect(status().isOk());
    }

    @Test
    void rolesAreEnforced() throws Exception {
        int order = order();
        int line = body(addLine(order, lot(5), 1));

        mvc.perform(get("/api/pedidos")).andExpect(status().isUnauthorized());
        mvc.perform(as("OPERARIO", get("/api/pedidos"))).andExpect(status().isOk());
        mvc.perform(as("OPERARIO", get("/api/pedidos/en-uso"))).andExpect(status().isOk());
        mvc.perform(as("OPERARIO", post("/api/pedidos")).content("{}")).andExpect(status().isForbidden());
        mvc.perform(as("OPERARIO", post("/api/pedidos/" + order + "/cancelar"))).andExpect(status().isForbidden());
        mvc.perform(as("OPERARIO", delete("/api/pedidos/" + order + "/lineas/" + line))).andExpect(status().isForbidden());
        mvc.perform(as("ENCARGADO", post("/api/pedidos")).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(as("OPERARIO", post("/api/pedidos/" + order + "/preparar"))
                        .content("{\"lines\":[" + consumption(line, 1) + "]}"))
                .andExpect(status().isNoContent());
        mvc.perform(as("OPERARIO", post("/api/pedidos/" + order + "/entregar"))).andExpect(status().isNoContent());
    }

    private int preparedOrder() throws Exception {
        int order = order();
        int line = body(addLine(order, lot(5), 2));
        prepare(order, consumption(line, 2)).andExpect(status().isNoContent());
        return order;
    }

    private ResultActions deliver(int order) throws Exception {
        return mvc.perform(admin(post("/api/pedidos/" + order + "/entregar")));
    }

    @Test
    void onlyPreparedOrdersCanBeDelivered() throws Exception {
        int open = order();
        body(addLine(open, lot(5), 1));
        deliver(open).andExpect(status().isConflict());
        deliver(999999).andExpect(status().isNotFound());

        int order = preparedOrder();
        deliver(order).andExpect(status().isNoContent());
        deliver(order).andExpect(status().isConflict());

        String json = mvc.perform(admin(get("/api/pedidos/" + order)))
                .andExpect(jsonPath("$.order.status").value("ENTREGADO"))
                .andExpect(jsonPath("$.order.deliveredBy").value("Usuario Test"))
                .andExpect(jsonPath("$.events[0].eventType").value("ENTREGADO"))
                .andReturn().getResponse().getContentAsString();
        long deliveredAt = ((Number) JsonPath.read(json, "$.order.deliveredAt")).longValue();
        long autoCloseAt = ((Number) JsonPath.read(json, "$.order.autoCloseAt")).longValue();
        assertEquals(48 * 3600L, autoCloseAt - deliveredAt);

        // Un pedido entregado ya no se puede cancelar ni modificar
        mvc.perform(admin(post("/api/pedidos/" + order + "/cancelar"))).andExpect(status().isConflict());
        addLine(order, lot(5), 1).andExpect(status().isConflict());
    }

    @Test
    void deliveredOrdersCloseAutomaticallyAfterTheDeadline() throws Exception {
        int order = preparedOrder();
        deliver(order).andExpect(status().isNoContent());
        int stillPrepared = preparedOrder();

        // Recién entregado: con el plazo de 48 h aún no se cierra
        orderService.closeDelivered();
        mvc.perform(admin(get("/api/pedidos/" + order))).andExpect(jsonPath("$.order.status").value("ENTREGADO"));

        // Con plazo 0 ya ha vencido; el que solo está preparado no se toca
        assertTrue(orderService.closeDelivered(0) >= 1);
        mvc.perform(admin(get("/api/pedidos/" + order)))
                .andExpect(jsonPath("$.order.status").value("CERRADO"))
                .andExpect(jsonPath("$.order.closedBy").value("Sistema"))
                .andExpect(jsonPath("$.order.autoCloseAt").value(nullValue()))
                .andExpect(jsonPath("$.events[0].eventType").value("CERRADO"))
                .andExpect(jsonPath("$.events[1].eventType").value("ENTREGADO"));
        mvc.perform(admin(get("/api/pedidos/" + stillPrepared))).andExpect(jsonPath("$.order.status").value("PREPARADO"));

        // Los cerrados salen en su apartado y no en la lista de pedidos en curso
        mvc.perform(admin(get("/api/pedidos?estado=CERRADO")))
                .andExpect(jsonPath("$[?(@.orderId == " + order + ")]").exists());
        mvc.perform(admin(get("/api/pedidos?estado=ABIERTO,PREPARADO,ENTREGADO")))
                .andExpect(jsonPath("$[?(@.orderId == " + order + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.orderId == " + stillPrepared + ")]").exists());
    }

    @Test
    void lotShowsQuantityInActiveOrdersAndRecordsTheClose() throws Exception {
        int lot = lot(10);
        int open = order();
        body(addLine(open, lot, 1));
        int order = order();
        int line = body(addLine(order, lot, 4));
        mvc.perform(admin(get("/api/lotes/" + lot))).andExpect(jsonPath("$.inUseQuantity", closeTo(5, 0.0001)));

        // Preparado y entregado sigue en uso con lo consumido de verdad
        prepare(order, consumption(line, 3)).andExpect(status().isNoContent());
        deliver(order).andExpect(status().isNoContent());
        mvc.perform(admin(get("/api/lotes/" + lot))).andExpect(jsonPath("$.inUseQuantity", closeTo(4, 0.0001)));

        // Al cerrarse deja de contar y el cierre sale en el historial del lote
        orderService.closeDelivered(0);
        mvc.perform(admin(get("/api/lotes/" + lot + "/trazabilidad")))
                .andExpect(jsonPath("$.lot.inUseQuantity", closeTo(1, 0.0001)))
                .andExpect(jsonPath("$.history[0].actionType").value("CERRADO"))
                .andExpect(jsonPath("$.history[0].quantityAffected", closeTo(3, 0.0001)));
    }
}

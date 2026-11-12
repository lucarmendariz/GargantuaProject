package com.example.trazabilidadapi;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityApiTest extends IntegrationTestBase {

    private static final String LOGIN = "/api/auth/login";
    private static final String SCAN = "{\"lotId\":1,\"actionType\":\"EN_PRODUCCION\",\"operator\":\"Otro\",\"quantityAffected\":1.5}";

    @Test
    void withoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/trazabilidad/lotes/LOT-2026-05-19A/status")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/historial/escanear").contentType(MediaType.APPLICATION_JSON).content(SCAN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenIsRejected() throws Exception {
        mvc.perform(get("/api/productos").header(HttpHeaders.AUTHORIZATION, "Bearer no-es-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsUsableToken() throws Exception {
        String body = mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"luca\",\"password\":\"moncorp2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andReturn().getResponse().getContentAsString();
        String token = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        mvc.perform(get("/api/productos/admin").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        mvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"luca\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void operarioOnlyReachesItsEndpoints() throws Exception {
        String auth = bearer("OPERARIO");
        // Lee el catalogo para elegir el lote en Produccion, pero no lo cambia
        mvc.perform(get("/api/productos/admin").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk());
        mvc.perform(delete("/api/productos/1").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/historial/lote/1").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isForbidden());
        mvc.perform(get("/api/trazabilidad/lotes/LOT-2026-05-19A/status").header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk());
    }

    @Test
    void encargadoManagesInventoryButCannotScan() throws Exception {
        String auth = bearer("ENCARGADO");
        mvc.perform(get("/api/productos/admin").header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isOk());
        mvc.perform(post("/api/historial/escanear").header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON).content(SCAN))
                .andExpect(status().isForbidden());
    }

    @Test
    void scanUsesOperatorFromTokenNotFromBody() throws Exception {
        mvc.perform(post("/api/historial/escanear").header(HttpHeaders.AUTHORIZATION, bearer("OPERARIO"))
                        .contentType(MediaType.APPLICATION_JSON).content(SCAN))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/historial/lote/1").header(HttpHeaders.AUTHORIZATION, bearer("ADMIN")))
                .andExpect(jsonPath("$[?(@.operator == 'Usuario Test' && @.operatorUsername == 'test_operario')]").exists())
                .andExpect(jsonPath("$[?(@.operator == 'Otro')]").doesNotExist());
    }

    @Test
    void corsOnlyAllowsConfiguredOrigin() throws Exception {
        mvc.perform(options(LOGIN).header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
        mvc.perform(options(LOGIN).header(HttpHeaders.ORIGIN, "http://malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }
}

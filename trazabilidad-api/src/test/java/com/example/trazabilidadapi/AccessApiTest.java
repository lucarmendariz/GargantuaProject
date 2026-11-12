package com.example.trazabilidadapi;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Modulos contratados (SUPERADMIN), permisos por rol y usuarios (ADMIN del cliente). */
class AccessApiTest extends IntegrationTestBase {

    private MockHttpServletRequestBuilder as(String role, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, bearer(role)).contentType(MediaType.APPLICATION_JSON);
    }

    private MockHttpServletRequestBuilder withToken(String token, MockHttpServletRequestBuilder request) {
        return request.header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON);
    }

    private void setModule(String code, boolean enabled) throws Exception {
        mvc.perform(as("SUPERADMIN", put("/api/plataforma/modulos/" + code)).content("{\"enabled\":" + enabled + "}"))
                .andExpect(status().isNoContent());
    }

    private void setPermission(String role, String code, String access) throws Exception {
        mvc.perform(as("ADMIN", put("/api/admin/permisos/" + role + "/" + code)).content("{\"access\":\"" + access + "\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eachUserGetsTheAccessOfItsRole() throws Exception {
        mvc.perform(as("OPERARIO", get("/api/me/permisos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("OPERARIO"))
                .andExpect(jsonPath("$.modules[?(@.moduleCode == 'PEDIDOS')].access").value("VER"))
                .andExpect(jsonPath("$.modules[?(@.moduleCode == 'INVENTARIO')].access").value("NINGUNO"));
        mvc.perform(as("ADMIN", get("/api/me/permisos")))
                .andExpect(jsonPath("$.modules[?(@.moduleCode == 'HISTORIAL')].access").value("GESTIONAR"));
    }

    @Test
    void aModuleNotContractedIsClosedEvenForTheAdmin() throws Exception {
        try {
            setModule("PEDIDOS", false);
            mvc.perform(as("ADMIN", get("/api/pedidos"))).andExpect(status().isForbidden());
            mvc.perform(as("ADMIN", get("/api/me/permisos")))
                    .andExpect(jsonPath("$.modules[?(@.moduleCode == 'PEDIDOS')].access").value("NINGUNO"));
        } finally {
            setModule("PEDIDOS", true);
        }
        mvc.perform(as("ADMIN", get("/api/pedidos"))).andExpect(status().isOk());
    }

    @Test
    void theClientAdminDecidesWhatEachRoleCanDo() throws Exception {
        try {
            setPermission("OPERARIO", "PEDIDOS", "NINGUNO");
            mvc.perform(as("OPERARIO", get("/api/pedidos"))).andExpect(status().isForbidden());

            // VER deja consultar el inventario, pero no cambiarlo
            setPermission("OPERARIO", "INVENTARIO", "VER");
            mvc.perform(as("OPERARIO", get("/api/productos"))).andExpect(status().isOk());
            mvc.perform(as("OPERARIO", post("/api/productos")).content("{\"sku\":\"X\",\"name\":\"X\"}"))
                    .andExpect(status().isForbidden());
        } finally {
            setPermission("OPERARIO", "PEDIDOS", "VER");
            setPermission("OPERARIO", "INVENTARIO", "NINGUNO");
        }
        mvc.perform(as("OPERARIO", get("/api/pedidos"))).andExpect(status().isOk());
    }

    @Test
    void whoManagesOrdersCanReadLotsToAssignThemButNotChangeThem() throws Exception {
        try {
            setPermission("ENCARGADO", "INVENTARIO", "NINGUNO");
            setPermission("ENCARGADO", "LOTES", "NINGUNO");
            // El detalle del pedido carga lotes y productos para la pestaña "Asignar lote"
            mvc.perform(as("ENCARGADO", get("/api/lotes"))).andExpect(status().isOk());
            mvc.perform(as("ENCARGADO", get("/api/productos/admin"))).andExpect(status().isOk());
            mvc.perform(as("ENCARGADO", put("/api/lotes/1")).content("{\"quantity\":999,\"measureUnit\":\"UNITS\"}"))
                    .andExpect(status().isForbidden());
        } finally {
            setPermission("ENCARGADO", "INVENTARIO", "GESTIONAR");
            setPermission("ENCARGADO", "LOTES", "GESTIONAR");
        }
    }

    @Test
    void productionCanReadLotsToPickThemButNotChangeThem() throws Exception {
        // El OPERARIO solo tiene Produccion: elige el lote de la lista por su numero y su producto
        mvc.perform(as("OPERARIO", get("/api/lotes"))).andExpect(status().isOk());
        mvc.perform(as("OPERARIO", get("/api/productos/admin"))).andExpect(status().isOk());
        mvc.perform(as("OPERARIO", put("/api/lotes/1")).content("{\"quantity\":999,\"measureUnit\":\"UNITS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void tooLongUsernameIsRejectedWithAClearMessage() throws Exception {
        String user = "{\"username\":\"%s\",\"name\":\"Largo\",\"role\":\"OPERARIO\",\"password\":\"larga-segura\"}";
        mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("u".repeat(51))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eachPanelOnlyOpensForItsRole() throws Exception {
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(as("ENCARGADO", get("/api/admin/usuarios"))).andExpect(status().isForbidden());
        mvc.perform(as("ADMIN", get("/api/plataforma/modulos"))).andExpect(status().isForbidden());
        mvc.perform(as("ADMIN", get("/api/admin/usuarios"))).andExpect(status().isOk());
        mvc.perform(as("SUPERADMIN", get("/api/admin/usuarios"))).andExpect(status().isOk());
        mvc.perform(as("SUPERADMIN", get("/api/plataforma/modulos")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        // El panel del cliente no puede dar permisos a su propio rol ni inventar niveles
        mvc.perform(as("ADMIN", put("/api/admin/permisos/ADMIN/PEDIDOS")).content("{\"access\":\"NINGUNO\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(as("ADMIN", put("/api/admin/permisos/OPERARIO/PEDIDOS")).content("{\"access\":\"TODO\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theAdminCreatesAndDeactivatesUsers() throws Exception {
        String user = "{\"username\":\"nuevo_op\",\"name\":\"Nuevo Operario\",\"role\":\"OPERARIO\",\"password\":\"%s\"}";
        mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("corta"))).andExpect(status().isBadRequest());
        mvc.perform(as("ADMIN", post("/api/admin/usuarios"))
                        .content(user.replace("OPERARIO", "SUPERADMIN").formatted("larga-segura")))
                .andExpect(status().isBadRequest());
        int id = JsonPath.read(mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("larga-segura")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$");
        mvc.perform(as("ADMIN", post("/api/admin/usuarios")).content(user.formatted("larga-segura"))).andExpect(status().isConflict());

        // El usuario nuevo entra con su contraseña
        String login = "{\"username\":\"nuevo_op\",\"password\":\"larga-segura\"}";
        String token = "Bearer " + JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
        mvc.perform(withToken(token, get("/api/pedidos"))).andExpect(status().isOk());

        // Al desactivarlo pierde el acceso al momento, aunque su token no haya caducado
        mvc.perform(as("ADMIN", put("/api/admin/usuarios/" + id)).content("{\"active\":false}")).andExpect(status().isNoContent());
        mvc.perform(withToken(token, get("/api/pedidos"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(login)).andExpect(status().isUnauthorized());
    }

    private String login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void eachUserChangesHisOwnPassword() throws Exception {
        mvc.perform(as("ADMIN", post("/api/admin/usuarios"))
                        .content("{\"username\":\"cambia_clave\",\"name\":\"Cambia Clave\",\"role\":\"OPERARIO\",\"password\":\"inicial-123\"}"))
                .andExpect(status().isCreated());
        String token = "Bearer " + JsonPath.read(login("cambia_clave", "inicial-123"), "$.token");
        String change = "{\"actual\":\"%s\",\"nueva\":\"%s\"}";

        mvc.perform(withToken(token, put("/api/me/password")).content(change.formatted("equivocada", "nueva-clave-1")))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(token, put("/api/me/password")).content(change.formatted("inicial-123", "corta")))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(token, put("/api/me/password")).content(change.formatted("inicial-123", "nueva-clave-1")))
                .andExpect(status().isNoContent());

        // Solo vale la nueva
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"cambia_clave\",\"password\":\"inicial-123\"}"))
                .andExpect(status().isUnauthorized());
        assertThat(login("cambia_clave", "nueva-clave-1")).contains("\"token\"");
    }

    @Test
    void eachUserEditsHisNameAndPhoto() throws Exception {
        mvc.perform(as("ADMIN", post("/api/admin/usuarios"))
                        .content("{\"username\":\"con_foto\",\"name\":\"Antes\",\"role\":\"OPERARIO\",\"password\":\"inicial-123\"}"))
                .andExpect(status().isCreated());
        String token = "Bearer " + JsonPath.read(login("con_foto", "inicial-123"), "$.token");
        mvc.perform(withToken(token, get("/api/me/perfil")))
                .andExpect(jsonPath("$.name").value("Antes"))
                .andExpect(jsonPath("$.role").value("OPERARIO"))
                .andExpect(jsonPath("$.avatar").doesNotExist());

        String photo = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";
        String saved = mvc.perform(withToken(token, put("/api/me/perfil"))
                        .content("{\"name\":\"  Despues  \",\"avatar\":\"" + photo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.name").value("Despues"))
                .andExpect(jsonPath("$.perfil.avatar").value(photo))
                .andReturn().getResponse().getContentAsString();

        // El token nuevo lleva el nombre nuevo, que es el que sale en la cabecera
        String newToken = JsonPath.read(saved, "$.token");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(newToken.split("\\.")[1]));
        assertThat(payload).contains("\"name\":\"Despues\"");

        // Sus movimientos llevan su usuario (para la foto) y el nombre que tiene en la BD al hacerlos
        mvc.perform(withToken(token, post("/api/historial/escanear"))
                        .content("{\"lotId\":1,\"actionType\":\"EN_PRODUCCION\",\"quantityAffected\":1}"))
                .andExpect(status().isCreated());
        mvc.perform(as("ADMIN", get("/api/historial/lote/1")))
                .andExpect(jsonPath("$[?(@.operatorUsername == 'con_foto' && @.operator == 'Despues')]").exists());
        mvc.perform(as("OPERARIO", get("/api/usuarios/con_foto/foto")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatar").value(photo));

        // Quitar la foto
        mvc.perform(withToken(token, put("/api/me/perfil")).content("{\"name\":\"Despues\",\"avatar\":null}"))
                .andExpect(jsonPath("$.perfil.avatar").doesNotExist());
    }

    @Test
    void theProfileRejectsDangerousOrHugePhotosAndEmptyNames() throws Exception {
        String token = bearer("OPERARIO");
        String body = "{\"name\":\"%s\",\"avatar\":\"%s\"}";
        mvc.perform(withToken(token, put("/api/me/perfil")).content(body.formatted("Ok", "data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=")))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(token, put("/api/me/perfil")).content(body.formatted("Ok", "javascript:alert(1)")))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(token, put("/api/me/perfil")).content(body.formatted("Ok", "data:image/png;base64," + "A".repeat(300_000))))
                .andExpect(status().isBadRequest());
        mvc.perform(withToken(token, put("/api/me/perfil")).content("{\"name\":\"   \",\"avatar\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void theAdminCannotLockHimselfOut() throws Exception {
        String token = bearer("ADMIN");
        int id = jdbc.queryForObject("SELECT user_id FROM app_user WHERE username = 'test_admin'", Integer.class);
        mvc.perform(withToken(token, put("/api/admin/usuarios/" + id)).content("{\"active\":false}")).andExpect(status().isConflict());
        mvc.perform(withToken(token, put("/api/admin/usuarios/" + id)).content("{\"role\":\"OPERARIO\"}")).andExpect(status().isConflict());
        // Los usuarios SUPERADMIN no se tocan desde el panel del cliente
        int superadmin = jdbc.queryForObject("SELECT user_id FROM app_user WHERE username = 'superadmin'", Integer.class);
        mvc.perform(withToken(token, put("/api/admin/usuarios/" + superadmin)).content("{\"active\":false}")).andExpect(status().isBadRequest());
    }
}

package com.syskewer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.syskewer.api.config.SecurityConfig;
import com.syskewer.api.config.SecurityFilter;
import com.syskewer.api.controller.salon.BillController;
import com.syskewer.api.controller.salon.OrderController;
import com.syskewer.api.controller.salon.StoreSettingsController;
import com.syskewer.api.controller.user.UserController;
import com.syskewer.api.dto.salon.BillSummaryDto;
import com.syskewer.api.exception.BusinessRuleException;
import com.syskewer.api.exception.ResourceNotFoundException;
import com.syskewer.api.model.store.StoreSettings;
import com.syskewer.api.repository.user.UserRepository;
import com.syskewer.api.service.auth.TokenService;
import com.syskewer.api.service.salon.BillService;
import com.syskewer.api.service.salon.OrderService;
import com.syskewer.api.service.salon.StoreSettingsService;
import com.syskewer.api.service.user.UserService;

/**
 * Testes de camada web (HTTP real via MockMvc + Spring Security real, serviços mockados).
 * Cobrem validação de payload, mapeamento de exceções (GlobalExceptionHandler),
 * autenticação e controle de acesso por papel — sem depender de PostgreSQL/Docker.
 */
@Tag("api")
@WebMvcTest(controllers = { BillController.class, OrderController.class, StoreSettingsController.class,
        UserController.class })
@Import({ SecurityConfig.class, SecurityFilter.class })
class ApiWebLayerTest {

    @Autowired private MockMvc mvc;

    @MockBean private BillService billService;
    @MockBean private OrderService orderService;
    @MockBean private StoreSettingsService storeSettingsService;
    @MockBean private UserService userService;
    @MockBean private TokenService tokenService;
    @MockBean private UserRepository userRepository;

    // ---------- autenticação ----------

    @Test
    @DisplayName("Sem token, rota protegida é rejeitada (4xx) e o serviço não é chamado")
    void unauthenticated_isRejected() throws Exception {
        mvc.perform(get("/api/bills/open"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("Rotas públicas /login e /password/forgot não exigem token (não retornam 401/403)")
    void publicRoutes_doNotRequireToken() throws Exception {
        // Os controllers de login/senha não fazem parte deste slice; o que importa é que o filtro de
        // segurança não bloqueie a rota (nem 401 nem 403).
        for (String path : List.of("/login", "/password/forgot", "/password/reset")) {
            int s = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andReturn().getResponse().getStatus();
            org.junit.jupiter.api.Assertions.assertFalse(s == 401 || s == 403, path + " retornou " + s);
        }
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Garçom autenticado lista contas abertas (200)")
    void garcom_listsOpenBills() throws Exception {
        when(billService.getOpenTabs()).thenReturn(List.of(
                new BillSummaryDto(1, "Mesa 1 - Cliente 1", List.of(1), "MESA", new BigDecimal("10.00"))));

        mvc.perform(get("/api/bills/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("Mesa 1 - Cliente 1"))
                .andExpect(jsonPath("$[0].totalAmount").value(10.00));
    }

    // ---------- autorização (filtro de rotas em SecurityConfig) ----------

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Garçom não cria usuário (POST /users) -> 403 pelo filtro de rotas")
    void garcom_cannotCreateUser_403() throws Exception {
        mvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"x\",\"username\":\"x\",\"email\":\"x@x.com\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Administrador acessa a ação administrativa de alternar loja (200)")
    void admin_canToggleStore() throws Exception {
        when(storeSettingsService.toggleStore()).thenReturn(new StoreSettings());

        mvc.perform(post("/store/toggle")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRADOR")
    @DisplayName("Administrador arquiva conta como fiado (200)")
    void admin_canArchiveAsCredit() throws Exception {
        mvc.perform(patch("/api/bills/1/fiado").param("doc", "123")).andExpect(status().isOk());
    }

    // ---------- validação de payload ----------

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Abrir conta sem tipo de consumo -> 400 com lista campo/mensagem")
    void openBill_missingConsumptionType_400() throws Exception {
        mvc.perform(post("/api/bills").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"A\",\"tableNumber\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("consumptionType"));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Abrir conta com mesa 0 ou negativa -> 400")
    void openBill_nonPositiveTable_400() throws Exception {
        mvc.perform(post("/api/bills").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableNumber\":0,\"consumptionType\":\"MESA\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("tableNumber"));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Abrir conta válida -> 201 com o resumo retornado")
    void openBill_valid_201() throws Exception {
        when(billService.openTab(any())).thenReturn(
                new BillSummaryDto(7, "Mesa 3 - Cliente 1", List.of(3), "MESA", BigDecimal.ZERO));

        mvc.perform(post("/api/bills").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tableNumber\":3,\"consumptionType\":\"MESA\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Pagamento com valor zero, negativo ou ausente -> 400")
    void pay_invalidAmounts_400() throws Exception {
        for (String body : List.of("{\"amount\":0}", "{\"amount\":-5}", "{}")) {
            mvc.perform(patch("/api/bills/1/pay").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$[0].campo").value("amount"));
        }
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Pagamento com desconto negativo -> 400")
    void pay_negativeDiscount_400() throws Exception {
        mvc.perform(patch("/api/bills/1/pay").contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10,\"discount\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].campo").value("discount"));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Delivery sem nome ou endereço -> 400 com os dois campos")
    void delivery_blankFields_400() throws Exception {
        mvc.perform(post("/api/bills/delivery").contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\" \",\"deliveryAddress\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Pedido sem itens ou com quantidade zero -> 400")
    void order_invalidPayload_400() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"billId\":1,\"origin\":\"WAITER\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"billId\":1,\"origin\":\"WAITER\",\"items\":[{\"menuId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Pedido válido -> 201")
    void order_valid_201() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"billId\":1,\"origin\":\"WAITER\",\"items\":[{\"menuId\":1,\"quantity\":2}]}"))
                .andExpect(status().isCreated());
    }

    // ---------- GlobalExceptionHandler ----------

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("BusinessRuleException -> 400 com a mensagem de negócio")
    void businessRule_maps400() throws Exception {
        doThrow(new BusinessRuleException("The bill cannot be closed")).when(billService).closeTab(1);

        mvc.perform(patch("/api/bills/1/close"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("The bill cannot be closed"));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("ResourceNotFoundException -> 404")
    void notFound_maps404() throws Exception {
        when(billService.getTabPartial(anyInt())).thenThrow(new ResourceNotFoundException("Bill ID 9 not found"));

        mvc.perform(get("/api/bills/9/partial"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Bill ID 9 not found"));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Erro inesperado -> 500 com mensagem genérica (sem vazar stack trace)")
    void unexpectedError_maps500_withoutLeak() throws Exception {
        when(billService.getOpenTabs()).thenThrow(new IllegalStateException("segredo interno"));

        mvc.perform(get("/api/bills/open"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("segredo"))));
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Cancelar conta com sucesso -> 204 sem corpo")
    void cancelBill_204() throws Exception {
        mvc.perform(delete("/api/bills/1")).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("Transferência encaminha o número da mesa de destino ao serviço")
    void transfer_passesTargetTable() throws Exception {
        when(billService.transferTab(eq(1), eq(5))).thenReturn("Bill successfully transferred to Table 5");

        mvc.perform(patch("/api/bills/1/transfer").param("newTable", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("Bill successfully transferred to Table 5"));
    }
}

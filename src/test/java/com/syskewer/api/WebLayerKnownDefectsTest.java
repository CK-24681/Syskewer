package com.syskewer.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
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
import com.syskewer.api.controller.salon.StoreSettingsController;
import com.syskewer.api.dto.salon.BillSummaryDto;
import com.syskewer.api.repository.user.UserRepository;
import com.syskewer.api.service.auth.TokenService;
import com.syskewer.api.service.salon.BillService;
import com.syskewer.api.service.salon.StoreSettingsService;

/**
 * Defeitos de camada web/segurança encontrados na execução (ver relatório de
 * avaliação de saída). Descrevem o comportamento ESPERADO conforme padrão HTTP
 * e README. Mantidos {@code @Disabled} até a correção.
 */
@Disabled("Defeitos conhecidos (DEF-08..11) - remover @Disabled para reproduzir")
@Tag("known-defect")
@WebMvcTest(controllers = { BillController.class, StoreSettingsController.class })
@Import({ SecurityConfig.class, SecurityFilter.class })
class WebLayerKnownDefectsTest {

    @Autowired private MockMvc mvc;

    @MockBean private BillService billService;
    @MockBean private StoreSettingsService storeSettingsService;
    @MockBean private TokenService tokenService;
    @MockBean private UserRepository userRepository;

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("DEF-08: garçom em rota @PreAuthorize de admin (/store/toggle) deve receber 403, não 500")
    void garcom_toggleStore_shouldBe403() throws Exception {
        mvc.perform(post("/store/toggle")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("DEF-08b: garçom em /api/bills/{id}/fiado (admin) deve receber 403, não 500")
    void garcom_archiveAsCredit_shouldBe403() throws Exception {
        mvc.perform(patch("/api/bills/1/fiado").param("doc", "123")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DEF-09: requisição sem token deve receber 401 Unauthorized (hoje retorna 403)")
    void unauthenticated_shouldBe401() throws Exception {
        mvc.perform(get("/api/bills/open")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("DEF-11: rota inexistente deve retornar 404, não 500 (handler genérico engole NoResourceFoundException)")
    void unknownRoute_shouldBe404() throws Exception {
        mvc.perform(get("/api/rota-que-nao-existe")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "GARCOM")
    @DisplayName("DEF-10: abrir conta de BALCÃO não deveria exigir número de mesa (README) -> esperado 201")
    void openBalcao_withoutTableNumber_shouldBe201() throws Exception {
        when(billService.openTab(any())).thenReturn(
                new BillSummaryDto(1, "Cliente Balcão", List.of(), "BALCAO", BigDecimal.ZERO));

        mvc.perform(post("/api/bills").contentType(MediaType.APPLICATION_JSON)
                .content("{\"consumptionType\":\"BALCAO\"}"))
                .andExpect(status().isCreated());
    }
}

package rs.ac.uns.ftn.sitapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.ac.uns.ftn.sitapp.controller.UserController;
import rs.ac.uns.ftn.sitapp.service.UserService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("dev")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void allowsSearchWithoutAuthenticationInDevelopment() throws Exception {
        when(userService.search("ana", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/search").param("query", "ana"))
                .andExpect(status().isOk());
    }

    @Test
    void allowsCorsRequestsFromAngularDevelopmentServer() throws Exception {
        mockMvc.perform(options("/api/users/search")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:4200"
                ));
    }

    @Test
    void rejectsCorsRequestsFromOtherOrigins() throws Exception {
        mockMvc.perform(options("/api/users/search")
                        .header(HttpHeaders.ORIGIN, "https://example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}

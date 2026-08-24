package rs.ac.uns.ftn.sitapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import rs.ac.uns.ftn.sitapp.dto.UserResponse;
import rs.ac.uns.ftn.sitapp.service.UserService;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void returnsMatchingUsers() throws Exception {
        var response = new UserResponse(
                7L,
                "alice",
                "Alice",
                "Andric",
                "+38160111222"
        );
        when(userService.search("ali", 12L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/users/search")
                        .param("query", "ali")
                        .param("currentUserId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].firstName").value("Alice"))
                .andExpect(jsonPath("$[0].lastName").value("Andric"))
                .andExpect(jsonPath("$[0].phoneNumber").value("+38160111222"));
        verify(userService).search("ali", 12L);
    }

    @Test
    void returnsEmptyListForBlankQuery() throws Exception {
        when(userService.search("   ", null)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/search").param("query", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(userService).search("   ", null);
    }

    @Test
    void returnsBadRequestWhenQueryParameterIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/search"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }

    @Test
    void returnsBadRequestWhenQueryIsTooLong() throws Exception {
        mockMvc.perform(get("/api/users/search").param("query", "a".repeat(101)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(userService);
    }
}

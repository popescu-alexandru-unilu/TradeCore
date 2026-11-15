package dev.tradecore;

import dev.tradecore.api.error.GlobalExceptionHandler;
import dev.tradecore.api.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
@WebMvcTest
class GlobalExceptionHandlerTest {
    @Autowired
    private MockMvc mockMvc;
    @RestController
    public static class TestController {
        @GetMapping("/api/test/orders/{id}")
        public String getOrder(@PathVariable Long id) {
            throw new ResourceNotFoundException("Order " + id + " does not exist");
        }
        @GetMapping("/api/test/invalid")
        public String invalid(){
            throw new IllegalArgumentException("Must be positive the quantity");
        }
        @GetMapping("/api/test/boom")
        public String boom(){
            throw new IllegalStateException(" Something went wrong");
        }
    }

    @Test
    void returns404WhenOrderNotFound() throws Exception {
        mockMvc.perform(get("/api/test/orders/8")).andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404)).andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND")).andExpect(jsonPath("$.path").value("/api/test/orders/8")).andExpect(jsonPath("$.message").value("Order 8 does not exist"));
    }
    @Test 
    void returns400WhenRequestIsInvalid()throws Exception{
        mockMvc.perform(get("/api/test/invalid")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.code").value("BAD_REQUEST")).andExpect(jsonPath("$.path").value("/api/test/invalid")).andExpect(jsonPath("$.message").value("Must be positive the quantity"));
    }
    @Test
    void returns500WithoutLeakingDetails() throws Exception{
        mockMvc.perform(get("/api/test/boom")).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.status").value(500)).andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR")).andExpect(jsonPath("$.path").value("/api/test/boom")).andExpect(jsonPath("$.message").value("Something went wrong"));
    }
}
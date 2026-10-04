package com.gkcontas.modulith.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gkcontas.modulith.support.TestPostgres;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole application, with every module present.
 *
 * <p>The module test next door proves each piece stands alone; this one proves they add up
 * to the flow a user sees. Both are needed: isolated tests that never meet produce modules
 * that each work and together do nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    protected static final Duration TIMEOUT = Duration.ofSeconds(20);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        TestPostgres.registerOn(registry);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected UUID placeOrder(String sku, int quantity, String amount) throws Exception {
        String body = mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"%s","quantity":%d,"amount":%s,"customerEmail":"ana@example.com"}
                                """.formatted(sku, quantity, amount)))
                // 202: the order exists, the flow has not finished.
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    protected JsonNode order(UUID id) {
        try {
            return objectMapper.readTree(mockMvc.perform(get("/orders/{id}", id))
                    .andReturn().getResponse().getContentAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Named {@code json} and not {@code get}: a helper called {@code get} would shadow the
     * statically imported request builder of the same name, and the compiler error that
     * follows points at the call site rather than at the collision.
     */
    protected JsonNode json(String path, Object... args) {
        try {
            return objectMapper.readTree(mockMvc.perform(get(path, args))
                    .andReturn().getResponse().getContentAsString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Waits for the choreography to settle on a final status. */
    protected JsonNode awaitStatus(UUID id, String expected) {
        await().atMost(TIMEOUT)
                .until(() -> expected.equals(order(id).get("status").asText()));
        return order(id);
    }
}

package com.example.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CartOrderControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    private String createUserAndLogin(String username, String password) throws Exception {
        mvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\",\"confirmPassword\":\"" + password + "\"}"))
                .andExpect(status().isOk());

        var login = mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String token = login.getResponse().getHeader("Authorization");
        assertThat(token).isNotBlank();
        return token;
    }

    private long firstItemId(String token) throws Exception {
        var list = mvc.perform(get("/api/item").header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode items = om.readTree(list.getResponse().getContentAsString());
        assertThat(items.size()).isGreaterThan(0);
        return items.get(0).get("id").asLong();
    }

    @Test
    void protected_userLookup_requiresAuth_401() throws Exception {
        mvc.perform(get("/api/user/username/{username}", "nobody"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void userLookup_withToken_200() throws Exception {
        String username = "flow_" + UUID.randomUUID();
        String token = createUserAndLogin(username, "password123");

        mvc.perform(get("/api/user/username/{username}", username)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void cart_add_remove_submit_order_and_history() throws Exception {
        String username = "order_" + UUID.randomUUID();
        String token = createUserAndLogin(username, "password123");
        long itemId = firstItemId(token);

        // add to cart (quantity 2)
        String addBody = "{\"username\":\"" + username + "\",\"itemId\":" + itemId + ",\"quantity\":2}";
        mvc.perform(post("/api/cart/addToCart")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2));

        // remove one item (quantity 1)
        String removeBody = "{\"username\":\"" + username + "\",\"itemId\":" + itemId + ",\"quantity\":1}";
        mvc.perform(post("/api/cart/removeFromCart")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(removeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));

        // submit order
        mvc.perform(post("/api/order/submit/{username}", username)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username));

        // history contains at least 1 order
        mvc.perform(get("/api/order/history/{username}", username)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void addToCart_withInvalidItem_returns404() throws Exception {
        String username = "baditem_" + UUID.randomUUID();
        String token = createUserAndLogin(username, "password123");

        String body = "{\"username\":\"" + username + "\",\"itemId\":999999,\"quantity\":1}";
        mvc.perform(post("/api/cart/addToCart")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }
}

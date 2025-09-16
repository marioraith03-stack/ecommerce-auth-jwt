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
class ItemControllerAuthTest {

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

    @Test
    void items_withoutToken_is401() throws Exception {
        mvc.perform(get("/api/item"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void items_withToken_returnsList_and_itemById_and_itemByName() throws Exception {
        String username = "itemtest_" + UUID.randomUUID();
        String token = createUserAndLogin(username, "password123");

        // list items
        var list = mvc.perform(get("/api/item")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        JsonNode items = om.readTree(list.getResponse().getContentAsString());
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThan(0);

        long firstId = items.get(0).get("id").asLong();
        String firstName = items.get(0).get("name").asText();

        // by id
        mvc.perform(get("/api/item/id/{id}", firstId)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstId));

        // by name
        mvc.perform(get("/api/item/name/{name}", firstName)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(firstName));
    }
}

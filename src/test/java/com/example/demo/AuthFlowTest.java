package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;

    private String tokenFor(String u, String p) throws Exception {
        mvc.perform(post("/api/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\",\"confirmPassword\":\"" + p + "\"}"))
                .andExpect(status().isOk());

        MvcResult res = mvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + u + "\",\"password\":\"" + p + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        return res.getResponse().getHeader("Authorization");
    }

    @Test
    void protected_withoutToken_401() throws Exception {
        mvc.perform(get("/api/order/history/test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protected_withToken_200() throws Exception {
        String token = tokenFor("testuser", "password123");
        mvc.perform(get("/api/order/history/testuser")
                        .header("Authorization", token))
                .andExpect(status().isOk());
    }
}

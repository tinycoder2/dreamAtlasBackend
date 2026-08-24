//package com.example.dreamjournal.controller;
//
//import com.example.dreamjournal.exception.GlobalExceptionHandler;
//import com.example.dreamjournal.service.DayLogService;
//import com.example.dreamjournal.service.DreamService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.http.MediaType;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest({DayLogController.class, DreamController.class, GlobalExceptionHandler.class})
//class ValidationControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private DayLogService dayLogService;
//
//    @MockBean
//    private DreamService dreamService;
//
//    @Test
//    void rejectsInvalidSleepHours() throws Exception {
//        mockMvc.perform(put("/api/users/user-1/days/2026-08-18")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"sleepHours\":25,\"sleepQuality\":\"GOOD\"}"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.status").value(400));
//    }
//
//    @Test
//    void rejectsBlankDreamText() throws Exception {
//        mockMvc.perform(post("/api/users/user-1/days/2026-08-18/dreams")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"text\":\"   \",\"sortOrder\":0}"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.status").value(400));
//    }
//
//    @Test
//    void rejectsNegativeSortOrder() throws Exception {
//        mockMvc.perform(post("/api/users/user-1/days/2026-08-18/dreams")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("{\"text\":\"text\",\"sortOrder\":-1}"))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.status").value(400));
//    }
//}

package com.niedzszym.carbon_intensity_api.controller;

import com.niedzszym.carbon_intensity_api.dto.ChargingWindowResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.service.ChargingWindowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChargingWindowController.class)
class ChargingWindowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChargingWindowService chargingWindowService;

    @Test
    void shouldReturn200AndChargingWindowJson_whenServiceReturnsValidData() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 6, 16, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 16, 17, 0);
        ChargingWindowResponse response = new ChargingWindowResponse(start, end, 6, 72.5);

        when(chargingWindowService.findOptimalWindow(3)).thenReturn(response);

        mockMvc.perform(get("/charging-window").param("hours", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start").value("2026-06-16T14:00:00"))
                .andExpect(jsonPath("$.end").value("2026-06-16T17:00:00"))
                .andExpect(jsonPath("$.intervalCount").value(6))
                .andExpect(jsonPath("$.cleanEnergyPercent").value(72.5));
    }

    @Test
    void shouldReturn200_whenHoursIsMinimum() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 6, 16, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 16, 9, 0);
        ChargingWindowResponse response = new ChargingWindowResponse(start, end, 2, 50.0);

        when(chargingWindowService.findOptimalWindow(1)).thenReturn(response);

        mockMvc.perform(get("/charging-window").param("hours", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalCount").value(2));
    }

    @Test
    void shouldReturn200_whenHoursIsMaximum() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 6, 16, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 16, 6, 0);
        ChargingWindowResponse response = new ChargingWindowResponse(start, end, 12, 45.0);

        when(chargingWindowService.findOptimalWindow(6)).thenReturn(response);

        mockMvc.perform(get("/charging-window").param("hours", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalCount").value(12));
    }

    @Test
    void shouldReturn400_whenHoursIsZero() throws Exception {
        mockMvc.perform(get("/charging-window").param("hours", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400_whenHoursIsAboveMax() throws Exception {
        mockMvc.perform(get("/charging-window").param("hours", "7"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn503_whenServiceThrowsExternalApiException() throws Exception {
        when(chargingWindowService.findOptimalWindow(3))
                .thenThrow(new ExternalApiException("Not enough intervals for window size"));

        mockMvc.perform(get("/charging-window").param("hours", "3"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("External API unavailable"))
                .andExpect(jsonPath("$.message").value("Not enough intervals for window size"));
    }

    @Test
    void shouldReturnJsonContentType() throws Exception {
        ChargingWindowResponse response = new ChargingWindowResponse(
                LocalDateTime.of(2026, 6, 16, 12, 0),
                LocalDateTime.of(2026, 6, 16, 14, 0),
                4, 60.0);

        when(chargingWindowService.findOptimalWindow(2)).thenReturn(response);

        mockMvc.perform(get("/charging-window").param("hours", "2"))
                .andExpect(status().isOk())
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentType())
                                .isEqualTo(MediaType.APPLICATION_JSON_VALUE));
    }
}

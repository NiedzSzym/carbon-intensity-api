package com.niedzszym.carbon_intensity_api.controller;

import com.niedzszym.carbon_intensity_api.dto.EnergyMixResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.model.DailyEnergyMix;
import com.niedzszym.carbon_intensity_api.service.EnergyMixService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnergyMixController.class)
class EnergyMixControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnergyMixService energyMixService;

    @Test
    void shouldReturn200AndEnergyMixJson_whenServiceReturnsValidData() throws Exception {
        LocalDate today = LocalDate.of(2026, 6, 16);
        DailyEnergyMix todayMix = new DailyEnergyMix(today, Map.of("biomass", 10.0, "gas", 40.0), 30.0);
        DailyEnergyMix tomorrowMix = new DailyEnergyMix(today.plusDays(1), Map.of("biomass", 15.0, "gas", 35.0), 35.0);
        DailyEnergyMix dayAfterMix = new DailyEnergyMix(today.plusDays(2), Map.of("biomass", 20.0, "gas", 30.0), 40.0);
        EnergyMixResponse response = new EnergyMixResponse(todayMix, tomorrowMix, dayAfterMix);

        when(energyMixService.getThreeDayEnergyMix()).thenReturn(response);

        mockMvc.perform(get("/energy-mix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today.date").value("2026-06-16"))
                .andExpect(jsonPath("$.today.cleanEnergyPercent").value(30.0))
                .andExpect(jsonPath("$.today.fuelMixAverages.biomass").value(10.0))
                .andExpect(jsonPath("$.today.fuelMixAverages.gas").value(40.0))
                .andExpect(jsonPath("$.tomorrow.date").value("2026-06-17"))
                .andExpect(jsonPath("$.tomorrow.cleanEnergyPercent").value(35.0))
                .andExpect(jsonPath("$.tomorrow.fuelMixAverages.biomass").value(15.0))
                .andExpect(jsonPath("$.dayAfterTomorrow.date").value("2026-06-18"))
                .andExpect(jsonPath("$.dayAfterTomorrow.cleanEnergyPercent").value(40.0))
                .andExpect(jsonPath("$.dayAfterTomorrow.fuelMixAverages.biomass").value(20.0));
    }

    @Test
    void shouldReturn200WithEmptyFuelMix_whenServiceReturnsEmptyFuelMix() throws Exception {
        LocalDate today = LocalDate.of(2026, 6, 16);
        DailyEnergyMix todayMix = new DailyEnergyMix(today, Map.of(), 0.0);
        DailyEnergyMix tomorrowMix = new DailyEnergyMix(today.plusDays(1), Map.of(), 0.0);
        DailyEnergyMix dayAfterMix = new DailyEnergyMix(today.plusDays(2), Map.of(), 0.0);
        EnergyMixResponse response = new EnergyMixResponse(todayMix, tomorrowMix, dayAfterMix);

        when(energyMixService.getThreeDayEnergyMix()).thenReturn(response);

        mockMvc.perform(get("/energy-mix"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today.fuelMixAverages").isEmpty())
                .andExpect(jsonPath("$.today.cleanEnergyPercent").value(0.0));
    }

    @Test
    void shouldReturn503_whenServiceThrowsExternalApiException() throws Exception {
        when(energyMixService.getThreeDayEnergyMix())
                .thenThrow(new ExternalApiException("Carbon Intensity API returned no data"));

        mockMvc.perform(get("/energy-mix"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("External API unavailable"))
                .andExpect(jsonPath("$.message").value("Carbon Intensity API returned no data"));
    }

    @Test
    void shouldReturn503_whenServiceThrowsRestClientException() throws Exception {
        when(energyMixService.getThreeDayEnergyMix())
                .thenThrow(new org.springframework.web.client.RestClientException("Connection refused"));

        mockMvc.perform(get("/energy-mix"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("External API unavailable"))
                .andExpect(jsonPath("$.message").value("Connection refused"));
    }

    @Test
    void shouldReturnJsonContentType() throws Exception {
        LocalDate today = LocalDate.of(2026, 6, 16);
        DailyEnergyMix todayMix = new DailyEnergyMix(today, Map.of(), 0.0);
        EnergyMixResponse response = new EnergyMixResponse(todayMix, todayMix, todayMix);

        when(energyMixService.getThreeDayEnergyMix()).thenReturn(response);

        mockMvc.perform(get("/energy-mix"))
                .andExpect(status().isOk())
                .andExpect(result ->
                        org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentType())
                                .isEqualTo(MediaType.APPLICATION_JSON_VALUE));
    }
}

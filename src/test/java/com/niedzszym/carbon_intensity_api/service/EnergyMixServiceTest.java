package com.niedzszym.carbon_intensity_api.service;

import com.niedzszym.carbon_intensity_api.client.CarbonIntensityClient;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationData;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationResponse;
import com.niedzszym.carbon_intensity_api.dto.EnergyMixResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.mapper.EnergyIntervalMapper;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyMixServiceTest {

    @Mock
    private CarbonIntensityClient carbonIntensityClient;

    @Mock
    private EnergyIntervalMapper energyIntervalMapper;

    @InjectMocks
    private EnergyMixService energyMixService;


    private EnergyInterval buildInterval(LocalDate date, double cleanEnergyPercent, Map<String, Double> fuelMix) {
        return new EnergyInterval(date.atStartOfDay(), fuelMix, cleanEnergyPercent);
    }

    private CiGenerationResponse buildResponse(List<CiGenerationData> data) {
        return new CiGenerationResponse(data);
    }

    //Happy path

    @Test
    void shouldReturnThreeDayEnergyMix_whenApiReturnsValidData() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = today.plusDays(2);

        Map<String, Double> fuelMix = Map.of("biomass", 10.0, "gas", 40.0, "nuclear", 20.0);

        EnergyInterval intervalToday = buildInterval(today, 30.0, fuelMix);
        EnergyInterval intervalTomorrow = buildInterval(tomorrow, 40.0, fuelMix);
        EnergyInterval intervalDayAfter = buildInterval(dayAfter, 50.0, fuelMix);

        CiGenerationResponse apiResponse = buildResponse(List.of(
                new CiGenerationData("2024-01-01T00:00Z", "2024-01-01T00:30Z", List.of()),
                new CiGenerationData("2024-01-02T00:00Z", "2024-01-02T00:30Z", List.of()),
                new CiGenerationData("2024-01-03T00:00Z", "2024-01-03T00:30Z", List.of())
        ));

        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(apiResponse);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(0))).thenReturn(intervalToday);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(1))).thenReturn(intervalTomorrow);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(2))).thenReturn(intervalDayAfter);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result).isNotNull();
        assertThat(result.today().date()).isEqualTo(today);
        assertThat(result.tomorrow().date()).isEqualTo(tomorrow);
        assertThat(result.dayAfterTomorrow().date()).isEqualTo(dayAfter);
    }

    @Test
    void shouldCalculateCorrectAverageCleanEnergyPercent_forEachDay() {
        // given
        LocalDate today = LocalDate.now();
        Map<String, Double> fuelMix = Map.of("biomass", 10.0, "gas", 40.0);

        // dwa interwały dla tego samego dnia - average powinno wynieść (20+40)/2 = 30.0
        EnergyInterval interval1 = new EnergyInterval(today.atTime(0, 0), fuelMix, 20.0);
        EnergyInterval interval2 = new EnergyInterval(today.atTime(0, 30), fuelMix, 40.0);
        EnergyInterval intervalTomorrow = buildInterval(today.plusDays(1), 50.0, fuelMix);
        EnergyInterval intervalDayAfter = buildInterval(today.plusDays(2), 60.0, fuelMix);

        CiGenerationResponse apiResponse = buildResponse(List.of(
                new CiGenerationData("today1", "today1", List.of()),
                new CiGenerationData("today2", "today2", List.of()),
                new CiGenerationData("tomorrow", "tomorrow", List.of()),
                new CiGenerationData("dayAfter", "dayAfter", List.of())
        ));

        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(apiResponse);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(0))).thenReturn(interval1);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(1))).thenReturn(interval2);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(2))).thenReturn(intervalTomorrow);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(3))).thenReturn(intervalDayAfter);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result.today().cleanEnergyPercent()).isEqualTo(30.0);
    }

    @Test
    void shouldCalculateCorrectFuelMixAverages_forEachDay() {
        // given
        LocalDate today = LocalDate.now();

        EnergyInterval interval1 = new EnergyInterval(today.atTime(0, 0), Map.of("biomass", 10.0), 10.0);
        EnergyInterval interval2 = new EnergyInterval(today.atTime(0, 30), Map.of("biomass", 20.0), 20.0);
        EnergyInterval intervalTomorrow = buildInterval(today.plusDays(1), 50.0, Map.of("biomass", 15.0));
        EnergyInterval intervalDayAfter = buildInterval(today.plusDays(2), 60.0, Map.of("biomass", 15.0));

        CiGenerationResponse apiResponse = buildResponse(List.of(
                new CiGenerationData("t1", "t1", List.of()),
                new CiGenerationData("t2", "t2", List.of()),
                new CiGenerationData("t3", "t3", List.of()),
                new CiGenerationData("t4", "t4", List.of())
        ));

        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(apiResponse);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(0))).thenReturn(interval1);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(1))).thenReturn(interval2);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(2))).thenReturn(intervalTomorrow);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(3))).thenReturn(intervalDayAfter);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result.today().fuelMixAverages()).containsEntry("biomass", 15.0);
    }

    @Test
    void shouldSortDailyMixesByDateAscending() {
        // given
        LocalDate today = LocalDate.now();

        EnergyInterval intervalDayAfter = buildInterval(today.plusDays(2), 50.0, Map.of());
        EnergyInterval intervalTomorrow = buildInterval(today.plusDays(1), 40.0, Map.of());
        EnergyInterval intervalToday = buildInterval(today, 30.0, Map.of());

        CiGenerationResponse apiResponse = buildResponse(List.of(
                new CiGenerationData("d1", "d1", List.of()),
                new CiGenerationData("d2", "d2", List.of()),
                new CiGenerationData("d3", "d3", List.of())
        ));

        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(apiResponse);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(0))).thenReturn(intervalDayAfter);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(1))).thenReturn(intervalTomorrow);
        when(energyIntervalMapper.toEnergyInterval(apiResponse.data().get(2))).thenReturn(intervalToday);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result.today().date()).isEqualTo(today);
        assertThat(result.tomorrow().date()).isEqualTo(today.plusDays(1));
        assertThat(result.dayAfterTomorrow().date()).isEqualTo(today.plusDays(2));
    }

    //Edge cases

    @Test
    void shouldThrowExternalApiException_whenApiReturnsNull() {
        // given
        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> energyMixService.getThreeDayEnergyMix())
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void shouldThrowExternalApiException_whenApiReturnsNullData() {
        // given
        when(carbonIntensityClient.getCiGenerationInterval(any(), any()))
                .thenReturn(new CiGenerationResponse(null));

        // when / then
        assertThatThrownBy(() -> energyMixService.getThreeDayEnergyMix())
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void shouldThrowExternalApiException_whenApiReturnsEmptyData() {
        // given
        when(carbonIntensityClient.getCiGenerationInterval(any(), any()))
                .thenReturn(new CiGenerationResponse(List.of()));

        // when / then
        assertThatThrownBy(() -> energyMixService.getThreeDayEnergyMix())
                .isInstanceOf(ExternalApiException.class);
    }
}
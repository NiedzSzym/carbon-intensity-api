package com.niedzszym.carbon_intensity_api.service;

import com.niedzszym.carbon_intensity_api.dto.EnergyMixResponse;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import com.niedzszym.carbon_intensity_api.provider.CarbonIntensityIntervalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnergyMixServiceTest {

    @Mock
    private CarbonIntensityIntervalProvider intervalProvider;

    @InjectMocks
    private EnergyMixService energyMixService;

    private EnergyInterval buildInterval(LocalDate date, double cleanEnergyPercent, Map<String, Double> fuelMix) {
        return new EnergyInterval(date.atStartOfDay(), fuelMix, cleanEnergyPercent);
    }

    //Happy path

    @Test
    void shouldReturnThreeDayEnergyMix_whenApiReturnsValidData() {
        // given
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate dayAfter = today.plusDays(2);

        Map<String, Double> fuelMix = Map.of("biomass", 10.0, "gas", 40.0, "nuclear", 20.0);

        List<EnergyInterval> intervals = List.of(
                buildInterval(today, 30.0, fuelMix),
                buildInterval(tomorrow, 40.0, fuelMix),
                buildInterval(dayAfter, 50.0, fuelMix)
        );

        when(intervalProvider.fetchIntervalsForNextDays(3)).thenReturn(intervals);

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
        List<EnergyInterval> intervals = List.of(
                new EnergyInterval(today.atTime(0, 0), fuelMix, 20.0),
                new EnergyInterval(today.atTime(0, 30), fuelMix, 40.0),
                buildInterval(today.plusDays(1), 50.0, fuelMix),
                buildInterval(today.plusDays(2), 60.0, fuelMix)
        );

        when(intervalProvider.fetchIntervalsForNextDays(3)).thenReturn(intervals);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result.today().cleanEnergyPercent()).isEqualTo(30.0);
    }

    @Test
    void shouldCalculateCorrectFuelMixAverages_forEachDay() {
        // given
        LocalDate today = LocalDate.now();

        List<EnergyInterval> intervals = List.of(
                new EnergyInterval(today.atTime(0, 0), Map.of("biomass", 10.0), 10.0),
                new EnergyInterval(today.atTime(0, 30), Map.of("biomass", 20.0), 20.0),
                buildInterval(today.plusDays(1), 50.0, Map.of("biomass", 15.0)),
                buildInterval(today.plusDays(2), 60.0, Map.of("biomass", 15.0))
        );

        when(intervalProvider.fetchIntervalsForNextDays(3)).thenReturn(intervals);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result.today().fuelMixAverages()).containsEntry("biomass", 15.0);
    }

    @Test
    void shouldSortDailyMixesByDateAscending() {
        // given
        LocalDate today = LocalDate.now();

        List<EnergyInterval> intervals = List.of(
                buildInterval(today.plusDays(2), 50.0, Map.of()),
                buildInterval(today.plusDays(1), 40.0, Map.of()),
                buildInterval(today, 30.0, Map.of())
        );

        when(intervalProvider.fetchIntervalsForNextDays(3)).thenReturn(intervals);

        // when
        EnergyMixResponse result = energyMixService.getThreeDayEnergyMix();

        // then
        assertThat(result.today().date()).isEqualTo(today);
        assertThat(result.tomorrow().date()).isEqualTo(today.plusDays(1));
        assertThat(result.dayAfterTomorrow().date()).isEqualTo(today.plusDays(2));
    }
}

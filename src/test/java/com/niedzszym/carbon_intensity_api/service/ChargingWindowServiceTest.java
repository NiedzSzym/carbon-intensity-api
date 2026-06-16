package com.niedzszym.carbon_intensity_api.service;

import com.niedzszym.carbon_intensity_api.dto.ChargingWindowResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import com.niedzszym.carbon_intensity_api.provider.CarbonIntensityIntervalProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChargingWindowServiceTest {

    @Mock
    private CarbonIntensityIntervalProvider intervalProvider;

    @InjectMocks
    private ChargingWindowService chargingWindowService;

    private EnergyInterval buildInterval(LocalDateTime from, double cleanEnergyPercent) {
        return new EnergyInterval(from, Map.of(), cleanEnergyPercent);
    }

    @Test
    void shouldReturnOptimalWindow_whenCleanEnergyPeaksInMiddle() {
        // given: 8 intervals, 3-hour window (6 intervals)
        // values: 10, 20, 50, 60, 70, 80, 30, 20
        // windows: [0-5]=48.33, [1-6]=51.67, [2-7]=51.67 → first max (startIdx=1)
        List<EnergyInterval> intervals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 6, 16, 0, 0);
        intervals.add(buildInterval(base, 10.0));
        intervals.add(buildInterval(base.plusMinutes(30), 20.0));
        intervals.add(buildInterval(base.plusMinutes(60), 50.0));
        intervals.add(buildInterval(base.plusMinutes(90), 60.0));
        intervals.add(buildInterval(base.plusMinutes(120), 70.0));
        intervals.add(buildInterval(base.plusMinutes(150), 80.0));
        intervals.add(buildInterval(base.plusMinutes(180), 30.0));
        intervals.add(buildInterval(base.plusMinutes(210), 20.0));

        when(intervalProvider.fetchIntervalsForNextDays(2)).thenReturn(intervals);

        // when
        ChargingWindowResponse result = chargingWindowService.findOptimalWindow(3);

        // then
        assertThat(result.start()).isEqualTo(base.plusMinutes(30));
        assertThat(result.end()).isEqualTo(base.plusMinutes(180));
        assertThat(result.intervalCount()).isEqualTo(6);
        assertThat(result.cleanEnergyPercent()).isEqualTo(310.0 / 6.0);
    }

    @Test
    void shouldReturnOptimalWindow_whenCleanEnergyIsHighestAtStart() {
        List<EnergyInterval> intervals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 6, 16, 0, 0);
        // values: 90, 80, 70, 40, 30, 20, 10, 10
        // k=4 (2 hours), best window: indices 0-3, avg=70
        intervals.add(buildInterval(base, 90.0));
        intervals.add(buildInterval(base.plusMinutes(30), 80.0));
        intervals.add(buildInterval(base.plusMinutes(60), 70.0));
        intervals.add(buildInterval(base.plusMinutes(90), 40.0));
        intervals.add(buildInterval(base.plusMinutes(120), 30.0));
        intervals.add(buildInterval(base.plusMinutes(150), 20.0));

        when(intervalProvider.fetchIntervalsForNextDays(2)).thenReturn(intervals);

        // when
        ChargingWindowResponse result = chargingWindowService.findOptimalWindow(2);

        // then
        assertThat(result.start()).isEqualTo(base);
        assertThat(result.end()).isEqualTo(base.plusMinutes(120));
        assertThat(result.intervalCount()).isEqualTo(4);
        assertThat(result.cleanEnergyPercent()).isEqualTo(280.0 / 4.0);
    }

    @Test
    void shouldReturnOptimalWindow_whenCleanEnergyIsHighestAtEnd() {
        List<EnergyInterval> intervals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 6, 16, 0, 0);
        // values: 10, 20, 30, 60, 80, 90
        // k=4, window 2-5: avg=65
        intervals.add(buildInterval(base, 10.0));
        intervals.add(buildInterval(base.plusMinutes(30), 20.0));
        intervals.add(buildInterval(base.plusMinutes(60), 30.0));
        intervals.add(buildInterval(base.plusMinutes(90), 60.0));
        intervals.add(buildInterval(base.plusMinutes(120), 80.0));
        intervals.add(buildInterval(base.plusMinutes(150), 90.0));

        when(intervalProvider.fetchIntervalsForNextDays(2)).thenReturn(intervals);

        // when
        ChargingWindowResponse result = chargingWindowService.findOptimalWindow(2);

        // then
        assertThat(result.start()).isEqualTo(base.plusMinutes(60));
        assertThat(result.end()).isEqualTo(base.plusMinutes(180));
        assertThat(result.intervalCount()).isEqualTo(4);
        assertThat(result.cleanEnergyPercent()).isEqualTo(260.0 / 4.0);
    }

    @Test
    void shouldReturnWindowWithExactSize_whenIntervalsEqualWindowSize() {
        List<EnergyInterval> intervals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 6, 16, 0, 0);
        intervals.add(buildInterval(base, 50.0));
        intervals.add(buildInterval(base.plusMinutes(30), 60.0));

        when(intervalProvider.fetchIntervalsForNextDays(2)).thenReturn(intervals);

        // when
        ChargingWindowResponse result = chargingWindowService.findOptimalWindow(1);

        // then
        assertThat(result.start()).isEqualTo(base);
        assertThat(result.end()).isEqualTo(base.plusMinutes(60));
        assertThat(result.intervalCount()).isEqualTo(2);
        assertThat(result.cleanEnergyPercent()).isEqualTo(55.0);
    }

    @Test
    void shouldThrowExternalApiException_whenNotEnoughIntervals() {
        List<EnergyInterval> intervals = new ArrayList<>();
        intervals.add(buildInterval(LocalDateTime.of(2026, 6, 16, 0, 0), 50.0));

        when(intervalProvider.fetchIntervalsForNextDays(2)).thenReturn(intervals);

        assertThatThrownBy(() -> chargingWindowService.findOptimalWindow(3))
                .isInstanceOf(ExternalApiException.class)
                .hasMessageContaining("Not enough intervals");
    }

    @Test
    void shouldHandleOneHourWindow() {
        List<EnergyInterval> intervals = new ArrayList<>();
        LocalDateTime base = LocalDateTime.of(2026, 6, 16, 0, 0);
        // values: 10, 80, 20, 30
        // k=2 (1h), best window: indices 1-2, avg=50
        intervals.add(buildInterval(base, 10.0));
        intervals.add(buildInterval(base.plusMinutes(30), 80.0));
        intervals.add(buildInterval(base.plusMinutes(60), 20.0));
        intervals.add(buildInterval(base.plusMinutes(90), 30.0));

        when(intervalProvider.fetchIntervalsForNextDays(2)).thenReturn(intervals);

        // when
        ChargingWindowResponse result = chargingWindowService.findOptimalWindow(1);

        // then
        assertThat(result.intervalCount()).isEqualTo(2);
        assertThat(result.cleanEnergyPercent()).isEqualTo(100.0 / 2.0);
        assertThat(result.start()).isEqualTo(base.plusMinutes(30));
        assertThat(result.end()).isEqualTo(base.plusMinutes(90));
    }
}

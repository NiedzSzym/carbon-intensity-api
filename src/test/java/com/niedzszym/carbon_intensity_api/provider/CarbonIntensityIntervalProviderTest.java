package com.niedzszym.carbon_intensity_api.provider;

import com.niedzszym.carbon_intensity_api.client.CarbonIntensityClient;
import com.niedzszym.carbon_intensity_api.client.dto.CiFuelMix;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationData;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationResponse;
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
class CarbonIntensityIntervalProviderTest {

    @Mock
    private CarbonIntensityClient carbonIntensityClient;

    @Mock
    private EnergyIntervalMapper energyIntervalMapper;

    @InjectMocks
    private CarbonIntensityIntervalProvider provider;

    @Test
    void shouldReturnMappedIntervals_whenApiReturnsValidData() {
        // given
        CiGenerationData data1 = new CiGenerationData("2026-06-16T00:00Z", "2026-06-16T00:30Z",
                List.of(new CiFuelMix("biomass", 10.0), new CiFuelMix("gas", 40.0)));
        CiGenerationData data2 = new CiGenerationData("2026-06-16T00:30Z", "2026-06-16T01:00Z",
                List.of(new CiFuelMix("biomass", 15.0), new CiFuelMix("gas", 35.0)));
        CiGenerationResponse response = new CiGenerationResponse(List.of(data1, data2));

        EnergyInterval interval1 = new EnergyInterval(
                LocalDate.of(2026, 6, 16).atTime(0, 0), Map.of("biomass", 10.0, "gas", 40.0), 10.0);
        EnergyInterval interval2 = new EnergyInterval(
                LocalDate.of(2026, 6, 16).atTime(0, 30), Map.of("biomass", 15.0, "gas", 35.0), 15.0);

        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(response);
        when(energyIntervalMapper.toEnergyInterval(data1)).thenReturn(interval1);
        when(energyIntervalMapper.toEnergyInterval(data2)).thenReturn(interval2);

        // when
        List<EnergyInterval> result = provider.fetchIntervalsForNextDays(3);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(interval1);
        assertThat(result.get(1)).isEqualTo(interval2);
    }

    @Test
    void shouldThrowExternalApiException_whenApiReturnsNull() {
        // given
        when(carbonIntensityClient.getCiGenerationInterval(any(), any())).thenReturn(null);

        // when / then
        assertThatThrownBy(() -> provider.fetchIntervalsForNextDays(3))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void shouldThrowExternalApiException_whenApiReturnsNullData() {
        // given
        when(carbonIntensityClient.getCiGenerationInterval(any(), any()))
                .thenReturn(new CiGenerationResponse(null));

        // when / then
        assertThatThrownBy(() -> provider.fetchIntervalsForNextDays(3))
                .isInstanceOf(ExternalApiException.class);
    }

    @Test
    void shouldThrowExternalApiException_whenApiReturnsEmptyData() {
        // given
        when(carbonIntensityClient.getCiGenerationInterval(any(), any()))
                .thenReturn(new CiGenerationResponse(List.of()));

        // when / then
        assertThatThrownBy(() -> provider.fetchIntervalsForNextDays(3))
                .isInstanceOf(ExternalApiException.class);
    }
}

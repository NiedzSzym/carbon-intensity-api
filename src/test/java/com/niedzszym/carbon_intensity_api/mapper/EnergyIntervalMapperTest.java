package com.niedzszym.carbon_intensity_api.mapper;

import com.niedzszym.carbon_intensity_api.client.dto.CiFuelMix;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationData;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class EnergyIntervalMapperTest {

    private EnergyIntervalMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EnergyIntervalMapper();
    }

    //Mapping

    @Test
    void shouldMapFromDateTimeCorrectly() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                List.of(new CiFuelMix("biomass", 5.5))
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then
        assertThat(result.from()).isEqualTo(LocalDateTime.of(2024, 1, 1, 11, 30));
    }

    @Test
    void shouldMapFuelMixCorrectly() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                List.of(
                        new CiFuelMix("biomass", 5.5),
                        new CiFuelMix("nuclear", 21.0),
                        new CiFuelMix("gas", 30.0)
                )
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then
        assertThat(result.fuelMix())
                .containsEntry("biomass", 5.5)
                .containsEntry("nuclear", 21.0)
                .containsEntry("gas", 30.0);
    }

    //Calculation of clean energy ratio

    @Test
    void shouldCalculateCleanEnergyPercentFromCleanSourcesOnly() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                List.of(
                        new CiFuelMix("biomass", 5.0),
                        new CiFuelMix("nuclear", 20.0),
                        new CiFuelMix("hydro", 3.0),
                        new CiFuelMix("wind", 15.0),
                        new CiFuelMix("solar", 7.0),
                        new CiFuelMix("gas", 40.0),
                        new CiFuelMix("coal", 10.0)
                )
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then - tylko clean sources: 5+20+3+15+7 = 50.0
        assertThat(result.cleanEnergyPercent()).isEqualTo(50.0);
    }

    @Test
    void shouldReturnZeroCleanEnergyPercent_whenNoCleanSources() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                List.of(
                        new CiFuelMix("gas", 60.0),
                        new CiFuelMix("coal", 40.0)
                )
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then
        assertThat(result.cleanEnergyPercent()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnFullCleanEnergyPercent_whenAllSourcesAreClean() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                List.of(
                        new CiFuelMix("biomass", 20.0),
                        new CiFuelMix("nuclear", 30.0),
                        new CiFuelMix("hydro", 10.0),
                        new CiFuelMix("wind", 25.0),
                        new CiFuelMix("solar", 15.0)
                )
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then
        assertThat(result.cleanEnergyPercent()).isEqualTo(100.0);
    }

    //Edge cases

    @Test
    void shouldReturnEmptyFuelMix_whenGenerationMixIsNull() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                null
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then
        assertThat(result.fuelMix()).isEmpty();
        assertThat(result.cleanEnergyPercent()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnEmptyFuelMix_whenGenerationMixIsEmpty() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "2024-01-01T11:30Z",
                "2024-01-01T12:00Z",
                List.of()
        );

        // when
        EnergyInterval result = mapper.toEnergyInterval(interval);

        // then
        assertThat(result.fuelMix()).isEmpty();
        assertThat(result.cleanEnergyPercent()).isEqualTo(0.0);
    }

    @Test
    void shouldThrowException_whenDateTimeIsInvalidFormat() {
        // given
        CiGenerationData interval = new CiGenerationData(
                "invalid-date",
                "2024-01-01T12:00Z",
                List.of(new CiFuelMix("biomass", 5.0))
        );

        // when / then
        assertThatThrownBy(() -> mapper.toEnergyInterval(interval))
                .isInstanceOf(DateTimeParseException.class);
    }
}
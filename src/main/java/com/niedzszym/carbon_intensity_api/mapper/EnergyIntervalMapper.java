package com.niedzszym.carbon_intensity_api.mapper;

import com.niedzszym.carbon_intensity_api.client.dto.CiFuelMix;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationData;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import com.niedzszym.carbon_intensity_api.model.enums.CleanEnergySource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class EnergyIntervalMapper {
    private static final Set<String> CLEAN_ENERGY_SOURCES = Stream.of(CleanEnergySource.values())
            .map(source -> source.name().toLowerCase())
            .collect(Collectors.toSet());

    public EnergyInterval toEnergyInterval(CiGenerationData interval) {
        LocalDateTime from = parseDateTime(interval.from());
        Map<String, Double> fuelMix = toFuelMixMap(interval.generationmix());
        double cleanEnergyPercent = calculateCleanEnergyPercent(fuelMix);

        return new EnergyInterval(from, fuelMix, cleanEnergyPercent);
    }

    private LocalDateTime parseDateTime(String dateTime) {
        return OffsetDateTime.parse(dateTime).toLocalDateTime();
    }

    private Map<String, Double> toFuelMixMap(List<CiFuelMix> generationMix) {
        if (generationMix == null) {
            return Map.of();
        }
        return generationMix.stream()
                .collect(Collectors.toMap(CiFuelMix::fuel, CiFuelMix::perc));
    }

    private double calculateCleanEnergyPercent(Map<String, Double> fuelMix) {
        return fuelMix.entrySet().stream()
                .filter(entry -> CLEAN_ENERGY_SOURCES.contains(entry.getKey()))
                .mapToDouble(Map.Entry::getValue)
                .sum();
    }
}

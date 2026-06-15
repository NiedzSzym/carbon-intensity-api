package com.niedzszym.carbon_intensity_api.dto;

import java.time.LocalDate;
import java.util.Map;

public record DailyEnergyMixResponse(
        LocalDate date,
        Map<String, Double> generationMixAverages,
        double cleanEnergyPercent
) {
}

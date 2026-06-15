package com.niedzszym.carbon_intensity_api.model;

import java.time.LocalDateTime;
import java.util.Map;

public record EnergyInterval (
        LocalDateTime from,
        Map<String, Double> fuelMix,
        double cleanEnergyPercent
) {
}

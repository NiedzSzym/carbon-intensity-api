package com.niedzszym.carbon_intensity_api.model;

import java.time.ZonedDateTime;
import java.util.Map;

public record EnergyInterval (
        ZonedDateTime from,
        Map<String, Double> fuelMix,
        double cleanEnergyPercent
) {
}

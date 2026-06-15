package com.niedzszym.carbon_intensity_api.model;


import java.time.LocalDate;
import java.util.Map;

public record DailyEnergyMix (
    LocalDate date,
    Map<String, Double> fuelMixAverages,
    double cleanEnergyPercent

) {
}

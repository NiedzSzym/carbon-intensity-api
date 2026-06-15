package com.niedzszym.carbon_intensity_api.dto;

import com.niedzszym.carbon_intensity_api.model.DailyEnergyMix;

public record EnergyMixResponse(
    DailyEnergyMix today,
    DailyEnergyMix tomorrow,
    DailyEnergyMix dayAfterTomorrow
) {
}

package com.niedzszym.carbon_intensity_api.dto;

public record GenerationMixResponse(
    DailyGenerationMixResponse today,
    DailyGenerationMixResponse tomorrow,
    DailyGenerationMixResponse dayAfterTomorrow
) {
}

package com.niedzszym.carbon_intensity_api.client.dto;

import java.util.List;

public record CiGenerationResponse(
        String from,
        String to,
        List<CiFuelMix> GenerationMix
) {
}

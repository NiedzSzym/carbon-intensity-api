package com.niedzszym.carbon_intensity_api.client.dto;

import java.util.List;

public record CiGenerationData(
        String from,
        String to,
        List<CiFuelMix> generationmix
) {
}

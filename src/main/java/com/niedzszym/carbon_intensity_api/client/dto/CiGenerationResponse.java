package com.niedzszym.carbon_intensity_api.client.dto;

import java.util.List;

public record CiGenerationResponse(
        List<CiGenerationData> data
) {
}

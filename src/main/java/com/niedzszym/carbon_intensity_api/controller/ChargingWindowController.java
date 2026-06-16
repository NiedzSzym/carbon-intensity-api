package com.niedzszym.carbon_intensity_api.controller;

import com.niedzszym.carbon_intensity_api.dto.ChargingWindowResponse;
import com.niedzszym.carbon_intensity_api.service.ChargingWindowService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class ChargingWindowController {

    private final ChargingWindowService chargingWindowService;

    public ChargingWindowController(ChargingWindowService chargingWindowService) {
        this.chargingWindowService = chargingWindowService;
    }

    @GetMapping("/charging-window")
    public ChargingWindowResponse getOptimalChargingWindow(
            @RequestParam @Min(1) @Max(6) int hours) {
        return chargingWindowService.findOptimalWindow(hours);
    }
}

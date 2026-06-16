package com.niedzszym.carbon_intensity_api.controller;

import com.niedzszym.carbon_intensity_api.dto.EnergyMixResponse;
import com.niedzszym.carbon_intensity_api.service.EnergyMixService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnergyMixController {

    private final EnergyMixService energyMixService;

    public EnergyMixController(EnergyMixService energyMixService) {
        this.energyMixService = energyMixService;
    }

    @GetMapping("/energy-mix")
    public EnergyMixResponse getThreeDayEnergyMix() {
        return energyMixService.getThreeDayEnergyMix();
    }
}

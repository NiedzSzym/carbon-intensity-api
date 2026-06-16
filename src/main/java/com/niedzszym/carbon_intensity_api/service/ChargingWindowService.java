package com.niedzszym.carbon_intensity_api.service;

import com.niedzszym.carbon_intensity_api.dto.ChargingWindowResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import com.niedzszym.carbon_intensity_api.provider.CarbonIntensityIntervalProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChargingWindowService {
    private final CarbonIntensityIntervalProvider intervalProvider;

    public ChargingWindowService(CarbonIntensityIntervalProvider intervalProvider) {
        this.intervalProvider = intervalProvider;
    }

    public ChargingWindowResponse findOptimalWindow(int hours) {
        int k = hours * 2;
        List<EnergyInterval> intervals = intervalProvider.fetchIntervalsForNextDays(2);

        if (intervals.size() < k) {
            throw new ExternalApiException("Not enough intervals for window size");
        }

        double[] cleanPercents = intervals.stream()
                .mapToDouble(EnergyInterval::cleanEnergyPercent).toArray();

        // Sliding window
        double windowSum = 0;
        for (int i = 0; i < k; i++) {
            windowSum += cleanPercents[i];
        }
        double bestAvg = windowSum / k;
        int bestStart = 0;

        for (int i = k; i < cleanPercents.length; i++) {
            windowSum = windowSum - cleanPercents[i - k] + cleanPercents[i];
            double avg = windowSum / k;
            if (avg > bestAvg) {
                bestAvg = avg;
                bestStart = i - k + 1;
            }
        }

        LocalDateTime start = intervals.get(bestStart).from();
        LocalDateTime end = intervals.get(bestStart + k - 1).from().plusMinutes(30);
        return new ChargingWindowResponse(start, end, k, bestAvg);
    }
}

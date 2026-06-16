package com.niedzszym.carbon_intensity_api.service;

import com.niedzszym.carbon_intensity_api.client.CarbonIntensityClient;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationResponse;
import com.niedzszym.carbon_intensity_api.dto.EnergyMixResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.mapper.EnergyIntervalMapper;
import com.niedzszym.carbon_intensity_api.model.DailyEnergyMix;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class EnergyMixService {
    CarbonIntensityClient carbonIntensityClient;
    EnergyIntervalMapper energyIntervalMapper;

    public EnergyMixService(CarbonIntensityClient carbonIntensityClient, EnergyIntervalMapper energyIntervalMapper) {
        this.carbonIntensityClient = carbonIntensityClient;
        this.energyIntervalMapper = energyIntervalMapper;
    }

    public EnergyMixResponse getThreeDayEnergyMix() {
        List<EnergyInterval> intervals = fetchIntervalsForNextDays(3);
        Map<LocalDate, List<EnergyInterval>> groupedByDay = groupByDate(intervals);
        List<DailyEnergyMix> dailyMixes = groupedByDay.entrySet().stream()
                .map(entry -> toDailyEnergyMix(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyEnergyMix::date))
                .toList();

        return buildResponse(dailyMixes);
    }

    private List<EnergyInterval> fetchIntervalsForNextDays(int days) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        String from = today.atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String to = today.plusDays(days).atStartOfDay().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        CiGenerationResponse response = carbonIntensityClient.getCiGenerationInterval(from, to);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ExternalApiException("Carbon Intensity API returned no data for range: " + from + " to " + to);
        }

        return response.data().stream()
                .map(energyIntervalMapper::toEnergyInterval)
                .toList();
    }

    private Map<LocalDate, List<EnergyInterval>> groupByDate(List<EnergyInterval> intervals) {
        return intervals.stream()
                .collect(Collectors.groupingBy(interval -> interval.from().toLocalDate()));
    }

    private DailyEnergyMix toDailyEnergyMix(LocalDate date, List<EnergyInterval> dayIntervals) {
        Map<String, Double> fuelMixAverages = calculateFuelMixAverages(dayIntervals);
        double cleanEnergyPercent = calculateAverageCleanEnergyPercent(dayIntervals);

        return new DailyEnergyMix(date, fuelMixAverages, cleanEnergyPercent);
    }

    private Map<String, Double> calculateFuelMixAverages(List<EnergyInterval> intervals) {
        return intervals.stream()
                .flatMap(interval -> interval.fuelMix().entrySet().stream())
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.averagingDouble(Map.Entry::getValue)
                ));
    }

    private double calculateAverageCleanEnergyPercent(List<EnergyInterval> intervals) {
        return intervals.stream()
                .mapToDouble(EnergyInterval::cleanEnergyPercent)
                .average()
                .orElse(0.0);
    }

    private EnergyMixResponse buildResponse(List<DailyEnergyMix> dailyMixes) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        Map<LocalDate, DailyEnergyMix> mixByDate = dailyMixes.stream()
                .collect(Collectors.toMap(DailyEnergyMix::date, Function.identity()));

        DailyEnergyMix todayMix = mixByDate.get(today);
        if (todayMix == null) {
            throw new ExternalApiException("No energy mix data available for today");
        }

        return new EnergyMixResponse(
                todayMix,
                mixByDate.get(today.plusDays(1)),
                mixByDate.get(today.plusDays(2))
        );
    }
}

package com.niedzszym.carbon_intensity_api.provider;
import com.niedzszym.carbon_intensity_api.client.CarbonIntensityClient;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationResponse;
import com.niedzszym.carbon_intensity_api.exception.ExternalApiException;
import com.niedzszym.carbon_intensity_api.mapper.EnergyIntervalMapper;
import com.niedzszym.carbon_intensity_api.model.EnergyInterval;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;


@Component
public class CarbonIntensityIntervalProvider {
    private final CarbonIntensityClient carbonIntensityClient;
    private final EnergyIntervalMapper energyIntervalMapper;
    public CarbonIntensityIntervalProvider(CarbonIntensityClient carbonIntensityClient,
                                           EnergyIntervalMapper energyIntervalMapper) {
        this.carbonIntensityClient = carbonIntensityClient;
        this.energyIntervalMapper = energyIntervalMapper;
    }
    public List<EnergyInterval> fetchIntervalsForNextDays(int days) {
        LocalDate today = LocalDate.now(ZoneId.of("Europe/London"));
        String from = today.atStartOfDay().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String to = today.plusDays(days).atStartOfDay().atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        CiGenerationResponse response = carbonIntensityClient.getCiGenerationInterval(from, to);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new ExternalApiException(
                    "Carbon Intensity API returned no data for range: " + from + " to " + to);
        }
        return response.data().stream()
                .map(energyIntervalMapper::toEnergyInterval)
                .toList();
    }
}
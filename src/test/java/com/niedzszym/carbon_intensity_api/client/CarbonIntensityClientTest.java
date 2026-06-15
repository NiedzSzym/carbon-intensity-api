package com.niedzszym.carbon_intensity_api.client;


import com.niedzszym.carbon_intensity_api.client.CarbonIntensityClient;
import com.niedzszym.carbon_intensity_api.client.dto.CiFuelMix;
import com.niedzszym.carbon_intensity_api.client.dto.CiGenerationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonIntensityClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CarbonIntensityClient carbonIntensityClient;

    @Test
    void shouldReturnGenerationResponse_whenApiCallSucceeds() {
        // given
        String from = "2024-01-01T00:00Z";
        String to = "2024-01-01T00:30Z";

        CiGenerationResponse expectedResponse = new CiGenerationResponse(from, to,
                List.of(new CiFuelMix("biomass", 5.5), new CiFuelMix("nuclear", 21.0)));

        when(restTemplate.getForObject(
                "https://api.carbonintensity.org.uk/generation/{from}/{to}",
                CiGenerationResponse.class,
                from, to))
                .thenReturn(expectedResponse);

        // when
        CiGenerationResponse result = carbonIntensityClient.getCiGenerationInterval(from, to);

        // then
        assertThat(result).isEqualTo(expectedResponse);
        verify(restTemplate).getForObject(
                "https://api.carbonintensity.org.uk/generation/{from}/{to}",
                CiGenerationResponse.class,
                from, to);
    }

    @Test
    void shouldReturnNull_whenApiReturnsNull() {
        // given
        String from = "2024-01-01T00:00Z";
        String to = "2024-01-01T00:30Z";

        when(restTemplate.getForObject(any(), eq(CiGenerationResponse.class), any(), any()))
                .thenReturn(null);

        // when
        CiGenerationResponse result = carbonIntensityClient.getCiGenerationInterval(from, to);

        // then
        assertThat(result).isNull();
    }

    @Test
    void shouldPropagateException_whenRestTemplateThrows() {
        // given
        String from = "2024-01-01T00:00Z";
        String to = "2024-01-01T00:30Z";

        when(restTemplate.getForObject(any(), eq(CiGenerationResponse.class), any(), any()))
                .thenThrow(new RestClientException("Connection refused"));

        // when / then
        assertThrows(RestClientException.class,
                () -> carbonIntensityClient.getCiGenerationInterval(from, to));
    }
}

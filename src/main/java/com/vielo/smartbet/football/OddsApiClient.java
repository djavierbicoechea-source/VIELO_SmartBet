package com.vielo.smartbet.football;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class OddsApiClient {

    private final WebClient webClient;
    private final OddsApiProperties props;

    public OddsApiClient(WebClient webClient, OddsApiProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public Mono<List<OddsApiResponse>> getUpcomingOdds(String sportKey) {
        String url = UriComponentsBuilder.fromHttpUrl(props.baseUrl())
                .pathSegment("v4", "sports", sportKey, "odds")
                .queryParam("apiKey", props.key())
                .queryParam("regions", props.regions())
                .queryParam("markets", props.markets())
                .queryParam("oddsFormat", "decimal")
                .queryParam("dateFormat", "iso")
                .toUriString();

        return webClient.get()
                .uri(url)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Odds API error: " + body))))
                .bodyToMono(new ParameterizedTypeReference<List<OddsApiResponse>>() {});
    }
}

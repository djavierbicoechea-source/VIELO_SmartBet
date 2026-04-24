package com.vielo.smartbet.football;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@Component
public class ApiFootballClient {

    private final WebClient webClient;
    private final ApiFootballProperties props;

    public ApiFootballClient(WebClient webClient, ApiFootballProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public Mono<ApiFootballFixturesResponse> getFixtures(LocalDate date) {
        String base = props.baseUrl();
        return webClient.get()
                .uri(base + "/fixtures?date=" + date)
                .header("x-apisports-key", props.key())
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Fixtures API error: " + body))))
                .bodyToMono(ApiFootballFixturesResponse.class);
    }

    public Mono<ApiFootballOddsResponse> getOddsByFixture(long fixtureId) {
        String base = props.baseUrl();
        return webClient.get()
                .uri(base + "/odds?fixture=" + fixtureId)
                .header("x-apisports-key", props.key())
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("Odds API error: " + body))))
                .bodyToMono(ApiFootballOddsResponse.class);
    }
}

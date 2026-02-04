package com.farias.sentinel.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class SlackService {

    private final WebClient webClient;
    @Value("${sentinel.slack.webhook}")
    private String slackWebhookUrl;

    public SlackService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public void enviarNotificacion(String mensaje) {
        // slack espera un JSON con la llave "text"
        Map<String, String> body = Map.of("text", mensaje);

        webClient.post()
                .uri(slackWebhookUrl)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .subscribe(response -> System.out.println("Slack notificado: " + response),
                        error -> System.err.println("Error notificando a Slack: " + error.getMessage()));
    }
}

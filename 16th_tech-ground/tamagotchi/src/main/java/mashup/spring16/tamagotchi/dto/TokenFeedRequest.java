package mashup.spring16.tamagotchi.dto;

public record TokenFeedRequest(String apiToken, long inputTokens, long outputTokens) {
}
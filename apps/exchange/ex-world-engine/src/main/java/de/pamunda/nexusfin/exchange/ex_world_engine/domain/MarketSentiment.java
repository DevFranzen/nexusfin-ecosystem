package de.pamunda.nexusfin.exchange.ex_world_engine.domain;

public record MarketSentiment(double score) {

    public MarketSentiment {
        if (score < -1.0 || score > 1.0) {
            throw new IllegalArgumentException(
                "Sentiment score must be between -1.0 and 1.0, got: " + score
            );
        }
    }

    public static MarketSentiment fromScore(double score) {
        return new MarketSentiment(score);
    }

    public static MarketSentiment neutral() {
        return new MarketSentiment(0.0);
    }

    public static MarketSentiment bullish(double intensity) {
        if (intensity < 0.0 || intensity > 1.0) {
            throw new IllegalArgumentException("Intensity must be between 0.0 and 1.0");
        }
        return new MarketSentiment(intensity);
    }

    public static MarketSentiment bearish(double intensity) {
        if (intensity < 0.0 || intensity > 1.0) {
            throw new IllegalArgumentException("Intensity must be between 0.0 and 1.0");
        }
        return new MarketSentiment(-intensity);
    }

    public boolean isBullish() {
        return score > 0.0;
    }

    public boolean isBearish() {
        return score < 0.0;
    }

    public boolean isNeutral() {
        return score == 0.0;
    }

    @Override
    public String toString() {
        if (isNeutral()) {
            return "NEUTRAL";
        }
        return String.format("%s (%.2f)", isBullish() ? "BULLISH" : "BEARISH", Math.abs(score));
    }
}

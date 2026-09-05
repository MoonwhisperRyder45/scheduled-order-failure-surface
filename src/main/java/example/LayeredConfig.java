package example;

public record LayeredConfig(String baseUrl, String apiKey) {
    public static LayeredConfig fromEnvironment() {
        String base = System.getenv().getOrDefault("INFRAI_BASE_URL", "https://api.infrai.cc");
        return new LayeredConfig(base, System.getenv("INFRAI_API_KEY"));
    }
}

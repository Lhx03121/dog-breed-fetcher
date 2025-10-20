package dogapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final Function<String, String> httpGet;
    public DogApiBreedFetcher() {
        this.httpGet = url -> {
            try {
                java.net.URL u = new java.net.URL(url);
                java.net.URLConnection raw = u.openConnection();
                raw.setConnectTimeout(10_000);
                raw.setReadTimeout(15_000);
                raw.setRequestProperty("User-Agent", "DogBreedFetcher/1.0");
                try (java.io.InputStream is = raw.getInputStream();
                     java.io.InputStreamReader isr =
                             new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
                     java.io.BufferedReader br = new java.io.BufferedReader(isr)) {
                    StringBuilder sb = new StringBuilder();
                    char[] buf = new char[4096];
                    int n;
                    while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
                    return sb.toString();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    public DogApiBreedFetcher(Function<String, String> httpGet) {
        this.httpGet = Objects.requireNonNull(httpGet);
    }

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedNotFoundException {
        if (breed == null || breed.trim().isEmpty()) {
            throw new BreedNotFoundException("Breed is blank");
        }
        String url = "https://dog.ceo/api/breed/" + breed.trim().toLowerCase() + "/list";

        final String body;
        try {
            body = httpGet.apply(url);
        } catch (RuntimeException re) {
            throw new BreedNotFoundException("Failed to fetch sub-breeds for: " + breed, re.getCause());
        }

        java.util.regex.Matcher mStatus = java.util.regex.Pattern
                .compile("\"status\"\\s*:\\s*\"([^\"]*)\"")
                .matcher(body);
        String status = mStatus.find() ? mStatus.group(1) : "";

        if (!"success".equalsIgnoreCase(status)) {
            java.util.regex.Matcher mMsg = java.util.regex.Pattern
                    .compile("\"message\"\\s*:\\s*\"([^\"]*)\"")
                    .matcher(body);
            String msg = mMsg.find() ? mMsg.group(1)
                    : "Breed not found (main breed does not exist)";
            throw new BreedNotFoundException(msg);
        }

        java.util.regex.Matcher mArray = java.util.regex.Pattern
                .compile("\"message\"\\s*:\\s*\\[(.*?)\\]", java.util.regex.Pattern.DOTALL)
                .matcher(body);
        List<String> subs = new ArrayList<>();
        if (mArray.find()) {
            String inside = mArray.group(1); // 例如 "afghan","basset",...
            java.util.regex.Matcher each = java.util.regex.Pattern
                    .compile("\"([^\"]*)\"")
                    .matcher(inside);
            while (each.find()) subs.add(each.group(1));
        }
        java.util.Collections.sort(subs);
        return subs;
    }
}
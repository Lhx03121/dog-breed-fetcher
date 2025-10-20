package dogapi;

import java.util.ArrayList;
import java.util.List;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedNotFoundException {
        // return statement included so that the starter code can compile and run.

        if (breed == null || breed.trim().isEmpty()) {
            throw new BreedNotFoundException("Breed is blank");
        }
        String urlStr = "https://dog.ceo/api/breed/" + breed.trim().toLowerCase() + "/list";

        java.net.HttpURLConnection conn = null;
        java.io.InputStream is = null;
        try {
            java.net.URL url = new java.net.URL(urlStr);
            java.net.URLConnection raw = url.openConnection();
            raw.setConnectTimeout(10_000);
            raw.setReadTimeout(15_000);
            raw.setRequestProperty("User-Agent", "DogBreedFetcher/1.0");

            int code = 200;
            if (raw instanceof java.net.HttpURLConnection) {
                conn = (java.net.HttpURLConnection) raw;
                code = conn.getResponseCode();
                is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            } else {
                is = raw.getInputStream();
            }
            if (is == null) throw new java.io.IOException("No response stream, HTTP " + code);

            // 读取完整响应（UTF-8）
            java.io.InputStreamReader isr =
                    new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8);
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader br = new java.io.BufferedReader(isr)) {
                char[] buf = new char[4096];
                int n;
                while ((n = br.read(buf)) != -1) sb.append(buf, 0, n);
            }
            String body = sb.toString();

            // 解析 status
            java.util.regex.Matcher mStatus = java.util.regex.Pattern
                    .compile("\"status\"\\s*:\\s*\"([^\"]*)\"")
                    .matcher(body);
            String status = mStatus.find() ? mStatus.group(1) : "";

            if (!"success".equalsIgnoreCase(status)) {
                // 包括 "error"：抛 BreedNotFoundException（message 来自返回）
                java.util.regex.Matcher mMsg = java.util.regex.Pattern
                        .compile("\"message\"\\s*:\\s*\"([^\"]*)\"")
                        .matcher(body);
                String msg = mMsg.find() ? mMsg.group(1)
                        : "Breed not found (main breed does not exist)";
                throw new BreedNotFoundException(msg);
            }

            // 成功：message 是字符串数组 → 解析 ["a","b",...]
            java.util.regex.Matcher mArray = java.util.regex.Pattern
                    .compile("\"message\"\\s*:\\s*\\[(.*?)\\]", java.util.regex.Pattern.DOTALL)
                    .matcher(body);
            List<String> subs = new ArrayList<>();
            if (mArray.find()) {
                String inside = mArray.group(1);
                java.util.regex.Matcher each = java.util.regex.Pattern
                        .compile("\"([^\"]*)\"")
                        .matcher(inside);
                while (each.find()) subs.add(each.group(1));
            }

            java.util.Collections.sort(subs);
            return subs;
        } catch (Exception e) {
            throw new BreedNotFoundException("Failed to fetch sub-breeds for: " + breed, e);
        } finally {
            if (is != null) try { is.close(); } catch (Exception ignored) {}
            if (conn != null) conn.disconnect();
        }
    }
}
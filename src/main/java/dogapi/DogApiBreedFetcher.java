package dogapi;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;

/**
 * BreedFetcher implementation that relies on the dog.ceo API.
 * Note that all failures get reported as BreedNotFoundException
 * exceptions to align with the requirements of the BreedFetcher interface.
 */
public class DogApiBreedFetcher implements BreedFetcher {
    private final OkHttpClient client = new OkHttpClient();

    /**
     * Fetch the list of sub breeds for the given breed from the dog.ceo API.
     * @param breed the breed to fetch sub breeds for
     * @return list of sub breeds for the given breed
     * @throws BreedNotFoundException if the breed does not exist (or if the API call fails for any reason)
     */
    @Override
    public List<String> getSubBreeds(String breed) throws BreedNotFoundException {
        if (breed == null || breed.isBlank()) {
            throw new BreedNotFoundException(breed);
        }

        final String url = "https://dog.ceo/api/breed/" + breed.trim().toLowerCase() + "/list";
        Request req = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "DogBreedFetcher/1.0")
                .build();

        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful() || resp.body() == null) {
                throw new BreedNotFoundException(breed);
            }
            String body = resp.body().string();
            JSONObject root = new JSONObject(body);
            String status = root.optString("status", "");
            if (!"success".equalsIgnoreCase(status)) {
                throw new BreedNotFoundException(breed);
            }
            JSONArray arr = root.getJSONArray("message");
            List<String> subs = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                subs.add(arr.getString(i));
            }
            Collections.sort(subs);
            return subs;
        } catch (IOException e) {
            throw new BreedNotFoundException(breed);
            //wwww
        }
    }
}
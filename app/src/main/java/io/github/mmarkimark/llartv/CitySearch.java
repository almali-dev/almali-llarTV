package io.github.mmarkimark.llartv;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The weather city search of the settings: Open-Meteo's geocoding API (free, no key), queried by name in the
 * interface language. Open-Meteo has no list of cities, only this search. Only SettingsActivity calls it, when the
 * owner asks for a search; the chosen city is written to home.conf as location and location_name.
 */
final class CitySearch {
    static final int COUNT = 10;            // the matches offered, the most populated first
    /** A search answer of 10 matches is a few KB; a larger body fails the search. */
    static final int MAX_BYTES = 256 * 1024;

    /** One match: its name, the region and country it is in (empty when Open-Meteo has none) and coordinates. */
    static final class City {
        final String name, region, country;
        final double lat, lon;

        City(String name, String region, String country, double lat, double lon) {
            this.name = name; this.region = region; this.country = country; this.lat = lat; this.lon = lon;
        }

        /** "Girona, Catalunya, Espanya": what tells two places of the same name apart; a repeated part shows once. */
        String label() {
            Set<String> parts = new LinkedHashSet<>();
            for (String p : new String[]{name, region, country}) if (!p.isEmpty()) parts.add(p);
            return String.join(", ", parts);
        }

        /** The home.conf location value, "lat,lon". */
        String location() { return String.format(Locale.ROOT, "%.5f,%.5f", lat, lon); }
    }

    private CitySearch() {}

    /** Open-Meteo answers nothing for a single letter, so a search needs two. */
    static boolean searchable(String name) { return name != null && name.trim().length() >= 2; }

    static String url(String name, String language) {
        String q;
        try { q = URLEncoder.encode(name.trim(), "UTF-8").replace("+", "%20"); }
        catch (java.io.UnsupportedEncodingException e) { throw new IllegalStateException(e); }   // UTF-8 always exists
        return "https://geocoding-api.open-meteo.com/v1/search?name=" + q + "&count=" + COUNT
                + "&language=" + language + "&format=json";
    }

    /** The matches of a search answer, in its order; empty when nothing matched (the answer has no results). */
    static List<City> parse(String json) throws Exception {
        List<City> out = new ArrayList<>();
        JSONArray results = new JSONObject(json).optJSONArray("results");
        if (results == null) return out;
        for (int i = 0; i < results.length(); i++) {
            JSONObject r = results.optJSONObject(i);
            if (r == null || !r.has("latitude") || !r.has("longitude")) continue;
            String name = r.optString("name", "").trim();
            if (name.isEmpty()) continue;
            out.add(new City(name, r.optString("admin1", "").trim(), r.optString("country", "").trim(),
                    r.getDouble("latitude"), r.getDouble("longitude")));
        }
        return out;
    }

    static String read(HttpURLConnection c) throws Exception { return Http.text(c, MAX_BYTES); }

    /** Searches Open-Meteo; throws when the request or the answer fails. Never on the main thread. */
    static List<City> search(String name, String language) throws Exception {
        return parse(read(DailyWallpaper.open(url(name, language))));
    }
}

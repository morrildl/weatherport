/*
 * Copyright (C) 2013 Dan Morrill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dude.morrildl.weatherport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;

import android.util.JsonReader;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

/**
 * Utility class that handles network accesses and JSON parsing. This is quick
 * and dirty, but the JSON is extremel simple, so it's okay.
 */
public class WeatherLoader {
    /**
     * Courier POJO to simply carry named values around.
     */
    public static class WeatherInfo {
        public String dewPoint;

        public String elevation;
        public String humidity;
        public String ICAO;
        public int imageId;
        public LatLng latLng;
        public String name;
        public String pressure;
        public String temperature;
        public String windDirection;
        public String windSpeed;

        public WeatherInfo() {
        }
    }

    // Weather data lives here. Can you dig it?
    public static final String GEONAMES_URL = "http://ws.geonames.org/weatherIcaoJSON?ICAO=";

    private static final HashMap<String, Integer> weatherImageMap;
    static {
        // I found a list of string constants in some GNOME sample code somewhere
        weatherImageMap = new HashMap<String, Integer>();

        weatherImageMap.put("drizzle", R.drawable.drizzle);
        weatherImageMap.put("light showers rain", R.drawable.drizzle);
        weatherImageMap.put("light rain", R.drawable.drizzle);

        weatherImageMap.put("rain", R.drawable.rain);

        weatherImageMap.put("light snow", R.drawable.snow);
        weatherImageMap.put("snow grains", R.drawable.snow);

        weatherImageMap.put("few clouds", R.drawable.partly_cloudy);
        weatherImageMap.put("scattered clouds", R.drawable.partly_cloudy);

        weatherImageMap.put("clear sky", R.drawable.sunny);

        weatherImageMap.put("broken clouds", R.drawable.cloudy);
        weatherImageMap.put("overcast", R.drawable.cloudy);

    }

    /**
     * Fetches weather data for given airport code. Will attempt to fetch
     * arbitrary strings; there might be a code injection issue here, but I
     * don't think so, since we never exec the results, only parse it as a JSON
     * string with (one of) the system JSON parser(s).
     * 
     * Probably won't work on airports outside the US, but has no protection
     * against that.
     * 
     * @param airport
     *            a 4-digit airport code, starting with K (which means US)
     * @return a WeatherInfo instance with data, or null if an error occurred
     */
    public static WeatherInfo fetchDataFor(String airport) {
        try {
            URL url = new URL(GEONAMES_URL + airport);
            JsonReader jsonReader = new JsonReader(new BufferedReader(new InputStreamReader(
                    url.openStream())));

            WeatherInfo info = new WeatherInfo();
            String name, value;
            String clouds = null, conditions = null;
            float lat = 0, lng = 0;

            // response is always "{ weatherObservation": { ... } }"
            jsonReader.beginObject();
            jsonReader.nextName();
            jsonReader.beginObject();

            while (jsonReader.hasNext()) {
                name = jsonReader.nextName();
                value = jsonReader.nextString();

                if ("clouds".equals(name)) {
                    clouds = value;
                } else if ("weatherCondition".equals(name)) {
                    conditions = value;
                } else if ("windDirection".equals(name)) {
                    info.windDirection = value;
                } else if ("windSpeed".equals(name)) {
                    info.windSpeed = value;
                } else if ("elevation".equals(name)) {
                    info.elevation = value;
                } else if ("ICAO".equals(name)) {
                    info.ICAO = value;
                } else if ("stationName".equals(name)) {
                    info.name = value;
                } else if ("seaLevelPressure".equals(name)) {
                    info.pressure = value;
                } else if ("temperature".equals(name)) {
                    info.temperature = value;
                } else if ("dewPoint".equals(name)) {
                    info.dewPoint = value;
                } else if ("humidity".equals(name)) {
                    info.humidity = value;
                } else if ("lat".equals(name)) {
                    lat = Float.parseFloat(value);
                } else if ("lng".equals(name)) {
                    lng = Float.parseFloat(value);
                }
            }
            jsonReader.endObject();
            jsonReader.close();

            // combine composite values (i.e. made of multiple keys in the
            // JSON object) after we are done parsing
            if (lat != 0f && lng != 0f) {
                info.latLng = new LatLng(lat, lng);
            }
            info.imageId = getImageIdFor(clouds, conditions);

            return info;
        } catch (IOException e) {
            // The JSON in question is very simple, so we don't worry too much
            // about errors
            Log.e("WeatherLoader", "error fetching or parsing", e);
            return null;
        }
    }

    /**
     * Given two parameters, attempts to recognize map them to an image, for a
     * little eye candy in the UI. Conditions takes precedence over clouds, if
     * present.
     * 
     * @param clouds
     *            from geonames JSON
     * @param conditions
     *            from geonames JSON
     * @return the resource ID of an image appropriate to the given weather
     *         conditions
     */
    private static int getImageIdFor(String clouds, String conditions) {
        Integer id = weatherImageMap.get(conditions);
        if (id == null) {
            id = weatherImageMap.get(clouds);
            if (id == null) {
                Log.w("WeatherLoader", "unrecognized weather code (" + clouds + ", " + conditions
                        + ")");
                return R.drawable.sunny;
            }
        }
        return id;
    }
}

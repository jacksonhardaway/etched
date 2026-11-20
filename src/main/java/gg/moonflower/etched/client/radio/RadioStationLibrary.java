package gg.moonflower.etched.client.radio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import gg.moonflower.etched.api.sound.download.ShareLinks;
import gg.moonflower.etched.common.radio.RadioStation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RadioStationLibrary {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Comparator<RadioStation> STATION_ORDER = Comparator
            .comparing(RadioStation::name, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(RadioStation::url);

    private final Path file;
    private List<RadioStation> stations = List.of();

    public RadioStationLibrary(Path file) {
        this.file = file;
    }

    public void load() throws IOException {
        if (!Files.exists(this.file)) {
            this.stations = List.of();
            return;
        }

        try {
            JsonElement root = JsonParser.parseString(Files.readString(this.file, StandardCharsets.UTF_8));
            if (!root.isJsonArray()) {
                throw new IOException("Radio station library must contain a JSON array");
            }

            List<RadioStation> loaded = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray()) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                String name = string(object, "name");
                String url = string(object, "url");
                RadioStation station = new RadioStation(name, url);
                if (!station.isEmpty()) {
                    loaded.add(station);
                }
            }
            this.stations = normalize(loaded);
        } catch (JsonParseException exception) {
            throw new IOException("Radio station library contains invalid JSON", exception);
        }
    }

    public List<RadioStation> stations() {
        return this.stations;
    }

    public boolean containsUrl(String url) {
        return this.findByUrl(url).isPresent();
    }

    public Optional<RadioStation> findByUrl(String url) {
        String key = normalizedUrl(url);
        return this.stations.stream().filter(station -> normalizedUrl(station.url()).equals(key)).findFirst();
    }

    public void add(RadioStation station) throws IOException {
        List<RadioStation> updated = new ArrayList<>(this.stations);
        updated.add(station);
        this.replace(updated);
    }

    public void update(String originalUrl, RadioStation updatedStation) throws IOException {
        String originalKey = normalizedUrl(originalUrl);
        String updatedKey = normalizedUrl(updatedStation.url());
        if (!originalKey.equals(updatedKey) && this.containsUrl(updatedStation.url())) {
            throw new FileAlreadyExistsException(updatedStation.url());
        }

        List<RadioStation> updated = new ArrayList<>(this.stations);
        for (int i = 0; i < updated.size(); i++) {
            if (normalizedUrl(updated.get(i).url()).equals(originalKey)) {
                updated.set(i, updatedStation);
                this.replace(updated);
                return;
            }
        }
    }

    public void remove(String url) throws IOException {
        String key = normalizedUrl(url);
        this.replace(this.stations.stream()
                .filter(station -> !normalizedUrl(station.url()).equals(key))
                .toList());
    }

    private void replace(List<RadioStation> updated) throws IOException {
        List<RadioStation> normalized = normalize(updated);
        JsonArray root = new JsonArray();
        for (RadioStation station : normalized) {
            JsonObject object = new JsonObject();
            object.addProperty("name", station.name());
            object.addProperty("url", station.url());
            root.add(object);
        }

        Path parent = this.file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = this.file.resolveSibling(this.file.getFileName() + ".tmp");
        Files.writeString(temporary, GSON.toJson(root), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            Files.move(temporary, this.file, StandardCopyOption.REPLACE_EXISTING);
        }
        this.stations = normalized;
    }

    private static List<RadioStation> normalize(List<RadioStation> stations) {
        Map<String, RadioStation> unique = new LinkedHashMap<>();
        for (RadioStation station : stations) {
            if (station != null && !station.isEmpty()) {
                unique.putIfAbsent(normalizedUrl(station.url()), station);
            }
        }
        return unique.values().stream().sorted(STATION_ORDER).toList();
    }

    private static String normalizedUrl(String url) {
        return ShareLinks.toDirectAudioUrl(url == null ? "" : url.strip());
    }

    private static String string(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }
}

package gg.moonflower.etched.client.sound.download;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.io.IOException;

@FunctionalInterface
public interface SourceRequest<T> {

    T process(JsonObject json) throws IOException, JsonParseException;
}

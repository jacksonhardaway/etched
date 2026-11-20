package gg.moonflower.etched.api.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Mp3InputStreamTest {

    private static final String MONO_MP3 = "SUQzBAAAAAAAIlRTU0UAAAAOAAADTGF2ZjYzLjEuMTAwAAAAAAAAAAAAAAD/+0DAAAAAAAAAAAAAAAAAAAAAAABJbmZvAAAADwAAAAUAAALAAGhoaGhoaGhoaGhoaGhoaGhoaGiOjo6Ojo6Ojo6Ojo6Ojo6Ojo6OjrS0tLS0tLS0tLS0tLS0tLS0tLS02tra2tra2tra2tra2tra2tra2tr//////////////////////////wAAAABMYXZjNjMuMS4AAAAAAAAAAAAAAAAkAwYAAAAAAAACwJoYjnAAAAAAAP/7EMQAAAR4I2FUMIAwngipgzRwAEUAbTukAAd3d3Py3DgYGBualAQRB8P8EOD4PvqBA4XDHT7uXfZh3//oJi9vjoj0dzrLkThG9kh2UjJwhAb4LR4bfjYFTvUDQl+dBpX//8GSiAAE//sSxAODxWQtJh3ngCCiBaKBv2VAwBgAS/IMAPMCgGEwlgmzCYR7MpgU4wVwFQEBiAABVLC+EmYr//+0QOGUvi4JgIAYeJGQF5qsgYtmIZ/GDnGFoCqeawGVZtALWodpv0pqiPqwSE0u//sQxAODxQgtFgBr4sCfBaJBr+lAEYkIZ4ideIYMANMmg1AdhgYABCBgG4eAHUu2lvVEa36af//USVnQKIAZc4BHgi6T/jCFgMg1nQAHFgiQTZhEQDE0E68ILqKb6P//Y0XTT0RnU6T/+xLEBQPFHC0UDX9KAL4Fo8K9gAQJmcDHjnmDZEBRpQQJOYG8AYHFGmaAAIehLWEduk/R//80r8LgFmAqAKhPBQCBgYBKGGcFMYVDABuTDdmBsFUYNgGhgaAXEQBidgKQxCTV/3IZxEX/+xDEAoAFRDtOGZaAAIuG4cOYkAD98ZAgQCmtZWBO2uHjj0ZoWZZrhOxzpGPNyUBERYDhUFfiI970NEoShKMnmSSTUMEQOlmAyfEECJ8BQAgS5ZE1waBrZwV5Y8pMQU1FMy4xMDBVVQ==";

    @Test
    void decodesOnlyTheValidSamplesFromMonoFrames() throws Exception {
        byte[] encoded = Base64.getDecoder().decode(MONO_MP3);

        try (Mp3InputStream input = new Mp3InputStream(new ByteArrayInputStream(encoded))) {
            assertEquals(11_520, input.readAllBytes().length);
        }
    }
}

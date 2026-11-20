package gg.moonflower.etched.api.sound.metadata;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IcyMetadataInputStream extends FilterInputStream {

    private static final Pattern STREAM_TITLE = Pattern.compile("(?:^|;)StreamTitle='(.*?)';", Pattern.DOTALL);

    private final int metadataInterval;
    private final Consumer<String> titleListener;
    private int audioRemaining;
    private boolean ended;
    private String lastTitle = "";

    public IcyMetadataInputStream(InputStream input, int metadataInterval, Consumer<String> titleListener) {
        super(input);
        if (metadataInterval <= 0) {
            throw new IllegalArgumentException("metadataInterval must be positive");
        }
        this.metadataInterval = metadataInterval;
        this.audioRemaining = metadataInterval;
        this.titleListener = titleListener;
    }

    @Override
    public int read() throws IOException {
        if (!this.prepareAudio()) {
            return -1;
        }
        int value = super.read();
        if (value < 0) {
            this.ended = true;
            return -1;
        }
        this.audioRemaining--;
        return value;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) throws IOException {
        Objects.checkFromIndexSize(offset, length, bytes.length);
        if (length == 0) {
            return 0;
        }
        if (!this.prepareAudio()) {
            return -1;
        }
        int read = super.read(bytes, offset, Math.min(length, this.audioRemaining));
        if (read < 0) {
            this.ended = true;
            return -1;
        }
        this.audioRemaining -= read;
        return read;
    }

    private boolean prepareAudio() throws IOException {
        if (this.ended) {
            return false;
        }
        if (this.audioRemaining > 0) {
            return true;
        }

        int lengthByte = super.read();
        if (lengthByte < 0) {
            this.ended = true;
            return false;
        }
        int metadataLength = lengthByte * 16;
        if (metadataLength > 0) {
            byte[] metadata = this.in.readNBytes(metadataLength);
            if (metadata.length < metadataLength) {
                this.ended = true;
                return false;
            }
            this.acceptMetadata(metadata);
        }
        this.audioRemaining = this.metadataInterval;
        return true;
    }

    private void acceptMetadata(byte[] metadata) {
        String value = new String(metadata, StandardCharsets.ISO_8859_1).replace("\0", "");
        Matcher matcher = STREAM_TITLE.matcher(value);
        if (!matcher.find()) {
            return;
        }
        String title = matcher.group(1).strip();
        if (!title.equals(this.lastTitle)) {
            this.lastTitle = title;
            this.titleListener.accept(title);
        }
    }
}

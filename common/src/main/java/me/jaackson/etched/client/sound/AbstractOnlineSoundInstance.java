package me.jaackson.etched.client.sound;

import me.jaackson.etched.Etched;
import me.jaackson.etched.client.sound.download.DownloadProgressListener;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.jetbrains.annotations.Nullable;

/**
 * @author Ocelot
 */
public class AbstractOnlineSoundInstance extends AbstractSoundInstance {

    private final String url;
    private final String subtitle;
    private final int attenuationDistance;
    private final DownloadProgressListener progressListener;

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, SoundSource source, DownloadProgressListener progressListener) {
        this(url, subtitle, 16, source, progressListener);
    }

    public AbstractOnlineSoundInstance(String url, @Nullable String subtitle, int attenuationDistance, SoundSource source, DownloadProgressListener progressListener) {
        super(new ResourceLocation(Etched.MOD_ID, DigestUtils.sha1Hex(url)), source);
        this.url = url;
        this.subtitle = subtitle;
        this.attenuationDistance = attenuationDistance;
        this.progressListener = progressListener;
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        WeighedSoundEvents weighedSoundEvents = new WeighedSoundEvents(this.getLocation(), this.subtitle);
        weighedSoundEvents.addSound(new OnlineSound(this.getLocation(), this.url, this.attenuationDistance, this.progressListener));
        this.sound = weighedSoundEvents.getSound();
        return weighedSoundEvents;
    }

    public static class OnlineSound extends Sound {

        private final String url;
        private final DownloadProgressListener progressListener;

        public OnlineSound(ResourceLocation location, String url, int attenuationDistance, DownloadProgressListener progressListener) {
            super(location.toString(), 1.0F, 1.0F, 1, Type.FILE, true, false, attenuationDistance);
            this.url = url;
            this.progressListener = progressListener;
        }

        public String getURL() {
            return url;
        }

        public DownloadProgressListener getProgressListener() {
            return progressListener;
        }
    }
}
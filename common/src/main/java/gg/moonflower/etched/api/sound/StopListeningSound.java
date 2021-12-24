package gg.moonflower.etched.api.sound;

import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

/**
 * Wrapper for {@link SoundInstance} that respects {@link SoundStopListener}.
 *
 * @author Ocelot
 */
public class StopListeningSound implements SoundInstance, SoundStopListener {

    private final SoundInstance source;
    private final SoundStopListener listener;
    private boolean ignoringEvents;

    public StopListeningSound(SoundInstance source, SoundStopListener listener) {
        this.source = source;
        this.listener = listener;
        this.ignoringEvents = false;
    }

    public void stopListening() {
        this.ignoringEvents = true;
    }

    @Override
    public ResourceLocation getLocation() {
        return this.source.getLocation();
    }

    @Nullable
    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        return this.source.resolve(soundManager);
    }

    @Override
    public Sound getSound() {
        return this.source.getSound();
    }

    @Override
    public SoundSource getSource() {
        return this.source.getSource();
    }

    @Override
    public boolean isLooping() {
        return this.source.isLooping();
    }

    @Override
    public boolean isRelative() {
        return this.source.isRelative();
    }

    @Override
    public int getDelay() {
        return this.source.getDelay();
    }

    @Override
    public float getVolume() {
        return this.source.getVolume();
    }

    @Override
    public float getPitch() {
        return this.source.getPitch();
    }

    @Override
    public double getX() {
        return this.source.getX();
    }

    @Override
    public double getY() {
        return this.source.getY();
    }

    @Override
    public double getZ() {
        return this.source.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return this.source.getAttenuation();
    }

    @Override
    public void onStop() {
        if (!this.ignoringEvents)
            this.listener.onStop();
    }
}

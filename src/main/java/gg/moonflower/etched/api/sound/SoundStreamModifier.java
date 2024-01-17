package gg.moonflower.etched.api.sound;

import net.minecraft.client.sounds.AudioStream;

public interface SoundStreamModifier {
    
    AudioStream modifyStream(AudioStream stream);
}

package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.item.BoomboxItem;
import gg.moonflower.etched.core.hook.extension.VillagerExtension;
import gg.moonflower.etched.core.registry.EtchedTags;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements VillagerExtension {

    @Unique
    private boolean dancing;

    @Unique
    private BlockPos musicPos;

    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public void aiStep() {
        if (this.level.isClientSide()) {
            List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().inflate(3.45), entity -> {
                if (!entity.isAlive() || entity.isSpectator())
                    return false;
                if (entity == Minecraft.getInstance().player && BoomboxItem.getPlayingHand((LivingEntity) entity) == null)
                    return false;

                return SoundTracker.getEntitySound(entity.getId()) != null;
            });

            if (!entities.isEmpty()) {
                this.dancing = true;
            } else if (this.musicPos == null || !this.musicPos.closerThan(this.position(), 3.46) || !this.level.getBlockState(this.musicPos).is(Blocks.JUKEBOX) && !this.level.getBlockState(this.musicPos).is(EtchedTags.AUDIO_PROVIDER)) {
                this.dancing = false;
                this.musicPos = null;
            }

            if (this.dancing) {
                if (this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
                    this.ambientSoundTime = -this.getAmbientSoundInterval();
                    if (this.random.nextBoolean()) {
                        this.level.addParticle(ParticleTypes.NOTE, this.getRandomX(1.0), this.getY(1.0F) + this.random.nextDouble() / 2, this.getRandomZ(1.0), this.random.nextInt(25) / 24F, 0.0, 0.0);
                        if (this.random.nextDouble() > 0.75) {
                            this.level.playSound(Minecraft.getInstance().player, this.blockPosition(), SoundEvents.VILLAGER_YES, this.getSoundSource(), this.getSoundVolume(), this.getVoicePitch());
                        }
                    }
                }
            }
        }

        super.aiStep();
    }

    @Override
    public void setRecordPlayingNearby(BlockPos pos, boolean isPartying) {
        this.musicPos = pos;
        this.dancing = isPartying;
    }

    @Override
    public boolean isDancing() {
        return dancing;
    }
}
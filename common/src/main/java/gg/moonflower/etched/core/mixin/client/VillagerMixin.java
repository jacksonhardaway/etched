package gg.moonflower.etched.core.mixin.client;

import gg.moonflower.etched.api.sound.SoundTracker;
import gg.moonflower.etched.common.item.BoomboxItem;
import gg.moonflower.etched.core.hook.extension.VillagerExtension;
import gg.moonflower.etched.core.registry.EtchedTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
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
            List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(this.position().x() - 3.46, this.position().y() - 3.46, this.position().z() - 3.46, this.position().x() + 3.46, this.position().y() + 3.46, this.position().z() + 3.46), entity -> {
                if (entity instanceof LivingEntity) {
                    if (BoomboxItem.getPlayingHand((LivingEntity) entity) != null) {
                        return true;
                    }

                    if (entity instanceof Player)
                        return false;
                }

                return SoundTracker.getEntitySound(entity.getId()) != null;
            });

            if (!entities.isEmpty()) {
                this.dancing = true;
            } else if (this.musicPos == null || !this.musicPos.closerThan(this.position(), 3.46) || !this.level.getBlockState(this.musicPos).is(Blocks.JUKEBOX) && !this.level.getBlockState(this.musicPos).is(EtchedTags.AUDIO_PROVIDER)) {
                this.dancing = false;
                this.musicPos = null;
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

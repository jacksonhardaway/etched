package me.jaackson.etched.common.block.entity;

import me.jaackson.etched.EtchedRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EtcherBlockEntity extends BlockEntity {
    public EtcherBlockEntity() {
        super(EtchedRegistry.ETCHER_BLOCK_ENTITY.get());
    }
}

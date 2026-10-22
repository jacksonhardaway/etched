package gg.moonflower.etched.core.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;

/**
 * Plays the radio stream on clients while the radio is moving with a contraption.
 */
public class RadioMovementBehaviour implements MovementBehaviour {

    @Override
    public boolean mustTickWhileDisabled() {
        return true; // The stream has to stop when contraption controls turn radios off
    }

    @Override
    public void tick(MovementContext context) {
        if (context.world.isClientSide()) {
            CreateCompatClient.tickRadio(context);
        }
    }
}

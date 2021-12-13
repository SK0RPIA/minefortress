package org.minefortress.mixins.interaction;

import com.chocohead.mm.api.ClassTinkerers;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMode.class)
public class FortressGameModeMixin {

    @Inject(method = "setAbilities", at = @At("TAIL"))
    public void setAbilities(PlayerAbilities abilities, CallbackInfo ci) {
        GameMode currentGameMode = (GameMode) (Object) this;

        if(currentGameMode == ClassTinkerers.getEnum(GameMode.class, "FORTRESS")) {
            abilities.allowFlying = true;
            abilities.creativeMode = true;
            abilities.invulnerable = true;
        }
    }

}

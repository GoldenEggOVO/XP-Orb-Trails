package dev.goldeneggovo.xporbtrails.mixin;

import dev.goldeneggovo.xporbtrails.TrailRenderer;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void xporbtrails$addTrail(CallbackInfo ci) {
        TrailRenderer.track((ExperienceOrb) (Object) this);
    }
}

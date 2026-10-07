package dev.goldeneggovo.xporbtrails.mixin;

import dev.goldeneggovo.xporbtrails.TrailRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "close", at = @At("RETURN"))
    private void xporbtrails$close(CallbackInfo ci) {
        TrailRenderer.close();
    }
}

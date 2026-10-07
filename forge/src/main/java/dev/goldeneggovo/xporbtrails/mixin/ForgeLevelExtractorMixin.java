package dev.goldeneggovo.xporbtrails.mixin;

import dev.goldeneggovo.xporbtrails.TrailRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public abstract class ForgeLevelExtractorMixin {
    @Inject(method = "extract", at = @At("RETURN"))
    private void xporbtrails$extract(DeltaTracker deltaTracker, Camera camera, float partialTick, CallbackInfo ci) {
        TrailRenderer.extract(Minecraft.getInstance().level, camera.position(),
                deltaTracker.getGameTimeDeltaPartialTick(true));
    }
}

package dev.goldeneggovo.xporbtrails.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.goldeneggovo.xporbtrails.TrailRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void xporbtrails$render(CallbackInfo ci) {
        var camera = levelRenderState.cameraRenderState;
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix().mul(camera.viewRotationMatrix);
        try {
            TrailRenderer.render(camera.pos);
        } finally {
            modelView.popMatrix();
        }
    }
}

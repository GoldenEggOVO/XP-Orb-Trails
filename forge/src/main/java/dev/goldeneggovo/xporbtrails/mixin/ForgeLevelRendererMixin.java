package dev.goldeneggovo.xporbtrails.mixin;

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
public abstract class ForgeLevelRendererMixin {
    @Shadow @Final private LevelRenderState levelRenderState;

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lorg/joml/Matrix4fStack;popMatrix()Lorg/joml/Matrix4fStack;"))
    private void xporbtrails$render(CallbackInfo ci) {
        TrailRenderer.render(levelRenderState.cameraRenderState.pos);
    }
}

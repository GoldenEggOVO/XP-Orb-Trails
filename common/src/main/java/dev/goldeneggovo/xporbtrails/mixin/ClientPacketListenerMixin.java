package dev.goldeneggovo.xporbtrails.mixin;

import dev.goldeneggovo.xporbtrails.TrailRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.entity.ExperienceOrb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow private ClientLevel level;

    // Vanilla has moved packet handling onto the client thread before reaching this point.
    @Inject(method = "handleTakeItemEntity", at = @At("TAIL"))
    private void xporbtrails$confirmPickup(ClientboundTakeItemEntityPacket packet, CallbackInfo ci) {
        if (level.getEntity(packet.getItemId()) instanceof ExperienceOrb orb) TrailRenderer.confirmPickup(orb);
    }
}

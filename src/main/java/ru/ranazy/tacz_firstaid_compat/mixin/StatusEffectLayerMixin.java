package ru.ranazy.tacz_firstaid_compat.mixin;

import ichttt.mods.firstaid.client.StatusEffectLayer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.blaze3d.vertex.PoseStack;

@Mixin(value = StatusEffectLayer.class, remap = false)
public class StatusEffectLayerMixin {

    @Redirect(
        method = "render", 
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"
        ),
        remap = false
    )
    private void firstaid$scaleUnconsciousText(GuiGraphics guiGraphics, Font font, Component text, int x, int y, int color) {
        int textWidth = font.width(text);
        int screenWidth = guiGraphics.guiWidth();
        int maxWidth = screenWidth - 20;

        if (textWidth > maxWidth) {
            float scale = (float) maxWidth / textWidth;
            PoseStack poseStack = guiGraphics.pose();
            poseStack.pushPose();
            poseStack.translate(x, y, 0);
            poseStack.scale(scale, scale, 1.0F);
            // Draw centered at scaled 0,0 since we translated to x, y
            guiGraphics.drawCenteredString(font, text, 0, 0, color);
            poseStack.popPose();
        } else {
            guiGraphics.drawCenteredString(font, text, x, y, color);
        }
    }
}

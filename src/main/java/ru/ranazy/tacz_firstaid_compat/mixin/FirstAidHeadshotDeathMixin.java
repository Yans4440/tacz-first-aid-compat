package ru.ranazy.tacz_firstaid_compat.mixin;

import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.common.damagesystem.PlayerDamageModel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PlayerDamageModel.class, remap = false)
public abstract class FirstAidHeadshotDeathMixin extends AbstractPlayerDamageModel {

    public FirstAidHeadshotDeathMixin(AbstractDamageablePart head, AbstractDamageablePart leftArm, AbstractDamageablePart leftLeg, AbstractDamageablePart leftFoot, AbstractDamageablePart body, AbstractDamageablePart rightArm, AbstractDamageablePart rightLeg, AbstractDamageablePart rightFoot) {
        super(head, leftArm, leftLeg, leftFoot, body, rightArm, rightLeg, rightFoot);
    }

    @Inject(method = "isDead", at = @At("HEAD"), cancellable = true)
    private void firstaid$dieOnHeadshot(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (ru.ranazy.tacz_firstaid_compat.CompatConfig.SERVER.instantDeathOnHeadshot.get()) {
            if (this.HEAD.currentHealth <= 0.0f) {
                cir.setReturnValue(true);
            }
        }
    }
}

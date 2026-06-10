package ru.ranazy.tacz_firstaid_compat.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.distribution.IDamageDistributionAlgorithm;
import ichttt.mods.firstaid.common.EventHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.ranazy.tacz_firstaid_compat.compat.firstaid.TacZDamageDistribution;
import ru.ranazy.tacz_firstaid_compat.mixininterface.EntityKineticBulletExtension;

/**
 * Intercepts FirstAid's damage handling to provide precise bodypart targeting for TacZ weapons.
 */
@Mixin(value = EventHandler.class, remap = false)
public class FirstAidEventHandlerMixin {

    @Unique
    private static final ThreadLocal<IDamageDistributionAlgorithm> tacZ_firstaid$customDistribution = new ThreadLocal<>();

    /**
     * Intercepts damage events to detect TacZ bullets.
     * Sets up custom distribution algorithms before FirstAid processes the damage.
     */
    @Inject(
        method = "handleCustomPlayerDamage",
        at = @At(
            value = "INVOKE",
            target = "Lichttt/mods/firstaid/common/util/CommonUtils;getDamageModel(Lnet/minecraft/world/entity/player/Player;)Lichttt/mods/firstaid/api/damagesystem/AbstractPlayerDamageModel;",
            shift = At.Shift.AFTER
        )
    )
    private static void tacZ_firstaid$detectTacZDamage(Player player, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity directEntity = source.getDirectEntity();

        if (directEntity instanceof EntityKineticBullet) {
            Vec3 hitLocation = ((EntityKineticBulletExtension) directEntity).tacZ_firstaid$getLastHitLocation();

            if (hitLocation != null) {
                tacZ_firstaid$customDistribution.set(new TacZDamageDistribution(hitLocation));
                return;
            }
        }

        tacZ_firstaid$customDistribution.remove();
    }

    /**
     * Wraps the handleDamageTaken call to use our custom distribution if available.
     * WrapOperation is more robust than Redirect for cross-mod compatibility.
     */
    @WrapOperation(
        method = "lambda$handleCustomPlayerDamage$2",
        at = @At(
            value = "INVOKE",
            target = "Lichttt/mods/firstaid/common/damagesystem/distribution/DamageDistribution;handleDamageTaken(Lichttt/mods/firstaid/api/distribution/IDamageDistributionAlgorithm;Lichttt/mods/firstaid/api/damagesystem/AbstractPlayerDamageModel;FLnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/damagesource/DamageSource;ZZ)F"
        )
    )
    private static float tacZ_firstaid$replaceDistribution(
            IDamageDistributionAlgorithm originalAlgorithm,
            AbstractPlayerDamageModel damageModel,
            float damage,
            Player player,
            DamageSource source,
            boolean addStat,
            boolean redistributeIfLeft,
            Operation<Float> original
    ) {
        try {
            IDamageDistributionAlgorithm custom = tacZ_firstaid$customDistribution.get();
            
            if (custom != null) {
                // Call original logic but with our custom algorithm replaced as the first argument
                return original.call(custom, damageModel, damage, player, source, addStat, redistributeIfLeft);
            }

            // Fallback to original FirstAid distribution
            return original.call(originalAlgorithm, damageModel, damage, player, source, addStat, redistributeIfLeft);
        } finally {
            tacZ_firstaid$customDistribution.remove();
        }
    }
}

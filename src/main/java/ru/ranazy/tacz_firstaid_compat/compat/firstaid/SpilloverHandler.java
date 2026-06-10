package ru.ranazy.tacz_firstaid_compat.compat.firstaid;

import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.enums.EnumPlayerPart;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import ichttt.mods.firstaid.common.util.CommonUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles damage spillover from limbs to torso when limb health is depleted.
 * Calls FirstAid's bodyPart.damage() directly, which bypasses vanilla i-frames.
 */
public class SpilloverHandler {

    /**
     * Applies spillover damage from a limb to the torso.
     * @param player The player receiving spillover damage
     * @param sourcePart The bodypart that was originally hit
     * @param remainingDamage The damage that exceeded the limb's health
     * @param isExplosion Whether the damage came from an explosion
     */
    public static void applySpillover(
        Player player,
        EnumPlayerPart sourcePart,
        float remainingDamage,
        boolean isExplosion
    ) {
        // Skip spillover for critical parts (they handle death differently)
        if (sourcePart == EnumPlayerPart.HEAD || sourcePart == EnumPlayerPart.BODY) {
            return;
        }

        AbstractPlayerDamageModel damageModel = CommonUtils.getDamageModel(player);
        if (damageModel == null) {
            return;
        }

        // Special handling for feet: damage spills to leg first, then to body
        if (sourcePart == EnumPlayerPart.LEFT_FOOT || sourcePart == EnumPlayerPart.RIGHT_FOOT) {
            handleFootSpillover(player, damageModel, sourcePart, remainingDamage);
            return;
        }

        // Standard spillover for arms and legs: goes to body
        // Bullets: 0.8 (20% energy loss in flesh), Explosions: 1.0 (shockwave transfers fully)
        float ratio = isExplosion ? 1.0f : 0.8f;
        float spilloverDamage = remainingDamage * ratio;

        if (spilloverDamage <= 0) {
            return;
        }

        AbstractDamageablePart bodyPart = damageModel.getFromEnum(EnumPlayerPart.BODY);
        bodyPart.damage(spilloverDamage, player, true);
        syncPart(player, damageModel, EnumPlayerPart.BODY);
    }

    /**
     * Handles spillover from feet specifically: 80% to leg, if leg is destroyed then 40% to body.
     */
    private static void handleFootSpillover(
        Player player,
        AbstractPlayerDamageModel damageModel,
        EnumPlayerPart footPart,
        float remainingDamage
    ) {
        EnumPlayerPart legPart = (footPart == EnumPlayerPart.LEFT_FOOT)
            ? EnumPlayerPart.LEFT_LEG
            : EnumPlayerPart.RIGHT_LEG;

        AbstractDamageablePart leg = damageModel.getFromEnum(legPart);
        float legSpillover = remainingDamage * 0.8f;

        if (leg.currentHealth > 0) {
            float legUndelivered = leg.damage(legSpillover, player, true);
            syncPart(player, damageModel, legPart);

            if (legUndelivered > 0) {
                applyToBody(player, damageModel, legUndelivered);
            }
        } else {
            // Leg is destroyed, 40% goes to body instead (50% energy loss)
            float bodySpillover = remainingDamage * 0.4f;
            applyToBody(player, damageModel, bodySpillover);
        }
    }

    /**
     * Helper to apply damage to body and sync.
     */
    private static void applyToBody(Player player, AbstractPlayerDamageModel damageModel, float damage) {
        if (damage <= 0) {
            return;
        }
        AbstractDamageablePart body = damageModel.getFromEnum(EnumPlayerPart.BODY);
        body.damage(damage, player, true);
        syncPart(player, damageModel, EnumPlayerPart.BODY);
    }

    /**
     * Syncs a specific part to the client.
     */
    private static void syncPart(Player player, AbstractPlayerDamageModel damageModel, EnumPlayerPart part) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new MessageSyncDamageModel(serverPlayer.getId(), damageModel, false));
        }
    }
}

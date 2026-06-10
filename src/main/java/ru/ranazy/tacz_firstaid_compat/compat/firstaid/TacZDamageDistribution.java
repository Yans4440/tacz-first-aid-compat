package ru.ranazy.tacz_firstaid_compat.compat.firstaid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.FirstAidConfig;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.distribution.IDamageDistributionAlgorithm;
import ichttt.mods.firstaid.api.enums.EnumPlayerPart;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import ichttt.mods.firstaid.common.util.ArmorUtils;
import ichttt.mods.firstaid.common.util.CommonUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Custom damage distribution algorithm for TacZ bullets.
 * Uses precise 3D hitbox detection instead of FirstAid's default random/height-based distribution.
 */
public class TacZDamageDistribution implements IDamageDistributionAlgorithm {

    public static final MapCodec<TacZDamageDistribution> CODEC = MapCodec.unit(() -> new TacZDamageDistribution(Vec3.ZERO));

    private final Vec3 hitLocation;

    public TacZDamageDistribution(Vec3 hitLocation) {
        this.hitLocation = hitLocation;
    }

    @Override
    public float distributeDamage(
        float damage,
        Player player,
        DamageSource source,
        boolean addStat
    ) {
        AbstractPlayerDamageModel damageModel = CommonUtils.getDamageModel(player);
        if (damageModel == null) {
            return damage;
        }

        float currentDamage = damage;

        // Convert world hit to local coordinates
        Vec3 localHit = CoordinateTransform.worldToLocal(hitLocation, player);

        // Determine which bodypart was hit, fallback to closest part
        EnumPlayerPart hitPart = BodypartHitbox.getHitPart(localHit);
        if (hitPart == null) {
            hitPart = BodypartHitbox.getClosestPart(localHit);
        }

        // Determine armor slot based on custom mapping
        EquipmentSlot armorSlot = getMappedSlot(hitPart);

        // Apply armor absorption
        currentDamage = ArmorUtils.applyArmor(player, player.getItemBySlot(armorSlot), source, currentDamage, armorSlot);
        if (currentDamage <= 0F) {
            return 0F;
        }

        // Apply enchantment modifiers
        currentDamage = ArmorUtils.applyEnchantmentModifiers(player, armorSlot, source, currentDamage);
        if (currentDamage <= 0F) {
            return 0F;
        }

        // Apply damage to the specific part
        AbstractDamageablePart part = damageModel.getFromEnum(hitPart);

        // FirstAid often leaves 1HP if configuration allows
        float minHealth = (part.canCauseDeath && FirstAidConfig.SERVER.useFriendlyRandomDistribution.get()) ? 1.0f : 0f;

        float undelivered = part.damage(currentDamage, player, addStat, minHealth);

        // Sync to client
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new MessageSyncDamageModel(serverPlayer.getId(), damageModel, false));
        }

        if (undelivered > 0 && hitPart != EnumPlayerPart.BODY && hitPart != EnumPlayerPart.HEAD) {
            // Apply spillover to body
            SpilloverHandler.applySpillover(player, hitPart, undelivered, false);
        }

        // We return 0 as we've already handled the "excess" via spillover logic
        return 0f;
    }

    /**
     * Maps a body part to an equipment slot for armor protection calculation,
     * following the user's specific protection requirements.
     */
    private EquipmentSlot getMappedSlot(EnumPlayerPart part) {
        switch (part) {
            case HEAD:
                return EquipmentSlot.HEAD;
            case BODY:
            case LEFT_ARM:
            case RIGHT_ARM:
                return EquipmentSlot.CHEST;
            case LEFT_LEG:
            case RIGHT_LEG:
                return EquipmentSlot.LEGS;
            case LEFT_FOOT:
            case RIGHT_FOOT:
                return EquipmentSlot.FEET;
            default:
                return EquipmentSlot.CHEST;
        }
    }

    @Override
    public MapCodec<? extends IDamageDistributionAlgorithm> codec() {
        return CODEC;
    }
}

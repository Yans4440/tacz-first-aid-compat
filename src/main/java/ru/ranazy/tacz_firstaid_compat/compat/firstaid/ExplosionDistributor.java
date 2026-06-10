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

import java.util.*;

/**
 * Distributes explosion damage based on proximity of bodyparts to the blast center.
 * Uses distance-based falloff for realistic explosion damage.
 */
public class ExplosionDistributor implements IDamageDistributionAlgorithm {

    public static final MapCodec<ExplosionDistributor> CODEC = MapCodec.unit(() -> new ExplosionDistributor(Vec3.ZERO));

    private final Vec3 explosionCenter;

    public ExplosionDistributor(Vec3 explosionCenter) {
        this.explosionCenter = explosionCenter;
    }

    @Override
    public float distributeDamage(
        float totalDamage,
        Player player,
        DamageSource source,
        boolean addStat
    ) {
        AbstractPlayerDamageModel damageModel = CommonUtils.getDamageModel(player);
        if (damageModel == null) {
            return totalDamage;
        }

        // Calculate distances from explosion center to all bodypart centers
        Map<EnumPlayerPart, Double> distances = new HashMap<>();
        for (EnumPlayerPart part : EnumPlayerPart.values()) {
            Vec3 partWorldPos = CoordinateTransform.getPartWorldPosition(player, part);
            distances.put(part, partWorldPos.distanceToSqr(explosionCenter));
        }

        // Sort parts by proximity (closest first)
        List<Map.Entry<EnumPlayerPart, Double>> sortedParts = new ArrayList<>(distances.entrySet());
        sortedParts.sort(Map.Entry.comparingByValue());

        // Distribute damage with falloff
        // Closest: 50%, Second: 25%, Third: 15%, Rest: 10% split
        if (!sortedParts.isEmpty()) {
            applyExplosionDamage(damageModel, player, source, sortedParts.get(0).getKey(), totalDamage * 0.50f, addStat);
        }
        if (sortedParts.size() >= 2) {
            applyExplosionDamage(damageModel, player, source, sortedParts.get(1).getKey(), totalDamage * 0.25f, addStat);
        }
        if (sortedParts.size() >= 3) {
            applyExplosionDamage(damageModel, player, source, sortedParts.get(2).getKey(), totalDamage * 0.15f, addStat);
        }
        // Distribute remaining 10% among other parts
        if (sortedParts.size() > 3) {
            float remainingDamage = totalDamage * 0.10f;
            float perPart = remainingDamage / (sortedParts.size() - 3);
            for (int i = 3; i < sortedParts.size(); i++) {
                applyExplosionDamage(damageModel, player, source, sortedParts.get(i).getKey(), perPart, addStat);
            }
        }

        // Return 0 because we handled all damage components with our falloff distribution
        return 0f;
    }

    /**
     * Applies explosion damage to a specific part with armor absorption and spillover support.
     */
    private void applyExplosionDamage(
        AbstractPlayerDamageModel damageModel,
        Player player,
        DamageSource source,
        EnumPlayerPart partEnum,
        float damage,
        boolean addStat
    ) {
        float currentDamage = damage;
        EquipmentSlot armorSlot = getMappedSlot(partEnum);

        // Apply armor absorption
        currentDamage = ArmorUtils.applyArmor(player, player.getItemBySlot(armorSlot), source, currentDamage, armorSlot);
        if (currentDamage <= 0F) {
            return;
        }

        // Apply enchantment modifiers
        currentDamage = ArmorUtils.applyEnchantmentModifiers(player, armorSlot, source, currentDamage);
        if (currentDamage <= 0F) {
            return;
        }

        AbstractDamageablePart part = damageModel.getFromEnum(partEnum);
        float minHealth = (part.canCauseDeath && FirstAidConfig.SERVER.useFriendlyRandomDistribution.get()) ? 1.0f : 0f;

        float undelivered = part.damage(currentDamage, player, addStat, minHealth);

        // Sync to client
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new MessageSyncDamageModel(serverPlayer.getId(), damageModel, false));
        }

        if (undelivered > 0 && partEnum != EnumPlayerPart.BODY && partEnum != EnumPlayerPart.HEAD) {
            // Apply spillover to body (1.0 ratio for explosions)
            SpilloverHandler.applySpillover(player, partEnum, undelivered, true);
        }
    }

    /**
     * Maps a body part to an equipment slot for armor protection calculation.
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

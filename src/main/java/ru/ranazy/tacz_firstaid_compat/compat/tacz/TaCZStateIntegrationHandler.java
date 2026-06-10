package ru.ranazy.tacz_firstaid_compat.compat.tacz;

import com.tacz.guns.api.event.common.GunFireSelectEvent;
import com.tacz.guns.api.event.common.GunMeleeEvent;
import com.tacz.guns.api.event.common.GunReloadEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.entity.EntityKineticBullet;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.common.util.CommonUtils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import ru.ranazy.tacz_firstaid_compat.TaCZFirstAidCompat;

import java.util.Random;

@EventBusSubscriber(modid = TaCZFirstAidCompat.MOD_ID)
public class TaCZStateIntegrationHandler {

    private static final Random RANDOM = new Random();

    private static boolean isPlayerUnconscious(Player player) {
        if (player == null) return false;
        AbstractPlayerDamageModel damageModel = CommonUtils.getDamageModel(player);
        if (damageModel != null) {
            return damageModel.getUnconsciousTicks() > 0;
        }
        return false;
    }

    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        if (event.getShooter() instanceof Player player && isPlayerUnconscious(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGunMelee(GunMeleeEvent event) {
        if (event.getShooter() instanceof Player player && isPlayerUnconscious(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGunReload(GunReloadEvent event) {
        if (event.getEntity() instanceof Player player && isPlayerUnconscious(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onGunFireSelect(GunFireSelectEvent event) {
        if (event.getShooter() instanceof Player player && isPlayerUnconscious(player)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBulletJoinWorld(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!entity.level().isClientSide() && entity instanceof EntityKineticBullet bullet) {
            if (bullet.getOwner() instanceof Player player) {
                AbstractPlayerDamageModel damageModel = CommonUtils.getDamageModel(player);
                if (damageModel != null) {
                    int painLevel = damageModel.getPainLevel();
                    // Pain level starts from 1 to 5+. 
                    // Add inaccuracy if pain level >= 3 (medium/high pain)
                    if (painLevel >= 3) {
                        applyPainInaccuracy(bullet, painLevel);
                    }
                }
            }
        }
    }

    private static void applyPainInaccuracy(EntityKineticBullet bullet, int painLevel) {
        // Base spread calculation
        float spreadFactor = (painLevel - 2) * 0.05f; // Level 3 -> 0.05, Level 5 -> 0.15
        
        Vec3 currentMovement = bullet.getDeltaMovement();
        double speed = currentMovement.length();
        
        if (speed > 0) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * spreadFactor * speed;
            double offsetY = (RANDOM.nextDouble() - 0.5) * spreadFactor * speed;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * spreadFactor * speed;
            
            bullet.setDeltaMovement(currentMovement.add(offsetX, offsetY, offsetZ));
        }
    }
}

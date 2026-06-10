package ru.ranazy.tacz_firstaid_compat.compat.firstaid;

import com.tacz.guns.api.entity.IGunOperator;
import ichttt.mods.firstaid.api.enums.EnumPlayerPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Utility for converting world-space hit vectors to player-local coordinate space.
 * Handles player rotation and posture (standing/crawling).
 */
public class CoordinateTransform {

    /**
     * Converts a world-space hit position to player-local coordinates.
     * @param worldHit The hit position in world coordinates
     * @param player The player being hit
     * @return The hit position in local (player-relative) coordinates
     */
    public static Vec3 worldToLocal(Vec3 worldHit, Player player) {
        // Step 1: Translate to player-relative coordinates
        Vec3 relative = worldHit.subtract(player.position());

        // Step 2: Rotate by inverse player yaw to align with local axes
        float yaw = -player.getYRot() * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);

        double localX = relative.x * cos - relative.z * sin;
        double localZ = relative.x * sin + relative.z * cos;

        // Step 3: Apply crawl rotation if player is prone
        if (isCrawling(player)) {
            return rotateCrawl(new Vec3(localX, relative.y, localZ));
        } else {
            return new Vec3(localX, relative.y, localZ);
        }
    }

    /**
     * Checks if the player is in a crawling/prone state.
     * @param player The player to check
     * @return True if crawling
     */
    private static boolean isCrawling(Player player) {
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        if (operator == null || operator.getDataHolder() == null) {
            return false;
        }
        return operator.getDataHolder().isCrawling;
    }

    /**
     * Rotates the local coordinates 90 degrees around the X-axis for crawling players.
     * This transforms "up" (Y) into "forward" (Z) and vice versa.
     * @param local The local coordinates for a standing player
     * @return The adjusted local coordinates for a crawling player
     */
    private static Vec3 rotateCrawl(Vec3 local) {
        // Rotate 90 degrees around X-axis: Y becomes -Z, Z becomes Y
        return new Vec3(local.x, local.z, -local.y);
    }

    /**
     * Gets the center position of a bodypart in world coordinates.
     * @param player The player
     * @param part The bodypart
     * @return World position of the bodypart center
     */
    public static Vec3 getPartWorldPosition(Player player, EnumPlayerPart part) {
        Vec3 localCenter = BodypartHitbox.getPartCenter(part);
        return localToWorld(localCenter, player);
    }

    /**
     * Converts local coordinates back to world space.
     * @param local Local coordinates
     * @param player The player
     * @return World coordinates
     */
    public static Vec3 localToWorld(Vec3 local, Player player) {
        // Reverse crawl rotation if needed
        Vec3 adjusted;
        if (isCrawling(player)) {
            adjusted = new Vec3(local.x, -local.z, local.y);
        } else {
            adjusted = local;
        }

        // Rotate by player yaw
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);

        double worldX = adjusted.x * cos - adjusted.z * sin;
        double worldZ = adjusted.x * sin + adjusted.z * cos;

        // Translate to world position
        return player.position().add(worldX, adjusted.y, worldZ);
    }
}

package ru.ranazy.tacz_firstaid_compat.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.TacHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.ranazy.tacz_firstaid_compat.mixininterface.EntityKineticBulletExtension;

/**
 * Captures the exact hit location when a TacZ bullet hits an entity.
 * This data is used by FirstAid integration for precise bodypart targeting.
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public class EntityKineticBulletHitCaptureMixin implements EntityKineticBulletExtension {

    @Unique
    private Vec3 tacZ_firstaid$lastHitLocation = null;

    @Inject(
        method = "onHitEntity",
        at = @At("HEAD")
    )
    private void tacZ_firstaid$captureHitLocation(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        // Store the exact hit location in the bullet for FirstAid to use
        Vec3 hitLocation = result.getLocation();
        tacZ_firstaid$lastHitLocation = hitLocation;
    }

    @Override
    public Vec3 tacZ_firstaid$getLastHitLocation() {
        return tacZ_firstaid$lastHitLocation;
    }

    @Override
    public void tacZ_firstaid$setLastHitLocation(Vec3 location) {
        tacZ_firstaid$lastHitLocation = location;
    }
}

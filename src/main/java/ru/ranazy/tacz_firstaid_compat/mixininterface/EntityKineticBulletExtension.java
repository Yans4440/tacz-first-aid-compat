package ru.ranazy.tacz_firstaid_compat.mixininterface;

import net.minecraft.world.phys.Vec3;

/**
 * Interface to extend EntityKineticBullet with hit location tracking for FirstAid compatibility.
 */
public interface EntityKineticBulletExtension {
    
    Vec3 tacZ_firstaid$getLastHitLocation();
    
    void tacZ_firstaid$setLastHitLocation(Vec3 location);
}

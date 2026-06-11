package ru.ranazy.tacz_firstaid_compat;

import net.neoforged.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for [TaCZ] First Aid Compat.
 * This mod provides precise 3D hitbox-based damage distribution when using TacZ with First Aid.
 */
@Mod("tacz_firstaid_compat")
public class TaCZFirstAidCompat {
    
    public static final String MOD_ID = "tacz_firstaid_compat";
    public static final Logger LOGGER = LogManager.getLogger();

    public TaCZFirstAidCompat(net.neoforged.fml.ModContainer modContainer) {
        LOGGER.info("[TaCZ] First Aid Compat initialized - providing precise bodypart damage distribution");
        
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, CompatConfig.SERVER_SPEC);
    }
}

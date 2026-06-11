package ru.ranazy.tacz_firstaid_compat;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CompatConfig {

    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class Server {
        public final ModConfigSpec.BooleanValue instantDeathOnHeadshot;

        public Server(ModConfigSpec.Builder builder) {
            builder.push("general");
            
            instantDeathOnHeadshot = builder
                    .comment("If true, the player will die instantly when their head health reaches 0, bypassing the First Aid unconscious state.")
                    .define("instantDeathOnHeadshot", true);
                    
            builder.pop();
        }
    }
}

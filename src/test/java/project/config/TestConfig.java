package project.config;

import org.aeonbits.owner.ConfigCache;

public class TestConfig {
    private static final EnvConfig CONF = ConfigCache.getOrCreate(EnvConfig.class);

    public static EnvConfig getConfig() {
        return CONF;
    }
}

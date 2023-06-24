package project.config;

import org.aeonbits.owner.Config;

@Config.LoadPolicy(Config.LoadType.MERGE)
@Config.Sources({
        "system:properties",
        "system:env",
        "classpath:config/dev.properties"
})
public interface EnvConfig extends Config {
    @Key("ui.url")
    String uiUrl();
}

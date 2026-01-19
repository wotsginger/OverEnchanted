package club.sitmc.overEnchanted;

import org.bukkit.plugin.java.JavaPlugin;

public final class OverEnchanted extends JavaPlugin {

    public static OverEnchanted instance;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        if (!getServer().getPluginManager().isPluginEnabled("packetevents")) {
            getLogger().severe("Missing dependency: PacketEvents!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        PacketListener.init();
    }

    @Override
    public void onDisable() {
    }
}

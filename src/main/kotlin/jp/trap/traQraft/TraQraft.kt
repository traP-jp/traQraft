package jp.trap.traQraft

import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class TraQraft : JavaPlugin() {

    override fun onLoad() {
        logger.info("Hello, traQraft!")
    }

    override fun onEnable() {
        // Plugin startup logic
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

package jp.trap.traQraft

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class TraQraft : JavaPlugin() {
    private lateinit var listener: TraQraftListener
    lateinit var accountsManager: AccountsManager
        private set

    override fun onLoad() {
        logger.info("Loading traQraft...")
        accountsManager = AccountsManager()
        listener = TraQraftListener(this)
        saveDefaultConfig()
    }

    override fun onEnable() {
        Bukkit.setWhitelist(true)
        Bukkit.getPluginManager().registerEvents(listener, this)
        logger.info("traQraft Enabled!")
    }

    override fun onDisable() {
        logger.info("traQraft Disabled!")
    }
}

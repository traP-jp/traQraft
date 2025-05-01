package jp.trap.traQraft

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class TraQraft : JavaPlugin() {
    private lateinit var listener: TraQraftListener
    private lateinit var traQBot: TraQBot
    lateinit var accountsManager: AccountsManager
        private set

    override fun onLoad() {
        logger.info("Loading traQraft...")

        saveDefaultConfig()
        val traQPort = config.getInt("traQ.port")
        val traQVerificationToken = config.getString("traQ.verificationToken")
            ?: error("traQ.verificationToken is not set in config.yml")
        val traQBotAccessToken = config.getString("traQ.botAccessToken")
            ?: error("traQ.botAccessToken is not set in config.yml")

        accountsManager = AccountsManager()
        listener = TraQraftListener(this)
        traQBot = TraQBot(
            this,
            port = traQPort,
            verificationToken = traQVerificationToken,
            botAccessToken = traQBotAccessToken
        )
    }

    override fun onEnable() {
        traQBot.run()
        Bukkit.getPluginManager().registerEvents(listener, this)
        Bukkit.setWhitelist(true)
        logger.info("traQraft Enabled!")
    }

    override fun onDisable() {
        traQBot.stop()
        logger.info("traQraft Disabled!")
    }
}

package jp.trap.traQraft

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

@Suppress("unused")
class TraQraft : JavaPlugin() {
    lateinit var accountsManager: AccountsManager
        private set
    lateinit var traQBot: TraQBot
    private lateinit var listener: TraQraftListener

    override fun onLoad() {
        logger.info("Loading traQraft...")

        saveDefaultConfig()
        val traQPort = config.getInt("traQ.port")
        val traQVerificationToken =
            config.getString("traQ.verificationToken") ?: error("traQ.verificationToken is not set in config.yml")
        val traQBotAccessToken =
            config.getString("traQ.botAccessToken") ?: error("traQ.botAccessToken is not set in config.yml")
        val traQChannelIds = mapOf(
            "link" to (config.getString("traQ.channelIds.link")
                ?: error("traQ.channelIds.link is not set in config.yml")),
            "chat" to (config.getString("traQ.channelIds.chat")
                ?: error("traQ.channelIds.chat is not set in config.yml"))
        )

        accountsManager = AccountsManager(File(dataFolder, "accounts.json"))
        traQBot = TraQBot(
            this,
            traQPort,
            traQVerificationToken,
            traQBotAccessToken,
            traQChannelIds,
        )
        listener = TraQraftListener(this, traQChannelIds)
    }

    override fun onEnable() {
        traQBot.run()
        logger.info("traQraft Bot started on port ${traQBot.port}")

        Bukkit.getPluginManager().registerEvents(listener, this)
        Bukkit.setWhitelist(true)

        logger.info("traQraft Enabled!")
    }

    override fun onDisable() {
        traQBot.stop()

        logger.info("traQraft Disabled!")
    }
}

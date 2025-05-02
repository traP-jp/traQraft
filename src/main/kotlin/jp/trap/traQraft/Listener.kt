package jp.trap.traQraft

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import java.util.*
import java.util.logging.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val linkChannel = "gps/times/TwoSquirrels/playground" // TODO: configurable

class TraQraftListener(plugin: TraQraft) : Listener {
    private val logger: Logger = plugin.logger
    private val accountsManager: AccountsManager = plugin.accountsManager
    private val traQBot: TraQBot = plugin.traQBot

    // onPlayerLogin fires twice (https://github.com/PaperMC/Paper/issues/8676)
    private data class TokenCache(val token: String, val expiresIn: Duration = 5.seconds) {
        val expiredAt: Long = System.currentTimeMillis() + expiresIn.inWholeMilliseconds
        fun isExpired(): Boolean = System.currentTimeMillis() >= expiredAt
    }
    private val tokenCaches = mutableMapOf<UUID, TokenCache>()

    @EventHandler
    fun onPlayerLogin(event: PlayerLoginEvent) {
        val player = event.player
        if (player.isWhitelisted) return

        tokenCaches[player.uniqueId]?.run {
            if (isExpired()) tokenCaches.remove(player.uniqueId)
        }

        val token = tokenCaches.getOrPut(player.uniqueId) {
            TokenCache(accountsManager.createToken(player).getOrElse { exception ->
                logger.warning("Failed to create token for ${player.name}: ${exception.message}")
                event.disallow(
                    PlayerLoginEvent.Result.KICK_WHITELIST,
                    Component.text("traQ 連携のトークンの生成に失敗しました。\n")
                        .append(Component.text("サーバー管理者に連絡してください。")).color(NamedTextColor.YELLOW)
                )
                return
            })
        }.token

        event.disallow(
            PlayerLoginEvent.Result.KICK_WHITELIST,
            Component.text("traQ の\n\n").append(Component.text("#$linkChannel").color(NamedTextColor.AQUA))
                .append(Component.text("\n\nに\n\n"))
                .append(Component.text(token).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD))
                .append(Component.text("\n\nを送信して、traQ アカウントに連携してください。\n連携が完了したら、再度接続してください。"))
        )
    }
}

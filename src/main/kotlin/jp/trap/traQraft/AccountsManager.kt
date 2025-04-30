package jp.trap.traQraft

import org.bukkit.OfflinePlayer
import java.util.*

class AccountsManager {
    data class Account(val player: OfflinePlayer, val traQID: String? = null) {
        var token: Int? = null
            private set
        private var tokenExpired: Long = 0

        fun setToken(token: Int, lifetime: Long = 1000L * 60 * 5) {
            this.token = token
            this.tokenExpired = System.currentTimeMillis() + lifetime
        }

        fun isTokenExpired(): Boolean {
            return token == null || System.currentTimeMillis() > tokenExpired
        }
    }

    private val accounts = mutableMapOf<UUID, Account>()
    private val tokens = mutableMapOf<Int, Account>()

    init {
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                tokens.entries.removeIf { (_, account) -> account.isTokenExpired() }
            }
        }, 0L, 1000L * 60 * 5)
    }

    fun createToken(player: OfflinePlayer): Result<String> {
        val account = accounts.getOrPut(player.uniqueId) { Account(player) }
        account.token?.let {
            tokens.remove(it)
        }

        if (tokens.size >= 1000) {
            return Result.failure(IllegalStateException("Token limit reached"))
        }

        var token: Int
        do {
            token = (0..9999).random()
        } while (tokens.containsKey(token))
        account.setToken(token)
        tokens[token] = account

        return Result.success(token.toString().padStart(4, '0'))
    }
}

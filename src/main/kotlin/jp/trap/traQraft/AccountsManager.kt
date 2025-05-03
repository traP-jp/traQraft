package jp.trap.traQraft

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.io.File
import java.util.*

val hira = listOf(
    'あ', 'い', 'う', 'え', 'お',
    'か', 'き', 'く', 'け', 'こ',
    'さ', 'し', 'す', 'せ', 'そ',
    'た', 'ち', 'つ', 'て', 'と',
    'な', 'に', 'ぬ', 'ね', 'の',
    'は', 'ひ', 'ふ', 'へ', 'ほ',
    'ま', 'み', 'む', 'め', 'も',
    'や', 'ゆ', 'よ',
    'ら', 'り', 'る', 'れ', 'ろ',
    'わ', 'を',
)

class AccountsManager(val accountsFile: File) {
    private val json = Json {
        prettyPrint = true
        isLenient = true
        allowStructuredMapKeys = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Serializable
    private data class AccountData(val player: String, val traQ: String)

    private data class Account(val player: OfflinePlayer, var traQUuid: String? = null) {
        var token: List<Char>? = null
            private set
        private var tokenExpired: Long = 0

        fun setToken(token: List<Char>, lifetime: Long = 1000L * 60 * 5) {
            this.token = token
            this.tokenExpired = System.currentTimeMillis() + lifetime
        }

        fun clearToken() {
            token = null
            tokenExpired = 0
        }

        fun isTokenExpired(): Boolean {
            return token == null || System.currentTimeMillis() > tokenExpired
        }
    }

    private val accounts = mutableMapOf<UUID, Account>()
    private val tokens = mutableMapOf<List<Char>, Account>()

    private fun loadAccounts(): Boolean {
        if (!accountsFile.exists()) return false
        json.decodeFromString<List<AccountData>>(accountsFile.readText()).forEach { (player, traQ) ->
            val offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(player))
            accounts.getOrPut(offlinePlayer.uniqueId) { Account(offlinePlayer) }.apply {
                this.traQUuid = traQ
            }
        }
        return true
    }

    private fun saveAccounts() {
        accountsFile.writeText(json.encodeToString(accounts.flatMap { (playerUuid, account) ->
            account.traQUuid?.let { listOf(AccountData(playerUuid.toString(), it)) } ?: emptyList()
        }))
    }

    init {
        loadAccounts()

        // expired tokens cleaner
        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                tokens.entries.removeIf { (_, account) -> account.isTokenExpired() }
            }
        }, 0L, 1000L * 60 * 5)
    }

    fun createToken(player: OfflinePlayer): Result<String> {
        val account = accounts.getOrPut(player.uniqueId) { Account(player) }
        account.token?.let { tokens.remove(it) }

        // prevent unintended linking with incorrect tokens, or prevent tokens from overflowing
        if (tokens.size >= 1000) {
            return Result.failure(IllegalStateException("Token limit reached"))
        }

        // generate a new unique token
        var token: List<Char>
        do {
            // use hiragana to avoid notation inconsistency
            token = List(3) { hira.random() } // 45^3 = 91125 patterns
        } while (tokens.containsKey(token))
        account.setToken(token)
        tokens[token] = account

        return Result.success(token.joinToString(""))
    }

    fun link(traQUuid: String, tokenText: String): Result<OfflinePlayer> {
        val token: List<Char> = tokenText.filter { it in hira }.toList()
        tokens[token]?.let { account ->
            if (account.isTokenExpired()) {
                return Result.failure(IllegalStateException("Token expired"))
            }

            tokens.remove(token)
            loadAccounts()
            account.traQUuid = traQUuid
            account.clearToken()
            saveAccounts()

            return Result.success(account.player)
        } ?: return Result.failure(IllegalStateException("Invalid token"))
    }

    fun link(traQUuid: String, player: OfflinePlayer) {
        loadAccounts()
        val account = accounts.getOrPut(player.uniqueId) { Account(player) }
        account.traQUuid = traQUuid
        saveAccounts()
    }

    fun getTraQUuid(player: OfflinePlayer): String? {
        return accounts[player.uniqueId]?.traQUuid
    }

    fun getPlayers(traQUuid: String): List<OfflinePlayer> {
        return accounts.values.filter { it.traQUuid == traQUuid }.map { it.player }
    }
}

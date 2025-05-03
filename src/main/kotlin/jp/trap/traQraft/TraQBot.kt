package jp.trap.traQraft

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.bukkit.Bukkit
import java.time.Instant
import java.util.logging.Logger
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

object TraQApi {
    @Serializable
    data class Message(
        val id: String,
        //val userId: String,
        //val channelId: String,
        //val content: String,
        //val createdAt: String,
        //val updatedAt: String,
        //val pinned: Boolean,
        //val stamps: List<JsonObject>,
        //val threadId: String?,
    )

    @Serializable
    data class Channel(
        val id: String,
        //val parentId: String?,
        //val archived: Boolean,
        //val force: Boolean,
        val topic: String,
        val name: String,
        //val children: List<String>,
    )

    @Serializable
    data class ChannelPath(
        val path: String,
    )
}

object TraQEvent {
    @Serializable
    data class User(
        val id: String,
        val name: String,
        val displayName: String,
        //val iconId: String,
        //val bot: Boolean,
    )

    @Serializable
    data class Channel(
        val id: String,
        //val name: String,
        val path: String,
        //val parentId: String?,
        //val creator: User,
        //val createdAt: String,
        //val updatedAt: String,
    )

    @Serializable
    data class Message(
        val id: String,
        val user: User,
        val channelId: String,
        //val text: String,
        val plainText: String,
        //val embedded: JsonArray,
        //val createdAt: String,
        //val updatedAt: String,
    )
}

class TraQBot(
    private val plugin: TraQraft,
    val port: Int,
    private val verificationToken: String,
    private val botAccessToken: String,
    private val traQChannelIds: Map<String, String>,
) {
    private val logger: Logger = plugin.logger
    private val accountsManager: AccountsManager = plugin.accountsManager
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient(CIO) {
        install(ClientContentNegotiation) {
            json(json)
        }
        defaultRequest {
            url("https://q.trap.jp/api/v3/")
            headers.appendIfNameAbsent(HttpHeaders.Authorization, "Bearer $botAccessToken")
            headers.appendIfNameAbsent(HttpHeaders.ContentType, "application/json")
            headers.appendIfNameAbsent(HttpHeaders.Accept, "application/json")
        }
    }

    suspend fun postMessage(channelId: String, content: String, embed: Boolean = false): Result<TraQApi.Message> {
        val response = client.post("channels/$channelId/messages") {
            setBody(buildJsonObject {
                put("content", content)
                put("embed", embed)
            })
        }
        if (response.status != HttpStatusCode.Created) {
            return Result.failure(RuntimeException("${response.status}"))
        }
        return Result.success(response.body<TraQApi.Message>())
    }

    suspend fun editChannelTopic(channelId: String, topic: String): Result<Unit> {
        val response = client.put("channels/$channelId/topic") {
            setBody(buildJsonObject {
                put("topic", topic)
            })
        }
        if (response.status != HttpStatusCode.NoContent) {
            return Result.failure(RuntimeException("${response.status}"))
        }
        return Result.success(Unit)
    }

    suspend fun getChannel(channelId: String): Result<TraQApi.Channel> {
        val response = client.get("channels/$channelId")
        if (response.status != HttpStatusCode.OK) {
            return Result.failure(RuntimeException("${response.status}"))
        }
        return Result.success(response.body<TraQApi.Channel>())
    }

    suspend fun getChannelPath(channelId: String): Result<TraQApi.ChannelPath> {
        val response = client.get("channels/$channelId/path")
        if (response.status != HttpStatusCode.OK) {
            return Result.failure(RuntimeException("${response.status}"))
        }
        return Result.success(response.body<TraQApi.ChannelPath>())
    }

    private suspend fun onMessageCreated(message: TraQEvent.Message) {
        when (message.channelId) {
            traQChannelIds["link"] -> {
                val player = accountsManager.link(message.user.id, message.plainText).onFailure {
                    postMessage(
                        message.channelId,
                        "@${message.user.name}\n\n:no_entry: 合言葉を間違えているか、有効期限切れの可能性があります。再度 Minecraft サーバーに接続することで、新しい合言葉を発行できます。",
                        true
                    ).onFailure {
                        logger.warning("Failed to send message: ${it.message}")
                    }
                }.getOrNull() ?: return

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    player.isWhitelisted = true
                    logger.info("Linked ${player.name} (${player.uniqueId}) with ${message.user.name} (${message.user.id})")

                    CoroutineScope(Dispatchers.IO).launch {
                        postMessage(
                            message.channelId,
                            "@${message.user.name}\n\n:white_check_mark: Minecraft アカウントと連携しました！\n\nMCID: `${player.name}` (`${player.uniqueId}`)",
                            true
                        ).onFailure {
                            logger.warning("Failed to send message: ${it.message}")
                        }
                    }
                })
            }

            traQChannelIds["chat"] -> {
                // TODO
            }
        }
    }

    private val server = embeddedServer(Netty, port) {
        install(ServerContentNegotiation) {
            json(json)
        }

        routing {
            post("/") {
                val event = call.request.header("X-TRAQ-BOT-EVENT")
                val requestId = call.request.header("X-TRAQ-BOT-REQUEST-ID")
                val token = call.request.header("X-TRAQ-BOT-TOKEN")

                if (token != verificationToken) {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid Verification Token")
                    return@post
                }

                val body = call.receive<JsonObject>()

                val eventTimeMillis = body["eventTime"]!!.jsonPrimitive.content.let { Instant.parse(it).toEpochMilli() }
                val latencyMillis = System.currentTimeMillis() - eventTimeMillis
                logger.log(
                    if (latencyMillis < 5000) java.util.logging.Level.INFO else java.util.logging.Level.WARNING,
                    "Latency: $latencyMillis ms ($requestId)"
                )

                launch {
                    when (event) {
                        "JOINED" -> {
                            val channel = body["channel"]!!.let { json.decodeFromJsonElement<TraQEvent.Channel>(it) }
                            logger.info("Bot joined the channel: ${channel.path} (${channel.id})")
                        }

                        "LEFT" -> {
                            val channel = body["channel"]!!.let { json.decodeFromJsonElement<TraQEvent.Channel>(it) }
                            logger.info("Bot left the channel: ${channel.path} (${channel.id})")
                        }

                        "MESSAGE_CREATED" -> {
                            val message = body["message"]!!.let { json.decodeFromJsonElement<TraQEvent.Message>(it) }
                            onMessageCreated(message)
                        }
                    }
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }

    fun run() {
        // validate traQChannelIds
        traQChannelIds.forEach { (name, id) ->
            runBlocking {
                getChannel(id).onFailure {
                    throw RuntimeException("Not found channel $name: $id")
                }
            }
        }

        server.start(wait = false)
    }

    fun stop() {
        server.stop(1000, 5000)
    }
}

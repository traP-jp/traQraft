package jp.trap.traQraft

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.logging.Logger

@Serializable
data class EventTime(val eventTime: String) {
    fun toInstant(): Instant = Instant.parse(eventTime)
}

class TraQBot(plugin: TraQraft, port: Int, private val verificationToken: String, private val botAccessToken: String) {
    private val logger: Logger = plugin.logger
    private val accountsManager: AccountsManager = plugin.accountsManager

    fun run() {
        server.start(wait = false)
    }

    fun stop() {
        server.stop(1000, 5000)
    }

    private fun onMessageCreated() {
        // Handle message creation event
    }

    private val server = embeddedServer(Netty, port) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
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

                val eventTimeMillis = call.receive<EventTime>().toInstant().toEpochMilli()
                val pingTime: Long = System.currentTimeMillis() - eventTimeMillis
                if (pingTime < 1000) logger.info("Ping time: $pingTime ms ($requestId)")
                else logger.warning("Ping time exceeded: $pingTime ms ($requestId)")

                when (event) {
                    "JOINED" -> {
                        logger.info("Bot joined the channel: ${call.receiveText()}")
                    }

                    "LEFT" -> {
                        logger.info("Bot left the channel: ${call.receiveText()}")
                    }

                    "MESSAGE_CREATED" -> {
                        //val message = call.receive<Message>()
                        onMessageCreated()
                    }
                }

                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

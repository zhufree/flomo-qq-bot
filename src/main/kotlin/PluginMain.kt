package org.example.mirai.plugin

import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.mamoe.mirai.console.command.CommandManager.INSTANCE.unregister
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.CommandSender.Companion.toCommandSender
import net.mamoe.mirai.console.command.CompositeCommand
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.Listener
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.MessageContent
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.utils.info


object FlomoCommand : CompositeCommand(
    PluginMain, "flomo", "flo",
    description = "send flomo via api in console",
) {
    suspend fun CommandSender.setOwner(key: String) {
        // TODO
    }

    @SubCommand("send")
    suspend fun CommandSender.send(msg: String) {
        if (subject?.id == FlomoConfig.qqId) {
            if (msg.isEmpty()) return
            val key = FlomoConfig.flomoKey
            if (key.isNotEmpty()) {
                // post
                val client = KtorClient.getInstance()
                val jsonString = "{\"content\":\"$msg\"}"
                val response = client?.post<String> {
                    url("https://flomoapp.com/$key")
                    header("Content-Type", "application/json")
                    body = Json.parseToJsonElement(jsonString)
                }
                response?.let {
                    val jsonObj = KtorClient.json.decodeFromString(JsonObject.serializer(), response)
                    sendMessage(jsonObj["message"].toString())
                }
            } else {
                sendMessage("未绑定flomo api key，回复/flomo key [api-key]绑定")
            }
        }
    }
}

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "info.zhufree.flomo-plugin",
        name = "FlomoPlugin",
        version = "0.1.0"
    )
//    @OptIn(ConsoleExperimentalApi::class)
//    JvmPluginDescription.loadFromResource()
) {
    internal lateinit var commandListener: Listener<MessageEvent>
    override fun onEnable() {
        logger.info { "Plugin enabled" }
        FlomoConfig.reload()
        FlomoConfig.qqId = 963949236
        commandListener = globalEventChannel().subscribeAlways(
            MessageEvent::class,
            CoroutineExceptionHandler { _, throwable ->
                logger.error(throwable)
            },
            priority = EventPriority.MONITOR,
        ) call@{
//            if (!enabled) return@call
            val sender = kotlin.runCatching {
                this.toCommandSender()
            }.getOrNull() ?: return@call

            PluginMain.launch { // Async
                handleCommand(sender, message)
            }
        }
    }

    private suspend fun handleCommand(sender: CommandSender, message: MessageChain) {
        if (sender.subject?.id == FlomoConfig.qqId) {
            val content = message[MessageContent.Key]?.content?:""
            logger.info(content)
            if (content.startsWith("flomokey")) {
                val key = content.substringAfter("flomokey").trim()
                if (key.isNotEmpty()) {
                    if (FlomoConfig.flomoKey.isNotEmpty()) {
                        FlomoConfig.flomoKey = key
                        sender.sendMessage("绑定成功，您的api-key已更新")
                    } else {
                        FlomoConfig.flomoKey = key
                        sender.sendMessage("绑定成功")
                    }
                }
            }
            if (content.startsWith("flomo")) {
                val msg = content.substringAfter("flomo").trim()
                logger.info(msg)
                if (msg.isEmpty()) return
                val key = FlomoConfig.flomoKey
                if (key.isNotEmpty()) {
                    // post
                    val client = KtorClient.getInstance()
                    val jsonString = "{\"content\":\"$msg\"}"
                    val response = client?.post<String> {
                        url("https://flomoapp.com/$key")
                        header("Content-Type", "application/json")
                        body = Json.parseToJsonElement(jsonString)
                    }
                    response?.let {
                        val jsonObj = KtorClient.json.decodeFromString(JsonObject.serializer(), response)
                        sender.sendMessage(jsonObj["message"].toString())
                    }
                } else {
                    sender.sendMessage("未绑定flomo api key，回复flomokey [your-api-key]开始绑定")
                }
            }
        }
    }

    override fun onDisable() {
        super.onDisable()
        FlomoCommand.unregister()
        logger.error("Plugin disable")
    }
}
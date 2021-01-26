package org.example.mirai.plugin

import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.globalEventChannel
import net.mamoe.mirai.event.subscribeFriendMessages
import net.mamoe.mirai.message.data.MessageSource
import net.mamoe.mirai.message.data.content
import net.mamoe.mirai.message.nextMessage
import net.mamoe.mirai.utils.info

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "info.zhufree.flomo-plugin",
        name = "FlomoPlugin",
        version = "0.1.0"
    )
//    @OptIn(ConsoleExperimentalApi::class)
//    JvmPluginDescription.loadFromResource()
) {
    override fun onEnable() {
        logger.info { "Plugin loaded" }
        FlomoConfig.reload()
        FlomoConfig.qqId = 963949236
        handleMsg()
    }

    private fun handleMsg() {
        globalEventChannel().subscribeFriendMessages {
            sentBy(FlomoConfig.qqId) {
                logger.info("sentBy User")
                case("flomo") {
                    if (FlomoConfig.flomoKey.isNotEmpty()) {
                        subject.sendMessage("您已经设置api-key，继续输入可替换key")
                    } else {
                        subject.sendMessage("请发送flomo的api key:https://flomoapp.com/{api-key}")
                    }
                    // 绑定api key
                    val key = nextMessage { message[MessageSource.Key] != null }.content.trim()

                    if (key.isNotEmpty()) {
                        FlomoConfig.flomoKey = key
                        subject.sendMessage("绑定成功")
                    }
                    return@case
                }
                Regex("""flomo .*""") matching regex@{
                    logger.info("match: ${message[MessageSource.Key]?.content}")
                    val content = it.substringAfter("flomo").trim()
                    logger.info(content)
                    if (content.isEmpty()) return@regex
                    val key = FlomoConfig.flomoKey
                    if (key.isNotEmpty()) {
                        // post
                        val client = KtorClient.getInstance()
                        val jsonString = "{\"content\":\"$content\"}"
                        val response = client?.post<String> {
                            url("https://flomoapp.com/$key")
                            header("Content-Type", "application/json")
                            body = Json.parseToJsonElement(jsonString)
                        }
                        response?.let {
                            val jsonObj = KtorClient.json.decodeFromString(JsonObject.serializer(), response)
                            subject.sendMessage(jsonObj["message"].toString())
                        }
                        return@regex
                    } else {
                        subject.sendMessage("未绑定flomo api key，回复[flomo]开始绑定")
                        return@regex
                    }
                }
            }
            return@subscribeFriendMessages
        }
    }

    override fun onDisable() {
        super.onDisable()
        logger.error("Plugin disable")
    }
}
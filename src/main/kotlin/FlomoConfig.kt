package org.example.mirai.plugin

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.value

object FlomoConfig :AutoSavePluginConfig("flomo") {
    var qqId: Long by value()
    var flomoKey: String by value()
}
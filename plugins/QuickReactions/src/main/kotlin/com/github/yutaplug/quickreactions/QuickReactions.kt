package com.github.yutaplug.quickreactions

import android.content.Context
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.Hook
import com.discord.api.channel.Channel
import com.discord.models.domain.emoji.Emoji
import com.discord.models.domain.emoji.EmojiSet
import com.discord.models.guild.Guild
import com.discord.models.member.GuildMember
import com.discord.models.message.Message
import com.discord.models.user.MeUser
import com.discord.widgets.chat.list.actions.WidgetChatListActions

@AliucordPlugin
class QuickReactions : Plugin() {
    init {
        settingsTab = SettingsTab(QuickReactionsSettings::class.java, SettingsTab.Type.BOTTOM_SHEET)
            .withArgs(this)
    }

    private val recentEmojisField by lazy {
        WidgetChatListActions.Model::class.java.getDeclaredField("recentEmojis").apply { isAccessible = true }
    }

    var emojiIds: List<String>
        get() = settings.getString(EMOJIS, "").split(SEPARATOR).filter { it.isNotEmpty() }
        set(value) = settings.setString(EMOJIS, value.joinToString(SEPARATOR))

    var fillWithFrequent: Boolean
        get() = settings.getBool(FILL, true)
        set(value) = settings.setBool(FILL, value)

    override fun start(context: Context) {
        patcher.patch(
            WidgetChatListActions.Model.Companion::class.java.getDeclaredMethod(
                "create",
                Message::class.java,
                Guild::class.java,
                Long::class.javaObjectType,
                MeUser::class.java,
                GuildMember::class.java,
                Channel::class.java,
                CharSequence::class.java,
                Int::class.javaPrimitiveType,
                EmojiSet::class.java,
            ),
            Hook { call ->
                val model = call.result as? WidgetChatListActions.Model ?: return@Hook
                val emojis = quickEmojis(call.args[8] as EmojiSet) ?: return@Hook
                recentEmojisField.set(model, emojis)
            },
        )
    }

    /** The configured emojis usable in this channel, or null to keep Discord's frequently used row. */
    private fun quickEmojis(set: EmojiSet): List<Emoji>? {
        val ids = emojiIds
        if (ids.isEmpty()) return null
        val picked = ids.mapNotNull { set.emojiIndex[it] }
        if (picked.isEmpty()) return null
        if (!fillWithFrequent) return picked
        val pickedIds = picked.mapTo(HashSet()) { it.uniqueId }
        return picked + set.recentEmojis.filter { it.uniqueId !in pickedIds }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }

    companion object {
        private const val EMOJIS = "emojis"
        private const val FILL = "fillWithFrequent"
        private const val SEPARATOR = "\n"
    }
}

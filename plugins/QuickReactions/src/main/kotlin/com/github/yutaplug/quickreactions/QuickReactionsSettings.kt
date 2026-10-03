package com.github.yutaplug.quickreactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.widgets.BottomSheet
import com.discord.models.domain.emoji.Emoji
import com.discord.models.domain.emoji.EmojiSet
import com.discord.stores.StoreEmoji
import com.discord.stores.StoreStream
import com.discord.utilities.mg_recycler.MGRecyclerAdapter
import com.discord.views.CheckedSetting
import com.discord.widgets.chat.input.emoji.EmojiPickerContextType
import com.discord.widgets.chat.input.emoji.EmojiPickerListener
import com.discord.widgets.chat.input.emoji.EmojiPickerNavigator
import com.discord.widgets.chat.list.actions.EmojiItem
import com.discord.widgets.chat.list.actions.WidgetChatListActionsEmojisAdapter
import com.lytefast.flexinput.R
import rx.Subscriber
import rx.Subscription

class QuickReactionsSettings(private val plugin: QuickReactions) : BottomSheet() {
    private var emojiSet: EmojiSet? = null
    private var subscription: Subscription? = null
    private lateinit var adapter: WidgetChatListActionsEmojisAdapter
    private lateinit var status: TextView

    override fun onViewCreated(view: View, bundle: Bundle?) {
        super.onViewCreated(view, bundle)
        val context = requireContext()

        addView(TextView(context, null, 0, R.i.UiKit_Settings_Item_Header).apply { text = "Quick Reactions" })

        // Detach Discord's own reaction row so emojis and the add button render exactly as in the actions sheet.
        val template = LayoutInflater.from(context)
            .inflate(Utils.getResId("widget_chat_list_actions", "layout"), null, false)
        val row = template.findViewById<RecyclerView>(Utils.getResId("dialog_chat_actions_add_reaction_emojis_list", "id"))
        (row.parent as ViewGroup).removeView(row)
        adapter = MGRecyclerAdapter.configure(WidgetChatListActionsEmojisAdapter(row))
        adapter.onClickEmoji = { emoji ->
            plugin.emojiIds = plugin.emojiIds - emoji.uniqueId
            render()
        }
        adapter.onClickMoreEmojis = ::pickEmoji
        addView(row)

        status = TextView(context, null, 0, R.i.UiKit_Settings_Item_SubText)
        addView(status)

        addView(
            Utils.createCheckedSetting(
                context,
                CheckedSetting.ViewType.SWITCH,
                "Fill with frequently used",
                "Show your frequently used emojis after the ones above when there is room.",
            ).apply {
                isChecked = plugin.fillWithFrequent
                setOnCheckedListener { plugin.fillWithFrequent = it }
            },
        )

        addView(
            TextView(context, null, 0, R.i.UiKit_Settings_Item).apply {
                text = "Reset to frequently used"
                setOnClickListener {
                    plugin.emojiIds = emptyList()
                    render()
                }
            },
        )

        render()
        subscription = StoreStream.getEmojis()
            .getEmojiSet(StoreEmoji.EmojiContext.Global.INSTANCE, true, true)
            .U(object : Subscriber<EmojiSet>() {
                override fun onNext(set: EmojiSet) {
                    Utils.mainThread.post {
                        emojiSet = set
                        if (isAdded) render()
                    }
                }

                override fun onError(error: Throwable) {}

                override fun onCompleted() {}
            })
    }

    private fun pickEmoji() {
        EmojiPickerNavigator.launchBottomSheet(
            parentFragmentManager,
            object : EmojiPickerListener {
                override fun onEmojiPicked(emoji: Emoji) {
                    val id = emoji.uniqueId
                    plugin.emojiIds = plugin.emojiIds - id + id
                    if (isAdded) render()
                }
            },
            EmojiPickerContextType.Global.INSTANCE,
        ) {}
    }

    private fun render() {
        val ids = plugin.emojiIds
        val emojis = emojiSet?.let { set -> ids.mapNotNull { set.emojiIndex[it] } }.orEmpty()
        // Discord's setData(emojis, count) drops the add button when the list is empty, so build the items directly.
        adapter.setData(emojis.map { EmojiItem.EmojiData(it) } + EmojiItem.MoreEmoji.INSTANCE)
        status.text = if (ids.isEmpty()) {
            "Using your frequently used emojis. Tap + to choose your own."
        } else {
            "Tap an emoji to remove it, or + to add one. Emojis you can't use in a channel are skipped there."
        }
    }

    override fun onDestroyView() {
        subscription?.unsubscribe()
        subscription = null
        super.onDestroyView()
    }
}

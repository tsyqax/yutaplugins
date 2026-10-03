package com.github.yutaplug.readall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.aliucord.Utils
import com.discord.app.AppBottomSheet

/** Discord's native channel actions sheet, reduced to a single read all row. */
class ReadAllSheet : AppBottomSheet() {
    internal var label: String? = null
    internal var onReadAll: (() -> Unit)? = null

    override fun getContentViewResId() = Utils.getResId("widget_channels_list_item_actions", "layout")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // A restored sheet has no live callback attached to it.
        if (onReadAll == null) {
            dismissAllowingStateLoss()
            return
        }
        for (name in HIDDEN_ROWS) view.findViewById<View>(Utils.getResId(name, "id"))?.visibility = View.GONE

        view.findViewById<TextView>(Utils.getResId("channels_list_item_text_actions_title", "id"))?.text =
            view.context.getString(Utils.getResId("direct_messages", "string"))

        // Reuse the guild list's DM icon instead of the channel avatar.
        val avatar = view.findViewById<View>(Utils.getResId("channels_list_item_text_actions_icon", "id"))
        val iconFrame = avatar?.parent as? ViewGroup
        if (iconFrame != null) {
            val profile = LayoutInflater.from(view.context)
                .inflate(Utils.getResId("widget_guilds_list_item_profile", "layout"), iconFrame, false) as ViewGroup
            val dmIcon = profile.findViewById<View>(Utils.getResId("guilds_item_profile_avatar_wrap", "id"))
            if (dmIcon != null) {
                profile.removeView(dmIcon)
                iconFrame.removeAllViews()
                iconFrame.addView(dmIcon)
            }
        }

        view.findViewById<TextView>(Utils.getResId("text_action_mark_as_read", "id"))?.apply {
            label?.let { text = it }
            setOnClickListener {
                val callback = onReadAll ?: return@setOnClickListener
                onReadAll = null
                dismiss()
                callback()
            }
        }
    }

    override fun onDestroyView() {
        onReadAll = null
        super.onDestroyView()
    }

    companion object {
        private val HIDDEN_ROWS = arrayOf(
            "dm_action_profile",
            "text_action_mute",
            "text_action_thread_browser",
            "action_channel_settings",
            "action_channel_notifications",
            "action_invite",
            "developer_divider",
            "action_copy_id",
        )
    }
}

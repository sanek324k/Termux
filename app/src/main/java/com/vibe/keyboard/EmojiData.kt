package com.vibe.keyboard

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private fun e(s: String) = s.trim().split(Regex("\\s+"))

object EmojiData {
    val categories: List<Pair<String, List<String>>> = listOf(
        "😀" to e("😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 🙃 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😝 😜 🤪 🤨 🧐 🤓 😎 🤩 🥳 😏 😒 😞 😔 😟 😕 🙁 😣 😖 😫 😩 🥺 😢 😭 😤 😠 😡 🤬 🤯 😳 🥵 🥶 😱 😨 😰 😥 😓 🤗 🤔 🤭 🤫 🤥 😶 😐 😑 😬 🙄 😯 😦 😧 😮 😲 🥱 😴 🤤 😪 😵 🤐 🥴 🤢 🤮 🤧 😷 🤒 🤕 🤑 🤠 😈 👿 👹 👺 🤡 💩 👻 💀 👽 👾 🤖 🎃"),
        "👍" to e("👍 👎 👊 ✊ 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 ✌️ 🤞 🤟 🤘 👌 🤌 🤏 👈 👉 👆 👇 ☝️ ✋ 🤚 🖐️ 🖖 👋 🤙 💪 🖕 ✍️ 💅 🤳 👀 👅 👄 🧠 👂 👃 👣 👶 👦 👧 👨 👩 👴 👵"),
        "❤️" to e("❤️ 🧡 💛 💚 💙 💜 🖤 🤍 🤎 💔 ❣️ 💕 💞 💓 💗 💖 💘 💝 💟 ✨ ⭐ 🌟 💫 ⚡ 🔥 💥 💢 💦 💨 💤 💯 ✅ ❌ ❎ ❓ ❗ ‼️ ⁉️ ⚠️ 🚫 ⭕ 🔴 🟠 🟡 🟢 🔵 🟣 ⚫ ⚪ 🔶 🔷 🔔 🔕 ➕ ➖ ➗ ✖️ ♻️ ⬆️ ⬇️ ⬅️ ➡️ 🔝 🆗 🆒 🆕 🆓 🔞"),
        "🐶" to e("🐶 🐱 🐭 🐹 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🙈 🙉 🙊 🐔 🐧 🐦 🐤 🦆 🦅 🦉 🦇 🐺 🐗 🐴 🦄 🐝 🐛 🦋 🐌 🐞 🐜 🐢 🐍 🦎 🐙 🦑 🦐 🦀 🐡 🐠 🐟 🐬 🐳 🐋 🦈 🐊 🐘 🦒 🐕 🐩 🐈 🐇 🌵 🎄 🌲 🌳 🌴 🌱 🌿 🍀 🍁 🍂 🍃 🌺 🌻 🌹 🌷 🌼 🌸 💐 🍄 🌍 🌙 ☀️ ⛅ ☁️ ❄️ 🌈 🌊"),
        "🍔" to e("🍏 🍎 🍐 🍊 🍋 🍌 🍉 🍇 🍓 🍈 🍒 🍑 🍍 🥥 🥝 🍅 🍆 🥑 🥦 🥕 🌽 🌶️ 🥔 🍠 🥐 🍞 🥖 🧀 🥚 🍳 🥞 🥓 🥩 🍗 🍖 🌭 🍔 🍟 🍕 🥪 🌮 🌯 🥗 🍝 🍜 🍲 🍛 🍣 🍱 🥟 🍤 🍙 🍚 🍦 🍧 🍨 🍩 🍪 🎂 🍰 🧁 🍫 🍬 🍭 🍮 🍯 ☕ 🍵 🍺 🍻 🥂 🍷 🍸 🍹 🍾 🥤 🥛"),
        "⚽" to e("⚽ 🏀 🏈 ⚾ 🎾 🏐 🏉 🎱 🏓 🏸 🥊 🎯 🎮 🕹️ 🎲 🎰 🎳 🎨 🎬 🎤 🎧 🎸 🎹 🥁 🎷 🎺 🎻 🏆 🥇 🥈 🥉 🏅 🎁 🎉 🎊 🎈 🎀 🎂 🎵 🎶"),
        "🚗" to e("🚗 🚕 🚙 🚌 🏎️ 🚓 🚑 🚒 🚚 🚜 🏍️ 🚲 ✈️ 🚀 🛸 🚁 ⛵ 🚢 🚂 🚇 🏠 🏢 🏰 🗼 🗽 ⛪ 🌋 🏖️ 🏝️ 🌅 🌃 🌉 🎡 🎢"),
        "💡" to e("📱 💻 ⌨️ 🖥️ 🖨️ 🖱️ 💾 📷 📹 🎥 📺 📻 ⏰ ⌚ 🔋 🔌 💡 🔦 📚 📖 📝 ✏️ 📌 📎 ✂️ 🔒 🔓 🔑 🔨 🔧 💰 💳 💎 ⚙️ 🧲 💣 🔮 🛒 📦 📧 📞 🔍"),
    )
}

object EmojiStore {
    private var prefs: SharedPreferences? = null
    var recent by mutableStateOf<List<String>>(emptyList())

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("vibe_emoji", Context.MODE_PRIVATE)
        prefs = p
        recent = (p.getString("recent", "") ?: "").split(" ").filter { it.isNotBlank() }
    }

    fun add(em: String) {
        recent = (listOf(em) + recent.filter { it != em }).take(24)
        prefs?.edit()?.putString("recent", recent.joinToString(" "))?.apply()
    }
}

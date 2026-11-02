package com.vibe.keyboard

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import java.io.File

private const val EN_WORDS =
    "the be to of and a in that have it for not on with he as you do at this but his by from they we say her she or an will my one all would there their what so up out if about who get which go me when make can like time no just him know take people into year your good some could them see other than then now look only come its over think also back after use two how our work first well way even new want because any these give day most us is are was were been has had did said got going really thank thanks please yes hello hey okay sorry great love need tell call right still much where here why very should something tomorrow today tonight morning evening night home friend friends maybe never always again before little life help feel keep let find put man woman world school house place number part leave seem mean long both between those own under same another too through during without while last next might must every many each few more such already almost enough everything everyone anything anyone nothing someone probably actually definitely together weekend meeting message photo phone picture video music movie game food coffee dinner lunch breakfast birthday party welcome congratulations awesome amazing beautiful wonderful perfect interesting important different possible available ready finished working waiting coming looking talking thinking wanted needed started stopped called asked told thought remember forget understand believe wonder worry hope wish agree decide choose change move stay visit meet send write read watch listen play learn teach buy sell pay cost price free busy tired hungry happy sad angry late early soon quickly slowly whatever however although unless until whether instead anyway besides i i'm i'll i've i'd don't can't won't didn't doesn't isn't wasn't it's that's what's let's"

private const val RU_WORDS =
    "и в не на я что он с как а то все она так его но да ты к у же вы за бы по только ее мне было вот от меня еще нет о из ему теперь когда даже ну вдруг ли если уже или ни быть был него до вас нибудь опять уж вам сказал ведь там потом себя ничего ей может они тут где есть надо ней для мы тебя их чем была сам чтоб без будто чего раз тоже себе под будет тогда кто этот того потому этого какой совсем ним здесь этом один почти мой тем чтобы нее сейчас были куда зачем всех никогда можно при наконец два об другой хоть после над больше тот через эти нас про всего них какая много разве три эту моя впрочем хорошо свою этой перед иногда лучше чуть том нельзя такой им более всегда конечно всю между привет пока спасибо пожалуйста здравствуйте добрый день вечер утро ночь сегодня завтра вчера работа дом друг люди человек жизнь время рука глаза слово дело место город страна вопрос ответ деньги дорога машина телефон сообщение написать позвони позвонить скажи сказать хочу хочешь можешь могу буду будешь делаешь делать сделать пойдем пошли идем приходи приду придешь встретимся давай ладно окей отлично класс круто норм нормально понятно понял поняла согласен знаю знаешь думаю думаешь вижу видел смотри смотрю слушай люблю любишь скучаю извини прости почему когда как где куда откуда сколько какое какие который которая это эта эти тут снова очень просто также поэтому однако например вообще кстати наверное возможно действительно честно серьезно привет-привет доброе добрая хорошего приятного удачи поздравляю праздник подарок встреча время сегодня позже скоро быстро медленно долго недавно давно часто редко вместе отдельно сначала потом затем наконец внимание помощь проблема решение вариант результат начало конец следующий предыдущий новый старый большой маленький первый последний важно нужно должен можешь пожалуйста"

private const val RU_EXTRA =
    "это эта эти этот тот такое такие такая нужно надо можно нельзя хочу хочешь хотим хотите хотят могу можешь можем можете могут буду будешь будем будете будут была были есть нет вообще совсем почти иногда часто редко позже скоро давно недавно еще тоже также вдруг опять снова затем тогда пока потому хотя однако зато либо привет приветик здарова здравствуй бывай свидания спокойной доброе добрый благодарю извините простите супер круто плохо ужасно здорово классно прикольно смешно грустно весело скучно интересно понятно ясно ага угу ого делаю делаешь делает делаем делаете делают делал делала делали сделал сделала сделали сделаю сделаем иду идешь идет идем идете идут шел шла пошел пошла пойду пойдем пойдешь приходи приходите приду придешь придет придем пришел пришла пришли еду едешь едет едем едут поехал поехала поедем знаю знаешь знает знаем знают знал знала думаю думаешь думает думаем думал думала считаю считаешь вижу видишь видит видим видел видела смотри смотрите смотрю смотрел смотрела слышу слышишь слушай слушаю слушаешь говорю говоришь говорит говорим говорят говорил говорила сказал сказала сказали скажи скажите скажу спросил спросила спроси ответил ответила ответь отвечай пишу пишешь пишет пишем писал писала написал написала напиши напишу напишите читаю читаешь читал читала читай беру берешь берет берем возьму возьми дал дала дай дайте даю даешь даем получил получила получу получишь купил купила куплю купи купим плачу платишь заплатил жду ждешь ждет ждем ждал ждала подожди подождите помоги помогу помогите помощь помог помогла хочется получается получилось получится вышло выходит выйдет вернусь вернешься вернулся вернулась верни вернуть позвони позвоню позвонит позвонил позвонила звонок звонить звоню звонишь встретимся встреча встретил встретила увидимся увидел увидела скучаю скучаешь люблю любишь любит любим любил любила нравится нравлюсь понравилось понял поняла поняли понимаю понимаешь понимает понимаем помню помнишь помнит помнил забыл забыла забудь забыли забыть живу живешь живет живем живут жил жила работаю работаешь работает работаем работал работала учусь учишься учится учим учил учила играю играешь играет играем играл играла гуляю гуляешь гуляем гулять гуляли отдыхаю отдыхаешь отдыхаем спать сплю спишь спит спим спал спала ложусь ложишься проснулся проснулась просыпаюсь встаю встаешь вставай ешь ест едим ел ела кушать кушаю пью пьешь пьет пьем пил пила готовлю готовишь готовит готовим готовил купить продать открой открыть закрой закрыть включи включить выключи выключить найти найди нашел нашла нашли ищу ищешь ищет искал искала дом дома домой дела дело день дни дней ночью ночи утро утром вечером неделя недели месяц месяца месяцев год года лет годы час часа часов минута минуту минуты минут секунда секунду секунды время времени раз разу раза разы жизнь жизни человек человека люди людей людям друг друга другу друзья друзей друзьям подруга подруги девушка девушки парень парня парни мужчина мужчины женщина женщины ребенок ребенка дети детей мама мамы маме папа папы папе брат брата сестра сестры семья семьи родители дочь сын мальчик девочка бабушка дедушка работа работы работе работу работать учеба школа школе школу институт университет класс урок урока экзамен экзамены сессия деньги денег деньгам цена цены цену стоит стоимость бесплатно дорого дешево магазин магазина магазине рынок аптека банк карта карты карту телефона телефоне телефону номер номера номеру сообщения сообщению письмо письма почта вызов связь интернет сайт сайта приложение приложения игра игры игру фильм фильма фильмы кино музыка музыку песня песни видео фото фотка фотку картинка книга книги книгу еда еды вода воды чай кофе пиво вино хлеб мясо рыба овощи фрукты молоко сахар соль суп каша машина машины машину дороги улица улицы улицу города городе страна страны страну мир мира миру москва россия земля места месте стол стул кровать окно дверь комната квартира квартиры кухня ванная туалет погода погоды дождь снег солнце ветер холодно жарко тепло хорошая хорошие хороший плохая плохие плохой новая новые новый новое старая старые старый старое большая большие большой большое маленькая маленькие маленький красивая красивые красивый красивое умный умная глупый глупая веселый злой сильный слабый быстрый быстро медленно тихо громко далеко близко рядом внутри снаружи сверху снизу слева справа впереди сзади везде нигде откуда оттуда сюда туда одна одно одни две четыре пять шесть семь восемь девять десять двадцать тридцать сорок пятьдесят сто тысяча миллион первый вторая третье последний следующий каждый любой другая другие самый весь вся всем всех всему сама сами себе собой нам нами вам вами них ним ними ей им мне мной тебе тобой понедельник вторник среда среду четверг пятница пятницу суббота субботу воскресенье январь февраль март апрель май июнь июль август сентябрь октябрь ноябрь декабрь"

private fun words(s: String) = s.split(" ").filter { it.isNotBlank() }

fun currentWord(before: CharSequence): String {
    var i = before.length
    while (i > 0 && (before[i - 1].isLetter() || before[i - 1] == '\'')) i--
    val w = before.substring(i)
    return if (w.length > 30) "" else w
}

object Dictionary {
    private var prefs: SharedPreferences? = null
    private val user = LinkedHashMap<String, Int>()
    private val enBuiltin = words(EN_WORDS)
    private val ruBuiltin = words(RU_WORDS + " " + RU_EXTRA)
    private var en: List<String> = enBuiltin
    private var ru: List<String> = ruBuiltin
    private var enExtra: List<String> = emptyList()
    private val baseCache = HashMap<Lang, List<Pair<String, Int>>>()

    fun init(ctx: Context) {
        if (prefs != null) return
        val p = ctx.applicationContext.getSharedPreferences("vibe_dict", Context.MODE_PRIVATE)
        prefs = p
        loadExtras(ctx.applicationContext)
        p.getString("words", "")?.lineSequence()?.forEach { line ->
            val i = line.lastIndexOf(':')
            if (i > 0) line.substring(i + 1).toIntOrNull()?.let { user[line.substring(0, i)] = it }
        }
    }

    fun learn(raw: String) {
        val w = raw.lowercase().trim('\'')
        if (w.length < 2 || w.length > 24 || !w.all { it.isLetter() || it == '\'' }) return
        user[w] = (user[w] ?: 0) + 1
        if (user.size > 1500) {
            val keep = user.entries.sortedByDescending { it.value }.take(1200)
            user.clear()
            keep.forEach { user[it.key] = it.value }
        }
        persist()
    }

    private fun loadExtras(ctx: Context) {
        try {
            enExtra = ctx.assets.open("words_en_extra.txt").bufferedReader().readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
        }
        var impRu: List<String> = emptyList()
        var impEn: List<String> = emptyList()
        try {
            val fr = File(ctx.filesDir, "dict_ru.txt")
            if (fr.exists()) impRu = fr.readLines().filter { it.isNotBlank() }
            val fe = File(ctx.filesDir, "dict_en.txt")
            if (fe.exists()) impEn = fe.readLines().filter { it.isNotBlank() }
        } catch (e: Exception) {
        }
        ru = (ruBuiltin + impRu).distinct()
        en = (enBuiltin + impEn).distinct()
        baseCache.clear()
    }

    /** Загрузка своего словаря: текстовый файл, слово (и необязательно частота) в начале строки, частые слова выше. */
    fun importWords(ctx: Context, uri: Uri): Int {
        val ruL = ArrayList<String>()
        val enL = ArrayList<String>()
        val seenRu = HashSet<String>()
        val seenEn = HashSet<String>()
        try {
            val input = ctx.contentResolver.openInputStream(uri) ?: return 0
            input.bufferedReader().useLines { lines ->
                for (line in lines) {
                    val tok = line.trim().split(Regex("[\\s,;]+")).firstOrNull() ?: continue
                    val w = tok.lowercase()
                    if (w.length < 2 || w.length > 20 || !w.all { it.isLetter() }) continue
                    if (isCyr(w)) {
                        if (ruL.size < 60000 && seenRu.add(w)) ruL.add(w)
                    } else if (w.all { it in 'a'..'z' }) {
                        if (enL.size < 60000 && seenEn.add(w)) enL.add(w)
                    }
                }
            }
        } catch (e: Exception) {
            return 0
        }
        var n = 0
        if (ruL.size >= 100) { File(ctx.filesDir, "dict_ru.txt").writeText(ruL.joinToString("\n")); n += ruL.size }
        if (enL.size >= 100) { File(ctx.filesDir, "dict_en.txt").writeText(enL.joinToString("\n")); n += enL.size }
        if (n > 0) loadExtras(ctx.applicationContext)
        return n
    }

    fun swipeWords(lang: Lang): List<Pair<String, Int>> {
        val cyr = lang == Lang.RU
        val base = baseCache.getOrPut(lang) {
            val out = ArrayList<Pair<String, Int>>()
            val list = if (cyr) ru else en
            val nb = if (cyr) ruBuiltin.size else enBuiltin.size
            list.forEachIndexed { i, w ->
                if (w.length >= 2 && w.all { it.isLetter() }) {
                    out.add(w to (if (i < nb) i else minOf(nb + (i - nb) / 40, 600)))
                }
            }
            if (!cyr) enExtra.forEach { w -> out.add(w to 625) }
            out
        }
        val out = ArrayList<Pair<String, Int>>(base.size + user.size)
        out.addAll(base)
        user.forEach { (w, c) ->
            if (w.length >= 2 && w.all { it.isLetter() } && isCyr(w) == cyr) out.add(w to maxOf(0, 20 - c))
        }
        return out
    }

    fun clearUser() {
        user.clear()
        persist()
    }

    private fun persist() {
        prefs?.edit()
            ?.putString("words", user.entries.joinToString("\n") { "${it.key}:${it.value}" })
            ?.apply()
    }

    private fun isCyr(w: String) = w.any { it in 'а'..'я' || it == 'ё' }

    fun suggest(prefix: String, lang: Lang, limit: Int = 3): List<String> {
        val p = prefix.lowercase()
        val cyr = lang == Lang.RU
        val base = if (cyr) ru else en
        val out = LinkedHashSet<String>()
        if (p.isEmpty()) {
            user.entries.filter { it.value >= 2 && isCyr(it.key) == cyr }
                .sortedByDescending { it.value }.take(limit).forEach { out.add(it.key) }
            for (w in base) { if (out.size >= limit) break; out.add(w) }
        } else {
            user.entries.filter { it.value >= 2 && it.key.startsWith(p) && it.key != p }
                .sortedByDescending { it.value }.take(limit).forEach { out.add(it.key) }
            for (w in base) {
                if (out.size >= limit) break
                if (w.startsWith(p) && w != p) out.add(w)
            }
        }
        val cap = prefix.isNotEmpty() && prefix[0].isUpperCase()
        return out.take(limit).map { if (cap) it.replaceFirstChar { c -> c.uppercase() } else it }
    }
}

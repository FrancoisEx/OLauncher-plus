package app.olauncher.helper

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.provider.Telephony
import androidx.core.content.ContextCompat
import app.olauncher.R

/**
 * Olauncher+ : icones rapides homogenes.
 *
 * Plutot que l'icone d'origine de chaque appli (formes et styles disparates), on affiche
 * une icone au trait d'un meme jeu, choisie selon le ROLE de l'appli (telephone, messages,
 * navigateur, musique...). Ordre de detection :
 *  1. roles declares au systeme (appli qui compose, qui ouvre les liens, qui envoie des mails...) ;
 *  2. categorie declaree par l'appli (audio, image, cartes...) ;
 *  3. mots-cles dans l'identifiant ou le nom de l'appli ;
 *  4. a defaut, l'initiale du nom dans un cercle, dessinee dans le meme trait.
 */
object QuickIcons {

    private val cache = HashMap<String, Int>()

    /**
     * Le pack de pictogrammes, dans l'ordre de la grille de choix. La cle est enregistree
     * dans les reglages : ne jamais renommer une cle existante.
     */
    val pack: List<Pair<String, Int>> = listOf(
        "phone" to R.drawable.ic_quick_phone,
        "sms" to R.drawable.ic_quick_sms,
        "chat" to R.drawable.ic_quick_chat,
        "mail" to R.drawable.ic_quick_mail,
        "contacts" to R.drawable.ic_quick_contacts,
        "camera" to R.drawable.ic_quick_camera,
        "gallery" to R.drawable.ic_quick_gallery,
        "browser" to R.drawable.ic_quick_browser,
        "search" to R.drawable.ic_quick_search,
        "music" to R.drawable.ic_quick_music,
        "podcasts" to R.drawable.ic_quick_podcasts,
        "video" to R.drawable.ic_quick_video,
        "recorder" to R.drawable.ic_quick_recorder,
        "maps" to R.drawable.ic_quick_maps,
        "transport" to R.drawable.ic_quick_transport,
        "weather" to R.drawable.ic_quick_weather,
        "calendar" to R.drawable.ic_quick_calendar,
        "clock" to R.drawable.ic_quick_clock,
        "notes" to R.drawable.ic_quick_notes,
        "calculator" to R.drawable.ic_quick_calculator,
        "files" to R.drawable.ic_quick_files,
        "cloud" to R.drawable.ic_quick_cloud,
        "news" to R.drawable.ic_quick_news,
        "books" to R.drawable.ic_quick_books,
        "bank" to R.drawable.ic_quick_bank,
        "shopping" to R.drawable.ic_quick_shopping,
        "fitness" to R.drawable.ic_quick_fitness,
        "games" to R.drawable.ic_quick_games,
        "work" to R.drawable.ic_quick_work,
        "passwords" to R.drawable.ic_quick_passwords,
        "settings" to R.drawable.ic_quick_settings,
    )

    /** Icone d'une cle du pack, teintee ; null si la cle est inconnue. */
    fun packIcon(context: Context, key: String, color: Int): Drawable? {
        val res = pack.firstOrNull { it.first == key }?.second ?: return null
        return ContextCompat.getDrawable(context, res)?.mutate()?.apply { setTint(color) }
    }

    /**
     * Icone au trait, deja teintee de la couleur du texte de l'accueil.
     * [override] : cle du pack choisie a la main (vide = choix automatique).
     */
    fun iconFor(context: Context, packageName: String, label: String, color: Int, override: String = ""): Drawable {
        if (override.isNotEmpty()) packIcon(context, override, color)?.let { return it }
        val res = cache.getOrPut(packageName) { detect(context, packageName, label) }
        val drawable = if (res != 0) ContextCompat.getDrawable(context, res)?.mutate() else null
        if (drawable != null) {
            drawable.setTint(color)
            return drawable
        }
        return InitialDrawable(label.trim().firstOrNull()?.uppercaseChar() ?: '?', color, context.resources.displayMetrics.density)
    }

    /** A appeler quand une appli est installee ou desinstallee. */
    fun clearCache() = cache.clear()

    private fun detect(context: Context, pkg: String, label: String): Int {
        if (pkg.isEmpty()) return 0
        return try {
            byRole(context, pkg) ?: byCategory(context, pkg) ?: byKeyword(pkg, label) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
            byKeyword(pkg, label) ?: 0
        }
    }

    // ---------------------------------------------------------------------
    // 1. Roles declares au systeme
    // ---------------------------------------------------------------------

    private fun byRole(context: Context, pkg: String): Int? {
        val pm = context.packageManager

        fun handles(intent: Intent): Boolean = try {
            pm.queryIntentActivities(intent, 0).any { it.activityInfo?.packageName == pkg }
        } catch (e: Exception) {
            false
        }

        fun mainCategory(category: String): Boolean =
            handles(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, category))

        if (pkg == "com.android.settings" || handles(Intent(Settings.ACTION_SETTINGS))) return R.drawable.ic_quick_settings
        if (handles(Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))) return R.drawable.ic_quick_phone
        val defaultSms = try { Telephony.Sms.getDefaultSmsPackage(context) } catch (e: Exception) { null }
        if (pkg == defaultSms) return R.drawable.ic_quick_sms
        if (handles(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))) return R.drawable.ic_quick_camera
        if (mainCategory(Intent.CATEGORY_APP_EMAIL) || handles(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))))
            return R.drawable.ic_quick_mail
        if (mainCategory(Intent.CATEGORY_APP_BROWSER) ||
            handles(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com")).addCategory(Intent.CATEGORY_BROWSABLE))
        ) return R.drawable.ic_quick_browser
        if (mainCategory(Intent.CATEGORY_APP_CONTACTS)) return R.drawable.ic_quick_contacts
        if (mainCategory(Intent.CATEGORY_APP_CALENDAR)) return R.drawable.ic_quick_calendar
        if (handles(Intent(AlarmClock.ACTION_SHOW_ALARMS))) return R.drawable.ic_quick_clock
        if (mainCategory(Intent.CATEGORY_APP_CALCULATOR)) return R.drawable.ic_quick_calculator
        if (mainCategory(Intent.CATEGORY_APP_MAPS)) return R.drawable.ic_quick_maps
        if (mainCategory(Intent.CATEGORY_APP_MUSIC)) return R.drawable.ic_quick_music
        if (mainCategory(Intent.CATEGORY_APP_GALLERY)) return R.drawable.ic_quick_gallery
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mainCategory(Intent.CATEGORY_APP_FILES))
            return R.drawable.ic_quick_files
        if (mainCategory(Intent.CATEGORY_APP_MESSAGING)) return R.drawable.ic_quick_chat
        if (handles(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, "a")) && pkg.contains("search"))
            return R.drawable.ic_quick_search
        return null
    }

    // ---------------------------------------------------------------------
    // 2. Categorie declaree par l'appli
    // ---------------------------------------------------------------------

    private fun byCategory(context: Context, pkg: String): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val info = try {
            context.packageManager.getApplicationInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return when (info.category) {
            ApplicationInfo.CATEGORY_AUDIO -> R.drawable.ic_quick_music
            ApplicationInfo.CATEGORY_IMAGE -> R.drawable.ic_quick_gallery
            ApplicationInfo.CATEGORY_MAPS -> R.drawable.ic_quick_maps
            ApplicationInfo.CATEGORY_VIDEO -> R.drawable.ic_quick_video
            ApplicationInfo.CATEGORY_NEWS -> R.drawable.ic_quick_news
            ApplicationInfo.CATEGORY_GAME -> R.drawable.ic_quick_games
            ApplicationInfo.CATEGORY_SOCIAL -> R.drawable.ic_quick_chat
            else -> null // PRODUCTIVITY, ACCESSIBILITY... trop vagues
        }
    }

    // ---------------------------------------------------------------------
    // 3. Mots-cles (identifiant + nom affiche), en minuscules
    // ---------------------------------------------------------------------

    private val keywords: List<Pair<List<String>, Int>> = listOf(
        listOf("weather", "meteo", "météo", "wetter", "foreca", "meteoblue", "windy") to R.drawable.ic_quick_weather,
        listOf("recorder", "soundrecorder", "voicenote", "enregistreur", "dictaphone") to R.drawable.ic_quick_recorder,
        listOf("deezer", "spotify", "music", "musique", "qobuz", "tidal", "soundcloud", "radio") to R.drawable.ic_quick_music,
        listOf("podcast", "antennapod") to R.drawable.ic_quick_podcasts,
        listOf("mail", "outlook", "thunderbird", "k9", "fairemail", "proton.android.mail") to R.drawable.ic_quick_mail,
        listOf("firefox", "chrome", "browser", "navigateur", "brave", "vivaldi", "opera", "duckduckgo", "fennec") to R.drawable.ic_quick_browser,
        listOf("whatsapp", "signal", "telegram", "messenger", "threema", "element", "discord", "slack", "teams", "olvid") to R.drawable.ic_quick_chat,
        listOf("messag", "sms", "mms") to R.drawable.ic_quick_sms,
        listOf("dialer", "phone", "telephone", "téléphone", "contacts.call") to R.drawable.ic_quick_phone,
        listOf("contact") to R.drawable.ic_quick_contacts,
        listOf("camera", "caméra", "appareil photo", "opencamera") to R.drawable.ic_quick_camera,
        listOf("gallery", "galerie", "photos", "album", "aves", "fossify.gallery") to R.drawable.ic_quick_gallery,
        listOf("maps", "osmand", "organicmaps", "waze", "plans", "carte", "navigation", "magicearth") to R.drawable.ic_quick_maps,
        listOf("calendar", "agenda", "calendrier", "etar") to R.drawable.ic_quick_calendar,
        listOf("clock", "horloge", "alarm", "réveil", "deskclock") to R.drawable.ic_quick_clock,
        listOf("calculat", "calculette") to R.drawable.ic_quick_calculator,
        listOf("password", "bitwarden", "keepass", "1password", "authenticator", "authy", "aegis", "otp") to R.drawable.ic_quick_passwords,
        listOf("notes", "note", "keep", "joplin", "obsidian", "memo", "bloc-notes") to R.drawable.ic_quick_notes,
        listOf("files", "fichiers", "filemanager", "documentsui", "explorer") to R.drawable.ic_quick_files,
        listOf("youtube", "newpipe", "video", "vidéo", "vlc", "netflix", "player", "tv") to R.drawable.ic_quick_video,
        listOf("news", "actu", "journal", "lemonde", "liberation", "lefigaro", "ouest-france", "presse") to R.drawable.ic_quick_news,
        listOf("search", "recherche", "googlequicksearchbox", "qwant", "ecosia") to R.drawable.ic_quick_search,
        listOf("bank", "banque", "boursorama", "creditagricole", "credit", "societegenerale", "bnp", "lcl", "caisse", "lydia", "paypal", "revolut", "wise") to R.drawable.ic_quick_bank,
        listOf("sncf", "tag", "transit", "citymapper", "blablacar", "uber", "bus", "train", "tram", "transport", "ratp") to R.drawable.ic_quick_transport,
        listOf("amazon", "leboncoin", "vinted", "shop", "store", "market", "courses", "cdiscount", "fnac") to R.drawable.ic_quick_shopping,
        listOf("book", "livre", "kindle", "kobo", "reader", "lecture", "librera") to R.drawable.ic_quick_books,
        listOf("drive", "cloud", "kdrive", "nextcloud", "dropbox", "onedrive", "swisstransfer") to R.drawable.ic_quick_cloud,
        listOf("strava", "fitness", "sport", "garmin", "health", "santé", "komoot", "running") to R.drawable.ic_quick_fitness,
        listOf("office", "excel", "powerpoint", "libreoffice", "collabora") to R.drawable.ic_quick_work,
        listOf("game", "jeu", "chess", "echecs", "sudoku") to R.drawable.ic_quick_games,
        listOf("settings", "paramètres", "parametres", "réglages") to R.drawable.ic_quick_settings,
    )

    private fun byKeyword(pkg: String, label: String): Int? {
        val haystack = "${pkg.lowercase()} ${label.lowercase()}"
        // Les identifiants courts (« tv », « tag », « bus »...) ne comptent que comme mots entiers
        val words = haystack.split('.', ' ', '_', '-').toSet()
        for ((keys, res) in keywords) {
            for (k in keys) {
                if (k.length <= 3) {
                    if (k in words) return res
                } else if (haystack.contains(k)) return res
            }
        }
        return null
    }

    /** Repli : l'initiale de l'appli dans un cercle, au meme trait que les icones. */
    private class InitialDrawable(private val letter: Char, color: Int, private val density: Float) : Drawable() {
        private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
            this.color = color
        }
        private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        private val bounds = Rect()

        override fun getIntrinsicWidth() = (24 * density).toInt()
        override fun getIntrinsicHeight() = (24 * density).toInt()

        override fun draw(canvas: Canvas) {
            val b = getBounds()
            val size = minOf(b.width(), b.height()).toFloat()
            val cx = b.exactCenterX()
            val cy = b.exactCenterY()
            // Meme epaisseur que les pictogrammes (graisse 500 : environ 2,2 / 24 de la boite)
            stroke.strokeWidth = size * 2.2f / 24f
            // Meme encombrement que les icones Material : cercle de 20/24 de la boite
            canvas.drawCircle(cx, cy, size * 10f / 24f - stroke.strokeWidth / 2, stroke)
            text.textSize = size * 11f / 24f
            val s = letter.toString()
            text.getTextBounds(s, 0, 1, bounds)
            canvas.drawText(s, cx, cy - bounds.exactCenterY(), text)
        }

        override fun setAlpha(alpha: Int) {
            stroke.alpha = alpha
            text.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            stroke.colorFilter = colorFilter
            text.colorFilter = colorFilter
        }

        @Deprecated("Deprecated in Java")
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}

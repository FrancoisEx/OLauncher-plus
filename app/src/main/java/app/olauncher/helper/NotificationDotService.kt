package app.olauncher.helper

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Olauncher+ : ecoute les notifications pour afficher un point discret a cote
 * des applis qui en ont une en attente. Ne lit ni ne conserve le contenu des
 * notifications : seul le nom de l'application emettrice est retenu, en memoire.
 */
class NotificationDotService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        refresh()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connected = false
        packagesLiveData.postValue(emptySet())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = try {
            activeNotifications ?: emptyArray()
        } catch (e: Exception) {
            // Peut arriver si le service n'est pas (encore) lie
            emptyArray()
        }
        val packages = active
            .asSequence()
            // Notifications permanentes (musique, recharge, VPN...) ignorees :
            // le point signale seulement ce qui attend d'etre lu.
            .filter { !it.isOngoing && it.isClearable }
            .map { it.packageName }
            .filter { it != packageName }
            .toSet()
        if (packages != packagesLiveData.value) packagesLiveData.postValue(packages)
    }

    companion object {
        private val packagesLiveData = MutableLiveData<Set<String>>(emptySet())

        /** Applis ayant au moins une notification en attente. */
        val packages: LiveData<Set<String>> get() = packagesLiveData

        @Volatile
        var connected = false
            private set

        fun componentName(context: Context) = ComponentName(context, NotificationDotService::class.java)

        fun isPermissionGranted(context: Context): Boolean =
            NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
    }
}

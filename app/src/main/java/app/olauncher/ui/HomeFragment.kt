package app.olauncher.ui

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.provider.MediaStore
import android.provider.Telephony
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import app.olauncher.helper.DotDrawable
import app.olauncher.helper.QuickIcons
import app.olauncher.helper.getColorFromAttr
import app.olauncher.helper.createDialog
import app.olauncher.helper.OlDialog
import android.widget.LinearLayout
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import app.olauncher.helper.NotificationDotService
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.olauncher.MainViewModel
import app.olauncher.R
import app.olauncher.data.AppModel
import app.olauncher.data.Constants
import app.olauncher.data.Prefs
import app.olauncher.databinding.FragmentHomeBinding
import app.olauncher.helper.appUsagePermissionGranted
import app.olauncher.helper.dpToPx
import app.olauncher.helper.expandNotificationDrawer
import app.olauncher.helper.getChangedAppTheme
import app.olauncher.helper.getUserHandleFromString
import app.olauncher.helper.isPackageInstalled
import app.olauncher.helper.openAlarmApp
import app.olauncher.helper.openCalendar
import app.olauncher.helper.openCameraApp
import app.olauncher.helper.openDialerApp
import app.olauncher.helper.setPlainWallpaperByTheme
import app.olauncher.helper.showToast
import app.olauncher.listener.OnSwipeTouchListener
import app.olauncher.listener.ViewSwipeTouchListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : BaseFragment(), View.OnClickListener, View.OnLongClickListener {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager

    // Olauncher+ : applis ayant une notification en attente
    private var dotPackages: Set<String> = emptySet()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())
        viewModel = activity?.run {
            ViewModelProvider(this)[MainViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        deviceManager = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        initObservers()
        // Olauncher+ : « retour » referme un dossier ouvert (sinon, comportement d'origine)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, folderBackCallback)
        setHomeAlignment(prefs.homeAlignment)
        initSwipeTouchListener()
        initClickListeners()
    }

    override fun onResume() {
        super.onResume()
        populateHomeScreen(false)
        // Olauncher+ : rouvrir la fenetre d'un dossier (apres sa creation ou un ajout)
        val pending = prefs.pendingFolderEdit
        if (pending != 0) {
            prefs.pendingFolderEdit = 0
            if (prefs.isFolder(pending)) showFolderDialog(pending)
        }
        // Olauncher+ : relancer l'ecoute des notifications si Android l'a coupee
        if (dotsActive() && !NotificationDotService.connected) {
            try {
                android.service.notification.NotificationListenerService.requestRebind(
                    NotificationDotService.componentName(requireContext())
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModel.isOlauncherDefault()
        if (prefs.showStatusBar) showStatusBar()
        else hideStatusBar()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.lock -> {}
            // Home button for recents feature disabled
            // R.id.recents -> {}
            R.id.clock -> openClockApp()
            R.id.date -> openCalendarApp()
            R.id.setDefaultLauncher -> viewModel.resetLauncherLiveData.call()
            R.id.tvScreenTime -> openScreenTimeDigitalWellbeing()

            else -> {
                try { // Launch app
                    val appLocation = view.tag.toString().toInt()
                    homeAppClicked(appLocation)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun openClockApp() {
        if (prefs.clockAppPackage.isBlank())
            openAlarmApp(requireContext())
        else
            launchApp(
                "Clock",
                prefs.clockAppPackage,
                prefs.clockAppClassName,
                prefs.clockAppUser
            )
    }

    private fun openCalendarApp() {
        if (prefs.calendarAppPackage.isBlank())
            openCalendar(requireContext())
        else
            launchApp(
                "Calendar",
                prefs.calendarAppPackage,
                prefs.calendarAppClassName,
                prefs.calendarAppUser
            )
    }

    override fun onLongClick(view: View): Boolean {
        // Olauncher+ : appui long sur un dossier = sa fenetre de gestion
        val folderSlot = homeSlotOf(view)
        if (folderSlot != 0 && prefs.isFolder(folderSlot)) {
            showFolderDialog(folderSlot)
            return true
        }
        when (view.id) {
            R.id.homeApp1 -> showAppList(Constants.FLAG_SET_HOME_APP_1, prefs.appName1.isNotEmpty(), true)
            R.id.homeApp2 -> showAppList(Constants.FLAG_SET_HOME_APP_2, prefs.appName2.isNotEmpty(), true)
            R.id.homeApp3 -> showAppList(Constants.FLAG_SET_HOME_APP_3, prefs.appName3.isNotEmpty(), true)
            R.id.homeApp4 -> showAppList(Constants.FLAG_SET_HOME_APP_4, prefs.appName4.isNotEmpty(), true)
            R.id.homeApp5 -> showAppList(Constants.FLAG_SET_HOME_APP_5, prefs.appName5.isNotEmpty(), true)
            R.id.homeApp6 -> showAppList(Constants.FLAG_SET_HOME_APP_6, prefs.appName6.isNotEmpty(), true)
            R.id.homeApp7 -> showAppList(Constants.FLAG_SET_HOME_APP_7, prefs.appName7.isNotEmpty(), true)
            R.id.homeApp8 -> showAppList(Constants.FLAG_SET_HOME_APP_8, prefs.appName8.isNotEmpty(), true)
            R.id.clock -> {
                showAppList(Constants.FLAG_SET_CLOCK_APP)
                prefs.clockAppPackage = ""
                prefs.clockAppClassName = ""
                prefs.clockAppUser = ""
            }

            R.id.date -> {
                showAppList(Constants.FLAG_SET_CALENDAR_APP)
                prefs.calendarAppPackage = ""
                prefs.calendarAppClassName = ""
                prefs.calendarAppUser = ""
            }

            R.id.tvScreenTime -> {
                showAppList(Constants.FLAG_SET_SCREEN_TIME_APP)
                prefs.screenTimeAppPackage = ""
                prefs.screenTimeAppClassName = ""
                prefs.screenTimeAppUser = ""
            }

            R.id.setDefaultLauncher -> {
                prefs.hideSetDefaultLauncher = true
                binding.setDefaultLauncher.visibility = View.GONE
                if (viewModel.isOlauncherDefault.value != true) {
                    requireContext().showToast(R.string.set_as_default_launcher)
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                }
            }
        }
        return true
    }

    private fun initObservers() {
        if (prefs.firstSettingsOpen) {
            binding.firstRunTips.visibility = View.VISIBLE
            binding.setDefaultLauncher.visibility = View.GONE
        } else binding.firstRunTips.visibility = View.GONE

        viewModel.refreshHome.observe(viewLifecycleOwner) {
            populateHomeScreen(it)
        }
        viewModel.isOlauncherDefault.observe(viewLifecycleOwner, Observer {
            if (it != true) {
                if (prefs.dailyWallpaper && prefs.appTheme == AppCompatDelegate.MODE_NIGHT_YES) {
                    prefs.dailyWallpaper = false
                    viewModel.cancelWallpaperWorker()
                }
                setHomeAlignment()
            }
            if (binding.firstRunTips.isVisible) return@Observer
            binding.setDefaultLauncher.isVisible = it.not() && prefs.hideSetDefaultLauncher.not()
        })
        // Olauncher+ : points de notification
        NotificationDotService.packages.observe(viewLifecycleOwner) {
            dotPackages = it
            refreshNotificationDots()
        }
        viewModel.homeAppAlignment.observe(viewLifecycleOwner) {
            setHomeAlignment(it)
        }
        viewModel.toggleDateTime.observe(viewLifecycleOwner) {
            populateDateTime()
        }
        viewModel.screenTimeValue.observe(viewLifecycleOwner) {
            it?.let { binding.tvScreenTime.text = it }
        }
        // Home button for recents feature disabled
        // viewModel.showRecentApps.observe(viewLifecycleOwner) {
        //     binding.recents.performClick()
        // }
    }

    private fun initSwipeTouchListener() {
        val context = requireContext()
        binding.mainLayout.setOnTouchListener(getSwipeGestureListener(context))
        binding.homeApp1.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp1))
        binding.homeApp2.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp2))
        binding.homeApp3.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp3))
        binding.homeApp4.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp4))
        binding.homeApp5.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp5))
        binding.homeApp6.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp6))
        binding.homeApp7.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp7))
        binding.homeApp8.setOnTouchListener(getViewSwipeTouchListener(context, binding.homeApp8))
    }

    private fun initClickListeners() {
        binding.lock.setOnClickListener(this)
        // Home button for recents feature disabled
        // binding.recents.setOnClickListener(this)
        binding.clock.setOnClickListener(this)
        binding.date.setOnClickListener(this)
        binding.clock.setOnLongClickListener(this)
        binding.date.setOnLongClickListener(this)
        binding.setDefaultLauncher.setOnClickListener(this)
        binding.setDefaultLauncher.setOnLongClickListener(this)
        binding.tvScreenTime.setOnClickListener(this)
        binding.tvScreenTime.setOnLongClickListener(this)

        // These fire only on d-pad/keyboard events; touch is consumed by ViewSwipeTouchListener
        binding.homeApp1.setOnClickListener(this)
        binding.homeApp2.setOnClickListener(this)
        binding.homeApp3.setOnClickListener(this)
        binding.homeApp4.setOnClickListener(this)
        binding.homeApp5.setOnClickListener(this)
        binding.homeApp6.setOnClickListener(this)
        binding.homeApp7.setOnClickListener(this)
        binding.homeApp8.setOnClickListener(this)
        binding.homeApp1.setOnLongClickListener(this)
        binding.homeApp2.setOnLongClickListener(this)
        binding.homeApp3.setOnLongClickListener(this)
        binding.homeApp4.setOnLongClickListener(this)
        binding.homeApp5.setOnLongClickListener(this)
        binding.homeApp6.setOnLongClickListener(this)
        binding.homeApp7.setOnLongClickListener(this)
        binding.homeApp8.setOnLongClickListener(this)

        // Olauncher+ : icones rapides
        quickAppViews().forEachIndexed { index, view ->
            val slot = index + 1
            view.setOnClickListener { quickAppClicked(slot) }
            view.setOnLongClickListener {
                showAppList(Constants.FLAG_SET_QUICK_APP_1 + index, includeHiddenApps = true)
                true
            }
        }
    }

    private fun setHomeAlignment(horizontalGravity: Int = prefs.homeAlignment) {
        val verticalGravity = when (prefs.homeVerticalPosition) {
            Constants.VerticalPosition.TOP -> Gravity.TOP
            Constants.VerticalPosition.BOTTOM -> Gravity.BOTTOM
            else -> Gravity.CENTER_VERTICAL
        }
        binding.homeAppsLayout.gravity = horizontalGravity or verticalGravity
        binding.dateTimeLayout.gravity = horizontalGravity
        binding.homeApp1.gravity = horizontalGravity
        binding.homeApp2.gravity = horizontalGravity
        binding.homeApp3.gravity = horizontalGravity
        binding.homeApp4.gravity = horizontalGravity
        binding.homeApp5.gravity = horizontalGravity
        binding.homeApp6.gravity = horizontalGravity
        binding.homeApp7.gravity = horizontalGravity
        binding.homeApp8.gravity = horizontalGravity
        positionQuickApps(horizontalGravity, verticalGravity)
        updateTopSpacing()
    }

    /**
     * Olauncher+ : en position « Haut », la liste (et les icones) commencent juste sous
     * l'heure et la date, quelle que soit leur taille ; sinon on garde les marges d'origine.
     */
    private var defaultTopPadding = -1
    private var defaultQuickTopMargin = -1

    private fun updateTopSpacing() {
        val layout = binding.homeAppsLayout
        val quickParams = binding.quickAppsLayout.layoutParams as FrameLayout.LayoutParams
        if (defaultTopPadding < 0) defaultTopPadding = layout.paddingTop
        if (defaultQuickTopMargin < 0) defaultQuickTopMargin = quickParams.topMargin
        if (prefs.homeVerticalPosition != Constants.VerticalPosition.TOP) {
            layout.setPaddingRelative(layout.paddingStart, defaultTopPadding, layout.paddingEnd, layout.paddingBottom)
            quickParams.topMargin = defaultQuickTopMargin
            binding.quickAppsLayout.layoutParams = quickParams
            return
        }
        binding.dateTimeLayout.doOnLayout {
            if (_binding == null) return@doOnLayout
            val below = if (binding.dateTimeLayout.isVisible) binding.dateTimeLayout.bottom + 32.dpToPx() else 0
            val top = maxOf(defaultTopPadding, below)
            binding.homeAppsLayout.setPaddingRelative(
                binding.homeAppsLayout.paddingStart, top,
                binding.homeAppsLayout.paddingEnd, binding.homeAppsLayout.paddingBottom
            )
            val params = binding.quickAppsLayout.layoutParams as FrameLayout.LayoutParams
            params.topMargin = top
            binding.quickAppsLayout.layoutParams = params
        }
    }

    private fun populateDateTime() {
        binding.dateTimeLayout.isVisible = prefs.dateTimeVisibility != Constants.DateTime.OFF
        binding.clock.isVisible = Constants.DateTime.isTimeVisible(prefs.dateTimeVisibility)
        binding.date.isVisible = Constants.DateTime.isDateVisible(prefs.dateTimeVisibility)

//        var dateText = SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date())
        val dateFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
        var dateText = dateFormat.format(Date())

        if (!prefs.showStatusBar) {
            val battery = (requireContext().getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (battery > 0)
                dateText = getString(R.string.day_battery, dateText, battery)
        }
        binding.date.text = dateText.replace(".,", ",")
        updateTopSpacing() // l'heure a pu apparaitre ou disparaitre
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun populateScreenTime() {
        if (requireContext().appUsagePermissionGranted().not()) return

        viewModel.getTodaysScreenTime()
        binding.tvScreenTime.visibility = View.VISIBLE

        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val horizontalMargin = if (isLandscape) 64.dpToPx() else 10.dpToPx()
        val marginTop = if (isLandscape) {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 36.dpToPx() else 56.dpToPx()
        } else {
            if (prefs.dateTimeVisibility == Constants.DateTime.DATE_ONLY) 45.dpToPx() else 72.dpToPx()
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = marginTop
            marginStart = horizontalMargin
            marginEnd = horizontalMargin
            gravity = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END
        }
        binding.tvScreenTime.layoutParams = params
        binding.tvScreenTime.setPadding(10.dpToPx())
    }

    private fun populateHomeScreen(appCountUpdated: Boolean) {
        removeFolderViews()
        if (appCountUpdated) hideHomeApps()
        populateDateTime()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            populateScreenTime()

        populateQuickApps()

        val homeAppsNum = prefs.homeAppsNum
        if (homeAppsNum == 0) return

        binding.homeApp1.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp1, prefs.appName1, prefs.appPackage1, prefs.appUser1, prefs.isShortcut1, prefs.shortcutId1)) {
            prefs.appName1 = ""
            prefs.appPackage1 = ""
        }
        if (homeAppsNum == 1) return

        binding.homeApp2.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp2, prefs.appName2, prefs.appPackage2, prefs.appUser2, prefs.isShortcut2, prefs.shortcutId2)) {
            prefs.appName2 = ""
            prefs.appPackage2 = ""
        }
        if (homeAppsNum == 2) return

        binding.homeApp3.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp3, prefs.appName3, prefs.appPackage3, prefs.appUser3, prefs.isShortcut3, prefs.shortcutId3)) {
            prefs.appName3 = ""
            prefs.appPackage3 = ""
        }
        if (homeAppsNum == 3) return

        binding.homeApp4.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp4, prefs.appName4, prefs.appPackage4, prefs.appUser4, prefs.isShortcut4, prefs.shortcutId4)) {
            prefs.appName4 = ""
            prefs.appPackage4 = ""
        }
        if (homeAppsNum == 4) return

        binding.homeApp5.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp5, prefs.appName5, prefs.appPackage5, prefs.appUser5, prefs.isShortcut5, prefs.shortcutId5)) {
            prefs.appName5 = ""
            prefs.appPackage5 = ""
        }
        if (homeAppsNum == 5) return

        binding.homeApp6.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp6, prefs.appName6, prefs.appPackage6, prefs.appUser6, prefs.isShortcut6, prefs.shortcutId6)) {
            prefs.appName6 = ""
            prefs.appPackage6 = ""
        }
        if (homeAppsNum == 6) return

        binding.homeApp7.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp7, prefs.appName7, prefs.appPackage7, prefs.appUser7, prefs.isShortcut7, prefs.shortcutId7)) {
            prefs.appName7 = ""
            prefs.appPackage7 = ""
        }
        if (homeAppsNum == 7) return

        binding.homeApp8.visibility = View.VISIBLE
        if (!setHomeAppText(binding.homeApp8, prefs.appName8, prefs.appPackage8, prefs.appUser8, prefs.isShortcut8, prefs.shortcutId8)) {
            prefs.appName8 = ""
            prefs.appPackage8 = ""
        }
    }

    private fun setHomeAppText(
        textView: TextView,
        appName: String,
        packageName: String,
        userString: String,
        isShortcut: Boolean,
        shortcutId: String?,
    ): Boolean {
        // Olauncher+ : un dossier n'a pas d'appli a verifier
        val slot = homeSlotOf(textView)
        if (slot != 0 && prefs.isFolder(slot)) {
            textView.text = folderLabel(appName)
            return true
        }
        // Get user handle for the app/shortcut
        val userHandle = getUserHandleFromString(requireContext(), userString)

        // If it's a shortcut, verify it still exists
        if (isShortcut) {
            val launcherApps = requireContext().getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

            // Query for the specific shortcut
            val query = LauncherApps.ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
            }

            try {
                val shortcuts = launcherApps.getShortcuts(query, userHandle)
                // Check if our shortcut still exists
                if (shortcuts?.any { it.id == shortcutId } == true) {
                    textView.text = labelWithDot(appName, packageName)
                    return true
                }
                textView.text = ""
                return false
            } catch (e: Exception) {
                e.printStackTrace()
                textView.text = ""
                return false
            }
        }

        // Regular app check
        if (isPackageInstalled(requireContext(), packageName, userString)) {
            textView.text = labelWithDot(appName, packageName)
            return true
        }
        textView.text = ""
        return false
    }

    private fun hideHomeApps() {
        binding.homeApp1.visibility = View.GONE
        binding.homeApp2.visibility = View.GONE
        binding.homeApp3.visibility = View.GONE
        binding.homeApp4.visibility = View.GONE
        binding.homeApp5.visibility = View.GONE
        binding.homeApp6.visibility = View.GONE
        binding.homeApp7.visibility = View.GONE
        binding.homeApp8.visibility = View.GONE
    }

    private fun launchAppOrShortcut(
        appName: String,
        packageName: String,
        activityClassName: String?,
        shortcutId: String?,
        isShortcut: Boolean,
        userString: String,
        fallback: (() -> Unit)? = null,
    ) {
        if (appName.isEmpty()) {
            showLongPressToast()
            return
        }
        if (isShortcut && !shortcutId.isNullOrEmpty()) {
            launchShortcut(
                packageName = packageName,
                shortcutId = shortcutId,
                shortcutLabel = appName,
                userString = userString
            )
        } else if (packageName.isNotEmpty()) {
            launchApp(
                appName = appName,
                packageName = packageName,
                activityClassName = activityClassName,
                userString = userString
            )
        } else {
            fallback?.invoke()
        }
    }

    private fun launchShortcut(shortcutId: String, packageName: String, shortcutLabel: String, userString: String) {
        viewModel.selectedApp(
            AppModel.PinnedShortcut(
                shortcutId = shortcutId,
                appLabel = shortcutLabel,
                user = getUserHandleFromString(requireContext(), userString),
                key = null,
                appPackage = packageName,
                isNew = false,
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun launchApp(appName: String, packageName: String, activityClassName: String?, userString: String) {
        viewModel.selectedApp(
            AppModel.App(
                appLabel = appName,
                key = null,
                appPackage = packageName,
                activityClassName = activityClassName,
                isNew = false,
                user = getUserHandleFromString(requireContext(), userString)
            ),
            Constants.FLAG_LAUNCH_APP
        )
    }

    private fun homeAppClicked(location: Int) {
        if (prefs.isFolder(location)) {
            openFolder(location)
            return
        }
        launchAppOrShortcut(
            appName = prefs.getAppName(location),
            packageName = prefs.getAppPackage(location),
            activityClassName = prefs.getAppActivityClassName(location),
            shortcutId = prefs.getShortcutId(location),
            isShortcut = prefs.getIsShortcut(location),
            userString = prefs.getAppUser(location)
        )
    }

    private fun openSwipeRightApp() {
        if (!prefs.swipeRightEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeRight,
            packageName = prefs.appPackageSwipeRight,
            activityClassName = prefs.appActivityClassNameRight,
            shortcutId = prefs.shortcutIdSwipeRight,
            isShortcut = prefs.isShortcutSwipeRight,
            userString = prefs.appUserSwipeRight,
            fallback = { openDialerApp(requireContext()) }
        )
    }

    private fun openSwipeLeftApp() {
        if (!prefs.swipeLeftEnabled) return
        launchAppOrShortcut(
            appName = prefs.appNameSwipeLeft,
            packageName = prefs.appPackageSwipeLeft,
            activityClassName = prefs.appActivityClassNameSwipeLeft,
            shortcutId = prefs.shortcutIdSwipeLeft,
            isShortcut = prefs.isShortcutSwipeLeft,
            userString = prefs.appUserSwipeLeft,
            fallback = { openCameraApp(requireContext()) }
        )
    }

    private fun showAppList(flag: Int, rename: Boolean = false, includeHiddenApps: Boolean = false) {
        viewModel.getAppList(includeHiddenApps)
        try {
            findNavController().navigate(
                R.id.action_mainFragment_to_appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
        } catch (e: Exception) {
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf(
                    Constants.Key.FLAG to flag,
                    Constants.Key.RENAME to rename
                )
            )
            e.printStackTrace()
        }
    }

    private fun lockPhone() {
        requireActivity().runOnUiThread {
            try {
                deviceManager.lockNow()
            } catch (e: SecurityException) {
                requireContext().showToast(getString(R.string.please_turn_on_double_tap_to_unlock), Toast.LENGTH_LONG)
                findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
            } catch (e: Exception) {
                requireContext().showToast(getString(R.string.launcher_failed_to_lock_device), Toast.LENGTH_LONG)
                prefs.lockModeOn = false
            }
        }
    }

    private fun showStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.show(WindowInsets.Type.statusBars())
        else
            @Suppress("DEPRECATION", "InlinedApi")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
    }

    private fun hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            requireActivity().window.insetsController?.hide(WindowInsets.Type.statusBars())
        else {
            @Suppress("DEPRECATION")
            requireActivity().window.decorView.apply {
                systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            }
        }
    }

    private fun changeAppTheme() {
        if (prefs.dailyWallpaper.not()) return
        val changedAppTheme = getChangedAppTheme(requireContext(), prefs.appTheme)
        prefs.appTheme = changedAppTheme
        if (prefs.dailyWallpaper) {
            setPlainWallpaperByTheme(requireContext(), changedAppTheme)
            viewModel.setWallpaperWorker()
        }
        requireActivity().recreate()
    }

    private fun openScreenTimeDigitalWellbeing() {
        if (prefs.screenTimeAppPackage.isNotBlank()) {
            launchApp(
                "Screen Time",
                prefs.screenTimeAppPackage,
                prefs.screenTimeAppClassName,
                prefs.screenTimeAppUser
            )
            return
        }
        val intent = Intent()
        try {
            intent.setClassName(
                Constants.DIGITAL_WELLBEING_PACKAGE_NAME,
                Constants.DIGITAL_WELLBEING_ACTIVITY
            )
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                intent.setClassName(
                    Constants.DIGITAL_WELLBEING_SAMSUNG_PACKAGE_NAME,
                    Constants.DIGITAL_WELLBEING_SAMSUNG_ACTIVITY
                )
                startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showLongPressToast() = requireContext().showToast(getString(R.string.long_press_to_select_app))

    private fun textOnClick(view: View) = onClick(view)

    private fun textOnLongClick(view: View) = onLongClick(view)

    private fun getSwipeGestureListener(context: Context): View.OnTouchListener {
        return object : OnSwipeTouchListener(context) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                expandNotificationDrawer(requireContext())
            }

            override fun onLongClick() {
                super.onLongClick()
                try {
                    findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
                    viewModel.firstOpen(false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onDoubleClick() {
                super.onDoubleClick()
                if (!prefs.lockModeOn) return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    binding.lock.performClick()
                else
                    lockPhone()
            }

            override fun onClick() {
                super.onClick()
                if (openFolderSlot != 0) {
                    closeFolder()
                    return
                }
                viewModel.checkForMessages.call()
            }
        }
    }

    private fun getViewSwipeTouchListener(context: Context, view: View): View.OnTouchListener {
        return object : ViewSwipeTouchListener(context, view) {
            override fun onSwipeLeft() {
                super.onSwipeLeft()
                openSwipeLeftApp()
            }

            override fun onSwipeRight() {
                super.onSwipeRight()
                openSwipeRightApp()
            }

            override fun onSwipeUp() {
                super.onSwipeUp()
                showAppList(Constants.FLAG_LAUNCH_APP)
            }

            override fun onSwipeDown() {
                super.onSwipeDown()
                expandNotificationDrawer(requireContext())
            }

            override fun onLongClick(view: View) {
                super.onLongClick(view)
                textOnLongClick(view)
            }

            override fun onClick(view: View) {
                super.onClick(view)
                textOnClick(view)
            }
        }
    }

    // =====================================================================
    // Olauncher+ : icones rapides sur le bord oppose aux favoris
    // =====================================================================

    private fun quickAppViews(): List<ImageView> = listOf(
        binding.quickApp1, binding.quickApp2, binding.quickApp3, binding.quickApp4, binding.quickApp5
    )

    /** Place la colonne d'icones du cote oppose aux favoris (a droite si les favoris sont centres). */
    private fun positionQuickApps(horizontalGravity: Int, verticalGravity: Int) {
        val side = if (horizontalGravity == Gravity.END) Gravity.START else Gravity.END
        val params = binding.quickAppsLayout.layoutParams as FrameLayout.LayoutParams
        params.gravity = side or verticalGravity
        binding.quickAppsLayout.layoutParams = params
        updateHomeAppsSidePadding(side)
    }

    /** Evite que les noms longs des favoris passent sous les icones rapides. */
    private fun updateHomeAppsSidePadding(side: Int = if (prefs.homeAlignment == Gravity.END) Gravity.START else Gravity.END) {
        val layout = binding.homeAppsLayout
        val base = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 0 else 24.dpToPx()
        val extra = if (prefs.quickAppsNum > 0) 64.dpToPx() else 0
        val start = base + if (side == Gravity.START) extra else 0
        val end = base + if (side == Gravity.END) extra else 0
        layout.setPaddingRelative(start, layout.paddingTop, end, layout.paddingBottom)
    }

    private fun populateQuickApps() {
        val num = prefs.quickAppsNum.coerceIn(0, 5)
        binding.quickAppsLayout.isVisible = num > 0
        updateHomeAppsSidePadding()
        if (num == 0) return
        setDefaultQuickApps()
        QuickIcons.clearCache() // une appli a pu etre installee ou remplacee

        val textColor = binding.homeApp1.currentTextColor
        quickAppViews().forEachIndexed { index, view ->
            val slot = index + 1
            if (slot > num) {
                view.visibility = View.GONE
                return@forEachIndexed
            }
            view.visibility = View.VISIBLE
            val packageName = prefs.getQuickAppPackage(slot)
            val installed = packageName.isNotEmpty() &&
                isPackageInstalled(requireContext(), packageName, prefs.getQuickAppUser(slot))

            if (installed) {
                // Icone au trait d'un meme jeu, choisie selon le role de l'appli
                view.setImageDrawable(
                    QuickIcons.iconFor(
                        requireContext(), packageName, prefs.getQuickAppLabel(slot), textColor,
                        prefs.getQuickAppIcon(slot)
                    )
                )
                view.alpha = 0.9f
                view.contentDescription = prefs.getQuickAppLabel(slot)
                applyQuickAppDot(view, packageName)
            } else {
                view.foreground = null
                // Emplacement vide : un « + » discret, appui long pour choisir l'appli
                val plus = ContextCompat.getDrawable(requireContext(), R.drawable.ic_quick_add)?.mutate()
                plus?.colorFilter = PorterDuffColorFilter(textColor, PorterDuff.Mode.SRC_IN)
                view.setImageDrawable(plus)
                view.alpha = 0.4f
                view.contentDescription = getString(R.string.long_press_to_choose_quick_app)
            }
        }
    }

    /** Au premier affichage : Telephone, Messages et Appareil photo par defaut. */
    private fun setDefaultQuickApps() {
        if (prefs.quickAppsDefaultsSet) return
        prefs.quickAppsDefaultsSet = true
        val context = requireContext()
        val pm = context.packageManager
        val user = android.os.Process.myUserHandle().toString()

        val packages = mutableListOf<String?>()
        try {
            packages += pm.resolveActivity(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (e: Exception) { e.printStackTrace() }
        try {
            packages += Telephony.Sms.getDefaultSmsPackage(context)
        } catch (e: Exception) { e.printStackTrace() }
        try {
            packages += pm.resolveActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
        } catch (e: Exception) { e.printStackTrace() }

        var slot = 1
        for (pkg in packages.filterNotNull().filter { it != "android" && pm.getLaunchIntentForPackage(it) != null }.distinct()) {
            if (slot > 5) break
            if (prefs.getQuickAppPackage(slot).isEmpty()) {
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) {
                    pkg
                }
                // Classe laissee vide : launchApp choisit l'activite de lancement
                prefs.setQuickApp(slot, label, pkg, "", user)
            }
            slot++
        }
    }

    private fun quickAppClicked(slot: Int) {
        val packageName = prefs.getQuickAppPackage(slot)
        if (packageName.isEmpty()) {
            requireContext().showToast(getString(R.string.long_press_to_choose_quick_app))
            return
        }
        launchApp(
            appName = prefs.getQuickAppLabel(slot),
            packageName = packageName,
            activityClassName = prefs.getQuickAppClassName(slot),
            userString = prefs.getQuickAppUser(slot)
        )
    }

    // =====================================================================
    // Olauncher+ : dossiers
    // Un favori peut etre un dossier : le toucher remplace la liste des favoris par son
    // contenu (applis et sites), au meme endroit et dans le meme style. Retour, un toucher
    // dans le vide ou la ligne « ‹ Nom » le referment. Appui long : fenetre de gestion.
    // =====================================================================

    private var openFolderSlot = 0
    private val folderViews = mutableListOf<View>()
    private var folderDialog: OlDialog? = null

    private val folderBackCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() = closeFolder()
    }

    private fun homeAppViews(): List<TextView> = listOf(
        binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
        binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8
    )

    /** Emplacement 1..8 d'une vue de favori, 0 pour toute autre vue. */
    private fun homeSlotOf(view: View): Int {
        if (_binding == null) return 0
        val index = homeAppViews().indexOf(view)
        return if (index >= 0) index + 1 else 0
    }

    /** Nom d'un dossier sur l'accueil, suivi d'un discret « › ». */
    private fun folderLabel(name: String): CharSequence {
        val builder = SpannableStringBuilder(name).append("  ")
        val start = builder.length
        builder.append("›")
        builder.setSpan(RelativeSizeSpan(0.7f), start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return builder
    }

    private fun visibleFolderItems(slot: Int): List<Prefs.FolderItem> =
        prefs.getFolderItems(prefs.getFolderId(slot)).filter {
            it.isShortcut || isPackageInstalled(requireContext(), it.packageName, it.user)
        }

    private fun openFolder(slot: Int) {
        val items = visibleFolderItems(slot)
        if (items.isEmpty()) {
            // Dossier vide : on passe directement a sa fenetre pour le remplir
            showFolderDialog(slot)
            return
        }
        removeFolderViews()
        openFolderSlot = slot
        folderBackCallback.isEnabled = true
        homeAppViews().forEach { it.visibility = View.GONE }

        val context = requireContext()
        val gravity = prefs.homeAlignment
        val layout = binding.homeAppsLayout
        val padding = resources.getDimensionPixelSize(R.dimen.home_app_padding_vertical)

        val header = TextView(context, null, 0, R.style.TextSmall).apply {
            text = if (gravity == Gravity.END) prefs.getAppName(slot) + "  ›"
            else "‹  " + prefs.getAppName(slot)
            this.gravity = gravity
            alpha = 0.6f
            setPadding(0, 0, 0, padding)
            setOnClickListener { closeFolder() }
            setOnLongClickListener { showFolderDialog(slot); true }
        }
        layout.addView(header, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))
        folderViews += header

        val itemViews = items.map { item ->
            TextView(context, null, 0, R.style.TextLarge).apply {
                text = labelWithDot(item.label, item.packageName)
                this.gravity = gravity
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, padding, 0, padding)
                isFocusable = true
                setOnTouchListener(object : ViewSwipeTouchListener(context, this) {
                    override fun onSwipeUp() = showAppList(Constants.FLAG_LAUNCH_APP)
                    override fun onSwipeDown() = expandNotificationDrawer(requireContext())
                    override fun onLongClick(view: View) = showFolderDialog(slot)
                    override fun onClick(view: View) = launchFolderItem(item)
                })
                setOnClickListener { launchFolderItem(item) }
            }
        }
        itemViews.forEach {
            layout.addView(it, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        folderViews += itemViews
        fitFolderToScreen(header, itemViews)
    }

    /**
     * Le dossier doit tenir a l'ecran : si ses lignes depassent la place disponible, on
     * resserre d'abord l'espacement, puis, si besoin, on reduit la taille du texte.
     */
    private fun fitFolderToScreen(header: TextView, items: List<TextView>) {
        val layout = binding.homeAppsLayout
        layout.doOnLayout {
            if (_binding == null || openFolderSlot == 0 || items.isEmpty()) return@doOnLayout
            val available = layout.height - layout.paddingTop - layout.paddingBottom
            val total = header.height + items.sumOf { it.height }
            if (total <= available) return@doOnLayout
            val n = items.size
            val pad = items[0].paddingTop
            val textHeight = items[0].height - 2 * pad
            val excess = total - available
            val newPad = maxOf(2.dpToPx(), pad - (excess + 2 * n - 1) / (2 * n))
            items.forEach { it.setPadding(it.paddingLeft, newPad, it.paddingRight, newPad) }
            val room = available - header.height - n * 2 * newPad
            if (room < n * textHeight && textHeight > 0) {
                val scale = (room.toFloat() / (n * textHeight)).coerceIn(0.5f, 1f)
                items.forEach { it.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, it.textSize * scale) }
            }
        }
    }

    private fun launchFolderItem(item: Prefs.FolderItem) {
        launchAppOrShortcut(
            appName = item.label,
            packageName = item.packageName,
            activityClassName = item.className,
            shortcutId = item.shortcutId,
            isShortcut = item.isShortcut,
            userString = item.user
        )
    }

    /** Retire le contenu du dossier ouvert, sans reafficher les favoris. */
    private fun removeFolderViews() {
        if (_binding != null) folderViews.forEach { binding.homeAppsLayout.removeView(it) }
        folderViews.clear()
        openFolderSlot = 0
        folderBackCallback.isEnabled = false
    }

    private fun closeFolder() {
        if (openFolderSlot == 0) return
        removeFolderViews()
        populateHomeScreen(true)
    }

    // ---------------------------------------------------------------------
    // Fenetre de gestion d'un dossier : nom, elements (renommer, ▲ ▼ ✕), ajout, defaire
    // ---------------------------------------------------------------------

    private fun openKeyboard(dialog: OlDialog, field: View) {
        dialog.window?.apply {
            clearFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            )
            setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        field.requestFocus()
        field.postDelayed({
            val imm = field.context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                android.view.inputmethod.InputMethodManager
            imm.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun showFolderDialog(slot: Int) {
        val folderId = prefs.getFolderId(slot)
        if (folderId.isEmpty()) return
        folderDialog?.dismiss()
        val wasOpen = openFolderSlot == slot
        val items = prefs.getFolderItems(folderId).toMutableList()
        val labelEdits = mutableListOf<EditText>()
        lateinit var nameEdit: EditText
        lateinit var dialog: OlDialog

        fun save() {
            labelEdits.forEachIndexed { i, edit ->
                val text = edit.text.toString().trim()
                if (i < items.size && text.isNotEmpty()) items[i] = items[i].copy(label = text)
            }
            prefs.setFolderItems(folderId, items)
            val name = nameEdit.text.toString().trim()
            if (name.isNotEmpty()) prefs.setFolderName(slot, name)
        }

        dialog = requireContext().createDialog(R.string.folder, R.string.okay) { container ->
            val context = container.context
            val density = resources.displayMetrics.density
            val color = context.getColorFromAttr(R.attr.primaryColor)
            fun dp(v: Int) = (v * density).toInt()

            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                // Prend le focus a l'ouverture : le clavier ne surgit qu'au toucher d'un nom
                isFocusableInTouchMode = true
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
            }
            fun plainEdit(styleRes: Int, initial: String, hintRes: Int) =
                EditText(context, null, 0, styleRes).apply {
                    setText(initial)
                    setHint(hintRes)
                    setTextColor(color)
                    setHintTextColor(androidx.core.graphics.ColorUtils.setAlphaComponent(color, 0x66))
                    setSingleLine(true)
                    imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                    background = null
                    setPadding(0, dp(8), 0, dp(8))
                    // Le clavier ne s'ouvrait pas : la fenetre, ouverte « non focalisable »
                    // pour ne pas faire reapparaitre la barre d'etat, bloquait la saisie.
                    // On la rend saisissable et on demande le clavier au toucher.
                    setOnFocusChangeListener { v, hasFocus -> if (hasFocus) openKeyboard(dialog, v) }
                    setOnClickListener { v -> openKeyboard(dialog, v) }
                }

            root.addView(TextView(context, null, 0, R.style.TextSmall).apply {
                text = getString(R.string.folder_name_caption)
                setTextColor(color)
                alpha = 0.5f
                textSize = 12f
            })
            nameEdit = plainEdit(R.style.TextSmallBold, prefs.getAppName(slot), R.string.folder_name_hint)
            root.addView(nameEdit)
            root.addView(View(context).apply {
                setBackgroundColor(color)
                alpha = 0.3f
            }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)).apply {
                bottomMargin = dp(8)
            })

            val list = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
            root.addView(list)

            fun fill() {
                list.removeAllViews()
                labelEdits.clear()
                items.forEachIndexed { index, item ->
                    val row = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                    }
                    val edit = plainEdit(R.style.TextSmall, item.label, R.string.folder_name_hint)
                    labelEdits += edit
                    row.addView(edit, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    fun button(symbol: String, description: Int, enabled: Boolean, onClick: () -> Unit) =
                        TextView(context, null, 0, R.style.TextSmallBold).apply {
                            text = symbol
                            contentDescription = getString(description)
                            setTextColor(color)
                            alpha = if (enabled) 1f else 0.15f
                            isEnabled = enabled
                            setPadding(dp(12), dp(8), dp(12), dp(8))
                            if (enabled) setOnClickListener {
                                save()
                                onClick()
                                prefs.setFolderItems(folderId, items)
                                fill()
                            }
                        }
                    row.addView(button("▲", R.string.move_up, index > 0) {
                        items.add(index - 1, items.removeAt(index))
                    })
                    row.addView(button("▼", R.string.move_down, index < items.size - 1) {
                        items.add(index + 1, items.removeAt(index))
                    })
                    row.addView(button("✕", R.string.folder_remove_item, true) {
                        items.removeAt(index)
                    })
                    list.addView(row)
                }

                val full = items.size >= Prefs.FOLDER_MAX_ITEMS
                list.addView(TextView(context, null, 0, R.style.TextSmall).apply {
                    text = getString(if (full) R.string.folder_full else R.string.folder_add)
                    setTextColor(color)
                    alpha = if (full) 0.5f else 1f
                    setPadding(0, dp(12), 0, dp(8))
                    if (!full) setOnClickListener {
                        save()
                        prefs.pendingFolderEdit = slot
                        dialog.setOnDismissListener(null)
                        dialog.dismiss()
                        folderDialog = null
                        showAppList(Constants.FLAG_ADD_FOLDER_ITEM_1 + slot - 1, includeHiddenApps = true)
                    }
                })
            }
            fill()

            var confirm = false
            root.addView(TextView(context, null, 0, R.style.TextSmall).apply {
                text = getString(R.string.folder_undo)
                setTextColor(color)
                alpha = 0.6f
                setPadding(0, dp(12), 0, dp(4))
                setOnClickListener {
                    if (!confirm) {
                        confirm = true
                        text = getString(R.string.folder_undo_confirm)
                        alpha = 1f
                        return@setOnClickListener
                    }
                    prefs.removeFolder(slot)
                    dialog.setOnDismissListener(null)
                    dialog.dismiss()
                    folderDialog = null
                    removeFolderViews()
                    populateHomeScreen(true)
                }
            })
            // Jusqu'a 10 elements + clavier : la fenetre doit pouvoir defiler
            android.widget.ScrollView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) }
                isVerticalScrollBarEnabled = false
                addView(root.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                })
            }
        }
        dialog.setOnDismissListener {
            save()
            folderDialog = null
            if (_binding == null) return@setOnDismissListener
            removeFolderViews()
            populateHomeScreen(true)
            if (wasOpen && visibleFolderItems(slot).isNotEmpty()) openFolder(slot)
        }
        folderDialog = dialog
        dialog.showRespectingStatusBar()
    }

    // =====================================================================
    // Olauncher+ : points de notification
    // =====================================================================

    private fun dotsActive(): Boolean =
        prefs.notificationDots && NotificationDotService.isPermissionGranted(requireContext())

    /** Nom de l'appli suivi (ou precede, si alignement a droite) d'un petit point. */
    private fun labelWithDot(appName: String, packageName: String): CharSequence {
        if (!dotsActive() || packageName.isEmpty() || packageName !in dotPackages) return appName
        val dot = "\u25CF"
        val builder = SpannableStringBuilder()
        if (prefs.homeAlignment == Gravity.END) {
            builder.append(dot)
            builder.setSpan(RelativeSizeSpan(0.4f), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("  ").append(appName)
        } else {
            builder.append(appName).append("  ")
            val start = builder.length
            builder.append(dot)
            builder.setSpan(RelativeSizeSpan(0.4f), start, start + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }

    private fun applyQuickAppDot(view: ImageView, packageName: String) {
        view.foreground = if (dotsActive() && packageName in dotPackages)
            DotDrawable(binding.homeApp1.currentTextColor, 3.5f * resources.displayMetrics.density)
        else null
    }

    /** Met a jour les points sans recharger tout l'ecran d'accueil. */
    private fun refreshNotificationDots() {
        if (_binding == null) return
        val homeViews = listOf(
            binding.homeApp1, binding.homeApp2, binding.homeApp3, binding.homeApp4,
            binding.homeApp5, binding.homeApp6, binding.homeApp7, binding.homeApp8
        )
        homeViews.forEachIndexed { index, view ->
            val slot = index + 1
            val name = prefs.getAppName(slot)
            if (slot <= prefs.homeAppsNum && name.isNotEmpty() && view.text.isNotEmpty() && !prefs.isFolder(slot))
                view.text = labelWithDot(name, prefs.getAppPackage(slot))
        }
        quickAppViews().forEachIndexed { index, view ->
            val pkg = prefs.getQuickAppPackage(index + 1)
            if (view.isVisible && pkg.isNotEmpty()) applyQuickAppDot(view, pkg)
        }
    }

    override fun onStop() {
        // Olauncher+ : en revenant sur l'accueil, on retrouve les favoris, pas un dossier ouvert
        if (_binding != null) removeFolderViews()
        super.onStop()
    }

    override fun onDestroyView() {
        folderDialog?.dismiss()
        folderDialog = null
        openFolderSlot = 0
        folderViews.clear()
        super.onDestroyView()
        _binding = null
        defaultTopPadding = -1
        defaultQuickTopMargin = -1
    }
}
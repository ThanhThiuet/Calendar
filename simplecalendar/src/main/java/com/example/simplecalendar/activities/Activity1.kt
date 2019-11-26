package com.example.simplecalendar.activities

import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.TransactionTooLargeException
import android.provider.MediaStore
import android.text.Html
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.simplecalendar.R
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.AlarmSound
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.SharedTheme
import java.io.File
import java.util.*
import kotlin.collections.HashMap

fun AppCompatActivity.updateActionBarTitle(text: String, color: Int = baseConfig.primaryColor) {
    supportActionBar?.title = Html.fromHtml("<font color='${color.getContrastColor().toHex()}'>$text</font>")
}

fun AppCompatActivity.updateActionBarSubtitle(text: String) {
    supportActionBar?.subtitle = Html.fromHtml("<font color='${baseConfig.primaryColor.getContrastColor().toHex()}'>$text</font>")
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.appLaunched(appId: String) {
    baseConfig.internalStoragePath = getInternalStoragePath()
    updateSDCardPath()
    baseConfig.appId = appId
    if (baseConfig.appRunCount == 0) {
        baseConfig.wasOrangeIconChecked = true
        checkAppIconColor()
    } else if (!baseConfig.wasOrangeIconChecked) {
        baseConfig.wasOrangeIconChecked = true
        val primaryColor = resources.getColor(R.color.color_primary)
        if (baseConfig.appIconColor != primaryColor) {
            getAppIconColors().forEachIndexed { index, color ->
                toggleAppIconColor(appId, index, color, false)
            }

            val defaultClassName = "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity"
            packageManager.setComponentEnabledSetting(ComponentName(baseConfig.appId, defaultClassName), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)

            val orangeClassName = "${baseConfig.appId.removeSuffix(".debug")}.activities.SplashActivity.Orange"
            packageManager.setComponentEnabledSetting(ComponentName(baseConfig.appId, orangeClassName), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

            baseConfig.appIconColor = primaryColor
            baseConfig.lastIconColor = primaryColor
        }
    }
    baseConfig.appRunCount++

    if (baseConfig.appRunCount > 225 && !baseConfig.wasRateUsPromptShown) {
        baseConfig.wasRateUsPromptShown = true
        RateUsDialog(this)
    }

    if (baseConfig.navigationBarColor == INVALID_NAVIGATION_BAR_COLOR) {
        baseConfig.defaultNavigationBarColor = window.navigationBarColor
        baseConfig.navigationBarColor = window.navigationBarColor
    }
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun BaseSimpleActivity1.isShowingSAFDialog(path: String): Boolean {
    return if (isPathOnSD(path) && (baseConfig.treeUri.isEmpty() || !hasProperStoredTreeUri(false))) {
        runOnUiThread {
            if (!isDestroyed && !isFinishing) {
                WritePermissionDialog(this, false) {
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                        putExtra("android.content.extra.SHOW_ADVANCED", true)
                        if (resolveActivity(packageManager) == null) {
                            type = "*/*"
                        }

                        if (resolveActivity(packageManager) != null) {
                            checkedDocumentPath = path
                            startActivityForResult(this, OPEN_DOCUMENT_TREE)
                        } else {
                            toast(R.string.unknown_error_occurred)
                        }
                    }
                }
            }
        }
        true
    } else {
        false
    }
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun BaseSimpleActivity1.isShowingOTGDialog(path: String): Boolean {
    return if (isPathOnOTG(path) && (baseConfig.OTGTreeUri.isEmpty() || !hasProperStoredTreeUri(true))) {
        showOTGPermissionDialog(path)
        true
    } else {
        false
    }
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
@RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
fun BaseSimpleActivity1.showOTGPermissionDialog(path: String) {
    runOnUiThread {
        if (!isDestroyed && !isFinishing) {
            WritePermissionDialog(this, true) {
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                    if (resolveActivity(packageManager) == null) {
                        type = "*/*"
                    }

                    if (resolveActivity(packageManager) != null) {
                        checkedDocumentPath = path
                        startActivityForResult(this, OPEN_DOCUMENT_TREE_OTG)
                    } else {
                        toast(R.string.unknown_error_occurred)
                    }
                }
            }
        }
    }
}

fun Activity.sharePathIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, newUri)
            type = getUriMimeType(path, newUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            try {
                if (resolveActivity(packageManager) != null) {
                    startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                } else {
                    toast(R.string.no_app_found)
                }
            } catch (e: RuntimeException) {
                if (e.cause is TransactionTooLargeException) {
                    toast(R.string.maximum_share_reached)
                } else {
                    showErrorToast(e)
                }
            }
        }
    }
}

fun Activity.sharePathsIntent(paths: ArrayList<String>, applicationId: String) {
    ensureBackgroundThread {
        if (paths.size == 1) {
            sharePathIntent(paths.first(), applicationId)
        } else {
            val uriPaths = ArrayList<String>()
            val newUris = paths.map {
                val uri = getFinalUriFromPath(it, applicationId) ?: return@ensureBackgroundThread
                uriPaths.add(uri.path.toString())
                uri
            } as ArrayList<Uri>

            var mimeType = uriPaths.getMimeType()
            if (mimeType.isEmpty() || mimeType == "*/*") {
                mimeType = paths.getMimeType()
            }

            Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = mimeType
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, newUris)

                try {
                    if (resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(this, getString(R.string.share_via)))
                    } else {
                        toast(R.string.no_app_found)
                    }
                } catch (e: RuntimeException) {
                    if (e.cause is TransactionTooLargeException) {
                        toast(R.string.maximum_share_reached)
                    } else {
                        showErrorToast(e)
                    }
                }
            }
        }
    }
}

fun Activity.setAsIntent(path: String, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_ATTACH_DATA
            setDataAndType(newUri, getUriMimeType(path, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(this, getString(R.string.set_as))

            if (resolveActivity(packageManager) != null) {
                startActivityForResult(chooser, REQUEST_SET_AS)
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun Activity.openEditorIntent(path: String, forceChooser: Boolean, applicationId: String) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        Intent().apply {
            action = Intent.ACTION_EDIT
            setDataAndType(newUri, getUriMimeType(path, newUri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            val parent = path.getParentPath()
            val newFilename = "${path.getFilenameFromPath().substringBeforeLast('.')}_1"
            val extension = path.getFilenameExtension()
            val newFilePath = File(parent, "$newFilename.$extension")

            val outputUri = if (isPathOnOTG(path)) newUri else getFinalUriFromPath("$newFilePath", applicationId)
            val resInfoList = packageManager.queryIntentActivities(this, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                grantUriPermission(packageName, outputUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra(REAL_FILE_PATH, path)

            if (resolveActivity(packageManager) != null) {
                try {
                    val chooser = Intent.createChooser(this, getString(R.string.edit_with))
                    startActivityForResult(if (forceChooser) chooser else this, REQUEST_EDIT_IMAGE)
                } catch (e: SecurityException) {
                    showErrorToast(e)
                }
            } else {
                toast(R.string.no_app_found)
            }
        }
    }
}

fun Activity.openPathIntent(path: String, forceChooser: Boolean, applicationId: String, forceMimeType: String = "", extras: HashMap<String, Boolean> = HashMap()) {
    ensureBackgroundThread {
        val newUri = getFinalUriFromPath(path, applicationId) ?: return@ensureBackgroundThread
        val mimeType = if (forceMimeType.isNotEmpty()) forceMimeType else getUriMimeType(path, newUri)
        Intent().apply {
            action = Intent.ACTION_VIEW
            setDataAndType(newUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

            if (applicationId == "com.simplemobiletools.gallery.pro" || applicationId == "com.simplemobiletools.gallery.pro.debug") {
                putExtra(IS_FROM_GALLERY, true)
            }

            for ((key, value) in extras) {
                putExtra(key, value)
            }

            putExtra(REAL_FILE_PATH, path)

            if (resolveActivity(packageManager) != null) {
                val chooser = Intent.createChooser(this, getString(R.string.open_with))
                try {
                    startActivity(if (forceChooser) chooser else this)
                } catch (e: NullPointerException) {
                    showErrorToast(e)
                }
            } else {
                if (!tryGenericMimeType(this, mimeType, newUri)) {
                    toast(R.string.no_app_found)
                }
            }
        }
    }
}

fun Activity.getFinalUriFromPath(path: String, applicationId: String): Uri? {
    val uri = try {
        ensurePublicUri(path, applicationId)
    } catch (e: Exception) {
        showErrorToast(e)
        return null
    }

    if (uri == null) {
        toast(R.string.unknown_error_occurred)
        return null
    }

    return uri
}

fun Activity.tryGenericMimeType(intent: Intent, mimeType: String, uri: Uri): Boolean {
    var genericMimeType = mimeType.getGenericMimeType()
    if (genericMimeType.isEmpty()) {
        genericMimeType = "*/*"
    }

    intent.setDataAndType(uri, genericMimeType)
    return if (intent.resolveActivity(packageManager) != null) {
        startActivity(intent)
        true
    } else {
        false
    }
}

fun Activity.hideKeyboard() {
    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow((currentFocus ?: View(this)).windowToken, 0)
    window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    currentFocus?.clearFocus()
}

fun Activity.showKeyboard(et: EditText) {
    et.requestFocus()
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.updateSharedTheme(sharedTheme: SharedTheme) {
    try {
        val contentValues = MyContentProvider.fillThemeContentValues(sharedTheme)
        applicationContext.contentResolver.update(MyContentProvider.MY_CONTENT_URI, contentValues, null, null)
    } catch (e: Exception) {
        showErrorToast(e)
    }
}

fun Activity.showPickSecondsDialogHelper(curMinutes: Int, isSnoozePicker: Boolean = false, showSecondsAtCustomDialog: Boolean = false,
                                         cancelCallback: (() -> Unit)? = null, callback: (seconds: Int) -> Unit) {
    val seconds = if (curMinutes > 0) curMinutes * 60 else curMinutes
    showPickSecondsDialog(seconds, isSnoozePicker, showSecondsAtCustomDialog, cancelCallback, callback)
}

fun Activity.showPickSecondsDialog(curSeconds: Int, isSnoozePicker: Boolean = false, showSecondsAtCustomDialog: Boolean = false,
                                   cancelCallback: (() -> Unit)? = null, callback: (seconds: Int) -> Unit) {
    hideKeyboard()
    val seconds = TreeSet<Int>()
    seconds.apply {
        if (!isSnoozePicker) {
            add(-1)
            add(0)
        }
        add(1 * MINUTE_SECONDS)
        add(5 * MINUTE_SECONDS)
        add(10 * MINUTE_SECONDS)
        add(30 * MINUTE_SECONDS)
        add(60 * MINUTE_SECONDS)
        add(curSeconds)
    }

    val items = ArrayList<RadioItem>(seconds.size + 1)
    seconds.mapIndexedTo(items) { index, value ->
        RadioItem(index, getFormattedSeconds(value, !isSnoozePicker), value)
    }

    var selectedIndex = 0
    seconds.forEachIndexed { index, value ->
        if (value == curSeconds) {
            selectedIndex = index
        }
    }

    items.add(RadioItem(-2, getString(R.string.custom)))

    RadioGroupDialog(this, items, selectedIndex, showOKButton = isSnoozePicker, cancelCallback = cancelCallback) {
        if (it == -2) {
            CustomIntervalPickerDialog(this, showSeconds = showSecondsAtCustomDialog) {
                callback(it)
            }
        } else {
            callback(it as Int)
        }
    }
}

fun BaseSimpleActivity.getAlarmSounds(type: Int, callback: (ArrayList<AlarmSound>) -> Unit) {
    val alarms = ArrayList<AlarmSound>()
    val manager = RingtoneManager(this)
    manager.setType(if (type == ALARM_SOUND_TYPE_NOTIFICATION) RingtoneManager.TYPE_NOTIFICATION else RingtoneManager.TYPE_ALARM)

    try {
        val cursor = manager.cursor
        var curId = 1
        val silentAlarm = AlarmSound(curId++, getString(R.string.no_sound), SILENT)
        alarms.add(silentAlarm)

        val defaultAlarm = getDefaultAlarmSound(type)
        alarms.add(defaultAlarm)

        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            var uri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX)
            val id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX)
            if (!uri.endsWith(id)) {
                uri += "/$id"
            }

            val alarmSound = AlarmSound(curId++, title, uri)
            alarms.add(alarmSound)
        }
        callback(alarms)
    } catch (e: Exception) {
        if (e is SecurityException) {
            handlePermission(PERMISSION_READ_STORAGE) {
                if (it) {
                    getAlarmSounds(type, callback)
                } else {
                    showErrorToast(e)
                    callback(ArrayList())
                }
            }
        } else {
            showErrorToast(e)
            callback(ArrayList())
        }
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun AppCompatActivity.checkAppSideloading(): Boolean {
    val isSideloaded = when (baseConfig.appSideloadingStatus) {
        SIDELOADING_TRUE -> true
        SIDELOADING_FALSE -> false
        else -> isAppSideloaded()
    }

    baseConfig.appSideloadingStatus = if (isSideloaded) SIDELOADING_TRUE else SIDELOADING_FALSE
    if (isSideloaded) {
        showSideloadingDialog()
    }

    return isSideloaded
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun AppCompatActivity.isAppSideloaded(): Boolean {
    return try {
        getDrawable(R.drawable.ic_camera_vector)
        false
    } catch (e: Exception) {
        true
    }
}

fun AppCompatActivity.showSideloadingDialog() {
    AppSideloadedDialog(this) {
        finish()
    }
}
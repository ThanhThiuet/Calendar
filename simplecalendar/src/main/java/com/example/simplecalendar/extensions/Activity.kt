package com.example.simplecalendar.extensions

import android.annotation.TargetApi
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.Html
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.simplecalendar.BuildConfig
import com.example.simplecalendar.R
import com.example.simplecalendar.activities.BaseSimpleActivity1
import com.example.simplecalendar.helpers.IcsExporter
import com.example.simplecalendar.models.Event
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.dialogs.*
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.AlarmSound
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.models.SharedTheme
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

fun BaseSimpleActivity.shareEvents(ids: List<Long>) {
    ensureBackgroundThread {
        val file = getTempFile()
        if (file == null) {
            toast(R.string.unknown_error_occurred)
            return@ensureBackgroundThread
        }

        val events = eventsDB.getEventsWithIds(ids) as ArrayList<Event>
        IcsExporter().exportEvents(this, file, events, false) {
            if (it == IcsExporter.ExportResult.EXPORT_OK) {
                sharePathIntent(file.absolutePath, BuildConfig.APPLICATION_ID)
            }
        }
    }
}

fun BaseSimpleActivity.getTempFile(): File? {
    val folder = File(cacheDir, "events")
    if (!folder.exists()) {
        if (!folder.mkdir()) {
            toast(R.string.unknown_error_occurred)
            return null
        }
    }

    return File(folder, "events.ics")
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

fun Activity.showKeyboard(et: EditText) {
    et.requestFocus()
    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT)
}

fun Activity.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
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

    val items = java.util.ArrayList<RadioItem>(seconds.size + 1)
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

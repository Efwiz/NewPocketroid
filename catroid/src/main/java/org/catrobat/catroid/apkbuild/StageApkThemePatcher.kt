package org.catrobat.catroid.apkbuild

import android.util.Log
import android.view.WindowManager
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.value.style.StyleBag
import com.reandroid.arsc.value.style.StyleBagItem

/**
 * Applies the stage cutout setting directly to APK templates before they are exported.
 *
 * Remote templates can lag behind the app source. Updating the compiled resource table
 * here keeps old templates compatible without requiring a template release for every fix.
 */
internal object StageApkThemePatcher {

    private const val TAG = "StageApkThemePatcher"
    private const val STAGE_THEME_NAME = "Theme.NewCatroid.Stage"

    fun enableCutoutDrawing(module: ApkModule): Boolean {
        return try {
            if (!module.hasTableBlock()) {
                Log.w(TAG, "Template has no resource table; keeping the original APK unchanged")
                return false
            }

            val themeResource = module.tableBlock.getResource(
                null as String?,
                "style",
                STAGE_THEME_NAME
            )

            if (themeResource == null || themeResource.isEmpty) {
                Log.w(TAG, "Stage theme '$STAGE_THEME_NAME' was not found in the template")
                return false
            }

            val attributeId = android.R.attr.windowLayoutInDisplayCutoutMode
            var updatedConfigurations = 0

            for (entry in themeResource) {
                val style = StyleBag.create(entry) ?: continue
                style.put(
                    attributeId,
                    StyleBagItem.integer(
                        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    )
                )
                updatedConfigurations++
            }

            if (updatedConfigurations == 0) {
                Log.w(TAG, "Stage theme exists, but no editable style configurations were found")
                false
            } else {
                Log.i(TAG, "Enabled short-edge cutout drawing in $updatedConfigurations stage theme configuration(s)")
                true
            }
        } catch (exception: Exception) {
            // A template patch must never stop APK generation on its own.
            Log.e(TAG, "Could not patch the stage theme; continuing with the original template", exception)
            false
        }
    }
}

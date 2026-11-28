package com.lightning.browser.ui.browser

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

data class AppHandler(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable?,
)

object AppHandlerResolver {

    private fun buildProbeIntent(context: Context, mimeOrScheme: String): Intent {
        if (mimeOrScheme.equals("mailto", ignoreCase = true)) {
            return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:user@example.com"))
        }
        val probeFile = File(File(context.cacheDir, "preview").apply { mkdirs() }, "probe")
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", probeFile)
        }.getOrElse {
            Uri.parse("content://${context.packageName}.fileprovider/preview_cache/probe")
        }
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeOrScheme)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun queryActivities(pm: PackageManager, intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }

    private fun resolveActivity(pm: PackageManager, intent: Intent): ResolveInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }

    fun queryHandlers(context: Context, mimeOrScheme: String): List<AppHandler> {
        val pm = context.packageManager
        val intent = buildProbeIntent(context, mimeOrScheme)
        val selfPkg = context.packageName
        return queryActivities(pm, intent)
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                if (activity.packageName == selfPkg || activity.packageName == "android") {
                    return@mapNotNull null
                }
                AppHandler(
                    packageName = activity.packageName,
                    className = activity.name,
                    label = info.loadLabel(pm).toString().ifBlank { activity.packageName },
                    icon = runCatching { info.loadIcon(pm) }.getOrNull(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun resolveSystemDefault(context: Context, mimeOrScheme: String): AppHandler? {
        val pm = context.packageManager
        val intent = buildProbeIntent(context, mimeOrScheme)
        val resolved = resolveActivity(pm, intent) ?: return null
        val activity = resolved.activityInfo ?: return null
        val pkg = activity.packageName
        // When Android shows the chooser ("Always / Just once"), resolveActivity returns "android" / ResolverActivity.
        if (pkg == "android" || pkg == context.packageName || activity.name.contains("ResolverActivity")) {
            return null
        }
        return AppHandler(
            packageName = pkg,
            className = activity.name,
            label = resolved.loadLabel(pm).toString().ifBlank { pkg },
            icon = runCatching { resolved.loadIcon(pm) }.getOrNull(),
        )
    }

    fun launchMailto(
        context: Context,
        uri: Uri,
        targetPackage: String? = null,
        targetClass: String? = null,
    ): Boolean {
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (!targetPackage.isNullOrBlank()) {
                if (!targetClass.isNullOrBlank()) {
                    component = ComponentName(targetPackage, targetClass)
                } else {
                    setPackage(targetPackage)
                }
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrElse {
            if (!targetPackage.isNullOrBlank()) {
                launchMailto(context, uri, null, null)
            } else {
                false
            }
        }
    }

    fun launchFile(
        context: Context,
        file: File,
        mimeType: String,
        targetPackage: String? = null,
        targetClass: String? = null,
    ): Boolean {
        val contentUri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull() ?: return false
        if (!targetPackage.isNullOrBlank()) {
            runCatching {
                context.grantUriPermission(targetPackage, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                clipData = android.content.ClipData.newRawUri(null, contentUri)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (!targetClass.isNullOrBlank()) {
                    component = ComponentName(targetPackage, targetClass)
                } else {
                    setPackage(targetPackage)
                }
            }
            return runCatching {
                android.util.Log.d("LightningDL", "launchFile pkg=$targetPackage cls=$targetClass mime=$mimeType uri=$contentUri")
                context.startActivity(intent)
                true
            }.getOrElse { err ->
                android.util.Log.w("LightningDL", "launchFile failed pkg=$targetPackage cls=$targetClass mime=$mimeType: ${err.message}")
                when {
                    !targetClass.isNullOrBlank() ->
                        launchFile(context, file, mimeType, targetPackage, null)
                    mimeType.startsWith("image/") && mimeType != "image/*" ->
                        launchFile(context, file, "image/*", targetPackage, null)
                    else ->
                        launchFile(context, file, mimeType, null, null)
                }
            }
        }
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            clipData = android.content.ClipData.newRawUri(null, contentUri)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (viewIntent.resolveActivity(context.packageManager) == null &&
            mimeType.startsWith("image/") && mimeType != "image/*"
        ) {
            viewIntent.setDataAndType(contentUri, "image/*")
        }
        val chooser = Intent.createChooser(viewIntent, file.name).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return runCatching {
            context.startActivity(chooser)
            true
        }.getOrDefault(false)
    }
}

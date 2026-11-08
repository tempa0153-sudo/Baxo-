package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Precision
import java.io.File
import java.io.FileOutputStream

fun persistCustomImageLocally(context: Context, sourceUriOrUrl: String): String {
    val trimmed = sourceUriOrUrl.trim()
    if (trimmed.isBlank()) return ""

    // If already stored inside app internal files, return immediately
    val internalDir = File(context.filesDir, "custom_images").apply { mkdirs() }
    if (trimmed.startsWith(internalDir.absolutePath) || trimmed.startsWith("file://" + internalDir.absolutePath)) {
        return trimmed
    }

    if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
        return try {
            val uri = Uri.parse(trimmed)
            val destFile = File(internalDir, "custom_img_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")
            
            // For low-end devices: decode with inSampleSize if the image is huge (e.g. 48MP camera shot)
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            var inSample = 1
            val maxDimension = 1280
            val maxEdge = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
            while ((maxEdge / inSample) > maxDimension) {
                inSample *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.RGB_565 // Conserves 50% memory on 2016 devices
            }

            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use { stream ->
                bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (bitmap != null) {
                FileOutputStream(destFile).use { out ->
                    bitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                bitmap?.recycle()
                destFile.absolutePath
            } else {
                // Raw stream copy fallback
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (destFile.exists() && destFile.length() > 0) destFile.absolutePath else trimmed
            }
        } catch (e: Exception) {
            trimmed
        }
    }
    return trimmed
}

fun resolveCoilData(context: Context, model: Any?): Any? {
    if (model == null) return null
    if (model is Int) return model
    if (model is File) return model
    if (model is Uri) return model
    val str = model.toString().trim()
    if (str.isBlank()) return null

    // Absolute file path
    if (str.startsWith("/")) {
        val file = File(str)
        if (file.exists()) return file
    }
    if (str.startsWith("file:///")) {
        val file = File(str.removePrefix("file://"))
        if (file.exists()) return file
    }

    // Direct content URI
    if (str.startsWith("content://")) {
        return Uri.parse(str)
    }

    // Local bundled drawable name
    val cleanName = when {
        str.startsWith("poster_") -> str.substringBefore(".")
        str.contains("poster_") -> str.substringAfterLast("/").substringBefore(".")
        else -> null
    }

    if (cleanName != null) {
        val resId = context.resources.getIdentifier(cleanName, "drawable", context.packageName)
        if (resId != 0) return resId
        val fallbackId = context.resources.getIdentifier(cleanName, "drawable", "com.example")
        if (fallbackId != 0) return fallbackId
    }
    return str
}

fun buildOptimizedImageRequest(
    context: Context,
    data: Any?,
    width: Int = 220,
    height: Int = 330,
    enableCrossfade: Boolean = false
): ImageRequest {
    val resolved = resolveCoilData(context, data)
    return ImageRequest.Builder(context)
        .data(resolved)
        .size(width, height)
        .precision(Precision.INEXACT)
        .allowRgb565(true)
        .allowHardware(Build.VERSION.SDK_INT >= 28) // Prevent hardware graphic buffer crash on Android 7-8 / low-end GPUs
        .crossfade(enableCrossfade)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .diskCachePolicy(CachePolicy.ENABLED)
        .build()
}

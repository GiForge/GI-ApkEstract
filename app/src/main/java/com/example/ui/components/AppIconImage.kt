package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val iconCache = object : LruCache<String, ImageBitmap>(150) {}

private fun drawableToBitmap(drawable: Drawable, sizePx: Int = 120): Bitmap {
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

suspend fun loadAppIcon(context: Context, packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
    iconCache.get(packageName)?.let { return@withContext it }

    try {
        val pm = context.packageManager
        val drawable = pm.getApplicationIcon(packageName)
        val bitmap = drawableToBitmap(drawable)
        val imageBitmap = bitmap.asImageBitmap()
        iconCache.put(packageName, imageBitmap)
        imageBitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    val context = LocalContext.current
    val imageBitmapState = produceState<ImageBitmap?>(initialValue = iconCache.get(packageName), key1 = packageName) {
        if (value == null) {
            value = loadAppIcon(context, packageName)
        }
    }

    val imageBitmap = imageBitmapState.value

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = "Icona $packageName",
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Android,
                contentDescription = "Icona $packageName",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}

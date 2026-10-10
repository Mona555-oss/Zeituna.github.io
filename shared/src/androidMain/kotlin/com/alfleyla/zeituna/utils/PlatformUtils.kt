package com.alfleyla.zeituna.utils

import android.util.Base64
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun base64Encode(input: ByteArray): String {
    return Base64.encodeToString(input, Base64.NO_WRAP)
}

actual fun hmacMd5(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacMD5")
    val secretKey = SecretKeySpec(key, "HmacMD5")
    mac.init(secretKey)
    return mac.doFinal(data)
}

actual fun platformLog(tag: String, message: String, isError: Boolean) {
    if (isError) {
        Log.e(tag, message)
    } else {
        Log.d(tag, message)
    }
}

actual fun platformOpenUrl(url: String) {
    // This is usually handled by the activity context in Android
}

actual fun platformGetCurrentUrl(): String {
    return "alfleyla://payment-success"
}

actual fun platformPutSessionData(key: String, value: String) {
    // In Android, this could use SharedPreferences if needed for cross-activity,
    // but the deep link usually carries the data or it's handled in the activity.
}

actual fun platformGetSessionData(key: String): String? {
    return null
}

actual fun platformGetTimeZoneId(): String {
    return TimeZone.getDefault().id
}

actual fun platformPickFile(allowedExtensions: List<String>, onResult: (ByteArray?, String?) -> Unit) {
    // File picking on Android is handled by Intents in Activity/Fragment
}

@Composable
actual fun PlatformVerticalScrollbar(modifier: Modifier, scrollState: ScrollState) {
    // Android uses native scrollbars or none for this type of UI
}

@Composable
actual fun PlatformVerticalScrollbar(modifier: Modifier, lazyListState: LazyListState) {
    // Android uses native scrollbars or none for this type of UI
}

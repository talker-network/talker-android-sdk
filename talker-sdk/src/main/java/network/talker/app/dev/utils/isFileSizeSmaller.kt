package network.talker.app.dev.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlin.math.absoluteValue

//internal fun isFileSizeSmaller(uri: Uri, context: Context, maxMb: Int) : Boolean {
//    val isSmaller: Boolean
//    val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
//    val fileValue = descriptor?.statSize ?: 0
//    val maxValue = (maxMb * 1024 * 1024).absoluteValue
//    Log.d(
//        "isFileSizeSmaller", "File size : ${fileValue / 1024 / 1024}"
//    )
//    Log.d(
//        "isFileSizeSmaller", "Max size : $maxValue"
//    )
//    isSmaller = fileValue <= maxValue
//    descriptor?.close()
//    return isSmaller
//}

internal fun isFileSizeSmaller(uri: Uri, context: Context, maxMb: Int): Boolean {
    val projection = arrayOf(OpenableColumns.SIZE)
    val cursor = context.contentResolver.query(uri, projection, null, null, null)

    var fileSize: Long = 0
    cursor?.use {
        if (it.moveToFirst()) {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            fileSize = it.getLong(sizeIndex)
        }
    }

    val maxValue = (maxMb * 1024 * 1024).absoluteValue
    Log.d("isFileSizeSmaller", "File size : ${fileSize.toFloat() / 1024F / 1024F}")
    Log.d("isFileSizeSmaller", "Max size : $maxValue")

    return fileSize <= maxValue
}
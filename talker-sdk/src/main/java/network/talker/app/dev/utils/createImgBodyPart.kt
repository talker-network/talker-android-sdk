package com.crm.buildyaar.builder.utils

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import network.talker.app.dev.TalkerGlobalVariables
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

fun createImgBodyPart(
    context: Context,
    uri: Uri?,
    label: String,
    isPdf: Boolean = false,
    name : String = ""
): MultipartBody.Part? {
    if (uri == null || uri == Uri.EMPTY) {
        return null
    }
    try {
        val filesDir = context.externalCacheDir
        val timestamp = if (name.isEmpty()){
            UUID.randomUUID().toString().filter { it.isLetterOrDigit() }
        }else{
            "$name${UUID.randomUUID().toString().filter { it.isLetterOrDigit() }}"
        }
        val file = File(filesDir, if (isPdf) "$timestamp.pdf" else "$timestamp.jpg")
        val outputStream = FileOutputStream(file)
        uri.let {
            context.contentResolver.openInputStream(
                it
            )
        }?.copyTo(outputStream)
        val requestBody =
            file.asRequestBody(if (isPdf) "pdf/*".toMediaTypeOrNull() else "image/*".toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(label, file.name, requestBody)
    }catch (e : java.lang.Exception){
        if (TalkerGlobalVariables.printLogs) {
            Log.d("createImgBodyPart", e.toString())
            Log.d("createImgBodyPart", uri.toString())
            Log.d("createImgBodyPart", label)
            Log.d("createImgBodyPart", isPdf.toString())
        }
        return null
    }
}

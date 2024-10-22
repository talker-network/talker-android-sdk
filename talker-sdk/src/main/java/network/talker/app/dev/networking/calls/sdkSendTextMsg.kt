package network.talker.app.dev.networking.calls

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import network.talker.app.dev.networking.RetrofitClient
import network.talker.app.dev.networking.data.ErrorData
import network.talker.app.dev.networking.data.UploadMessageResponse
import network.talker.app.dev.utils.AppUtils
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

//fun getFileFromUri(uri: Uri, context: Context): File? {
////    val mimeType = context.contentResolver.getType(uri)
////    val projection = when {
////        mimeType?.startsWith("image/") == true -> arrayOf(MediaStore.Images.Media.DATA)
////        mimeType == "application/pdf" -> arrayOf(MediaStore.Files.FileColumns.DATA)
////        else -> null
////    }
//    val cursor = context.contentResolver.query(uri, null, null, null, null)
//    cursor?.moveToFirst()
//    val columnIndex = cursor?.getColumnIndex(cursor.getColumnName(0))
//    val filePath = cursor?.getString(columnIndex!!)
//    cursor?.close()
//    return filePath?.let { File(it) }
//}

@SuppressLint("Range")
fun getFileName(uri: Uri, context: Context): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursorT: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursorT.use { cursor ->
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(if (cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME) == -1) 0 else cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }
    return result ?: "File"
}

fun getFileFromUri(uri: Uri, context: Context): File? {
    // Create a temp file in the cache directory of your app
    // Get the extension from the MIME type
    val extension = getFileName(uri, context).substringAfterLast(".")
    val tempFile = File.createTempFile(System.currentTimeMillis().toString(), if (extension == "pdf") ".pdf" else ".jpeg", context.cacheDir)
    try {
        // Open an InputStream from the URI
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Use the InputStream to copy the file content to the temp file
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
    return tempFile
}

internal suspend fun sdkSendMsg(
    context : Context,
    token : String,
    channelId : String,
    text : String,
    imageUri : Uri = Uri.EMPTY,
    documentUri : Uri = Uri.EMPTY,
    onSuccess : (UploadMessageResponse) -> Unit = {},
    onError : (ErrorData) -> Unit = {},
    onInternetNotAvailable : () -> Unit = {},
){
    if (AppUtils.isNetworkAvailable(context)){
        withContext(SupervisorJob() + Dispatchers.IO) {
            try {
                val file = (if (imageUri != Uri.EMPTY) getFileFromUri(imageUri, context)
                    else if(documentUri != Uri.EMPTY) getFileFromUri(documentUri, context)
                    else File("temp")) ?: File("temp")
                val response = RetrofitClient.retrofitApiService.SdkUploadMessage(
                    token = token,
                    channelId.toRequestBody("text/plain".toMediaTypeOrNull()),
                    text.toRequestBody("text/plain".toMediaTypeOrNull()),
                    if (imageUri == Uri.EMPTY) null else
                    MultipartBody.Part.createFormData(
                            "image",
                        file.name,
                        file.asRequestBody()
                    ),
                    if (documentUri == Uri.EMPTY) null else
                        MultipartBody.Part.createFormData(
                            "document",
                            file.name,
                            file.asRequestBody()
                    )
                )
                if (response.isSuccessful && response.body() != null && response.code() == 200){
                    val data = response.body()
                    if (data != null) {
                        withContext(Dispatchers.Main){
                            onSuccess(data)
                        }
                    }
                }else{
                    withContext(Dispatchers.Main){
                        onError(
                            ErrorData(
                                status = response.code(),
                                message = response.message()
                            )
                        )
                    }
                }
            }catch (e : Exception){
                withContext(Dispatchers.Main){
                    onError(
                        ErrorData(
                            status = 0,
                            message = e.localizedMessage ?: "Some error occurred"
                        )
                    )
                }
            }
        }
    }else{
        onInternetNotAvailable()
    }
}
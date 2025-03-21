package network.talker.sdk.sharedPreference

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import network.talker.sdk.networking.data.CreateUserModelData

internal class SharedPreference(private val context : Context) {
    private val sharedPreference = PreferenceManager.getDefaultSharedPreferences(context)

    fun setUserData(
        data : CreateUserModelData
    ){
        sharedPreference.edit {
            putString("name", data.name)
            putString("a_username", data.a_username)
            putString("a_pass", data.a_pass)
            putString("user_id", data.user_id)
            putString("user_auth_token", data.user_auth_token)
        }
    }

    fun getUserData() : CreateUserModelData {
        return CreateUserModelData(
            name = sharedPreference.getString("name", "") ?: "",
            a_username = sharedPreference.getString("a_username", "") ?: "",
            a_pass = sharedPreference.getString("a_pass", "") ?: "",
            user_id = sharedPreference.getString("user_id", "") ?: "",
            user_auth_token = sharedPreference.getString("user_auth_token", "") ?: "",
        )
    }

    fun setPrevUserId(prevUserId : String) {
        sharedPreference.edit {
            putString("prev_user_id", prevUserId)
        }
    }

    fun getPrevUserId() : String {
        return sharedPreference.getString("prev_user_id", "") ?: ""
    }

    fun clearPreference(){
        sharedPreference.edit().apply {
            clear()
            commit()
        }
    }
}
package com.picswap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.picswap.ui.AndroidPhotoFiles
import com.picswap.ui.PicSwapRoot
import com.picswap.ui.SettingsStore
import com.picswap.ui.SharedPrefsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore(SharedPrefsStore(applicationContext))
        val photos = AndroidPhotoFiles(applicationContext)
        setContent { PicSwapRoot(store, photos) }
    }
}

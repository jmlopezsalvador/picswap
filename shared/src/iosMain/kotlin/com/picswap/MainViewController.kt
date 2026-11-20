package com.picswap

import androidx.compose.ui.window.ComposeUIViewController
import com.picswap.ui.IosPhotoFiles
import com.picswap.ui.PicSwapRoot
import com.picswap.ui.SettingsStore
import com.picswap.ui.UserDefaultsStore
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    PicSwapRoot(SettingsStore(UserDefaultsStore()), IosPhotoFiles())
}

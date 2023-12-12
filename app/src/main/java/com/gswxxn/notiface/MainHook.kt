package com.gswxxn.notiface

import android.os.Build
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage{
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if ("com.android.systemui" != lpparam?.packageName) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE){
            NewSystemUIHookEntry(lpparam)
        } else {
            SystemUIHookEntry(lpparam)
        }
    }
}
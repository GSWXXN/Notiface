package com.gswxxn.notiface

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.callbacks.XC_LoadPackage

class MainHook : IXposedHookLoadPackage{
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
        if ("com.android.systemui" == lpparam?.packageName) {
            SystemUIHookEntry(lpparam)
        }
    }

}
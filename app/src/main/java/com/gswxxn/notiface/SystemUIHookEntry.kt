package com.gswxxn.notiface

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class SystemUIHookEntry(lpparam: XC_LoadPackage.LoadPackageParam) {
    private val mKeyguardUpdateMonitor by lazy {
        XposedHelpers.findClass("com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader)
    }
    private val mNotificationEntry by lazy {
        XposedHelpers.findClass(
            "com.android.systemui.statusbar.notification.collection.NotificationEntry",
            lpparam.classLoader
        )
    }
    private val mDependency by lazy {
        XposedHelpers.findClass("com.android.systemui.Dependency", lpparam.classLoader)
    }
    private val mNotificationViewHierarchyManager by lazy {
        XposedHelpers.findClass("com.android.systemui.statusbar.NotificationViewHierarchyManager", lpparam.classLoader)
    }
    private val mMiuiFaceUnlockManager by lazy {
        XposedHelpers.findClass("com.android.keyguard.faceunlock.MiuiFaceUnlockManager", lpparam.classLoader)
    }
    private val mKeyguardUpdateMonitorInjector by lazy {
        XposedHelpers.findClass("com.android.keyguard.injector.KeyguardUpdateMonitorInjector", lpparam.classLoader)
    }
    var flag = false

    init {
        XposedHelpers.findAndHookMethod(mNotificationEntry, "setSensitive",
            Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    if (flag) {
                        param!!.args[0] = false
                    }
                }
            })

        XposedHelpers.findAndHookMethod(mKeyguardUpdateMonitorInjector, "isFaceUnlock",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    super.beforeHookedMethod(param)
                    if (flag) {
                        param!!.result = true
                    }
                }
            })

        XposedHelpers.findAndHookMethod(mKeyguardUpdateMonitor, "onFaceAuthenticated",
            Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType,
            object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any? {
                    flag = true

                    XposedHelpers.callStaticMethod(mDependency, "get", mNotificationViewHierarchyManager)
                        .let { XposedHelpers.callMethod(it, "updateNotificationViews") }

                    XposedHelpers.callStaticMethod(mDependency, "get", mMiuiFaceUnlockManager)
                        .let { XposedHelpers.callMethod(it, "updateFaceUnlockView") }

                    flag = false
                    return null
                }
            })
    }
}
package com.gswxxn.notiface.wrapper

import android.animation.AnimatorSet
import android.content.Context
import android.graphics.drawable.Animatable
import android.widget.ImageView
import de.robv.android.xposed.XposedHelpers

class MiuiKeyguardFaceUnlockViewWrapper(private val instance: Any) {

    val mFaceUnlockAnimation = XposedHelpers.getObjectField(instance, "mFaceUnlockAnimation") as AnimatorSet?

    val mRingIV = XposedHelpers.getObjectField(instance, "mRingIV") as ImageView

    val mFaceIV = XposedHelpers.getObjectField(instance, "mFaceIV") as ImageView

    private val mUpdateMonitor: Any = XposedHelpers.getObjectField(instance, "mUpdateMonitor")

    val mLightLockWallpaper = XposedHelpers.getBooleanField(instance, "mLightLockWallpaper")

    val mSuccessAnimationListener: Any = XposedHelpers.getObjectField(instance, "mSuccessAnimationListener")

    val mContext = XposedHelpers.getObjectField(instance, "mContext") as Context

    var mSuccessfulAnimation
        get() = XposedHelpers.getObjectField(instance, "mSuccessfulAnimation") as Animatable
        set(value) = XposedHelpers.setObjectField(instance, "mSuccessfulAnimation", value)


    fun getContext() = XposedHelpers.callMethod(instance, "getContext") as Context

    fun setContentDescription(str: String) {
        XposedHelpers.callMethod(instance, "setContentDescription", str)
    }

    fun isPrimaryBouncerIsOrWillBeShowing() = XposedHelpers.callMethod(mUpdateMonitor, "isPrimaryBouncerIsOrWillBeShowing") as Boolean

}
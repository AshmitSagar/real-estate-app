package com.example.xmlrealestate.ui

import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.provider.Settings

internal fun propertyPhotoName(houseId: Int) = "property-photo-$houseId"

internal fun Context.catalogMotionEnabled(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        ValueAnimator.areAnimatorsEnabled()
    } else {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }

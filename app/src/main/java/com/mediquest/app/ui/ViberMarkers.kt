package com.mediquest.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.mediquest.app.R

object ViberMarkers {

    fun getIconForLevel(context: Context, level: Int): BitmapDescriptor {
        val resId = when (level) {
            in 1..4 -> R.drawable.ic_viber_level_1
            else -> R.drawable.ic_viber_level_1 // Por enquanto todos usam o mesmo base, mas podemos criar mais
        }
        
        val vectorDrawable = ContextCompat.getDrawable(context, resId) ?: return BitmapDescriptorFactory.defaultMarker()
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

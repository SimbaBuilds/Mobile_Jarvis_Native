package com.cameronhightower.mobilejarvisnative.utils

import android.content.Context
import android.widget.Toast

/**
 * Utility functions for UI interactions
 */
object UIUtils {
    /**
     * Show a toast message
     * 
     * @param context Application context
     * @param message Message to display
     * @param duration Toast duration (Toast.LENGTH_SHORT or Toast.LENGTH_LONG)
     */
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }
}

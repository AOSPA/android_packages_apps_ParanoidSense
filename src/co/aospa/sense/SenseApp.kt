package co.aospa.sense

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.util.Log
import co.aospa.sense.activities.EnrollActivity
import co.aospa.sense.util.Util

class SenseApp : Application() {

    override fun onCreate() {
        if (Util.IS_DEBUG_LOGGING) Log.d(TAG, "onCreate")
        super.onCreate()
        app = this
        packageManager.setComponentEnabledSetting(
            ComponentName(this, EnrollActivity::class.java),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        Util.setFaceUnlockAvailable(applicationContext)
    }

    override fun onTerminate() {
        if (Util.IS_DEBUG_LOGGING) {
            Log.d(TAG, "onTerminate")
        }
        super.onTerminate()
    }

    companion object {
        private const val TAG = "SenseApp"
        var app: SenseApp? = null
    }
}
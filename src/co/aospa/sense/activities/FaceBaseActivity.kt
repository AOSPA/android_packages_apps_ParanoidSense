package co.aospa.sense.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import co.aospa.sense.R
import co.aospa.sense.util.Constants

open class FaceBaseActivity : Activity() {

    private var mLaunchedConfirmLock = false
    @JvmField
    protected var mToken: ByteArray? = null
    protected var mUserId = 0

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setTheme(R.style.Theme_Sense_NoActionBar)
        mToken = intent.getByteArrayExtra(Constants.EXTRA_KEY_CHALLENGE_TOKEN)
        if (bundle != null && mToken == null) {
            mLaunchedConfirmLock = bundle.getBoolean(Constants.EXTRA_KEY_LAUNCHED_CONFIRM)
            mToken = bundle.getByteArray(Constants.EXTRA_KEY_CHALLENGE_TOKEN)
            mUserId = bundle.getInt(Intent.EXTRA_USER_ID)
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putBoolean(Constants.EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock)
        bundle.putByteArray(Constants.EXTRA_KEY_CHALLENGE_TOKEN, mToken)
    }

    protected fun parseIntent(intent: Intent) {
        intent.putExtra(Constants.EXTRA_KEY_CHALLENGE_TOKEN, mToken)
        intent.putExtra(Intent.EXTRA_USER_ID, mUserId)
    }
}
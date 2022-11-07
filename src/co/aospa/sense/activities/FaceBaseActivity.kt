package co.aospa.sense.activities

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import co.aospa.sense.R
import co.aospa.sense.util.Constants
import com.google.android.setupdesign.GlifLayout

abstract class FaceBaseActivity : FragmentActivity() {

    private var mLaunchedConfirmLock = false
    @JvmField
    protected var mToken: ByteArray? = null
    protected var mUserId = 0

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setTheme(R.style.SudThemeGlifV4)
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

    abstract fun getLayout(): GlifLayout

    open fun setHeaderText(res: Int) {
        val header: TextView = getLayout().headerTextView
        val previous: CharSequence = header.text
        val current = getText(res)
        if (previous !== current) {
            if (!TextUtils.isEmpty(current)) {
                header.accessibilityLiveRegion = 1
            }
            getLayout().headerText = current
            title = current
        }
    }

}
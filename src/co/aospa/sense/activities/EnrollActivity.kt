package co.aospa.sense.activities

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.res.Resources
import android.hardware.Camera
import android.hardware.face.FaceManager
import android.media.AudioAttributes
import android.os.*
import android.text.TextUtils
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.aospa.sense.R
import co.aospa.sense.controller.FaceEnrollController
import co.aospa.sense.controller.FaceEnrollController.CameraCallback
import co.aospa.sense.util.Constants
import co.aospa.sense.util.PreferenceHelper
import co.aospa.sense.util.Util
import co.aospa.sense.view.CircleSurfaceView
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.ThemeHelper


open class EnrollActivity : FaceBaseActivity() {

    private var mEnrollController: FaceEnrollController? = null
    private var mEnrollmentCancel = CancellationSignal()
    private var mFaceManager: FaceManager? = null
    private var mHandler: Handler? = null
    private var mHandlerThread: HandlerThread? = null
    private var mPreferenceHelper: PreferenceHelper? = null
    private var mSurfaceView: CircleSurfaceView? = null
    private var mEnrollVendorMessage: TextView? = null
    private var mProgress = 0.0f
    private var mHasCameraPermission = false
    private var mIsActivityPaused = false
    private var mIsFaceDetected = false

    private val mCameraCallback: CameraCallback = object : CameraCallback {
        override fun handleSaveFeature(data: ByteArray?, width: Int, height: Int, angle: Int): Int {
            return -1
        }

        override fun setDetectArea(size: Camera.Size?) {}
        override fun handleSaveFeatureResult(result: Int) {
            runOnUiThread {
                var errorString = 0
                when (result) {
                    Constants.MSG_UNLOCK_FACE_SCALE_TOO_SMALL -> errorString =
                        R.string.unlock_failed_face_small
                    Constants.MSG_UNLOCK_FACE_SCALE_TOO_LARGE -> errorString =
                        R.string.unlock_failed_face_large
                    Constants.MSG_UNLOCK_FACE_OFFSET_LEFT, Constants.MSG_UNLOCK_FACE_OFFSET_RIGHT, Constants.MSG_UNLOCK_FACE_ROTATED_LEFT, Constants.MSG_UNLOCK_FACE_ROTATED_RIGHT -> mProgress += 10.0f
                    Constants.MSG_UNLOCK_KEEP -> {
                        mProgress += 10.0f
                        mIsFaceDetected = true
                    }
                    Constants.MSG_UNLOCK_FACE_MULTI -> errorString =
                        R.string.unlock_failed_face_multi
                    Constants.MSG_UNLOCK_FACE_BLUR -> errorString = R.string.unlock_failed_face_blur
                    Constants.MSG_UNLOCK_FACE_NOT_COMPLETE -> errorString =
                        R.string.unlock_failed_face_not_complete
                    Constants.MSG_UNLOCK_DARKLIGHT -> errorString = R.string.attr_light_dark
                    Constants.MSG_UNLOCK_HIGHLIGHT -> errorString = R.string.attr_light_high
                    Constants.MSG_UNLOCK_HALF_SHADOW -> errorString = R.string.attr_light_shadow
                }
                if (mProgress < 100.0f) {
                    if (mIsFaceDetected) {
                        if (mProgress >= 60.0f) {
                            mProgress = 60.0f
                        }
                        mSurfaceView!!.setProgress(mProgress)
                    }
                    if (errorString != 0) {
                        mEnrollVendorMessage!!.setText(errorString)
                    }
                }
            }
        }

        override fun onTimeout() {
            if (!isFinishing && !isDestroyed) {
                if (Util.isFaceUnlockAvailable(this@EnrollActivity)) {
                    startFinishActivity()
                    return
                }
                if (!mIsActivityPaused) {
                    val intent = Intent()
                    intent.setClass(this@EnrollActivity, TryAgainActivity::class.java)
                    parseIntent(intent)
                    startActivity(intent)
                }
                finish()
            }
        }

        override fun onCameraError() {
            if (!isFinishing && !isDestroyed) {
                if (!mIsActivityPaused) {
                    val intent = Intent()
                    intent.setClass(this@EnrollActivity, TryAgainActivity::class.java)
                    parseIntent(intent)
                    startActivity(intent)
                }
                finish()
            }
        }

        override fun onFaceDetected() {
            mIsFaceDetected = true
            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(CLICK_VIBRATION, SONIFICATION)
        }
    }
    private val mEnrollmentCallback: FaceManager.EnrollmentCallback =
        object : FaceManager.EnrollmentCallback() {
            override fun onEnrollmentProgress(progress: Int) {
                if (progress == 0) {
                    runOnUiThread {
                        mProgress = 100.0f
                        mSurfaceView!!.setProgress(mProgress)
                        try {
                            if (mEnrollVendorMessage != null) {
                                mEnrollVendorMessage!!.text = ""
                            }
                            Handler(Looper.getMainLooper()).postDelayed({
                                val enrollActivity = this@EnrollActivity
                                if (!enrollActivity.isDestroyed) {
                                    startFinishActivity()
                                }
                            }, 2000)
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }

            override fun onEnrollmentHelp(helpMessageId: Int, charSequence: CharSequence) {
                runOnUiThread {
                    if (!TextUtils.isEmpty(charSequence)) {
                        mEnrollVendorMessage!!.text = charSequence
                    }
                }
            }

            override fun onEnrollmentError(errorMessageId: Int, charSequence: CharSequence) {
                if (!mIsActivityPaused) {
                    val intent = Intent()
                    intent.setClass(this@EnrollActivity, TryAgainActivity::class.java)
                    if (errorMessageId != Constants.MSG_UNLOCK_FAILED) {
                        parseIntent(intent)
                    } else {
                        setResult(-1)
                    }
                    startActivity(intent)
                }
                finish()
            }
        }

    override fun onCreate(bundle: Bundle?) {
        ThemeHelper.applyTheme(this)
        ThemeHelper.trySetDynamicColor(this)
        super.onCreate(bundle)
        mPreferenceHelper = PreferenceHelper(this)
        mHasCameraPermission = false
        mFaceManager = getSystemService(FaceManager::class.java)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != 0) {
            val shouldShowRequestPermissionRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.CAMERA
                )
            if (Util.IS_DEBUG_LOGGING) Log.i(
                TAG,
                "shouldShowRequestPermissionRationale: $shouldShowRequestPermissionRationale"
            )
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 0)
            return
        }
        synchronized(this) {
            mHasCameraPermission = true
            if (Util.IS_DEBUG_LOGGING) Log.i(TAG, "hasCameraPermission: > M")
        }
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true)
        super.onApplyThemeResource(theme, resid, first)
    }

    private fun init() {
        setContentView(R.layout.face_enroll)
        setHeaderText(R.string.face_enroll_title)
        getLayout().setDescriptionText(R.string.face_enroll_description)
        mSurfaceView = findViewById(R.id.camera_surface)
        mProgress = 0.0f
        mSurfaceView?.setProgress(0.0f)
        /**mSurfaceView.postDelayed(new Runnable() {
         * @Override
         * public void run() {
         * if (mIsFaceDetected) {
         * mProgress += 5.0f;
         * }
         * if (mProgress < 60.0f) {
         * mSurfaceView.setProgress(mProgress);
         * mSurfaceView.postDelayed(this, 500);
         * }
         * }
         * }, 500); */
        if (mEnrollController == null) {
            mEnrollController = FaceEnrollController.instance
            mEnrollController?.setSurfaceHolder(mSurfaceView?.holder)
        }
        if (mToken != null && mToken!!.isNotEmpty()) {
            mFaceManager!!.enroll(
                Util.getUserId(this),
                mToken,
                mEnrollmentCancel,
                mEnrollmentCallback,
                intArrayOf(1)
            )
        }
        mEnrollController!!.start(mCameraCallback, 15000)
        mEnrollVendorMessage = findViewById(R.id.face_vendor_message)
    }

    override fun getLayout(): GlifLayout {
        return findViewById(R.id.face_enroll)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy")
        if (mHandler != null) {
            mHandler!!.post {
                Log.i(TAG, "onDestroy handlerThread.quit()")
                mHandlerThread!!.quit()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        mIsActivityPaused = true
        if (mEnrollController != null) {
            mEnrollController!!.setSurfaceHolder(null)
            mEnrollController!!.stop(mCameraCallback)
            mEnrollController = null
        }
        mEnrollmentCancel.cancel()
    }

    override fun onResume() {
        super.onResume()
        mIsActivityPaused = false
        synchronized(this) {
            if (mHasCameraPermission) {
                if (Util.IS_DEBUG_LOGGING) {
                    Log.i(TAG, "onResume")
                }
                init()
            }
        }
    }

    private fun startFinishActivity() {
        val intent = Intent()
        if (mToken != null) {
            intent.putExtra(Constants.EXTRA_KEY_CHALLENGE_TOKEN, mToken)
        }
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId)
        }
        val enrollFinish = ComponentName.unflattenFromString(
            "com.android.settings/com.android.settings.biometrics.face.FaceEnrollFinish"
        )
        intent.component = enrollFinish
        startActivityForResult(intent, 1)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            setResult(resultCode)
            finish()
        } else if (requestCode == 2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != 0) {
                if (Util.IS_DEBUG_LOGGING) Log.i(TAG, "REQUEST_CAMERA finish")
                finish()
                return
            }
            if (Util.IS_DEBUG_LOGGING) Log.i(TAG, "REQUEST_CAMERA init")
            synchronized(this) {
                if (Util.IS_DEBUG_LOGGING) Log.i(TAG, "hasCameraPermission: REQUEST_CAMERA")
                mHasCameraPermission = true
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0) {
            synchronized(this) { mHasCameraPermission = true }
        } else if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CAMERA
            )
        ) {
            Toast.makeText(this@EnrollActivity, "Missing camera permission", Toast.LENGTH_SHORT)
                .show()
        } else {
            finish()
        }
    }

    companion object {
        private val TAG = EnrollActivity::class.java.simpleName
        private val CLICK_VIBRATION: VibrationEffect = VibrationEffect.get(0)
        private val SONIFICATION = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()
    }
}
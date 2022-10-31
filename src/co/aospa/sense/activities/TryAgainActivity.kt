package co.aospa.sense.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import co.aospa.sense.R

class TryAgainActivity : FaceBaseActivity() {

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContentView(R.layout.activity_try_again)
        val buttonTryAgain = findViewById<Button>(R.id.try_again_button)
        buttonTryAgain.setOnClickListener {
            val intent = Intent()
            intent.setClass(this@TryAgainActivity, EnrollActivity::class.java)
            parseIntent(intent)
            startActivity(intent)
            finish()
        }
        if (mToken == null) {
            buttonTryAgain.visibility = View.INVISIBLE
        }
    }

    public override fun onPause() {
        super.onPause()
        finish()
    }
}
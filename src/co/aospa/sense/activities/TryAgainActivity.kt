package co.aospa.sense.activities

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.View
import co.aospa.sense.R
import com.google.android.setupcompat.template.FooterBarMixin
import com.google.android.setupcompat.template.FooterButton
import com.google.android.setupdesign.GlifLayout
import com.google.android.setupdesign.util.ThemeHelper


class TryAgainActivity : FaceBaseActivity() {

    override fun onCreate(bundle: Bundle?) {
        ThemeHelper.applyTheme(this);
        ThemeHelper.trySetDynamicColor(this);
        super.onCreate(bundle)
        setContentView(R.layout.face_enroll_try_again)
        setHeaderText(R.string.face_try_again_title)
        getLayout().setDescriptionText(R.string.face_try_again_description)

        val footerBarMixin = getLayout().getMixin(FooterBarMixin::class.java) as FooterBarMixin
        footerBarMixin.primaryButton =
            FooterButton.Builder(this)
                .setText(R.string.btn_try_again)
                .setListener { setTryAgainButton() }
                .setButtonType(FooterButton.ButtonType.OTHER)
                .setTheme(R.style.SudGlifButton_Primary)
                .build()
        if (mToken == null) {
            footerBarMixin.primaryButton.visibility = View.INVISIBLE
        }
    }

    override fun onApplyThemeResource(theme: Resources.Theme, resid: Int, first: Boolean) {
        theme.applyStyle(R.style.SetupWizardPartnerResource, true)
        super.onApplyThemeResource(theme, resid, first)
    }

    override fun getLayout(): GlifLayout {
        return findViewById(R.id.face_enroll_try_again)
    }

    public override fun onPause() {
        super.onPause()
        finish()
    }

    private fun setTryAgainButton() {
        val intent = Intent()
        intent.setClass(this@TryAgainActivity, EnrollActivity::class.java)
        parseIntent(intent)
        startActivity(intent)
        finish()
    }

}
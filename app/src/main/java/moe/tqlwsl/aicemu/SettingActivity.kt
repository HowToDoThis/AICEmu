package moe.tqlwsl.aicemu

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import org.lsposed.hiddenapibypass.HiddenApiBypass

class SettingActivity : AppCompatActivity() {
    private var isHCEFSupported: Boolean = false
    private var isHCEFUnlocked: Boolean = false
    val TAG: String = "AICEmu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        isHCEFSupported =
            packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)
        Log.d(TAG, "isHCEFSupported:$isHCEFSupported")

        val textHCEF = findViewById<TextView>(R.id.hcef_support_text)
        if (isHCEFSupported) {
            textHCEF.setText(R.string.HCEF_support_true)
            textHCEF.setTextColor(Color.GREEN)
        } else {
            textHCEF.setText(R.string.HCEF_support_false)
            textHCEF.setTextColor(Color.RED)
        }

        if (isHCEFSupported) {
            val textUnlocker = findViewById<TextView>(R.id.unlocker_work_text)
            try {
                val globalVar = this.applicationContext as GlobalVar
                isHCEFUnlocked = globalVar.isHCEFUnlocked
                Log.d(TAG, "isHCEFUnlocked:$isHCEFUnlocked")
                if (isHCEFUnlocked) {
                    textUnlocker.setText(R.string.Unlocker_work_true)
                    textUnlocker.setTextColor(Color.GREEN)
                } else {
                    textUnlocker.setText(R.string.Unlocker_work_false)
                    textUnlocker.setTextColor(Color.RED)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                textUnlocker.setText(R.string.Unlocker_work_error)
            }
        }
    }

    companion object {
        @SuppressLint("SoonBlockedPrivateApi")
        private fun isValidSystemCode(systemCode: String): Boolean {
            val clazz = Class.forName("android.nfc.cardemulation.NfcFCardEmulation")
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                val method = clazz.getDeclaredMethod("isValidSystemCode", String::class.java)
                method.invoke(null, systemCode) as Boolean
            } else {
                HiddenApiBypass.invoke(clazz, null, "isValidSystemCode", systemCode) as Boolean
            }
        }
    }
}
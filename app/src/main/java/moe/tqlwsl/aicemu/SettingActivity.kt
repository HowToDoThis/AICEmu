package moe.tqlwsl.aicemu

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method

class SettingActivity : AppCompatActivity() {
    private var isHCEFSupported: Boolean = false
    private var isHCEFUnlocked: Boolean = false
    private var pmmtoolStatus: String? = ""
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

//            val textPmmtool = findViewById<TextView>(R.id.pmmtool_work_text)
//            pmmtoolStatus = getProperty("tmp.AICEmu.pmmtool");
//            if (pmmtoolStatus == "") {
//                textPmmtool.setText(R.string.Pmmtool_work_false)
//                textPmmtool.setTextColor(Color.RED)
//            }
//            else if (pmmtoolStatus == "0") {
//                textPmmtool.setText(R.string.Pmmtool_work_hook_failed)
//                textPmmtool.setTextColor(Color.RED)
//            }
//            else if (pmmtoolStatus == "1") {
//                textPmmtool.setText(R.string.Pmmtool_work_true)
//                textPmmtool.setTextColor(Color.GREEN)
//            }

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

        @SuppressLint("PrivateApi")
        fun getProperty(key: String?): String? {
            try {
                val c = Class.forName("android.os.SystemProperties")
                val set: Method = c.getMethod("get", String::class.java)
                return set.invoke(c, key)?.toString()
            } catch (e: java.lang.Exception) {
                Log.d("AICEmu", "getProperty exception")
                e.printStackTrace()
                return ""
            }
        }
    }
}
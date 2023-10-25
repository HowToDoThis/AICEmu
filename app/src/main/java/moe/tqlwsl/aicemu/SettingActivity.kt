package moe.tqlwsl.aicemu

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.lang.reflect.Method

class SettingActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences
    private var isHCEFSupported: Boolean = false
    private var isHCEFUnlocked: Boolean = false
    private var pmmtoolStatus: String? = ""
    val TAG: String = "AICEmu"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        isHCEFSupported = packageManager.hasSystemFeature(
            PackageManager.FEATURE_NFC_HOST_CARD_EMULATION_NFCF)
        Log.d(TAG, "[Setting] isHCEFSupported: $isHCEFSupported")

        val textHCEF = findViewById<TextView>(R.id.hcef_support_text)
        textHCEF.setText((when (isHCEFSupported) {
            true -> R.string.HCEF_support_true
            else -> R.string.HCEF_support_false
        }))
        textHCEF.setTextColor((if (isHCEFSupported) Color.GREEN else Color.RED))

        val textUnlocker = findViewById<TextView>(R.id.unlocker_work_text)
        if (isHCEFSupported) {
            try {
                val globalVar = this.applicationContext as GlobalVar
                isHCEFUnlocked = globalVar.isHCEFUnlocked
                Log.d(TAG, "[Setting] isHCEFUnlocked: $isHCEFUnlocked")
                textUnlocker.setText((when (isHCEFUnlocked) {
                    true -> R.string.Unlocker_work_true
                    else -> R.string.Unlocker_work_false
                }))
                textUnlocker.setTextColor((if (isHCEFUnlocked) Color.GREEN else Color.RED))
            } catch (e: Exception) {
                e.printStackTrace()
                textUnlocker.setText(R.string.Unlocker_work_error)
                textUnlocker.setTextColor(Color.RED)
            }

            val textPmmtool = findViewById<TextView>(R.id.pmmtool_work_text)
            pmmtoolStatus = getProperty("tmp.AICEmu.pmmtool")
            Log.d(TAG, "[Setting] loadPmmtool: $pmmtoolStatus")
            textPmmtool.setText((when (pmmtoolStatus) {
                "" -> R.string.Pmmtool_work_hook_failed
                "0" -> R.string.Pmmtool_work_false
                else -> R.string.Pmmtool_work_true
            }))
            textPmmtool.setTextColor((if (pmmtoolStatus == "1") Color.GREEN else Color.RED))

            val pmmtoolSwitch = findViewById<SwitchCompat>(R.id.pmmtool_switch)
            pmmtoolSwitch.isChecked = java.lang.Boolean.parseBoolean(getProperty("tmp.AICEmu.loadPmmtool"))
            pmmtoolSwitch.setOnCheckedChangeListener { _, isChecked ->
                setProperty("tmp.AICEmu.loadPmmtool", isChecked.toString())
                setProperty("tmp.AICEmu.pmmtool", "")
                Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -9 $(su -c pidof com.android.nfc)"))
            }
        }
    }

    companion object {
        @SuppressLint("PrivateApi")
        fun getProperty(key: String?): String? {
            return try {
                val c = Class.forName("android.os.SystemProperties")
                val set: Method = c.getMethod("get", String::class.java)
                set.invoke(c, key)?.toString()
            } catch (e: java.lang.Exception) {
                Log.d("AICEmu", "getProperty exception")
                e.printStackTrace()
                ""
            }
        }

        @SuppressLint("PrivateApi")
        fun setProperty(key: String?, value: String?) {
            try {
                val c = Class.forName("android.os.SystemProperties")
                val set: Method = c.getMethod("set", String::class.java, String::class.java)
                set.invoke(c, key, value)?.toString()
            } catch (e: java.lang.Exception) {
                Log.d("AICEmu", "setProperty exception")
                e.printStackTrace()
            }
        }
    }
}
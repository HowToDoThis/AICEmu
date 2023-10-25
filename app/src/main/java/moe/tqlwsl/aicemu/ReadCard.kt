package moe.tqlwsl.aicemu;

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast


class ReadCard : Activity(), NfcAdapter.ReaderCallback{

    private var nfcAdapter: NfcAdapter? = null
    private val TAG = "AICEmu-ReadCard"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.add_card)
        val closeButton = this.findViewById<ImageButton>(R.id.pop_up_close_button)
        closeButton.setOnClickListener { finish() }
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.let {
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000)
            it.enableReaderMode(this, this, NfcAdapter.FLAG_READER_NFC_F, options)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onTagDiscovered(tag: Tag) {
        val nfcF = NfcF.get(tag)
        nfcF?.let {
            val idm = it.tag.id
            val sysCode = it.systemCode.toHexString(false)
            val idmString = idm.toHexString(false)

            Log.d(TAG, "[TAG FOUND] IDm: $idmString, SysCode: $sysCode")
            runOnUiThread {
                Toast.makeText(this, "IDm: $idmString, SysCode: $sysCode", Toast.LENGTH_LONG).show()
            }
            val intent = Intent("moe.tqlwsl.aicemu.READ_CARD")
            intent.putExtra("Card_IDm", idmString)
            sendBroadcast(intent)
            finish()
        }
    }
    fun ByteArray.toHexString(hasSpace: Boolean = true) = this.joinToString("") {
        (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() + if (hasSpace) " " else ""
    }
}

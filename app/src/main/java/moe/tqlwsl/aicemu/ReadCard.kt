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
    private val TAG = "AICEmu"

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
            // 设置前台调度系统
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
            val idmString = toHexString(idm)

            Log.d(TAG, "IDm: $idmString")
            runOnUiThread {
                Toast.makeText(this, "IDm: $idmString", Toast.LENGTH_LONG).show()
            }
            val intent = Intent("moe.tqlwsl.aicemu.READ_CARD")
            intent.putExtra("Card_IDm", idmString)
            sendBroadcast(intent)
            finish()
        }
    }

    private fun toHexString(data: ByteArray): String {
        val sb = StringBuilder()
        for (b in data) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }
}

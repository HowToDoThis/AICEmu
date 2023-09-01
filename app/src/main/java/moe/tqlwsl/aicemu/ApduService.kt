package moe.tqlwsl.aicemu

import android.nfc.cardemulation.HostApduService
import android.os.Bundle


class ApduService : HostApduService() {
    override fun onDeactivated(reason: Int) {
        // placeholder
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle): ByteArray {
        // placeholder
        return ByteArray(0)
    }
}
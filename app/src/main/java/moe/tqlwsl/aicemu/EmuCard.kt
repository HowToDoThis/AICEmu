package moe.tqlwsl.aicemu

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle
import android.util.Log
import android.widget.Toast


class EmuCard : HostNfcFService() {
    private lateinit var card: FelicaCard
    private var TAG: String = "AICEmu-EmuCard"

    // byte utils
    fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
    fun ByteArray.toHexString(hasSpace: Boolean = true) = this.joinToString("") {
        (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() + if (hasSpace) " " else ""
    }


    // resp utils
    fun packResponse(respType: Byte, nfcid2: ByteArray, payload: ByteArray): ByteArray {
        var resp = ByteArray(1) + respType + nfcid2 + payload
        resp[0] = resp.size.toByte()
        return resp
    }

    override fun processNfcFPacket(commandPacket: ByteArray, extras: Bundle?): ByteArray? {
        val commandHexStr = commandPacket.toHexString()
        Log.d(TAG, "processNfcFPacket NFCF")
        Log.d(TAG, "received $commandHexStr")
        //Toast.makeText(this, "received $commandHexStr", Toast.LENGTH_LONG).show()

        if (commandPacket.size < 1 + 1 + 8 || (commandPacket.size.toByte() != commandPacket[0])) {
            Log.e(TAG, "processNfcFPacket: packet size error")
            return null
        }

        val nfcid2 = ByteArray(8)
        System.arraycopy(commandPacket, 2, nfcid2, 0, 8)
//        val myNfcid2 =
//            byteArrayOfInts(0x02, 0xFE, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
//        if (!Arrays.equals(myNfcid2, nfcid2)) {
//            Log.e(TAG, "processNfcFPacket: nfcid2 error")
//            return null
//        }

        if (commandPacket[1] == 0x06.toByte()) { // READ BLK
            val blockNum = commandPacket[13].toInt()
            val payload = ByteArray(2 + 1 + 16 * blockNum)
            payload[2] = blockNum.toByte()
            for (i in 0 until blockNum) {
                val id = commandPacket[13 + 2 + 2 * i]
                card.readBlock(id)?.let { System.arraycopy(it, 0, payload, 3 + 16 * i, 16) }
            }
            val resp = packResponse(0x07.toByte(), nfcid2, payload)
            val respHexStr = resp.toHexString()
            Log.d(TAG, "send $respHexStr")
            Toast.makeText(this, "Scanned", Toast.LENGTH_LONG).show()
            //Toast.makeText(this, "received $commandHexStr\n\nsend $respHexStr", Toast.LENGTH_LONG).show()
            return resp
        }
        else if (commandPacket[1] == 0x08.toByte()) { // WRITE BLK // not implemented
            val payload = ByteArray(2)
            val resp = packResponse(0x09.toByte(), nfcid2, payload)
            val respHexStr = resp.toHexString()
            Log.d(TAG, "send $respHexStr")
            //Toast.makeText(this, "received $commandHexStr\n\nsend $respHexStr", Toast.LENGTH_LONG).show()
            return resp
        }


        return byteArrayOfInts(0x04, 0x11, 0x45, 0x14)
        // sendResponsePacket(byteArrayOfInts(0x04, 0x11, 0x45, 0x14))
        // return null
    }



    override fun onCreate() {
        Log.d(TAG, "onCreate NFCF")
        super.onCreate()
        val globalVar = this.applicationContext as GlobalVar
        card = FelicaCard(this, globalVar.IDm)
        // Toast.makeText(this, "onCreate", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy NFCF")
        super.onDestroy()
        // Toast.makeText(this, "onDestroy", Toast.LENGTH_LONG).show()
    }
    override fun onDeactivated(reason: Int) {
        Log.d(TAG, "onDeactivated NFCF")
        // Toast.makeText(this, "onDeactivated", Toast.LENGTH_LONG).show()
    }
}

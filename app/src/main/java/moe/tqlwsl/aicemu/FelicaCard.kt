package moe.tqlwsl.aicemu


import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class FelicaCard(context: Context, IDm: String) {
    private var cardData = mutableMapOf<String, String>()
    private val gson = Gson()
    private val TAG = "AICEmu"
    private val fileName = "felica_template.json"

    init {
        Log.d(TAG, "Load Felica Data from assets/$fileName")
        val fileContent = readAssetFile(context, fileName)
        val mutableBlock = object : TypeToken<MutableMap<String, String>>() {}.type
        try {
            val jsonData = gson.fromJson<MutableMap<String, String>>(fileContent, mutableBlock)
            if (jsonData != null) {
                cardData = jsonData
                val block82 = ByteArray(16)
                val IDmBytes = IDm.decodeHex()
                System.arraycopy(IDmBytes, 0, block82, 0, 8)
                cardData["82"] = block82.toHexString()
            }
            Log.d(TAG,"[Felica Card Data]")
            Log.d(TAG, "=".repeat(53))
            for (en in cardData.entries) {
                Log.d(TAG,"[${en.key}]: ${en.value}")
            }
            Log.d(TAG, "=".repeat(53))
        } catch (e: IOException) {
            Log.e(TAG, "assets/$fileName Read Error")
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "assets/$fileName Syntax Error")
        }
    }

    // utils
    fun String.decodeHex(): ByteArray =
        this.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    fun ByteArray.toHexString(hasSpace: Boolean = true) = this.joinToString("") {
        (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() + if (hasSpace) " " else ""
    }

    fun readBlock(id: Byte): ByteArray? {
        var idStr = (id.toInt() and 0xFF).toString(16).padStart(2, '0')
        Log.d("HCEFService", "read block [$idStr]")
        var blockStr = cardData[idStr]
        if (blockStr == null) {
            idStr = idStr.uppercase()
            blockStr = cardData[idStr]
        }
        if (blockStr != null) {
            val blockData = blockStr.decodeHex()
            val resp = ByteArray(16)
            System.arraycopy(blockData, 0, resp, 0, blockData.size)
            return resp
        }
        Log.e(TAG, "Invalid Block $idStr")
        return null
    }

    fun writeBlock(id: Byte, data: ByteArray) { }

    private fun readAssetFile(context: Context, fileName: String): String {
        val stringBuilder = StringBuilder()
        val assetManager = context.assets
        val inputStream = assetManager.open(fileName)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        bufferedReader.forEachLine { line ->
            stringBuilder.append(line)
            stringBuilder.append("\n")
        }
        bufferedReader.close()
        inputStream.close()
        return stringBuilder.toString()
    }
}
package moe.tqlwsl.aicemu


import android.app.Application
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader

class FelicaCard(IDm: String) {
    private var cardData = mutableMapOf<String, String>()
    private val gson = Gson()

    init {
//        instance = this
//        val fileName = "felica_template.json"
//        Log.d("HCEFService", "Load Felica Data from assets/$fileName")
//        val fileContent = readAssetFile(applicationContext, fileName)
//        val mutableBlock = object : TypeToken<MutableMap<String, String>>() {}.type
//        try {
//            val jsonData = gson.fromJson<MutableMap<String, String>>(fileContent, mutableBlock)
//            if (jsonData != null) {
//                cardData = jsonData
//                val block82 = ByteArray(16)
//                val IDmBytes = IDm.decodeHex()
//                System.arraycopy(IDmBytes, 0, block82, 0, 16)
//                cardData["82"] = block82.toHexString()
//            }
        val block82 = ByteArray(16)
        Log.d("HCEFService","Felica Card $IDm")
        val IDmBytes = IDm.decodeHex()
        System.arraycopy(IDmBytes, 0, block82, 0, 8)
        cardData["82"] = block82.toHexString()
        val block = ByteArray(16)
        cardData["80"] = block.toHexString()
        cardData["86"] = block.toHexString()
        cardData["90"] = block.toHexString()
        cardData["91"] = block.toHexString()
        cardData["00"] = block.toHexString()
        Log.d("HCEFService","Felica Card Data")
        Log.d("HCEFService","=".repeat(53))
        for (en in cardData.entries) {
            Log.d("HCEFService","[${en.key}]: ${en.value}")
        }
        Log.d("HCEFService","=".repeat(53))
//        } catch (e: IOException) {
//            Log.e("Error", "File Read Error")
//        } catch (e: JsonSyntaxException) {
//            Log.e("Error", "File Syntax Error")
//        }
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

        Log.e("Error", "Invalid Block")
        return null
    }

    fun writeBlock(id: Byte, data: ByteArray) { }

    fun readAssetFile(context: Context, fileName: String): String {
        val stringBuilder = StringBuilder()

        // 获取AssetManager
        val assetManager = context.assets

        // 通过AssetManager打开文件
        val inputStream = assetManager.open(fileName)
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        // 逐行读取文件内容并将其添加到StringBuilder中
        bufferedReader.forEachLine { line ->
            stringBuilder.append(line)
            stringBuilder.append("\n")
        }

        // 关闭流资源
        bufferedReader.close()
        inputStream.close()

        return stringBuilder.toString()
    }
}
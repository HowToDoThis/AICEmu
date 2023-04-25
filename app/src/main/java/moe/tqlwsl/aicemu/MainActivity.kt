package moe.tqlwsl.aicemu

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.nfc.NfcAdapter
import android.nfc.cardemulation.NfcFCardEmulation
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import moe.tqlwsl.aicemu.databinding.ActivityMainBinding



internal data class Card(val name: String, val idm: String)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar
    private lateinit var readCardBroadcastReceiver: ReadCardBroadcastReceiver
    private lateinit var nfcFComponentName: ComponentName
    private lateinit var jsonFile: File
    private var nfcAdapter: NfcAdapter? = null
    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private var nfcPendingIntent: PendingIntent? = null
    private val gson = Gson()
    private var cards = mutableListOf<Card>()
    private val cardsJsonPath = "card.json"
    private var showCardID: Boolean = false
    private val TAG = "AICEmu"


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        // ui
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        findViewById<FloatingActionButton>(R.id.fab_add_card).setOnClickListener {
            val readCardIntent = Intent(this, ReadCard::class.java)
            startActivity(readCardIntent)
        }

        // load json file
        jsonFile = File(filesDir, cardsJsonPath)
        loadCards()

        // read card callback
        readCardBroadcastReceiver = ReadCardBroadcastReceiver()
        val intentFilter = IntentFilter("moe.tqlwsl.aicemu.READ_CARD")
        registerReceiver(readCardBroadcastReceiver, intentFilter)

        // check nfc
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        if (nfcAdapter == null) {
            Log.e(TAG, "NFC not supported")
            AlertDialog.Builder(this)
                .setTitle(R.string.error).setMessage("NFC not supported").setCancelable(false).show()
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            Log.e(TAG, "NFC is off")
            AlertDialog.Builder(this)
                .setTitle(R.string.error).setMessage("NFC is off").setCancelable(false).show()
            return
        }

        // add pendingintent in order not to read tag at home
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        nfcPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)


        // load hcefservice
        nfcFCardEmulation = NfcFCardEmulation.getInstance(nfcAdapter)
        nfcFComponentName = ComponentName(
            "moe.tqlwsl.aicemu",
            "moe.tqlwsl.aicemu.EmuCard"
        )
    }

    override fun onResume() {
        super.onResume()
        if (nfcPendingIntent != null) {
            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        }
        nfcFCardEmulation?.enableService(this, nfcFComponentName)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        nfcFCardEmulation?.disableService(this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(readCardBroadcastReceiver)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_menu_hide_id -> {
                showCardID = !showCardID
                item.title = if (showCardID) {
                    "Hide IDm"
                }
                else {
                    "Show IDm"
                }
                checkCardIDShadow()
                true
            }
            R.id.toolbar_menu_settings -> {
                Toast.makeText(applicationContext, "还没做（）\nUnder constuction...", Toast.LENGTH_LONG).show()
                // TODO
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkCardIDShadow() {
        val cardsLayout: ViewGroup = findViewById(R.id.mainList)
        for (i in 0 until cardsLayout.childCount) {
            val child = cardsLayout.getChildAt(i)
            if (child is CardView) {
                val idView = child.findViewById<TextView>(R.id.card_id)
                val idShadowView = child.findViewById<TextView>(R.id.card_id_shadow)
                if (showCardID) {
                    idView.visibility = View.VISIBLE
                    idShadowView.visibility = View.GONE
                }
                else {
                    idView.visibility = View.GONE
                    idShadowView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showCardMenu(v: View, cardView: View) {
        val popupMenu = PopupMenu(this, v)
        popupMenu.inflate(R.menu.card_menu)
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.card_menu_rename -> {
                    val textView = cardView.findViewById<TextView>(R.id.card_name)
                    val editText = cardView.findViewById<EditText>(R.id.card_name_edit)
                    textView.visibility = View.GONE
                    editText.visibility = View.VISIBLE
                    editText.setText(textView.text)
                    editText.requestFocus()
                    editText.setOnFocusChangeListener { _, hasFocus ->
                        if (!hasFocus) {
                            textView.text = editText.text
                            textView.visibility = View.VISIBLE
                            editText.visibility = View.GONE
                            saveCards()
                        }
                    }
                    true
                }
                R.id.card_menu_delete -> {
                    val parentLayout = cardView.parent as? ViewGroup
                    parentLayout?.removeView(cardView)
                    saveCards()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }




    private fun setIDm(idm: String): Boolean {
        nfcFCardEmulation?.disableService(this)
        val resultIdm = nfcFCardEmulation?.setNfcid2ForService(nfcFComponentName, idm)
        nfcFCardEmulation?.enableService(this, nfcFComponentName)
        return resultIdm == true
    }

    private fun setSys(sys: String): Boolean {
        nfcFCardEmulation?.disableService(this)
        val resultSys = nfcFCardEmulation?.registerSystemCodeForService(nfcFComponentName, sys)
        nfcFCardEmulation?.enableService(this, nfcFComponentName)
        return resultSys == true
    }

    private fun emuCard(cardView: View) {
        val globalVar = this.applicationContext as GlobalVar
        val cardIDmTextView = cardView.findViewById<TextView>(R.id.card_id)
        globalVar.IDm = cardIDmTextView.text.toString()
        //val resultIdm = setIDm(IDm)
        val resultIdm = setIDm("02fe000000000000") // hardcoded idm for sbga
        val resultSys = setSys("88B4") // hardcoded syscode for sbga

        val cardNameTextView = cardView.findViewById<TextView>(R.id.card_name)
        val cardName = cardNameTextView.text
        if (!resultIdm) {
            Toast.makeText(applicationContext, "Error IDm", Toast.LENGTH_LONG).show()
        }
        if (!resultSys) {
            Toast.makeText(applicationContext, "Error Sys", Toast.LENGTH_LONG).show()
        }
        if (resultIdm && resultSys) {
            Toast.makeText(applicationContext, "正在模拟$cardName...", Toast.LENGTH_LONG).show()
        }
    }


    private fun addCard(name: String, IDm: String?) {
        val cardsLayout: ViewGroup = findViewById(R.id.mainList)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cardView = inflater.inflate(R.layout.card, cardsLayout, false)
        cardsLayout.addView(cardView)
        val nameTextView = cardView.findViewById<TextView>(R.id.card_name)
        nameTextView.text = name
        val IDmTextView = cardView.findViewById<TextView>(R.id.card_id)
        IDmTextView.text = IDm
        checkCardIDShadow()
        val menuButton: ImageButton = cardView.findViewById(R.id.card_menu_button)
        menuButton.setOnClickListener {
            showCardMenu(it, cardView)
        }
        cardView.setOnTouchListener { v, _ ->
            val editText = v.findViewById<EditText>(R.id.card_name_edit)
            editText.clearFocus()
            emuCard(v)
            v.performClick()
            true
        }
    }

    private fun loadCards() {
        val mutableListCard = object : TypeToken<MutableList<Card>>() {}.type
        try {
            val jsonCards = gson.fromJson<MutableList<Card>>(jsonFile.readText(), mutableListCard)
            if (jsonCards != null) {
                cards = jsonCards
            }
        } catch (e: IOException) {
            Log.e(TAG, "$cardsJsonPath Read Error")
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "$cardsJsonPath Syntax Error")
        }
        val mainLayout: ViewGroup = findViewById(R.id.mainList)
        mainLayout.removeAllViews()
        for (card in cards) {
            addCard(card.name, card.idm)
        }
    }


    private fun saveCards() {
        cards.clear()
        val mainLayout: ViewGroup = findViewById(R.id.mainList)
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is CardView) {
                val nameView = child.findViewById<TextView>(R.id.card_name)
                val idView = child.findViewById<TextView>(R.id.card_id)
                cards.add(Card(nameView.text.toString(), idView.text as String))
            }
        }
        try {
            jsonFile.writeText(gson.toJson(cards).toString())
        } catch (e: IOException) {
            Log.e(TAG, "$cardsJsonPath Write Error")
        }
    }

    inner class ReadCardBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val idmString = intent.getStringExtra("Card_IDm")
            addCard("AIC Card", idmString)
            saveCards()
        }
    }

}
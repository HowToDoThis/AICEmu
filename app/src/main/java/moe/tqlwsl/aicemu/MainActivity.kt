package moe.tqlwsl.aicemu

import android.app.AlertDialog
import android.app.PendingIntent
import android.content.*
import android.nfc.NfcAdapter
import android.nfc.cardemulation.CardEmulation
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
import moe.tqlwsl.aicemu.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException


internal data class Card(val name: String, val idm: String)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar
    private lateinit var readCardBroadcastReceiver: ReadCardBroadcastReceiver
    private lateinit var nfcFComponentName: ComponentName
    private lateinit var jsonFile: File
    private lateinit var prefs: SharedPreferences
    private var nfcAdapter: NfcAdapter? = null
    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private var nfcPendingIntent: PendingIntent? = null
    private var cards = mutableListOf<Card>()
    private val gson = Gson()
    private val cardsJsonPath = "card.json"
    private val TAG = "AICEmu"
    private var showCardID: Boolean = false
    private var compatibleID: Boolean = false
    private var currentCardId: Int = -1


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
                .setTitle(R.string.error).setMessage(R.string.nfc_not_supported).setCancelable(true).show()
            return
        }
        if (!nfcAdapter!!.isEnabled) {
            Log.e(TAG, "NFC is off")
            AlertDialog.Builder(this)
                .setTitle(R.string.error).setMessage(R.string.nfc_not_on).setCancelable(true).show()
            return
        }

        // set default payment app
        var cardEmulation = CardEmulation.getInstance(nfcAdapter)
        val componentName = ComponentName(applicationContext, ApduService::class.java)
        val isDefault =
            cardEmulation.isDefaultServiceForCategory(componentName, CardEmulation.CATEGORY_PAYMENT)
        if (!isDefault) {
            val intent = Intent(CardEmulation.ACTION_CHANGE_DEFAULT)
            intent.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT)
            intent.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, componentName)
            startActivity(intent)
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

        // setting prefs
        prefs = applicationContext.getSharedPreferences("AICEmu", Context.MODE_WORLD_READABLE)
        currentCardId = prefs.getInt("currentCardId", -1)
        compatibleID = prefs.getBoolean("compatibleID", false)
        val compatibleButton: Button = findViewById(R.id.button_compatible)
        compatibleButton.text = (if (compatibleID) {
            getString(R.string.mode_compatible)
        }
        else {
            getString(R.string.mode_commmon)
        })
        compatibleButton.setOnClickListener {
            switchCompatible()
        }
    }

    override fun onResume() {
        super.onResume()
        if (nfcPendingIntent != null) {
            nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
        }
        nfcFCardEmulation?.enableService(this, nfcFComponentName)
        emuCurrentCard()
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
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (menu != null) {
            menu.findItem(R.id.toolbar_menu_compatible).setTitle(if (compatibleID) {
                R.string.compatible_off
            }
            else {
                R.string.compatible_on
            })
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.toolbar_menu_hide_id -> {
                showCardID = !showCardID
                item.setTitle(if (showCardID) {
                    R.string.hide_idm
                }
                else {
                    R.string.show_idm
                })
                checkCardIDShadow()
                true
            }
            R.id.toolbar_menu_compatible -> {
                switchCompatible()
                true
            }
            R.id.toolbar_menu_add_test_card -> {
                addCard("Test Card", "012e000000114514")
                true
            }
            R.id.toolbar_menu_settings -> {
                // Toast.makeText(applicationContext, "还没做完（）\nUnder constuction...", Toast.LENGTH_LONG).show()
                val settingIntent = Intent(this, SettingActivity::class.java)
                startActivity(settingIntent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun switchCompatible() {
        compatibleID = !compatibleID
        val compatibleButton: Button = findViewById(R.id.button_compatible)
        compatibleButton.text = (if (compatibleID) {
            getString(R.string.mode_compatible)
        }
        else {
            getString(R.string.mode_commmon)
        })
        val editor = prefs.edit()
        editor.putBoolean("compatibleID", compatibleID)
        editor.apply()
        emuCurrentCard()
    }

    private fun checkCardIDShadow() {
        val cardsLayout: ViewGroup = findViewById(R.id.mainList)
        for (i in 0 until cardsLayout.childCount) {
            val child = cardsLayout.getChildAt(i)
            if (child is CardView) {
                val idView = child.findViewById<TextView>(R.id.card_id)
                // val idShadowView = child.findViewById<TextView>(R.id.card_id_shadow)
                if (showCardID) {
                    idView.visibility = View.VISIBLE
                    // idShadowView.visibility = View.GONE
                }
                else {
                    idView.visibility = View.GONE
                    // idShadowView.visibility = View.VISIBLE
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

    private fun emuCurrentCard() {
        if (currentCardId != -1) {
            val cardsLayout: ViewGroup = findViewById(R.id.mainList)
            val currentCard = cardsLayout.getChildAt(currentCardId)
            if (currentCard != null) {
                emuCardview(currentCard)
            }
            else {
                currentCardId = -1
                val editor = prefs.edit()
                editor.putInt("currentCardId", currentCardId)
                editor.apply()
            }
        }
    }

    private fun emuCardview(cardView: View) {
        val cardIDmTextView = cardView.findViewById<TextView>(R.id.card_id)
        val cardNameTextView = cardView.findViewById<TextView>(R.id.card_name)
        emuCard(cardIDmTextView.text.toString(), cardNameTextView.text.toString())

        val mainLayout: ViewGroup = findViewById(R.id.mainList)
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is CardView) {
                val emuMark: ImageButton = child.findViewById(R.id.card_emu_mark_on)
                emuMark.visibility = View.GONE
                if (child == cardView) {
                    currentCardId = i
                    val editor = prefs.edit()
                    editor.putInt("currentCardId", currentCardId)
                    editor.apply()
                }
            }
        }
        val emuMark: ImageButton = cardView.findViewById(R.id.card_emu_mark_on)
        emuMark.visibility = View.VISIBLE
    }

    private fun emuCard(cardId: String, cardName: String) {
        val globalVar = this.applicationContext as GlobalVar
        globalVar.IDm = cardId
        var resultIdm = if (compatibleID) {
            // hardcoded idm for specific model e.g. Samsung S8
            // idm needs to start with 02, or syscode won't be added to polling ack
            // konmai reader reads this idm while sbga reader does not check this
            setIDm("02fe001145141919")
        }
        else {
            setIDm(globalVar.IDm)
        }
        val resultSys = setSys("88B4") // hardcoded syscode for sbga
        globalVar.isHCEFUnlocked = resultSys

        if (!resultIdm) {
            Toast.makeText(applicationContext, "Error IDm", Toast.LENGTH_LONG).show()
        }
        if (!resultSys) {
            Toast.makeText(applicationContext, "Error Sys", Toast.LENGTH_LONG).show()
        }
        if (resultIdm && resultSys) {
            if (compatibleID) {
                Toast.makeText(applicationContext, getString(R.string.Emulating_compatible, cardName), Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(applicationContext, getString(R.string.Emulating_common, cardName), Toast.LENGTH_LONG).show()
            }
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
        val emuButton: ImageButton = cardView.findViewById(R.id.card_emu_mark)
        emuButton.setOnClickListener {
            emuCardview(cardView)
        }
        cardView.setOnTouchListener { v, _ ->
            val editText = v.findViewById<EditText>(R.id.card_name_edit)
            editText.clearFocus()
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
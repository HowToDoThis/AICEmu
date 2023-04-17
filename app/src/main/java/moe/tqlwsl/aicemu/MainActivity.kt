package moe.tqlwsl.aicemu

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
import androidx.navigation.ui.AppBarConfiguration
import com.google.android.material.floatingactionbutton.FloatingActionButton
import moe.tqlwsl.aicemu.databinding.ActivityMainBinding
import java.io.File
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.io.IOException


internal data class Card(val name: String, val idm: String)

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar
    private var readCardBroadcastReceiver: ReadCardBroadcastReceiver? = null
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private val gson = Gson()
    private var cards = mutableListOf<Card>()
    private val jsonPath = "card.json"
    private lateinit var jsonFile: File
    private var showCardID: Boolean = false
    private var nfcFCardEmulation: NfcFCardEmulation? = null
    private var myComponentName: ComponentName? = null

    private val TAG = "AICEmu"


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // fab
        findViewById<FloatingActionButton>(R.id.fab_add_card).setOnClickListener {
            val readCardIntent = Intent(this, ReadCard::class.java)
            startActivity(readCardIntent)
        }

        // read card callback
        readCardBroadcastReceiver = ReadCardBroadcastReceiver()
        val intentFilter = IntentFilter("moe.tqlwsl.aicemu.READ_CARD")
        registerReceiver(readCardBroadcastReceiver, intentFilter)

        //  add intent in order not to read tag twice time
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        // load json file
        jsonFile = File(filesDir, jsonPath)
        loadCards()

        //
        nfcFCardEmulation = NfcFCardEmulation.getInstance(nfcAdapter)
        myComponentName = ComponentName(
            "moe.tqlwsl.aicemu",
            "moe.tqlwsl.aicemu.EmuCard"
        )
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null)
        val orgSys = nfcFCardEmulation?.getSystemCodeForService(myComponentName)
        if (orgSys != null) {
            setSys(orgSys)
        }
        nfcFCardEmulation?.enableService(this, myComponentName)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
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
                val mainLayout: ViewGroup = findViewById(R.id.mainList)
                if (showCardID) {
                    showCardID = false
                    for (i in 0 until mainLayout.childCount) {
                        val child = mainLayout.getChildAt(i)
                        if (child is CardView) {
                            val idView = child.findViewById<TextView>(R.id.card_id)
                            val idShadowView = child.findViewById<TextView>(R.id.card_id_shadow)
                            idView.visibility = View.GONE
                            idShadowView.visibility = View.VISIBLE
                        }
                   }
                }
                else {
                    showCardID = true
                    for (i in 0 until mainLayout.childCount) {
                        val child = mainLayout.getChildAt(i)
                        if (child is CardView) {
                            val idView = child.findViewById<TextView>(R.id.card_id)
                            val idShadowView = child.findViewById<TextView>(R.id.card_id_shadow)
                            idView.visibility = View.VISIBLE
                            idShadowView.visibility = View.GONE
                        }
                    }
                }
                true
            }
            R.id.toolbar_menu_settings -> {
                Toast.makeText(applicationContext, "还没做（）\nUnder constuction...", Toast.LENGTH_LONG).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun setIDm(idm: String): Boolean {
        nfcFCardEmulation?.disableService(this)
        val resultIdm = nfcFCardEmulation?.setNfcid2ForService(myComponentName, idm)
        nfcFCardEmulation?.enableService(this, myComponentName)
        return resultIdm == true
    }

    private fun setSys(sys: String): Boolean {
        nfcFCardEmulation?.disableService(this)
        val resultSys = nfcFCardEmulation?.registerSystemCodeForService(myComponentName, sys)
        nfcFCardEmulation?.enableService(this, myComponentName)
        return resultSys == true
    }




    private fun addCard(name: String, IDm: String?) {
        val mainLayout: ViewGroup = findViewById(R.id.mainList)
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val cardView = inflater.inflate(R.layout.card, mainLayout, false)
        mainLayout.addView(cardView)
        val nameTextView = cardView.findViewById<TextView>(R.id.card_name)
        nameTextView.text = name
        val IDmTextView = cardView.findViewById<TextView>(R.id.card_id)
        IDmTextView.text = IDm
        val menuButton: ImageButton = cardView.findViewById(R.id.card_menu_button)
        menuButton.setOnClickListener {
            showPopupMenu(it, cardView)
        }
        cardView.setOnTouchListener { v, _ ->

            val globalVar = this.applicationContext as GlobalVar
            val IDmTextView = v.findViewById<TextView>(R.id.card_id)
            globalVar.IDm = IDmTextView.text.toString()

            //setIDm(IDm)
            val resultIdm = setIDm("02fe000000000000")
            val resultSys = setSys("88B4")


            val nameTextView = v.findViewById<TextView>(R.id.card_name)
            nameTextView.text = name
            if (!resultIdm) {
                Toast.makeText(applicationContext, "Error IDm", Toast.LENGTH_LONG ).show()
            }
            if (!resultSys) {
                Toast.makeText(applicationContext, "Error Sys", Toast.LENGTH_LONG ).show()
            }
            if (resultIdm && resultSys) {
                Toast.makeText(applicationContext, "正在模拟$name...", Toast.LENGTH_LONG).show()
            }
            v.performClick()
            true
        }
    }

    private fun showPopupMenu(v: View, cardView: View) {
        val popupMenu = PopupMenu(this, v)
        popupMenu.inflate(R.menu.card_menu) // 加载菜单资源文件
        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.card_menu_rename -> {
                    val textView = cardView.findViewById<TextView>(R.id.card_name)
                    val editText = cardView.findViewById<EditText>(R.id.card_name_edit)
                    textView.visibility = View.GONE
                    editText.visibility = View.VISIBLE
                    editText.setText(textView.text)
                    editText.requestFocus()
                    editText.setOnFocusChangeListener { v, hasFocus ->
                        if (!hasFocus) {
                            textView.text = editText.text
                            textView.visibility = View.VISIBLE
                            editText.visibility = View.GONE
                            saveCards()
                        }
                    }
                    cardView.setOnTouchListener { v, _ ->
                        editText.clearFocus()
                        v.performClick()
                        true
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
        popupMenu.show() // 显示PopupMenu
    }


    private fun loadCards() {
        val mutableListCard = object : TypeToken<MutableList<Card>>() {}.type
        try {
            val jsonCards = gson.fromJson<MutableList<Card>>(jsonFile.readText(), mutableListCard)
            if (jsonCards != null) {
                cards = jsonCards
            }
        } catch (e: IOException) {
            Log.e("Error", "Save File Read Error")
        } catch (e: JsonSyntaxException) {
            Log.e("Error", "Save File Syntax Error")
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
            Log.e("Error", "Save File Write Error")
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
package com.vestfiber.inventorycounting

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.vestfiber.inventorycounting.DAL.*
import com.vestfiber.inventorycounting.datawedge.DWInterface
import com.vestfiber.inventorycounting.datawedge.DWReceiver
import com.vestfiber.inventorycounting.datawedge.ObservableObject
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.*
import com.vestfiber.inventorycounting.DAL.Result

class MainActivity : AppCompatActivity(), Observer, View.OnClickListener,
    VfNumberAdapter.ViewHolder.OnBatchListener {

    private var vfs: List<VfBatchNumber> = arrayListOf()
    private var version65OrOver = false
    private val dwInterface = DWInterface()
    private val receiver = DWReceiver()
    private val observableObject = ObservableObject
    private var countingId = 0
    private var initialized = false
    private var scannedObjects: ArrayList<ScannedObject> = arrayListOf()
    private var adapter = VfNumberAdapter(scannedObjects, this)
    private lateinit var recyclerView: RecyclerView
    private lateinit var mediaPlayer: MediaPlayer

    companion object {
        const val PROFILE_NAME = "InventoryScanVF"
        const val PROFILE_INTENT_ACTION = "com.vestfiber.inventorycounting.SCAN"
        const val PROFILE_INTENT_START_ACTIVITY = "2"
        const val HISTORY_FILE_NAME = "VF_Inventory_"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val saveButton = findViewById<Button>(R.id.saveButton)
        val clearButton = findViewById<Button>(R.id.clearButton)
        saveButton.setOnClickListener(this)
        clearButton.setOnClickListener(this)

        countingId = intent.getIntExtra(EXTRA_MESSAGE, 0)

        if (countingId == 0){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Błąd")
            builder.setMessage("Brak Id liczenia, skontaktuj sie z lekarzem lub farmaceutą")
            builder.setNeutralButton(R.string.ok) { dialog, _ ->

                val intent = Intent(this, CountingsActivity::class.java)
                dialog.dismiss()
                startActivity(intent)
            }

            builder.show()
        }
        else{
            getLoadedVfs()
        }

        observableObject.instance.addObserver(this)

        //  Register broadcast receiver to listen for responses from DW API
        val intentFilter = IntentFilter()
        intentFilter.addCategory(DWInterface.DATAWEDGE_RETURN_CATEGORY)
        intentFilter.addAction(PROFILE_INTENT_ACTION)
        registerReceiver(receiver, intentFilter)

        createDataWedgeProfile()
        mediaPlayer = MediaPlayer.create(this, R.raw.fart)
    }

    private fun getLoadedVfs() {
        lifecycleScope.launch{
            val result = VfService().listVfsOnScan(countingId)
            if (result is Result.Success<List<VfBatchNumber>>){
                runOnUiThread {
                    onResult(result.data)
                }
            }else if(result is Result.Error){
                Log.e("Error: ", result.exception.message!!)
                runOnUiThread {
                    onError(result.exception.message!!)
                }
            }
        }
    }

    private fun onError(exception: String) {
        val snackBarView = Snackbar.make(this.window.decorView.rootView, exception , Snackbar.LENGTH_LONG)
        val view = snackBarView.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        val tv = view.findViewById<View>(com.google.android.material.R.id.snackbar_text) as TextView
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        params.gravity = Gravity.TOP
        view.layoutParams = params
        view.background = ContextCompat.getDrawable(this,R.drawable.error_snack) // for custom background
        snackBarView.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        snackBarView.show()
    }

    private fun onSuccess(message: String) {
        val snackBarView = Snackbar.make(this.window.decorView.rootView, message , Snackbar.LENGTH_LONG)
        val view = snackBarView.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
//        view.background = ContextCompat.getDrawable(this,R.drawable.border) // for custom background
        snackBarView.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        snackBarView.show()

    }

    private fun onResult(data: List<VfBatchNumber>) {
        if (data.any()){
            vfs = data
        }
    }

    override fun update(o: Observable?, arg: Any?) {
        //  Invoked in response to the DWReceiver broadcast receiver
        val receivedIntent = arg as Intent
        //  This activity will only receive DataWedge version since that is all we ask for, the
        //  configuration activity is responsible for other return values such as enumerated scanners
        //  If the version is <= 6.5 we reduce the amount of configuration available.  There are
        //  smarter ways to do this, e.g. DW 6.4 introduces profile creation (without profile
        //  configuration) but to keep it simple, we just define a minimum of 6.5 for configuration
        //  functionality
        if (receivedIntent.hasExtra(DWInterface.DATAWEDGE_RETURN_VERSION)) {
            val version = receivedIntent.getBundleExtra(DWInterface.DATAWEDGE_RETURN_VERSION)
            val dataWedgeVersion =
                version?.getString(DWInterface.DATAWEDGE_RETURN_VERSION_DATAWEDGE)
            if (dataWedgeVersion != null && dataWedgeVersion >= "6.5" && !version65OrOver) {
                version65OrOver = true
                createDataWedgeProfile()
            }
        }
        else{
            onNewIntent(receivedIntent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.hasExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING) == true) {
            //  Handle scan intent received from DataWedge, add it to the list of scans
            val scanData = intent.getStringExtra(DWInterface.DATAWEDGE_SCAN_EXTRA_DATA_STRING)
            val isSame: (ScannedObject) -> Boolean = { it.Number == scanData }
            if (scannedObjects.any { isSame(it) })
                return

            if ((scanData?.startsWith("VF", ignoreCase = true)) == true) {
                val currentVf = VfBatchNumber(scanData, countingId)
                if (vfs.contains(currentVf)) currentVf.inC5 = true
                else mediaPlayer.start()
                scannedObjects.add(0, currentVf)
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)

            }
            else{
                if (!scanData!!.isDigitsOnly() || scanData.length > 7)
                    return
                val currentCoc = CocData(countingId, scanData)
                scannedObjects.add(0, currentCoc)
                adapter.notifyItemInserted(0)
                recyclerView.scrollToPosition(0)
            }
        }
    }

    private fun createDataWedgeProfile() {
        //  Create and configure the DataWedge profile associated with this application
        //  For readability's sake, I have not defined each of the keys in the DWInterface file
        val profileConfig = configureProfileSettings()
        configureBarcodeInput(profileConfig)
        dwInterface.sendCommandBundle(this, DWInterface.DATAWEDGE_SEND_SET_CONFIG, profileConfig)

        configureIntentOutput(profileConfig)

        disableKeyboardOutput(profileConfig)
    }

    private fun configureProfileSettings(): Bundle {
        dwInterface.sendCommandString(this, DWInterface.DATAWEDGE_SEND_CREATE_PROFILE, PROFILE_NAME)
        val profileConfig = Bundle()
        profileConfig.putString("PROFILE_NAME", PROFILE_NAME)
        profileConfig.putString("PROFILE_ENABLED", "true") //  These are all strings
        profileConfig.putString("CONFIG_MODE", "UPDATE")
        val appConfig = Bundle()
        appConfig.putString(
            "PACKAGE_NAME",
            packageName
        )      //  Associate the profile with this app
        appConfig.putStringArray(
            "ACTIVITY_LIST",
            arrayOf("com.vestfiber.inventorycounting.MainActivity")
        )
        profileConfig.putParcelableArray("APP_LIST", arrayOf(appConfig))
        return profileConfig
    }

    private fun configureBarcodeInput(profileConfig: Bundle) {
        val barcodeConfig = Bundle()
        barcodeConfig.putString("PLUGIN_NAME", "BARCODE")
        barcodeConfig.putString(
            "RESET_CONFIG",
            "true"
        ) //  This is the default but never hurts to specify
        val barcodeProps = Bundle()
        barcodeConfig.putBundle("PARAM_LIST", barcodeProps)
        profileConfig.putBundle("PLUGIN_CONFIG", barcodeConfig)
    }

    private fun configureIntentOutput(profileConfig: Bundle) {
        //  You can only configure one plugin at a time in some versions of DW, now do the intent output
        profileConfig.remove("PLUGIN_CONFIG")
        val intentConfig = Bundle()
        intentConfig.putString("PLUGIN_NAME", "INTENT")
        intentConfig.putString("RESET_CONFIG", "true")
        val intentProps = Bundle()
        intentProps.putString("intent_output_enabled", "true")
        intentProps.putString("intent_action", PROFILE_INTENT_ACTION)
        intentProps.putString("intent_delivery", PROFILE_INTENT_START_ACTIVITY)
        intentConfig.putBundle("PARAM_LIST", intentProps)
        profileConfig.putBundle("PLUGIN_CONFIG", intentConfig)
        dwInterface.sendCommandBundle(this, DWInterface.DATAWEDGE_SEND_SET_CONFIG, profileConfig)
    }

    private fun disableKeyboardOutput(profileConfig: Bundle) {
        //  Disable keyboard output
        profileConfig.remove("PLUGIN_CONFIG")
        val keystrokeConfig = Bundle()
        keystrokeConfig.putString("PLUGIN_NAME", "KEYSTROKE")
        keystrokeConfig.putString("RESET_CONFIG", "true")
        val keystrokeProps = Bundle()
        keystrokeProps.putString("keystroke_output_enabled", "false")
        keystrokeConfig.putBundle("PARAM_LIST", keystrokeProps)
        profileConfig.putBundle("PLUGIN_CONFIG", keystrokeConfig)
        dwInterface.sendCommandBundle(this, DWInterface.DATAWEDGE_SEND_SET_CONFIG, profileConfig)
    }

    override fun onResume() {
        super.onResume()

        //  initialized variable is a bit clunky but onResume() is called on each newIntent()
        if (!initialized) {
            //  Create profile to be associated with this application
            dwInterface.sendCommandString(this, DWInterface.DATAWEDGE_SEND_GET_VERSION, "")
            initialized = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    override fun onStart() {
        super.onStart()
        //  Persist the scanned barcodes
        readFile()
    }

    override fun onStop() {
        super.onStop()
        //  Persist the scanned barcodes
        writeFile()
    }

    private fun writeFile() {
        //  Persist the scans array to a file
        var objectOut: ObjectOutputStream? = null
        try {
            val fileOut =
                applicationContext.openFileOutput(HISTORY_FILE_NAME+countingId, Activity.MODE_PRIVATE)
            objectOut = ObjectOutputStream(fileOut)
            objectOut.writeObject(scannedObjects)
            fileOut.fd.sync()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (objectOut != null) {
                try {
                    objectOut.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    //  Credit: https://stackoverflow.com/questions/5816695/android-sharedpreferences-with-serializable-object
    private fun readFile() {
        //  Read in the previously persisted scans array, else create a new array if one does not exist
        var objectIn: ObjectInputStream? = null
        try {
            val fileIn = applicationContext.openFileInput(HISTORY_FILE_NAME+countingId)
            objectIn = ObjectInputStream(fileIn)
            @Suppress("UNCHECKED_CAST")
            scannedObjects = objectIn.readObject() as ArrayList<ScannedObject>
        } catch (e: FileNotFoundException) {
            scannedObjects = arrayListOf()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } catch (e: ClassCastException) {
            e.printStackTrace()
        } finally {
            if (objectIn != null) {
                try {
                    objectIn.close()
                } catch (e: IOException) {
                    // do nowt
                }

            }
        }
        adapter = VfNumberAdapter(scannedObjects, this)
        val recyclerView = findViewById<RecyclerView>(R.id.recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        adapter.notifyItemRangeInserted(0, scannedObjects.size)
    }

    override fun onClick(v: View?) {
        when(v?.id){
            R.id.clearButton -> {
                if (!scannedObjects.any()){
                    onError("Brak zeskanowanych VF")
                    return
                }
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Na pewno usunąć?")
                builder.setMessage("Czy na pewno usunąć wszystkie numery z listy?")
                builder.setPositiveButton(R.string.yes) { dialog, _ ->
                    clearScannedObjects()
                    dialog.dismiss()
                }
                builder.setNegativeButton(R.string.no) { dialog, _ ->
                    dialog.dismiss()
                }
                builder.show()
            }
            R.id.saveButton -> {
                if (!scannedObjects.any()){
                    onError("Brak zeskanowanych VF")
                    return
                }

                saveScannedobjects()
            }
        }
    }

    private fun saveScannedobjects() {
        lifecycleScope.launch{
            val result = VfService().saveScannedObjects(scannedObjects)
            if (result is Result.Success<String>){
                runOnUiThread {
                    onSuccess(result.data)
                    clearScannedObjects()
                }
            }else if(result is Result.Error){
                Log.e("Error: ", result.exception.message!!)
                runOnUiThread {
                    onError(result.exception.message!!)
                }
            }
        }
    }

    private fun clearScannedObjects() {
        adapter.notifyItemRangeRemoved(0, scannedObjects.size)
        scannedObjects.clear()
        writeFile()
    }

    override fun onBatchClick(position: Int) {
        val scanned = scannedObjects[position]

        if (scanned is VfBatchNumber){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Co zrobić z numerem?")
            builder.setMessage("Wybierz co zrobić.")

            if(scanned.notOK){
                builder.setPositiveButton("Oznacz jako OK") {dialog, _ ->
                    scanned.notOK = false
                    adapter.notifyItemChanged(position)
                    dialog.dismiss()
                }
            }
            else{
                builder.setPositiveButton("Oznacz jako błedne") {dialog, _ ->
                    scanned.notOK = true
                    adapter.notifyItemChanged(position)
                    dialog.dismiss()
                }
            }

            builder.setNegativeButton("Usuń"){dialog, _ ->
                scannedObjects.removeAt(position)
                adapter.notifyItemRemoved(position)
                dialog.dismiss()
            }

            builder.setNeutralButton("Anuluj"){dialog, _ ->
                dialog.dismiss()
            }
            builder.show()
        }
        else if(scanned is CocData){
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Co zrobić z COC?")
            builder.setMessage("Wybierz co zrobić.")

            builder.setPositiveButton("Usuń") {dialog, _ ->
                scannedObjects.removeAt(position)
                adapter.notifyItemRemoved(position)
                dialog.dismiss()
            }

            builder.setNegativeButton("Anuluj"){dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }
}
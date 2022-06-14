package com.vestfiber.inventorycounting

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.vestfiber.inventorycounting.DAL.CountingData
import com.vestfiber.inventorycounting.DAL.CountingsService
import com.vestfiber.inventorycounting.DAL.Result
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


const val EXTRA_MESSAGE = "com.vestfiber.inventorycounting.COUNTING"

class CountingsActivity : AppCompatActivity(), View.OnClickListener {

    private var countings: ArrayList<CountingData> = arrayListOf()
    private var adapter = CountingAdapter(countings)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_countings)

        val recyclerView = findViewById<RecyclerView>(R.id.countingsRecycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        adapter.onItemClick = { counting ->
            countingClick(counting)
        }
        val reloadButton = findViewById<Button>(R.id.reloadButton)
        reloadButton.setOnClickListener(this)
        getCountings()
    }

    private fun getCountings() {
        lifecycleScope.launch{
            val result = CountingsService().list()
            if (result is Result.Success<List<CountingData>>){
                runOnUiThread {
                    onResult(result.data)
                    onSuccess()
                }
            }else if(result is Result.Error){
                Log.e("Error: ", result.exception.message!!)
                runOnUiThread {
                    onError(result.exception.message!!)
                }
            }
        }
    }

    private fun onSuccess() {
        val snackBarView = Snackbar.make(this.window.decorView.rootView, "Załadowane dane z serwera" , Snackbar.LENGTH_LONG)
        val view = snackBarView.view
        val params = view.layoutParams as FrameLayout.LayoutParams
        params.gravity = Gravity.TOP
        view.layoutParams = params
//        view.background = ContextCompat.getDrawable(this,R.drawable.border) // for custom background
        snackBarView.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE
        snackBarView.show()
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

    private fun countingClick(counting: CountingData){
        val builder = AlertDialog.Builder(this)
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH)
        builder.setTitle("Otworzyć liczenie?")
        builder.setMessage("Czy otworzyć liczenie z ${dateFormatter.format(counting.countingDate)}")
        builder.setPositiveButton(R.string.yes) { dialog, _ ->
            val message = counting.id
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra(EXTRA_MESSAGE, message)
            }
            dialog.dismiss()
            startActivity(intent)
        }
        builder.setNegativeButton(R.string.no) { dialog, _ ->
            dialog.dismiss()
        }
        builder.show()
    }

    private fun onResult(result: List<CountingData>){
        adapter.notifyItemRangeRemoved(0, countings.size)
        countings.clear()
        countings.addAll(result)
        adapter.notifyItemRangeInserted(0, result.size)
    }

    override fun onClick(v: View?) {
        when (v?.id){
            R.id.reloadButton ->{
                getCountings()
            }
        }
    }


}
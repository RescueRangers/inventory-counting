package com.vestfiber.inventorycounting


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.view.menu.MenuView
import androidx.recyclerview.widget.RecyclerView
import com.vestfiber.inventorycounting.DAL.VfBatchNumber
import java.text.SimpleDateFormat
import java.util.*

class VfNumberAdapter(private val mList: List<VfBatchNumber>) :
    RecyclerView.Adapter<VfNumberAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)

        return ViewHolder(view)
    }

    inner class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        private val vfNumber: TextView = itemView.findViewById(R.id.vfNumberTextView)
        private val scanDate: TextView = itemView.findViewById(R.id.scanDateTextView)
        private val container: FrameLayout = itemView.findViewById(R.id.vfItem)


        fun bind(item: VfBatchNumber) {

            if (item.scanDate != null){
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH)
                scanDate.text = dateFormatter.format(item.scanDate)
            }

            vfNumber.text = item.vfNumber
            if (item.inC5){
                container.setBackgroundColor(Color.GREEN)
            }
            else{
                container.setBackgroundColor(Color.RED)
                vfNumber.setTextColor(Color.YELLOW)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val vfNumber = mList[position]
        holder.bind(vfNumber)
    }

    override fun getItemCount(): Int {
        return mList.size
    }
}
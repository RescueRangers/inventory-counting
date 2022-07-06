package com.vestfiber.inventorycounting


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.vestfiber.inventorycounting.DAL.CocData
import com.vestfiber.inventorycounting.DAL.ScannedObject
import com.vestfiber.inventorycounting.DAL.VfBatchNumber

class VfNumberAdapter(private val mList: List<ScannedObject>,
                      private val onBatchListener: ViewHolder.OnBatchListener
) :
    RecyclerView.Adapter<VfNumberAdapter.ViewHolder>() {
    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layout, parent, false)

        return ViewHolder(view, onBatchListener)
    }

    class ViewHolder(ItemView: View, private val onBatchListener: OnBatchListener)
        : RecyclerView.ViewHolder(ItemView), View.OnClickListener {
        private val vfNumber: TextView = itemView.findViewById(R.id.vfNumberTextView)
        private val notOkIcon: ImageView = itemView.findViewById(R.id.notOkIcon)
        private val container: FrameLayout = itemView.findViewById(R.id.vfItem)

        init {
            ItemView.setOnClickListener(this)
        }

        interface OnBatchListener{
            fun onBatchClick(position: Int)
        }

        fun bind(item: ScannedObject) {

            if (item is VfBatchNumber) {
                if (item.notOK) {
                    notOkIcon.visibility = View.VISIBLE
                }

                vfNumber.text = item.vfNumber
                if (item.inC5) {
                    container.setBackgroundColor(Color.GREEN)
                    vfNumber.setTextColor(Color.BLACK)
                } else {
                    container.setBackgroundColor(Color.YELLOW)
                    vfNumber.setTextColor(Color.BLACK)
                }
            }
            else if (item is CocData){
                vfNumber.text = item.cocNumber
                container.setBackgroundColor(Color.WHITE)
                vfNumber.setTextColor(Color.BLACK)
            }

        }

        override fun onClick(v: View?) {
            onBatchListener.onBatchClick(absoluteAdapterPosition)
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
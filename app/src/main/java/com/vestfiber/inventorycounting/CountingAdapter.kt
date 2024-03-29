package com.vestfiber.inventorycounting

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.vestfiber.inventorycounting.DAL.CountingData
import java.text.SimpleDateFormat
import java.util.*


class CountingAdapter(private val items: List<CountingData>,
                      private val onCountingListener: ViewHolder.OnCountingListener
):
    RecyclerView.Adapter<CountingAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onCountingViewListener(view: View?, position: Int)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.countings_layout, parent, false)

        return ViewHolder(view, onCountingListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class ViewHolder(itemView: View,
                     private val onCountingListener: OnCountingListener) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val date = itemView.findViewById<TextView>(R.id.scanTextView)
        init {
            itemView.setOnClickListener (this)
        }

        interface OnCountingListener{
            fun onCountingClick(position: Int)
        }

        fun bind(item: CountingData) {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)

            date.text = dateFormatter.format(item.countingDate)
        }

        override fun onClick(v: View?) {
            onCountingListener.onCountingClick(absoluteAdapterPosition)
        }
    }

}
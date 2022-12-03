package com.example.umd_gluten_free

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MyAdapter(private val mealList : ArrayList<Meal>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)

        return MyViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MyAdapter.MyViewHolder, position: Int) {
        val meal : Meal = mealList[position]
        holder.restaurantName.text = meal.name
        holder.location.text = meal.location

    }

    override fun getItemCount(): Int {
        return mealList.size
    }

    public class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val restaurantName : TextView = itemView.findViewById(R.id.name)
        val location : TextView = itemView.findViewById(R.id.location)
    }
}
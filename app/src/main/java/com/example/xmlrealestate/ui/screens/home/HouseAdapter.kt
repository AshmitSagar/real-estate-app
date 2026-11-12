package com.example.xmlrealestate.ui.screens.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.xmlrealestate.R
import com.example.xmlrealestate.common.Constants
import com.example.xmlrealestate.databinding.ItemHouseFactsBinding
import com.example.xmlrealestate.domain.model.House
import com.example.xmlrealestate.ui.displayPlace
import com.example.xmlrealestate.ui.displayPrice
import com.example.xmlrealestate.ui.displaySpecs

class HouseAdapter(
    private val actions: ListHousesActions
) : ListAdapter<House, HouseAdapter.HouseViewHolder>(DiffCallback) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
    }

    override fun getItemViewType(position: Int): Int =
        if (position == 0) R.layout.item_house else R.layout.item_house_compact

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return HouseViewHolder(view, actions)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        holder.bind(getItem(position), position == 0)
    }

    class HouseViewHolder(view: View, private val actions: ListHousesActions) :
        RecyclerView.ViewHolder(view) {

        private val facts = ItemHouseFactsBinding.bind(view.findViewById(R.id.houseFacts))
        private val image = view.findViewById<ImageView>(R.id.houseImageView)

        fun bind(house: House, featured: Boolean) {
            val context = itemView.context
            facts.housePrice.text = house.displayPrice()
            facts.housePrice.textSize = if (featured) 28f else 22f
            facts.housePlace.text = house.displayPlace(context)
            facts.houseSpecs.text = house.displaySpecs(context)
            image.load(Constants.IMAGE_RELATIVE_PATH + house.image) {
                placeholder(R.drawable.catalog_photo_placeholder)
                error(R.drawable.catalog_photo_placeholder)
            }
            itemView.setOnClickListener { actions.onHouseClicked(house) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<House>() {
        override fun areItemsTheSame(oldItem: House, newItem: House) = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: House, newItem: House) = oldItem == newItem
    }
}

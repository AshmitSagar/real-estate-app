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
import com.example.xmlrealestate.ui.propertyPhotoName

internal data class CatalogListing(val house: House, val featured: Boolean, val saved: Boolean)

internal class HouseAdapter(
    private val onOpen: (House, ImageView?) -> Unit,
    private val onSave: (Int) -> Unit
) : ListAdapter<CatalogListing, HouseAdapter.HouseViewHolder>(DiffCallback) {

    init {
        stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).house.id.toLong()

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).featured) R.layout.item_house else R.layout.item_house_compact

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HouseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        return HouseViewHolder(view)
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: HouseViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) onBindViewHolder(holder, position)
        else holder.bindSaved(getItem(position).saved)
    }

    inner class HouseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val facts = ItemHouseFactsBinding.bind(view.findViewById(R.id.houseFacts))
        private val image = view.findViewById<ImageView>(R.id.houseImageView)
        private val bookmark = view.findViewById<ImageView>(R.id.bookmarkProperty)
        private var loadedImage = false

        fun bind(listing: CatalogListing) {
            val house = listing.house
            val context = itemView.context
            facts.housePrice.text = house.displayPrice()
            facts.housePrice.textSize = if (listing.featured) 28f else 22f
            facts.housePlace.text = house.displayPlace(context)
            facts.houseSpecs.text = house.displaySpecs(context)
            val transitionName = propertyPhotoName(house.id)
            image.transitionName = transitionName
            image.tag = transitionName
            loadedImage = false
            image.load(Constants.IMAGE_RELATIVE_PATH + house.image) {
                placeholder(R.drawable.catalog_photo_placeholder)
                error(R.drawable.catalog_photo_placeholder)
                listener(onSuccess = { _, _ -> loadedImage = true })
            }
            itemView.setOnClickListener { onOpen(house, image.takeIf { loadedImage }) }
            bookmark.setOnClickListener { onSave(house.id) }
            bindSaved(listing.saved)
        }

        fun bindSaved(saved: Boolean) {
            bookmark.isSelected = saved
            bookmark.contentDescription = itemView.context.getString(
                if (saved) R.string.remove_saved_property else R.string.save_property
            )
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<CatalogListing>() {
        override fun areItemsTheSame(oldItem: CatalogListing, newItem: CatalogListing) =
            oldItem.house.id == newItem.house.id

        override fun areContentsTheSame(oldItem: CatalogListing, newItem: CatalogListing) = oldItem == newItem

        override fun getChangePayload(oldItem: CatalogListing, newItem: CatalogListing): Any? =
            Unit.takeIf { oldItem.copy(saved = newItem.saved) == newItem }
    }
}

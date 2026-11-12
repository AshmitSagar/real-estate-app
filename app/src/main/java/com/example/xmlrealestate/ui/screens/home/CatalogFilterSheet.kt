package com.example.xmlrealestate.ui.screens.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import com.example.xmlrealestate.R
import com.example.xmlrealestate.databinding.SheetCatalogFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class CatalogFilterSheet : BottomSheetDialogFragment() {
    private val catalog: ListHouseViewModel by viewModels({ requireParentFragment() })
    private var budget = 0
    private var bedrooms = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        SheetCatalogFiltersBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = SheetCatalogFiltersBinding.bind(view)
        val current = catalog.filters.value
        budget = savedInstanceState?.getInt(BUDGET_KEY) ?: current.maxPrice
        bedrooms = savedInstanceState?.getInt(BEDROOMS_KEY) ?: current.bedrooms

        fun updateCount() {
            val count = catalog.countMatching(current.copy(maxPrice = budget, bedrooms = bedrooms))
            val label = resources.getQuantityString(R.plurals.property_count, count, count)
            binding.applyFilters.text = getString(R.string.apply_property_filters, label)
        }

        populate(binding.budgetChoices, budgets, budget) {
            budget = it
            updateCount()
        }
        populate(binding.bedroomChoices, bedroomCounts, bedrooms) {
            bedrooms = it
            updateCount()
        }
        updateCount()
        binding.applyFilters.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(BUDGET_KEY to budget, BEDROOMS_KEY to bedrooms)
            )
            dismiss()
        }
    }

    private fun populate(group: ChipGroup, options: Map<Int, Int>, selection: Int, onSelect: (Int) -> Unit) {
        for ((value, label) in options) {
            val chip = layoutInflater.inflate(R.layout.item_filter_chip, group, false) as Chip
            chip.id = View.generateViewId()
            chip.tag = value
            chip.setText(label)
            group.addView(chip)
            if (value == selection) group.check(chip.id)
        }
        group.setOnCheckedStateChangeListener { _, ids ->
            ids.firstOrNull()?.let { onSelect(group.findViewById<Chip>(it).tag as Int) }
        }
    }

    override fun onStart() {
        super.onStart()
        val sheet = dialog as? BottomSheetDialog ?: return
        sheet.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        sheet.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(BUDGET_KEY, budget)
        outState.putInt(BEDROOMS_KEY, bedrooms)
    }

    internal companion object {
        const val RESULT_KEY = "catalogFilterResult"
        const val BUDGET_KEY = "budget"
        const val BEDROOMS_KEY = "bedrooms"

        val budgets = linkedMapOf(
            0 to R.string.any_budget,
            10_000_000 to R.string.budget_1_crore,
            15_000_000 to R.string.budget_1_5_crore,
            20_000_000 to R.string.budget_2_crore,
            30_000_000 to R.string.budget_3_crore
        )
        val bedroomCounts = linkedMapOf(
            0 to R.string.any_bedrooms,
            2 to R.string.two_bhk,
            3 to R.string.three_bhk,
            4 to R.string.four_bhk
        )
    }
}

package com.example.moneytracker.ui.add_transaction

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import com.example.moneytracker.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var txtDate: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton

    private val calendar: Calendar = Calendar.getInstance()
    private val sdf = SimpleDateFormat("EEEE, dd/MM/yyyy", Locale("vi"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.bottom_sheet_add_transaction,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        txtDate = view.findViewById(R.id.txtDate)
        btnPrev = view.findViewById(R.id.btnPrev)
        btnNext = view.findViewById(R.id.btnNext)

        updateDate()

        btnPrev.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            updateDate()
        }

        btnNext.setOnClickListener {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            updateDate()
        }

        txtDate.setOnClickListener {
            showDatePicker()
        }
    }

    private fun updateDate() {
        txtDate.text = sdf.format(calendar.time)
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                calendar.set(year, month, day)
                updateDate()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet =
            dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isDraggable = false
        }
    }
}
package io.github.hiromoo.kyutechtimetable

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.DialogFragment

class PickersDialog(
    context: Context,
    private val minValues: List<Int> = listOf(0),
    private val maxValues: List<Int> = listOf(0),
    private val values: List<Int> = listOf(0),
    private val displayedValuesGroup: List<List<String>?> = listOf(null),
    pickersNum: Int = 1
) : DialogFragment() {

    constructor(
        context: Context,
        minValue: Int,
        maxValue: Int,
        value: Int,
        displayedValues: List<String>? = null
    ) : this(
        context,
        listOf(minValue),
        listOf(maxValue),
        listOf(value),
        listOf(displayedValues)
    )

    private var numberPickers: List<NumberPicker> = List(pickersNum) { NumberPicker(context) }
    private var listener: OnClickPositiveButtonListener? = null

    fun interface OnClickPositiveButtonListener {
        fun onClick(dialog: DialogInterface?, id: Int, values: List<Int>)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater = requireActivity().layoutInflater
        val dialogView: View = inflater.inflate(R.layout.number_picker_container, null)
        val dialogContainer: LinearLayout = dialogView.findViewById(R.id.picker_container)

        for (i in numberPickers.indices) {
            numberPickers[i].run {
                minValue = this@PickersDialog.minValues[i]
                maxValue = this@PickersDialog.maxValues[i]
                value = this@PickersDialog.values[i]
                this@PickersDialog.displayedValuesGroup[i]?.let {
                    displayedValues = it.toTypedArray()
                }
            }
        }

        for (numberPicker in numberPickers) {
            dialogContainer.addView(numberPicker, LayoutParams(WRAP_CONTENT, MATCH_PARENT))
        }

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(dialogView) // Add action buttons
            .setPositiveButton(R.string.ok) { dialog, id ->
                listener?.onClick(dialog, id, numberPickers.map { it.value })
            }
            .setNegativeButton(R.string.cancel, null)

        return builder.create()
    }

    fun setOnClickPositiveButtonListener(listener: OnClickPositiveButtonListener) {
        this.listener = listener
    }
}
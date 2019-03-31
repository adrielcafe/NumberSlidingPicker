package it.sephiroth.android.library.numberpicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.AppCompatImageButton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class NumberPicker @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = R.attr.pickerStyle,
        defStyleRes: Int = R.style.NumberPicker_Filled) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    interface OnNumberPickerChangeListener {
        fun onProgressChanged(numberPicker: NumberPicker, progress: Int, fromUser: Boolean)
        fun onStartTrackingTouch(numberPicker: NumberPicker)
        fun onStopTrackingTouch(numberPicker: NumberPicker)
    }

    var numberPickerChangeListener: OnNumberPickerChangeListener? = null

    private lateinit var editText: EditText
    private lateinit var upButton: AppCompatImageButton
    private lateinit var downButton: AppCompatImageButton

    private var arrowStyle: Int
    private var editTextStyleId: Int

    private var maxDistance: Int

    private lateinit var data: Data

    @Suppress("MemberVisibilityCanBePrivate")
    fun setProgress(value: Int, fromUser: Boolean = true) {
        if (value != data.value) {
            data.value = value

            if (editText.text.toString() != data.value.toString())
                editText.setText(data.value.toString())

            numberPickerChangeListener?.onProgressChanged(this, progress, fromUser)
        }
    }

    var progress: Int
        get() = data.value
        set(value) = setProgress(value, false)

    var minValue: Int
        get() = data.minValue
        set(value) {
            data.minValue = value
        }

    var maxValue: Int
        get() = data.maxValue
        set(value) {
            data.maxValue = value
        }

    var stepSize: Int
        get() = data.stepSize
        set(value) {
            data.stepSize = value
        }

    init {
        setWillNotDraw(false)
        isFocusable = true
        isFocusableInTouchMode = true

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        val array = context.theme.obtainStyledAttributes(attrs, R.styleable.NumberPicker, defStyleAttr, defStyleRes)
        try {
            val maxValue = array.getInteger(R.styleable.NumberPicker_picker_max, 100)
            val minValue = array.getInteger(R.styleable.NumberPicker_picker_min, 0)
            val stepSize = array.getInteger(R.styleable.NumberPicker_picker_stepSize, 1)
            val orientation = array.getInteger(R.styleable.NumberPicker_picker_orientation, LinearLayout.VERTICAL)
            val value = array.getInteger(R.styleable.NumberPicker_android_progress, 0)
            arrowStyle = array.getResourceId(R.styleable.NumberPicker_picker_arrowStyle, 0)
            background = array.getDrawable(R.styleable.NumberPicker_android_background)
            editTextStyleId = array.getResourceId(R.styleable.NumberPicker_picker_editTextStyle, R.style.NumberPicker_EditTextStyle)
            maxDistance = context.resources.getDimensionPixelSize(R.dimen.picker_distance_max)

            data = Data(value, minValue, maxValue, stepSize, orientation)

            inflateChildren()

            editText.setText(data.value.toString())


        } finally {
            array.recycle()
        }

        initializeButtonActions()
    }

    private fun hideKeyboard() {
        val imm = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    private fun inflateChildren() {
        upButton = AppCompatImageButton(context)
        upButton.setImageResource(R.drawable.arrow_up_selector_24)
        upButton.setBackgroundResource(R.drawable.arrow_up_background)

        if (data.orientation == HORIZONTAL) {
            upButton.rotation = 90f
        }

        editText = EditText(ContextThemeWrapper(context, editTextStyleId), null, 0)
        editText.setLines(1)
        editText.setEms(max(abs(maxValue).toString().length, abs(minValue).toString().length))
        editText.isFocusableInTouchMode = true
        editText.isFocusable = true
        editText.isClickable = true
        editText.isLongClickable = false


        downButton = AppCompatImageButton(context)
        downButton.setImageResource(R.drawable.arrow_up_selector_24)
        downButton.setBackgroundResource(R.drawable.arrow_up_background)
        downButton.rotation = if (data.orientation == VERTICAL) 180f else -90f

        val params1 = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params1.weight = 0f

        val params2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT)
        params2.weight = 1f

        val params3 = LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params3.weight = 0f

        addView(downButton, params3)
        addView(editText, params2)
        addView(upButton, params1)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeButtonActions() {
        upButton.setOnTouchListener { _, event ->
            if (!isEnabled) {
                false
            } else {
                val action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        setProgress(progress + stepSize)
                        editText.clearFocus()
                        hideKeyboard()

                        upButton.requestFocus()
                        upButton.isPressed = true
                    }

                    MotionEvent.ACTION_UP -> {
                        upButton.isPressed = false
                    }
                }

                true
            }
        }

        downButton.setOnTouchListener { _, event ->
            if (!isEnabled) {
                false
            } else {
                val action = event.actionMasked
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        requestFocus()
                        setProgress(progress - stepSize)
                        editText.clearFocus()
                        hideKeyboard()

                        downButton.requestFocus()
                        downButton.isPressed = true
                    }

                    MotionEvent.ACTION_UP -> {
                        downButton.isPressed = false
                    }
                }

                true
            }
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            setBackgroundFocused(hasFocus)

            if (!hasFocus) {
                if (!editText.text.isNullOrEmpty()) {
                    setProgress(Integer.valueOf(editText.text.toString()), true)
                } else {
                    editText.setText(data.value.toString())
                }
            }
        }

        editText.setOnEditorActionListener { _, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_DONE -> {
                    editText.clearFocus()
                    true
                }
                else -> false
            }
        }
    }

    private fun setBackgroundFocused(hasFocus: Boolean) {
        if (hasFocus) {
            background?.state = FOCUSED_STATE_ARRAY
        } else {
            background?.state = UNFOCUSED_STATE_ARRAY
        }
    }

    companion object {
        val FOCUSED_STATE_ARRAY = intArrayOf(android.R.attr.state_focused)
        val UNFOCUSED_STATE_ARRAY = intArrayOf(0, -android.R.attr.state_focused)
    }
}


class Data(value: Int, minValue: Int, maxValue: Int, var stepSize: Int, val orientation: Int) {
    var value: Int = value
        set(value) {
            field = max(minValue, min(maxValue, value))
        }

    var maxValue: Int = maxValue
        set(newValue) {
            if (newValue < minValue) throw IllegalArgumentException("maxValue cannot be less than minValue")
            field = newValue
            if (value > newValue) value = newValue
        }

    var minValue: Int = minValue
        set(newValue) {
            if (newValue > maxValue) throw IllegalArgumentException("minValue cannot be great than maxValue")
            field = newValue
            if (newValue > value) value = newValue
        }

}

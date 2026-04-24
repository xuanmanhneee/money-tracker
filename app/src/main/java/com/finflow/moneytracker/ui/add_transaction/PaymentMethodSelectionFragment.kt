package com.finflow.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.finflow.moneytracker.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaymentMethodSelectionFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_SELECTED_PAYMENT_METHOD = "arg_selected_payment_method"

        fun newInstance(selectedPaymentMethod: PaymentMethod): PaymentMethodSelectionFragment {
            return PaymentMethodSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_PAYMENT_METHOD, selectedPaymentMethod.name)
                }
            }
        }
    }

    private lateinit var containerPaymentMethods: LinearLayout
    private lateinit var ivBackPayment: ImageView
    private var selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH

    interface OnPaymentMethodSelectedListener {
        fun onPaymentMethodSelected(paymentMethod: PaymentMethod)
    }

    private var listener: OnPaymentMethodSelectedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_payment_method_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        containerPaymentMethods = view.findViewById(R.id.containerPaymentMethods)
        ivBackPayment = view.findViewById(R.id.ivBackPayment)
        selectedPaymentMethod = arguments
            ?.getString(ARG_SELECTED_PAYMENT_METHOD)
            ?.let { methodName -> runCatching { PaymentMethod.valueOf(methodName) }.getOrNull() }
            ?: PaymentMethod.CASH

        // Danh sách phương thức thanh toán
        val paymentMethods = listOf(
            PaymentMethod.CASH,
            PaymentMethod.BANK
        )

        // Thêm các phương thức vào container
        val inflater = LayoutInflater.from(requireContext())
        paymentMethods.forEachIndexed { index, method ->
            val paymentView = inflater.inflate(R.layout.item_payment_method, containerPaymentMethods, false)

            val iconView = paymentView.findViewById<ImageView>(R.id.ivPaymentIcon)
            val nameView = paymentView.findViewById<TextView>(R.id.tvPaymentName)

            // Đặt icon đơn giản dựa trên phương thức
            val iconRes = when (method) {
                PaymentMethod.CASH -> R.drawable.ic_money
                PaymentMethod.BANK -> R.drawable.ic_category  // Tạm dùng, sau thay bằng icon ngân hàng
            }

            iconView.setImageResource(iconRes)
            nameView.text = method.displayName
            applySelectionState(paymentView, nameView, iconView, method == selectedPaymentMethod)

            paymentView.setOnClickListener {
                animateSelection(paymentView) {
                    selectedPaymentMethod = method
                    listener?.onPaymentMethodSelected(method)
                    dismiss()
                }
            }

            containerPaymentMethods.addView(paymentView)
            animateItemEntrance(paymentView, index)
        }

        // Nút quay lại
        ivBackPayment.setOnClickListener {
            dismiss()
        }
    }

    fun setOnPaymentMethodSelectedListener(listener: OnPaymentMethodSelectedListener) {
        this.listener = listener
    }

    override fun getTheme(): Int = R.style.ThemeOverlay_MoneyTracker_BottomSheet

    private fun applySelectionState(
        itemView: View,
        nameView: TextView,
        iconView: ImageView,
        isSelected: Boolean
    ) {
        itemView.background = ContextCompat.getDrawable(
            requireContext(),
            if (isSelected) R.drawable.bg_selectable_item_selected else R.drawable.bg_selectable_item_default
        )
        nameView.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isSelected) R.color.nav_item_active else R.color.text_primary
            )
        )
        iconView.alpha = if (isSelected) 1f else 0.9f
        itemView.scaleX = if (isSelected) 1.02f else 1f
        itemView.scaleY = if (isSelected) 1.02f else 1f
    }

    private fun animateSelection(target: View, onEnd: () -> Unit) {
        target.animate()
            .scaleX(0.97f)
            .scaleY(0.97f)
            .setDuration(80)
            .withEndAction {
                target.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(120)
                    .withEndAction(onEnd)
                    .start()
            }
            .start()
    }

    private fun animateItemEntrance(target: View, index: Int) {
        target.alpha = 0f
        target.translationY = 10f
        target.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay((index * 28L).coerceAtMost(150L))
            .setDuration(180)
            .start()
    }
}

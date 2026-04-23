package com.finflow.moneytracker.ui.add_transaction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.finflow.moneytracker.R
import com.finflow.moneytracker.ui.add_transaction.PaymentMethod
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PaymentMethodSelectionFragment : BottomSheetDialogFragment() {

    private lateinit var containerPaymentMethods: LinearLayout
    private lateinit var ivBackPayment: ImageView

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

        // Danh sách phương thức thanh toán
        val paymentMethods = listOf(
            PaymentMethod.CASH,
            PaymentMethod.BANK
        )

        // Thêm các phương thức vào container
        val inflater = LayoutInflater.from(requireContext())
        paymentMethods.forEach { method ->
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

            paymentView.setOnClickListener {
                listener?.onPaymentMethodSelected(method)
                dismiss()
            }

            containerPaymentMethods.addView(paymentView)
        }

        // Nút quay lại
        ivBackPayment.setOnClickListener {
            dismiss()
        }
    }

    fun setOnPaymentMethodSelectedListener(listener: OnPaymentMethodSelectedListener) {
        this.listener = listener
    }
}

package com.sts.sontalksign.feature.history

import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView

import com.sts.sontalksign.databinding.HistoryConversationBinding


class HistoryDetailConversationAdapter(private val historyDetailConList: ArrayList<HistoryDetailConversationModel>) :
    RecyclerView.Adapter<HistoryDetailConversationAdapter.CustomViewHolder>() {

    inner class CustomViewHolder(private val binding: HistoryConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(historyDetailConversation: HistoryDetailConversationModel) {
            binding.tvHistoryConversationText.text = historyDetailConversation.historyText

            // 왼쪽/오른쪽 여부에 따라 메시지 아이템을 조절
            val isLeftMessage = historyDetailConversation.isLeft
            if (isLeftMessage) {
                binding.llhHistoryConversationText.gravity = Gravity.END
                binding.tvHistoryConversationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                binding.llhHistoryConversationText.alpha = 0.5f // 투명도를 0.5로 설정
//                binding.tvHistoryConversationText.setPadding(0, 0, 0, 10) // 10dp padding at the bottom
            } else {
                binding.llhHistoryConversationText.gravity = Gravity.START
                binding.tvHistoryConversationText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
                binding.llhHistoryConversationText.alpha = 0.5f // 투명도를 0.5로 설정
//                binding.tvHistoryConversationText.setPadding(0, 0, 0, 10) // 10dp padding at the bottom
            }



        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val binding = HistoryConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)


    }

    override fun getItemCount(): Int {
        return historyDetailConList.size
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        holder.bind(historyDetailConList[position])
    }
}


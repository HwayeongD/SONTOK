package com.sts.sontalksign.feature.history

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
                // 왼쪽 메시지

                binding.llhHistoryConversationText.gravity = Gravity.START
//                layoutParams.leftToLeft = Gravity.LEFT
//                layoutParams.marginEnd = 200 // 오른쪽 마진 조절
            } else {
                // 오른쪽 메시지
//                layoutParams.marginStart = 200 // 왼쪽 마진 조절

                binding.llhHistoryConversationText.gravity = Gravity.END
//                layoutParams.leftToRight = Gravity.RIGHT
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


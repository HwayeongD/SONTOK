package com.sts.sontalksign.feature.history

import java.util.Date

data class HistoryListModel(
    val historyTitle: String, // 기록 제목
    val EndedTime: String, // 기록 끝난 시간
    val historyitemmodel: ArrayList<HistoryItemModel>,
)

data class HistoryItemModel(
    val historyItemTag: Int
)

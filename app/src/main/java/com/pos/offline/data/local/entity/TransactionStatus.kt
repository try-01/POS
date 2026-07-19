package com.pos.offline.data.local.entity

enum class TransactionStatus { COMPLETED, VOID }

val TransactionEntity.isVoid: Boolean
    get() = status == TransactionStatus.VOID.name
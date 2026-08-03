package com.example.smartpesa.data.mpesa

/**
 * M-Pesa transaction types parsed from SMS
 */
enum class TransactionType {
    /** Money sent to another M-Pesa user */
    SEND,

    /** Money received from another M-Pesa user or bank */
    RECEIVE,

    /** Payment to a business paybill or till number */
    PAYBILL,

    /** Goods purchased using Buy Goods till number */
    BUY_GOODS,

    /** Cash withdrawal from agent */
    WITHDRAWAL,

    /** Airtime purchase */
    AIRTIME,

    /** Utility token purchase (e.g., KPLC electricity) */
    TOKEN_PURCHASE,

    /** Cash deposit at agent */
    DEPOSIT,

    /** Fuliza loan repayment */
    FULIZA_REPAYMENT,

    /** Unrecognized M-Pesa message format */
    UNKNOWN
}

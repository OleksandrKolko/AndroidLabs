package com.example.lab17.task1.data

import com.google.gson.annotations.SerializedName

/**
 * Відповідь архівного API — це ОБ'ЄКТ, усередині якого є масив exchangeRate
 * (на відміну від поточних курсів, де сервер повертає одразу масив).
 *
 * Приклад: https://api.privatbank.ua/p24api/exchange_rates?json&date=01.11.2025
 */
data class ExchangeArchiveResponse(
    @SerializedName("date") val date: String?,
    @SerializedName("bank") val bank: String?,
    @SerializedName("baseCurrencyLit") val baseCurrencyLit: String?,
    @SerializedName("exchangeRate") val exchangeRate: List<ExchangeRate>?
)

/** Курс однієї валюти. Курси ПриватБанку можуть бути відсутні → Double? */
data class ExchangeRate(
    @SerializedName("baseCurrency") val baseCurrency: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("purchaseRate") val purchaseRate: Double?,
    @SerializedName("saleRate") val saleRate: Double?,
    @SerializedName("purchaseRateNB") val purchaseRateNB: Double?,
    @SerializedName("saleRateNB") val saleRateNB: Double?
)

package com.example.lab18.task1.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/** Відповідь архівного API — об'єкт із масивом exchangeRate */
data class ExchangeArchiveResponse(
    @SerializedName("date") val date: String?,
    @SerializedName("exchangeRate") val exchangeRate: List<ExchangeRateDto>?
)

data class ExchangeRateDto(
    @SerializedName("baseCurrency") val baseCurrency: String?,
    @SerializedName("currency") val currency: String?,
    @SerializedName("purchaseRate") val purchaseRate: Double?,
    @SerializedName("saleRate") val saleRate: Double?
)

interface PrivatBankApi {

    // Тепер suspend + Response: Worker дочекається відповіді й сам перевірить статус
    @GET("exchange_rates?json")
    suspend fun getExchangeArchive(@Query("date") date: String): Response<ExchangeArchiveResponse>
}

/** Спільні налаштування мережі */
object PrivatBankNetworkModule {

    private const val BASE_URL = "https://api.privatbank.ua/p24api/"

    val api: PrivatBankApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrivatBankApi::class.java)
    }
}

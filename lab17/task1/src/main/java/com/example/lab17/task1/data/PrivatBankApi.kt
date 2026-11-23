package com.example.lab17.task1.data

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface PrivatBankApi {

    // GET https://api.privatbank.ua/p24api/exchange_rates?json&date=dd.MM.yyyy
    @GET("exchange_rates?json")
    fun getExchangeArchive(@Query("date") date: String): Call<ExchangeArchiveResponse>
}

/** Мережевий модуль: один екземпляр Retrofit на весь застосунок */
object PrivatBankNetworkModule {

    private const val BASE_URL = "https://api.privatbank.ua/p24api/"   // має закінчуватися "/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: PrivatBankApi = retrofit.create(PrivatBankApi::class.java)
}

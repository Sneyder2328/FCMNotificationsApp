package com.sneyder.tjheboisalerts

import retrofit2.http.POST
import retrofit2.http.Path

interface TheBoisApi {

    companion object {
        const val END_POINT = "https://binance-alarm.herokuapp.com/"


    }

    @POST("user/{token}")
    suspend fun sendToken(
        @Path("token") token: String,
    ): Boolean

}
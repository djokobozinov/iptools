package io.bozinov.iptools.connections

import io.bozinov.iptools.models.IPDetails
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path


/**
 * Created by djokob on 5/23/17.
 *
 */
class ApiCalls {
    interface IPDetailsService {
        @GET("?json=1")
        fun getIPDetails(): Call<IPDetails>
    }
}


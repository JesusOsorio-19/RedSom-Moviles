package com.example.mybraintumordetection.HuggingFace


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface BrainTumorApi {

    @POST("/predict/")
    //fun predict(@Part file: MultipartBody.Part): Call<ResponseData>
    fun predict(@Body request: RequestData): Call<ResponseData>
}
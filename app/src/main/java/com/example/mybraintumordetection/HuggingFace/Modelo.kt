package  com.example.mybraintumordetection.HuggingFace

import okhttp3.MultipartBody

// La manera de como se envia el dato
//data class RequestData(val file: MultipartBody.Part)
data class RequestData(val data: String)
// esta es la manera como recibe algo
data class ResponseData(val prediction: String)
package com.senai.vsconnect_kotlin.models

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import java.util.*

class Login (
    var id: UUID,
    var nome : String,
    var setor : String,
    var funcao : String,
    var email : String,
    var face : String,
)
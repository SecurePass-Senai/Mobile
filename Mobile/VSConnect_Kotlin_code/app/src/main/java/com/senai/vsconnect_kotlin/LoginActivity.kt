package com.senai.vsconnect_kotlin

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.gson.JsonObject
import com.senai.vsconnect_kotlin.apis.EndpointInterface
import com.senai.vsconnect_kotlin.apis.RetrofitConfig
import com.senai.vsconnect_kotlin.databinding.ActivityLoginBinding
import com.senai.vsconnect_kotlin.models.Login
import com.senai.vsconnect_kotlin.models.Servico
import com.squareup.picasso.Picasso
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.*

class LoginActivity : AppCompatActivity() {

    //É uma propriedade privada  como o nome binding do tipo ActivityLoginBinding
    private lateinit var binding: ActivityLoginBinding

    private val clienteRetrofit = RetrofitConfig.obterInstanciaRetrofit()

    private val endpoints = clienteRetrofit.create(EndpointInterface::class.java)

    private val IMAGEM_PERFIL_REQUEST_CODE = 123

    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Atribui à variável binding um objeto que contém referências (propriedades) aos elementos definidos no layout
        binding = ActivityLoginBinding.inflate(layoutInflater)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Aguarde...")
        progressDialog.setCancelable(false)

        val sharedPreferences = getSharedPreferences("idUsuario", Context.MODE_PRIVATE)

        val editor = sharedPreferences.edit()

        editor.remove("idUsuario")

        editor.apply()

        //setOnClickListener é um ouvinte de clique
        //Ou seja, quando clicar no botão entrar irá cair nesse bloco
        binding.btnEntrar.setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, IMAGEM_PERFIL_REQUEST_CODE)
        }

        setContentView(binding.root)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == IMAGEM_PERFIL_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap

            if (imageBitmap != null) {
                atualizarImagemPerfil(imageBitmap)
            }
        }
    }

    private fun atualizarImagemPerfil(imagem: Bitmap) {
        progressDialog.show()

        val file = File(cacheDir, "temp_image.png")
        file.createNewFile()

        val outputStream = FileOutputStream(file)
        imagem.compress(Bitmap.CompressFormat.PNG, 100, outputStream)

        outputStream.close()

        val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
        val imagemPart = MultipartBody.Part.createFormData("image", file.name, requestFile)

        endpoints.login(imagemPart).enqueue(object : Callback<Servico> {
            override fun onResponse(call: Call<Servico>, response: Response<Servico>) {
                progressDialog.dismiss()

                when( response.code() ){
                    200 -> {

                        val responseApi = response.body();

                        responseApi?.let {

                            val dadosUsuario = it.id

                            val sharedPreferences = getSharedPreferences("idUsuario", Context.MODE_PRIVATE)

                            val editor = sharedPreferences.edit()

                            editor.putString("idUsuario", dadosUsuario.toString())

                            editor.apply()

                            // Redirecionando para a tela de main
                            val mainIntent = Intent(this@LoginActivity, MainActivity::class.java)

                            startActivity(mainIntent)

                            finish()

//                            Toast.makeText(this@LoginActivity, "${responseApi.id}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        Toast.makeText(this@LoginActivity, "Usuário não reconhecido", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Servico>, t: Throwable) {
                progressDialog.dismiss()
                // Tratar falha na requisição, se necessário
                Toast.makeText(this@LoginActivity, "Erro no processamento", Toast.LENGTH_SHORT).show()
            }
        })
    }

}

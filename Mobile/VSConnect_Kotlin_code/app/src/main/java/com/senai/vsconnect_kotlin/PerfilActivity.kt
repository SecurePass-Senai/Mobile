package com.senai.vsconnect_kotlin

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.senai.vsconnect_kotlin.apis.EndpointInterface
import com.senai.vsconnect_kotlin.apis.RetrofitConfig
import com.senai.vsconnect_kotlin.databinding.ActivityPerfilBinding
import com.senai.vsconnect_kotlin.models.Login
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class PerfilActivity : AppCompatActivity() {

    //É uma propriedade privada  como o nome binding do tipo ActivityLoginBinding
    private lateinit var binding: ActivityPerfilBinding

    private val clienteRetrofit = RetrofitConfig.obterInstanciaRetrofit()

    private val endpoints = clienteRetrofit.create(EndpointInterface::class.java)

    private val IMAGEM_PERFIL_REQUEST_CODE = 123

    private lateinit var dadosUsuario : Login

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_main)

        //Atribui à variável binding um objeto que contém referências (propriedades) aos elementos definidos no layout
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Buscando dados do usuario...")
        progressDialog.setCancelable(false)

        val sharedPreferences = getSharedPreferences("idUsuario", Context.MODE_PRIVATE)

        val idUsuario = sharedPreferences.getString("idUsuario", "")

        buscarUsuarioPorID( idUsuario.toString() )


        binding.leftImg.setOnClickListener() {
            val perfilIntent = Intent(this, MainActivity::class.java)

            startActivity(perfilIntent)
        }

        binding.iconeLapis.setOnClickListener(){
            mostrarOpcoesEscolhaImagem()
        }
    }

    override fun setSupportActionBar(toolbar: Toolbar?) {
        super.setSupportActionBar(toolbar)
    }

    private fun buscarUsuarioPorID(idUsuario: String) {
        progressDialog.show()

        endpoints.buscarUsuarioPorID( idUsuario ).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                val root: View = binding.root

                val responseApi = response.body();

                responseApi?.let {

                    // Aplicando os dados do usuario
                    val gson = Gson()
                    dadosUsuario = gson.fromJson(response.body().toString(), Login::class.java)

                    //nome
                    root.findViewById<TextView>(R.id.nome).text = dadosUsuario.nome

                    //funcao
                    root.findViewById<TextView>(R.id.funcao).text = dadosUsuario.funcao

                    //email
                    root.findViewById<TextView>(R.id.email).text = dadosUsuario.email

                    //setor
                    root.findViewById<TextView>(R.id.setor).text = dadosUsuario.setor

                    // face
                    val viewImagemPerfil = root.findViewById<ImageView>(R.id.id_view_imagem_perfil)
                    carregarImagemDaUrl( dadosUsuario.face, viewImagemPerfil )

                    progressDialog.dismiss()

                }

                // Usar Picasso para carregar e exibir a imagem na ImageView
//                Picasso.get().load(urlImagem).into(viewImagemPerfil)
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                Toast.makeText(this@PerfilActivity, "Erro na busca de dados", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun mostrarOpcoesEscolhaImagem(){
        // Criar Intents para escolher uma imagem da galeria ou capturar uma nova pela câmera
        val escolherImagemIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        val capturarImagemIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Obter os títulos para as opções de escolha de imagem
        val escolherImagemTitle = resources.getString(R.string.escolher_imagem)
        val capturarImagemTitle = resources.getString(R.string.capturar_imagem)

        // Criar um Intent Chooser para oferecer opções entre galeria e câmera
        val intentEscolhaImagem = Intent.createChooser(escolherImagemIntent, escolherImagemTitle)
        intentEscolhaImagem.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(capturarImagemIntent))

        // Iniciar a atividade esperando um resultado
        startActivityForResult(intentEscolhaImagem, IMAGEM_PERFIL_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        val view_imagem_perfil = binding.idViewImagemPerfil

        if (requestCode == IMAGEM_PERFIL_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            if (data?.data != null){
                val imagemSelecionadaUri = data.data

                val inputStream: InputStream? = contentResolver.openInputStream(imagemSelecionadaUri!!)

                val imagemSelecionadaBitmap = BitmapFactory.decodeStream(inputStream)

                view_imagem_perfil?.setImageURI(imagemSelecionadaUri)

                atualizarImagemPerfil(imagemSelecionadaBitmap)

            }else if (data?.action == "inline-data"){
                // Imagem capturada pela câmera
                val imagemCapturada = data.extras?.get("data") as Bitmap
                view_imagem_perfil?.setImageBitmap(imagemCapturada)

                atualizarImagemPerfil(imagemCapturada)
            }
        }
    }


    private fun atualizarImagemPerfil(imagem: Bitmap){

        // Criar um arquivo temporário para armazenar a imagem
        val file = File(cacheDir, "temp_image.png")
        file.createNewFile()

        // Salvar a imagem Bitmap no arquivo temporário
        val outputStream = FileOutputStream(file)
        imagem.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()

        // Criar partes Multipart para a imagem
        val requestFile = RequestBody.create(MediaType.parse("image/*"), file)
        val imagemPart = MultipartBody.Part.createFormData("imagem", file.name, requestFile)


        endpoints.editarImagemUsuario(imagemPart, dadosUsuario.id).enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                // Exibir mensagem de sucesso
                when( response.code() ){
                    200 -> {
                        Toast.makeText(this@PerfilActivity, "Foto atualizada com sucesso!!!", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(this@PerfilActivity, "Erro ao atualizar imagem de Perfil!!!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                // Exibir mensagem de erro
                Toast.makeText(this@PerfilActivity, "Erro ao atualizar imagem de Perfil.", Toast.LENGTH_SHORT).show()
            }

        })
    }

    fun carregarImagemDaUrl(urlString: String, imageView: ImageView) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    carregarBitmapDaUrl(urlString)
                }
                imageView.setImageBitmap(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    suspend fun carregarBitmapDaUrl(urlString: String): Bitmap {
        val url = URL(urlString)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()

        val inputStream = connection.inputStream
        return BitmapFactory.decodeStream(inputStream)
    }
}
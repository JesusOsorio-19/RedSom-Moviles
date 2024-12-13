package com.example.mybraintumordetection

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mybraintumordetection.HuggingFace.RequestData
import com.example.mybraintumordetection.HuggingFace.ResponseData
import com.example.mybraintumordetection.HuggingFace.RetrofitBrainTumor
import com.example.mybraintumordetection.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.UUID
import retrofit2.Call
import retrofit2.Response
import retrofit2.Callback


class MainActivity : AppCompatActivity() {
    var misbraintumors = ArrayList<BrainTumor>()
    lateinit var binding: ActivityMainBinding
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseDatabase: FirebaseDatabase

    val storageReference: StorageReference = FirebaseStorage.getInstance().getReference()

    var mBitmap = Bitmap.createBitmap(512,512, Bitmap.Config.ARGB_8888)
    private var Miuri: Uri? = null
    var adapter: MyAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnenviar.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                if (Miuri != null) {
                    val reference = storageReference.child("imgbraintumor/${UUID.randomUUID()}")
                    reference.putFile(Miuri!!).addOnSuccessListener {
                            reference.downloadUrl.addOnSuccessListener { uri ->
                                val imageUrl = uri.toString()
                                val nombre = binding.txtnombre.text.toString()

                                if (nombre.isBlank()) {
                                    Toast.makeText(applicationContext, "Ingrese un nombre válido", Toast.LENGTH_SHORT).show()
                                    return@addOnSuccessListener
                                }

                                predictTumor(imageUrl, nombre) // Envía la URL a la API
                            }.addOnFailureListener { exception ->
                                Toast.makeText(applicationContext, "Error al obtener URL: ${exception.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(applicationContext, "Error al subir la imagen: ${exception.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(applicationContext, "Seleccione una imagen primero", Toast.LENGTH_SHORT).show()
                }

            }
        })

        val escucha = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                misbraintumors.clear()
                for (postSnapshot in dataSnapshot.children) {
                    for (postSnapshot in postSnapshot.children) {
                        val nombre = postSnapshot.child("nombre").value.toString()
                        val key = postSnapshot.child("key").value.toString()
                        val urlBrainTumor = postSnapshot.child("urlBrainTumor").value.toString()
                        val existeBrainTumor = postSnapshot.child("existeBrainTumor").value.toString()
                        val brainTumor = BrainTumor(nombre, key, urlBrainTumor, existeBrainTumor)
                        misbraintumors.add(brainTumor)
                    }
                }
                val mensaje:String= "Dato cargados: "+misbraintumors.size

                Toast.makeText(applicationContext,mensaje, Toast.LENGTH_LONG).show()
                adapter = MyAdapter(applicationContext,misbraintumors)
                binding.lvmisbraintumors.adapter = adapter

                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(500)
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("TAG", "loadPost:onCancelled", databaseError.toException())
            }
        }

        databaseReference.addValueEventListener(escucha)
        binding.lvmisbraintumors.setOnItemClickListener { parent, view, position, id ->
            val mensaje:String= "Tumor cerebral: "+misbraintumors[position].nombre +" ==> "+ misbraintumors[position].existeBrainTumor
            Toast.makeText(applicationContext,mensaje, Toast.LENGTH_LONG).show()
        }

        binding.lvmisbraintumors.setOnItemLongClickListener(AdapterView.OnItemLongClickListener { arg0, v, index, arg3 ->
            val dbref = FirebaseDatabase.getInstance().getReference().child("Tumor cerebral")
            val query: Query = dbref
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(@NonNull dataSnapshot: DataSnapshot) {
                    for (postSnapshot in dataSnapshot.children) {
                        for (postSnapshot1 in postSnapshot.children) {
                            var key: String = postSnapshot.child("key").value.toString()
                            if (key == misbraintumors[index].key) {
                                postSnapshot.ref.removeValue()
                                break
                            }
                        }
                    }
                }

                override fun onCancelled(@NonNull databaseError: DatabaseError) {}
            })
            true
        })

        val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                Miuri = result.data?.data
                Miuri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    mBitmap = bitmap
                    binding.imgFoto.setImageBitmap(bitmap)
                }
            }
        }
        binding.btnseleccionar.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryLauncher.launch(galleryIntent)
            }
        })



    }

    private fun predictTumor(imageUrl: String, nombre: String) {
        if (nombre.isBlank()) {
            Toast.makeText(applicationContext, "Ingrese un nombre válido", Toast.LENGTH_SHORT).show()
            return
        }

        val requestData = RequestData(imageUrl)

        RetrofitBrainTumor.getinstance.predict(requestData).enqueue(object : Callback<ResponseData> {
            override fun onResponse(call: Call<ResponseData>, response: Response<ResponseData>) {
                if (response.isSuccessful) {
                    val prediction = response.body()?.prediction ?: "Sin predicción"

                    // Crear objeto BrainTumor con el nombre y predicción
                    val key = databaseReference.child("Tumor cerebral").push().key ?: ""
                    val brainTumor = BrainTumor(nombre, key, imageUrl, prediction)

                    // Guardar en Firebase
                    databaseReference.child("Tumor cerebral").child(key).setValue(brainTumor)

                } else {
                    Toast.makeText(applicationContext, "Error en la predicción", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                Toast.makeText(applicationContext, "Error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

}
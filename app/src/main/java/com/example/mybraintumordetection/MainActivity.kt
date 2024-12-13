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
                if(Miuri!=null){
                    var reference: StorageReference = storageReference.child("imgbraintumor/"+ UUID.randomUUID().toString())

                    reference.putFile(Miuri!!).addOnSuccessListener {
                        reference.downloadUrl.addOnSuccessListener { uri: Uri ->

                            val miURL:String = uri.toString()
                            val requestData = RequestData(miURL)
                            val nombre = binding.txtnombre.getText().toString()
                            val existeBrainTumor:String = "TipoX"
                            val key = databaseReference.child("Tumor cerebral").push().getKey()
                            val c = BrainTumor(nombre,  key.toString(),miURL,existeBrainTumor)
                            databaseReference.child("Tumor cerebral").push().setValue(c)
                            val mensaje:String= "Existe tumor cerebral: "+existeBrainTumor
                            Toast.makeText(applicationContext,mensaje, Toast.LENGTH_LONG).show()

                            binding.txtnombre.setText("")
                            val call = RetrofitBrainTumor.getinstance.predict(miURL)
                            /*call.enqueue(object : Callback<ResponseData> {
                                override fun onResponse(
                                    call: Call<ResponseData>,
                                    response: Response<ResponseData>
                                ) {
                                    if (response.isSuccessful) {
                                        val responseData = response.body()
                                        responseData?.let {
                                            val nombre = binding.txtnombre.getText().toString()
                                            val tipoHuella:String = it.prediction
                                            val key = databaseReference.child("Huella").push().getKey()
                                            val c = Huella(nombre,  key.toString(),miURL,tipoHuella)
                                            databaseReference.child("Huella").push().setValue(c)
                                            val mensaje:String= "Tipo Huella: "+tipoHuella
                                            Toast.makeText(applicationContext,mensaje,Toast.LENGTH_LONG).show()
                                            val myToast = Toast.makeText(applicationContext,mensaje,Toast.LENGTH_SHORT)
                                            myToast.show()
                                            binding.txtnombre.setText("")
                                        }
                                    } else {
                                        val myToast = Toast.makeText(applicationContext,"Error1",Toast.LENGTH_SHORT)
                                        myToast.show()
                                    }
                                }

                                override fun onFailure(call: Call<ResponseData>, t: Throwable) {
                                    val myToast = Toast.makeText(applicationContext,"Error2",Toast.LENGTH_SHORT)
                                    myToast.show()
                                }
                            })


                            */

                        }.addOnFailureListener { exception ->
                            Toast.makeText(applicationContext,"Image Retrived Failed: "+exception.message,
                                Toast.LENGTH_LONG).show()

                        }

                    }.addOnFailureListener {

                        Toast.makeText(applicationContext, "Fallo en subir imagen", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        })

        val escucha = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                misbraintumors.clear()
                for (postSnapshot in dataSnapshot.children) {
                    for (postSnapshot1 in postSnapshot.children) {
                        var nombre:String =postSnapshot1.child("nombre").value.toString()
                        var key:String =postSnapshot1.child("key").value.toString()
                        val urlHuella:String = postSnapshot1.child("urlBrainTumor").value.toString()
                        var tipoHuella:String =postSnapshot1.child("existeBrainTumor").value.toString()
                        var b: BrainTumor = BrainTumor(nombre,key,urlHuella,tipoHuella)
                        misbraintumors.add(b)
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
}
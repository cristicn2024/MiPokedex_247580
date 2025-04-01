package castro.cristina.practica12_247580

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    val REQUEST_IMAGE_GET = 1
    val CLOUD_NAME = "do8ack5lh"
    val UPLOAD_PRESET = "practica12"
    var imageUri: Uri? = null
    var imagePublicUrl: String? = null

    private lateinit var database: DatabaseReference
    private lateinit var name: EditText
    private lateinit var number: EditText
    private lateinit var btnUploadImage: Button
    private lateinit var btnSave: Button
    private lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        initCloudinary()
        database = FirebaseDatabase.getInstance().reference.child("pokemons")

        name = findViewById(R.id.etName)
        number = findViewById(R.id.etNumber)
        btnUploadImage = findViewById(R.id.btnUploadImage)
        btnSave = findViewById(R.id.btnSavePokemon)
        imageView = findViewById(R.id.imageView)

        btnUploadImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_IMAGE_GET)
        }

        btnSave.setOnClickListener {
            if (imageUri != null) {
                uploadImageToCloudinary() // Primero subimos la imagen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            } else {
                savePokemonToFirebase(null) // Guardamos sin imagen si no hay
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }

    fun initCloudinary() {
        val config: MutableMap<String, String> = HashMap()
        config["cloud_name"] = CLOUD_NAME
        try {
            MediaManager.init(this, config)
        } catch (e: IllegalStateException) {
            // Ya estaba inicializado, ignoramos el error
        }


    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_GET && resultCode == Activity.RESULT_OK) {
            val fullPhotoUrl: Uri? = data?.data
            if (fullPhotoUrl != null) {
                changeImage(fullPhotoUrl)
                imageUri = fullPhotoUrl
            }
        }
    }

    fun changeImage(uri: Uri) {
        try {
            imageView.setImageURI(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun uploadImageToCloudinary() {
        imageUri?.let { uri ->
            MediaManager.get().upload(uri)
                .unsigned(UPLOAD_PRESET)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String?) {
                        Log.i("OnStart", "Subida iniciada")
                    }

                    override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                        Log.i("onProgress", "Subiendo...")
                    }

                    override fun onSuccess(
                        requestId: String?,
                        resultData: MutableMap<Any?, Any?>?
                    ) {
                        imagePublicUrl = resultData?.get("url") as String?
                        savePokemonToFirebase(imagePublicUrl) // Guardar en Firebase solo después de subir la imagen
                    }

                    override fun onError(requestId: String?, error: ErrorInfo?) {
                        Log.e("onError", "Error al subir: ${error.toString()}")
                        savePokemonToFirebase(null) // Guardar sin imagen si hay error
                    }

                    override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                        Log.i("onReschedule", "Reprogramado")
                    }
                })
                .dispatch()
        }
    }

    fun savePokemonToFirebase(imageUrl: String?) {
        val nameText = name.text.toString()
        val numberText = number.text.toString().toIntOrNull()

        if (nameText.isNotEmpty() && numberText != null) {
            val pokemon = if (imageUrl != null) {
                Pokemon(numberText, nameText)
            } else {
                Pokemon(numberText, nameText)
            }

            val key = database.push().key
            if (key != null) {
                database.child(key).setValue(pokemon)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Pokémon guardado", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // Esto asegura que se recargue la actividad
                        startActivity(intent) // Llama a MainActivity nuevamente para actualizar los datos
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error al guardar", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            Toast.makeText(this, "Ingresa datos válidos", Toast.LENGTH_SHORT).show()
        }
    }

}

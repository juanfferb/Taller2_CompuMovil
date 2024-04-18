package com.example.taller2

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camara : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 123
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var image: ImageView
    private var photoURI: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        cameraExecutor = Executors.newSingleThreadExecutor()

        image = findViewById(R.id.foto)

        findViewById<Button>(R.id.camera).setOnClickListener {
            when {
                ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //Lanzamos la camara
                    startCamera()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this, android.Manifest.permission.CAMERA) -> {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE)
                }
                else -> {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.CAMERA),
                        CAMERA_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }

        findViewById<Button>(R.id.gallery).setOnClickListener {
            openGallery()
        }

        // Preparar el lanzador para el resultado de selección de imagen.
        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                image.setImageURI(uri)
            }
        }

        // Preparar el lanzador para el resultado de tomar foto.
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoURI?.let {
                    image.setImageURI(it)
                }
            }
        }

    }

    private fun openGallery() {
        imagePickerLauncher.launch("image/*")
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun startCamera(){
        val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "Foto nueva")
            values.put(MediaStore.Images.Media.DESCRIPTION, "Taller 2 foto")
            photoURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            photoURI?.let { uri ->
                takePictureLauncher.launch(uri)
            }

        } else {
            Toast.makeText(this, "No hay permiso", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),
                Datos.MY_PERMISSION_REQUEST_CAMARA)
        }
    }

    private fun createPhotoFile(): Uri {
        val photoFile = File(externalCacheDir, "temp_photo.jpg")
        return FileProvider.getUriForFile(this, "${packageName}.provider", photoFile)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Verificar si los permisos fueron concedidos
        when (requestCode) {
            Datos.MY_PERMISSION_REQUEST_CAMARA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Si los permisos fueron concedidos, iniciar la cámara
                    startCamera()
                } else {
                    // Si los permisos fueron denegados, mostrar un mensaje
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                Datos.REQUEST_IMAGE_CAPTURE -> {
                    photoURI?.let { uri ->
                        image.setImageURI(uri)
                        image.visibility = ImageView.VISIBLE
                    }
                }
                Datos.GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        image.setImageURI(uri)
                        image.visibility = ImageView.VISIBLE
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

package com.example.taller2

import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.Datos.Companion.MY_PERMISSION_REQUEST_CAMARA
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camara : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 123
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camara)

        cameraExecutor = Executors.newSingleThreadExecutor()

        findViewById<Button>(R.id.camera).setOnClickListener {
            startCamera()
        }
    }
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
    }

    private fun startCamera(){
        val permissionCheck = ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
        val takePictureIntent =  Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, Datos.REQUEST_IMAGE_CAPTURE);
            } else {
                Toast.makeText(this, "No hay camara", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "No hay permiso", Toast.LENGTH_SHORT).show()
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),
                Datos.MY_PERMISSION_REQUEST_CAMARA)
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Verificar si los permisos fueron concedidos
        when (requestCode) {
            MY_PERMISSION_REQUEST_CAMARA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Si los permisos fueron concedidos, iniciar la cámara
                    startCamera()
                } else {
                    // Si los permisos fueron denegados, mostrar un mensaje
                    Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
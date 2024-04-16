package com.example.taller2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val botonCamara = findViewById<ImageButton>(R.id.iconoCamara)
        val botonMapa = findViewById<ImageButton>(R.id.iconoMapa)
        val botonContactos = findViewById<ImageButton>(R.id.iconoContactos)

        botonCamara.setOnClickListener{
            val intent = Intent (this, Camara::class.java)
            startActivity(intent)
        }

        botonMapa.setOnClickListener {
            val intent2 = Intent(this, OsmMapActivity::class.java)
            startActivity(intent2)
        }

        botonContactos.setOnClickListener {
            val intent3 = Intent(this, Contactos::class.java)
            startActivity(intent3)
        }

    }
}
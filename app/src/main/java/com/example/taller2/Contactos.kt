package com.example.taller2

import android.content.pm.PackageManager
import android.database.Cursor
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.taller2.Datos.Companion.MY_PERMISSION_REQUEST_READ_CONTACTS

class Contactos : AppCompatActivity() {
    var mProjection: Array<String>? = null
    var mCursor: Cursor? = null
    var mContactsAdapter: ContactsAdaptador? = null
    var mlista: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        // 1. Variables
        mlista = findViewById(R.id.listaContactos)

        //2. Proyección
        mProjection =
            arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)

        //3. Adaptador
        mContactsAdapter = ContactsAdaptador(this, null, 0)
        mlista?.adapter = mContactsAdapter

        //4. Pedir Permiso
        when {
            ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED -> {

                //val textV = findViewById<TextView>(R.id.textView)
                //textV.text = "PERMISO CONCEDIDO !!!"
                //textV.setTextColor(Color.GREEN)

                //5. Cargar Contactos
                initView()
                Toast.makeText(this, "GRACIAS !!", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this, android.Manifest.permission.READ_CONTACTS
            ) -> {

                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    MY_PERMISSION_REQUEST_READ_CONTACTS
                )

            }

            else -> {
                requestPermissions(
                    arrayOf(android.Manifest.permission.READ_CONTACTS),
                    MY_PERMISSION_REQUEST_READ_CONTACTS
                )
            }
        }
    }

    fun initView() {
        mCursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI, mProjection, null, null, null
        )
        mContactsAdapter?.changeCursor(mCursor)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSION_REQUEST_READ_CONTACTS -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    //textV.text = "PERMISO CONCEDIDO !!!"
                    //textV.setTextColor(Color.GREEN)
                    initView()
                    Toast.makeText(this, "GRACIAS !!", Toast.LENGTH_SHORT).show()
                } else {

                    //textV.text = "PERMISO DENEGADO !!!"
                    //textV.setTextColor(Color.GREEN)
                    Toast.makeText(this, "FUNCIONALIDADES REDUCIDAS!!", Toast.LENGTH_SHORT).show()

                }
                return
            }

            else -> {
            }
        }
    }
}
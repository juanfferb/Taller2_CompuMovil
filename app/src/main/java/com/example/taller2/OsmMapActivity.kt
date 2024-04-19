package com.example.taller2

import android.Manifest
import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.taller2.Datos.Companion.MY_PERMISSION_REQUEST_LOCATION
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.Date
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import java.io.IOException


class OsmMapActivity : AppCompatActivity() {

    private lateinit var map : MapView
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var ultimaUbicacion: Location? = null
    //Para guardar la las ubicaciones en el archivo JSON
    private var localizaciones: JSONArray = JSONArray()
    //Para busqueda de puntos
    private var mGeocoder: Geocoder? = null
    //Para el marcador LONG-PRESS
    private var longPressedMarker: Marker? = null
    //Para rutas
    lateinit var roadManager: RoadManager
    private var roadOverlay: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_osm_map)

        // Inicializar el cliente de ubicación fusionada
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        map = findViewById(R.id.osmMap)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        // Revisar permisos
        checkLocationPermission()

        // Para buscar dentro del mapa

        val editText : EditText = findViewById(R.id.editText)
        editText.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
            }

                val addressString = editText.text.toString()
                if (addressString.isNotEmpty()) {
                    try {
                        val addresses = mGeocoder!!.getFromLocationName(addressString, 2)
                        if (addresses != null && addresses.isNotEmpty()) {
                            if (map != null && mGeocoder != null) {
                                val addressResult = addresses[0]
                                //Agregar Marcador al mapa
                                updateMarker(addressResult.latitude, addressResult.longitude)
                                createToastWithDistance(addressResult.latitude, addressResult.longitude)
                                drawRoute(GeoPoint(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude), GeoPoint(addressResult.latitude, addressResult.longitude))
                                //Agregar marcador en nuestra ubicación
                                val startMarker = Marker(map)
                                val myIcon = resources.getDrawable(R.drawable.location_pin,
                                    theme)
                                startMarker.icon = myIcon
                                startMarker.setPosition( GeoPoint(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude))
                                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                map.overlays.add(startMarker)
                                //Cambiar Zoom
                                val mapController: IMapController = map.controller
                                mapController.setZoom(15.0)
                            } else {
                                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    Toast.makeText(this, "La dirección esta vacía", Toast.LENGTH_SHORT).show()
                }
                true
            }


        //Para el LONG-PRESS
        map.overlays.add(createOverlayEvents())

        //Para ruta
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        roadManager = OSRMRoadManager(this, "ANDROID")
    }

    //Para ruta
    private fun drawRoute(start: GeoPoint, finish: GeoPoint) {
        val routePoints = ArrayList<GeoPoint>()
        routePoints.add(start)
        routePoints.add(finish)
        val road = roadManager.getRoad(routePoints)
        Log.i("OSM_acticity", "Route length: ${road.mLength} klm")
        Log.i("OSM_acticity", "Duration: ${road.mDuration / 60} min")
        if (map != null) {
            roadOverlay?.let { map.overlays.remove(it) }
            roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay?.outlinePaint?.color = Color.BLUE
            roadOverlay?.outlinePaint?.strokeWidth = 10f
            map.overlays.add(roadOverlay)
        }
    }

    private fun createToastWithDistance(lat: Double, lon: Double){
        val distanceToMyLocation = String.format("%.2f", distance(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude, lat, lon))
        Toast.makeText(this, "Distancia hasta tu ubicación: $distanceToMyLocation metros", Toast.LENGTH_SHORT).show()
    }
    private fun createOverlayEvents(): MapEventsOverlay? {
        val overlayEventos = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                return false
            }
            override fun longPressHelper(p: GeoPoint): Boolean {
                longPressOnMap(p)
                return true
            }
        })
        return overlayEventos
    }

    private fun longPressOnMap(p: GeoPoint) {
        longPressedMarker?.let { map.overlays.remove(it) }

        //Sacar título con latitud y longitud
        val addresses = mGeocoder!!.getFromLocation(p.latitude, p.longitude, 2)
        var title: String? = null
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0].getAddressLine(0)
            title = address.toString()
        }

        longPressedMarker = createMarker(p, title, null, R.drawable.location_pin)

        //Centrar vista
        val mapController: IMapController = map.controller
        mapController.setZoom(17.0)
        mapController.setCenter(p)

        longPressedMarker?.let { map.overlays.add(it) }

        createToastWithDistance(p.latitude, p.longitude)
        drawRoute(GeoPoint(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude), GeoPoint(p.latitude, p.longitude))
        //Agregar marcador en nuestra ubicación
        val startMarker = Marker(map)
        val myIcon = resources.getDrawable(R.drawable.location_pin,
            theme)
        startMarker.icon = myIcon
        startMarker.setPosition( GeoPoint(ultimaUbicacion!!.latitude, ultimaUbicacion!!.longitude))
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(startMarker)
    }

    // Esta se usa solo para crear el marcador de Long-Press
    private fun createMarker(p: GeoPoint, title: String?, desc: String?, iconID: Int): Marker? {
        // Obtener todos los overlays del mapa
        val mapOverlays = map.overlays

        // Recorrer la lista de overlays y eliminar aquellos que sean marcadores
        val markersToRemove = mutableListOf<Marker>()
        for (overlay in mapOverlays) {
            if (overlay is Marker) {
                markersToRemove.add(overlay)
            }
        }
        mapOverlays.removeAll(markersToRemove)
        // Actualizar el mapa
        map.invalidate();


        var marker: Marker? = null
        if (map != null) {
            marker = Marker(map)
            title?.let { marker.title = it }
            desc?.let { marker.subDescription = it }
            if (iconID != 0) {
                val myIcon = resources.getDrawable(iconID, this.theme)
                marker.icon = myIcon
            }

            marker.position = p
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }

        return marker
    }

    override fun onResume() {
        super.onResume()
        map.onResume()

        mGeocoder = Geocoder(baseContext)

        val mapController: IMapController = map.controller
        mapController.setZoom(18.0)

        //Cambiar de OSCURO -> CLARO o CLARO -> OSCURO
        val uiManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if(uiManager.nightMode == UiModeManager.MODE_NIGHT_YES)
            map.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no tienes permisos, solicitarlos al usuario
            requestLocationPermission()
        } else {
            // Si tienes permisos, mostrar la ubicación actual
            startLocationUpdates()
        }
    }

    private fun requestLocationPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
            //El usuario ya ha rechazado los permisos
            Toast.makeText(this, "Permisos denegados :(", Toast.LENGTH_SHORT).show()
        }else{
            //Pedir permisos
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_REQUEST_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Crear una solicitud de ubicación
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(2000) // Intervalo de actualización de ubicación en milisegundos

        // Configurar un callback para recibir actualizaciones de ubicación
        val locationCallback: LocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                // Obtener la ubicación actual del resultado
                val miUbi: Location? = locationResult.lastLocation
                if (miUbi != null) {
                    // Hacer algo con la ubicación obtenida
                    val latitude: Double = miUbi.getLatitude()
                    val longitude: Double = miUbi.getLongitude()
                    if(ultimaUbicacion == null){
                        ultimaUbicacion = miUbi;
                        updateMarker(latitude, longitude)
                    }
                    if(checkDistance(latitude, longitude)){
                        updateMarker(latitude, longitude)
                        writeJSONObject(latitude, longitude)
                    }
                    //Toast.makeText(this@OsmMapActivity, "Longitud: $longitude - Latitud = $latitude", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Solicitar actualizaciones de ubicación
        fusedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, null)

    }

    private fun updateMarker(lat: Double, lon: Double) {
        // Obtener todos los overlays del mapa
        val mapOverlays = map.overlays

        // Recorrer la lista de overlays y eliminar aquellos que sean marcadores
        for (overlay in mapOverlays) {
            if (overlay is Marker) {
                map.overlays.remove(overlay)
            }
        }
        // Actualizar el mapa
        map.invalidate();

        // Configurar la ubicación del marcador
        val npoint = GeoPoint(lat,  lon)

        //Centrar vista
        val mapController: IMapController = map.controller
        mapController.setZoom(18.0)
        mapController.setCenter(npoint)

        // Agregar un marcador en el mapa
        val startMarker = Marker(map)
        val myIcon = resources.getDrawable(R.drawable.location_pin,
            theme)
        startMarker.icon = myIcon
        startMarker.setPosition(npoint)
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(startMarker)

        // Actualizar el mapa
        map.invalidate()
    }

    private fun checkDistance(lat1: Double, lon1: Double): Boolean{
        var distanciaEntrePuntos = distance(this.ultimaUbicacion!!.latitude, this.ultimaUbicacion!!.longitude, lat1, lon1);
        if(distanciaEntrePuntos > 30.0){
            ultimaUbicacion!!.latitude = lat1;
            ultimaUbicacion!!.longitude = lon1;
            Toast.makeText(this, "Distancia: $distanciaEntrePuntos", Toast.LENGTH_SHORT).show()
            return true;
        }
        return false;
    }

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // Radio medio de la Tierra en kilómetros

        val lat1Radians = Math.toRadians(lat1)
        val lon1Radians = Math.toRadians(lon1)
        val lat2Radians = Math.toRadians(lat2)
        val lon2Radians = Math.toRadians(lon2)

        val dlon = lon2Radians - lon1Radians
        val dlat = lat2Radians - lat1Radians

        val a = sin(dlat / 2).pow(2) + cos(lat1Radians) * cos(lat2Radians) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distanceInKm = earthRadius * c
        return distanceInKm * 1000 // Convertir a metros
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == MY_PERMISSION_REQUEST_LOCATION){// Nuestros permisos
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){ // Validar que si se aceptaron ambos permisos
                // Permisos aceptados
                startLocationUpdates()
            }else{
                //El permiso no ha sido aceptado
                Toast.makeText(this, "Permisos denegados :(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //ESCRITURA EN MEMORIA
    private fun writeJSONObject(lat: Double, lon: Double) {
        localizaciones.put(MyLocation(
            Date(System.currentTimeMillis()), lat,
            lon).toJSON())
        var output: Writer?
        val filename = "locations.json"
        try {
            val file = File(baseContext.getExternalFilesDir(null), filename)
            Log.i("LOCATION", "Ubicacion de archivo: $file")
            output = BufferedWriter(FileWriter(file))
            output.write(localizaciones.toString())
            output.close()
            Toast.makeText(applicationContext, "Location saved", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.i("ERROR_JSON_FILE", "No se pudo escribir en el archivo")
        }
    }
}
package prueba.salas.com.localizacinysensores;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, View.OnClickListener, SensorEventListener {

    /**
     * Permiosos de acceso a ubicación.
     */
    private static final String[] PERMISOS = {
            android.Manifest.permission.ACCESS_FINE_LOCATION
    };

    /**
     * Código para solicitud de permiso de ubicación.
     */
    private static int REQUEST_CODE_UBICACION = 1;

    /**
     * Cliente de la API de Google.
     */
    private GoogleApiClient googleApiClient;

    /**
     * Representa un mapa de Google.
     */
    private GoogleMap googleMap;

    /**
     * Código para la selección del lugar.
     */
    private static final int PLACE_PICKER_REQUEST = 1;

    /**
     * Zoom predeterminado.
     */
    private static final int DEFAULT_ZOOM = 7;
    /**
     * Colores para los marcadores.
     */
    private static final float[] COLORES_MARCADORES;
    /**
     * Cantidad de colores para los marcadores.
     */
    private static final int NUMERO_COLORES = 8;
    /**
     * Número del color actual.
     */
    private int numeroColorSeleccionado = 0;

    /**
     * Inicializa el arreglo de los colores de marcadores.
     */
    static {
        COLORES_MARCADORES = new float[]{BitmapDescriptorFactory.HUE_AZURE, BitmapDescriptorFactory.HUE_BLUE, BitmapDescriptorFactory.HUE_CYAN, BitmapDescriptorFactory.HUE_GREEN, BitmapDescriptorFactory.HUE_ORANGE, BitmapDescriptorFactory.HUE_RED, BitmapDescriptorFactory.HUE_ROSE, BitmapDescriptorFactory.HUE_VIOLET, BitmapDescriptorFactory.HUE_YELLOW};
    }

    /**
     * Ubicación actual.
     */
    private LatLng ubicacionActual;

    /**
     * Temperatura actual capturada por el sensor.
     */
    private float temperaturaActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        int leer = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (leer == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, PERMISOS, REQUEST_CODE_UBICACION);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.frgMapa);
        mapFragment.getMapAsync(this);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tbrPrincipal);
        setSupportActionBar(toolbar);

        FloatingActionButton fabAgregarDestino = (FloatingActionButton) findViewById(R.id.fabAgregarDestino);
        fabAgregarDestino.setOnClickListener(this);

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        SensorManager smrSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        smrSensorManager.registerListener(this, smrSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_aeropuerto, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.mitConsultarTemperatura) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle(getString(R.string.temperatura_actual));

            NumberFormat numberFormat = new DecimalFormat("#0.00");

            StringBuilder sb = new StringBuilder();

            sb.append(String.format(Locale.US, "%s °C\n", numberFormat.format(temperaturaActual)));
            sb.append(String.format(Locale.US, "%s °F\n", numberFormat.format((9 * (temperaturaActual) / 5) + 32)));
            sb.append(String.format(Locale.US, "%s °K", numberFormat.format((temperaturaActual) + 273.15)));

            builder.setMessage(sb.toString());

            builder.setPositiveButton(getString(R.string.aceptar), null);

            builder.create().show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        int leer = ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION);

        if (leer == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, PERMISOS, REQUEST_CODE_UBICACION);
        }

        Location ultimaUbicacion = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (ultimaUbicacion != null) {
            ubicacionActual = new LatLng(ultimaUbicacion.getLatitude(), ultimaUbicacion.getLongitude());

            googleMap.addMarker(new MarkerOptions()
                    .position(ubicacionActual)
                    .title(getString(R.string.mi_ubicacion))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual, DEFAULT_ZOOM));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onClick(View v) {
        try {
            PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLACE_PICKER_REQUEST && resultCode == RESULT_OK) {


            Place place = PlacePicker.getPlace(this, data);

            googleMap.addMarker(new MarkerOptions()
                    .position(place.getLatLng())
                    .title(place.getName().toString())
                    .icon(BitmapDescriptorFactory.defaultMarker(COLORES_MARCADORES[numeroColorSeleccionado % NUMERO_COLORES])));

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(ubicacionActual)
                    .add(place.getLatLng())
                    .color(getResources().getColor(R.color.colorPrimary));

            googleMap.addPolyline(polylineOptions);

            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), DEFAULT_ZOOM));

            numeroColorSeleccionado++;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        capturarTemperatura(event.values[0]);
    }

    /**
     * Temperatura actual capturada por el sensor.
     * @param temperatura Temperatura actual.
     */
    private void capturarTemperatura(float temperatura) {
        temperaturaActual = temperatura;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}
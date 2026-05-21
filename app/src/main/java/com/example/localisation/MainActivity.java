package com.example.localisation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int CODE_PERM_POSITION = 404;
    private static final String API_ENDPOINT_URL = "http://10.0.2.2/localisation/createPosition.php";

    private TextView latitudeDisplay, longitudeDisplay, stateMessageDisplay;
    private View stateIndicatorDot;

    private RequestQueue networkQueue;
    private LocationManager trackingService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Liaison des composants graphiques
        latitudeDisplay = findViewById(R.id.tvLat);
        longitudeDisplay = findViewById(R.id.tvLon);
        stateMessageDisplay = findViewById(R.id.tvStatus);
        stateIndicatorDot = findViewById(R.id.statusDot);
        Button actionShowMap = findViewById(R.id.btnMap);

        // Initialisation des services requis
        networkQueue = Volley.newRequestQueue(getApplicationContext());
        trackingService = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Navigation vers la cartographie
        actionShowMap.setOnClickListener(view -> {
            Intent mapIntent = new Intent(MainActivity.this, MapsActivity.class);
            startActivity(mapIntent);
        });

        verifyAndRequestPermissions();
    }

    private void verifyAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeTracking();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, CODE_PERM_POSITION);
        }
    }

    @SuppressLint("MissingPermission")
    private void initializeTracking() {
        updateInterfaceStatus("Recherche du signal GPS...", "#E67E22");

        if (trackingService == null) return;

        // Activation des mises à jour sur les deux fournisseurs disponibles
        if (trackingService.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            trackingService.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this);
        }
        if (trackingService.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            trackingService.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 5f, this);
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        double currentLat = location.getLatitude();
        double currentLng = location.getLongitude();

        latitudeDisplay.setText(String.format(Locale.getDefault(), "%.6f°", currentLat));
        longitudeDisplay.setText(String.format(Locale.getDefault(), "%.6f°", currentLng));

        updateInterfaceStatus("Signal actif · envoi en cours...", "#2980B9");
        transmitCoordinates(currentLat, currentLng);
    }

    private void updateInterfaceStatus(String label, String colorCode) {
        int targetColor = Color.parseColor(colorCode);
        stateMessageDisplay.setText(label);
        stateMessageDisplay.setTextColor(targetColor);
        stateIndicatorDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(targetColor));
    }

    private void transmitCoordinates(final double lat, final double lng) {
        StringRequest coordPostRequest = new StringRequest(
                Request.Method.POST,
                API_ENDPOINT_URL,
                response -> updateInterfaceStatus("✓ Position sauvegardée", "#2ECC71"),
                error -> updateInterfaceStatus("✗ Erreur réseau", "#E74C3C")
        ) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> payload = new HashMap<>();
                SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                String hardwareId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                payload.put("latitude", String.valueOf(lat));
                payload.put("longitude", String.valueOf(lng));
                payload.put("date", isoFormatter.format(new Date()));
                payload.put("imei", hardwareId);

                return payload;
            }
        };

        networkQueue.add(coordPostRequest);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CODE_PERM_POSITION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeTracking();
            } else {
                updateInterfaceStatus("Permission refusée", "#E74C3C");
            }
        }
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        if (LocationManager.GPS_PROVIDER.equals(provider)) {
            updateInterfaceStatus("GPS désactivé", "#E74C3C");
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public void onProviderEnabled(@NonNull String provider) {}
}
package com.example.localisation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String FETCH_POSITIONS_ENDPOINT = "http://10.0.2.2/localisation/showPositions.php";

    private GoogleMap googleMapInstance;
    private RequestQueue networkRequestQueue;
    private TextView counterDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        counterDisplay = findViewById(R.id.tvMarkerCount);
        Button returnButton = findViewById(R.id.btnBack);

        returnButton.setOnClickListener(view -> finish());

        networkRequestQueue = Volley.newRequestQueue(getApplicationContext());

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        if (supportMapFragment != null) {
            supportMapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap gMap) {
        this.googleMapInstance = gMap;
        setupMapPreferences();
        retrieveLocationData();
    }

    /**
     * Applique les configurations visuelles et d'UI sur l'instance de la carte
     */
    private void setupMapPreferences() {
        try {
            boolean styleLoaded = googleMapInstance.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_dark));
        } catch (Exception ignored) {
            // Repli sur le thème par défaut si ressource introuvable
        }

        googleMapInstance.getUiSettings().setZoomControlsEnabled(true);
        googleMapInstance.getUiSettings().setCompassEnabled(true);
    }

    /**
     * Interroge le serveur pour récupérer la liste des coordonnées
     */
    private void retrieveLocationData() {
        counterDisplay.setText("Chargement...");

        JsonObjectRequest jsonRequest = new JsonObjectRequest(
                Request.Method.GET,
                FETCH_POSITIONS_ENDPOINT,
                null,
                response -> {
                    try {
                        JSONArray itemsList = response.getJSONArray("positions");
                        int totalItems = itemsList.length();

                        if (totalItems == 0) {
                            counterDisplay.setText("0 position enregistrée");
                            return;
                        }

                        // Gestion sémantique du pluriel alternative
                        String summaryText = totalItems + " position" + (totalItems > 1 ? "s localisée" : " localisée") + (totalItems > 1 ? "s" : "");
                        counterDisplay.setText(summaryText);

                        LatLngBounds.Builder areaBuilder = new LatLngBounds.Builder();

                        for (int index = 0; index < totalItems; index++) {
                            JSONObject singleObject = itemsList.getJSONObject(index);
                            double latitude = singleObject.getDouble("latitude");
                            double longitude = singleObject.getDouble("longitude");
                            String captureDate = singleObject.optString("date", "");

                            LatLng geographicPoint = new LatLng(latitude, longitude);
                            boolean isLatestElement = (index == totalItems - 1);

                            // Détermination de la couleur et du label du pointeur
                            float markerColor = isLatestElement ? BitmapDescriptorFactory.HUE_CYAN : BitmapDescriptorFactory.HUE_AZURE;
                            String markerTitle = isLatestElement ? "📍 Dernière position" : "Repère #" + (index + 1);

                            String infoSnippet = String.format(Locale.getDefault(),
                                    "%.5f, %.5f\n%s", latitude, longitude, captureDate);

                            googleMapInstance.addMarker(new MarkerOptions()
                                    .position(geographicPoint)
                                    .title(markerTitle)
                                    .snippet(infoSnippet)
                                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

                            areaBuilder.include(geographicPoint);
                        }

                        // Recentrage dynamique de la caméra
                        LatLngBounds finalCameraBounds = areaBuilder.build();
                        googleMapInstance.animateCamera(CameraUpdateFactory.newLatLngBounds(finalCameraBounds, 150));

                    } catch (JSONException e) {
                        counterDisplay.setText("Erreur de formatage des données");
                    }
                },
                error -> counterDisplay.setText("Erreur de connexion réseau")
        );

        networkRequestQueue.add(jsonRequest);
    }
}
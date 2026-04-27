package com.example.travelpath.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelpath.R;
import com.example.travelpath.models.UserPreferences;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TravelPathCriteriaFragment extends Fragment {

    private AutoCompleteTextView etDestination;
    private MapView mapView;
    private ChipGroup cgActivities;
    private TextInputEditText etMandatoryPlaces;
    private TextView tvBudgetText;
    private Slider sliderBudget;
    private TextView tvDurationText;
    private Slider sliderDuration;
    private MaterialButtonToggleGroup tgEffort;
    private MaterialButtonToggleGroup tgWeather;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private List<PlaceResult> currentResults = new ArrayList<>();

    private static class PlaceResult {
        String name;
        double lat, lon;

        PlaceResult(String name, double lat, double lon) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().setUserAgentValue(getContext().getPackageName());
        return inflater.inflate(R.layout.fragment_travel_path_criteria, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etDestination = view.findViewById(R.id.etDestination);
        mapView = view.findViewById(R.id.mapView);
        cgActivities = view.findViewById(R.id.cgActivities);
        etMandatoryPlaces = view.findViewById(R.id.etMandatoryPlaces);
        tvBudgetText = view.findViewById(R.id.tvBudgetText);
        sliderBudget = view.findViewById(R.id.sliderBudget);
        tvDurationText = view.findViewById(R.id.tvDurationText);
        sliderDuration = view.findViewById(R.id.sliderDuration);
        tgEffort = view.findViewById(R.id.tgEffort);
        tgWeather = view.findViewById(R.id.tgWeather);

        setupAutoComplete();
        setupMap();

        sliderBudget.addOnChangeListener((slider, value, fromUser) -> {
            tvBudgetText.setText("💰 Budget maximum: " + (int) value + " €");
        });

        sliderDuration.addOnChangeListener((slider, value, fromUser) -> {
            tvDurationText.setText("📅 Durée: " + (int) value + (value > 1 ? " jours" : " jour"));
        });

        view.findViewById(R.id.btnGenerate).setOnClickListener(v -> generatePaths());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
    }

    private void setupMap() {
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
    }

    private void setupAutoComplete() {
        ArrayAdapter<PlaceResult> adapter = new ArrayAdapter<PlaceResult>(getContext(), android.R.layout.simple_dropdown_item_1line, currentResults) {
            @NonNull
            @Override
            public android.widget.Filter getFilter() {
                return new android.widget.Filter() {
                    @Override
                    protected FilterResults performFiltering(CharSequence constraint) {
                        FilterResults results = new FilterResults();
                        results.values = currentResults;
                        results.count = currentResults.size();
                        return results;
                    }

                    @Override
                    protected void publishResults(CharSequence constraint, FilterResults results) {
                        notifyDataSetChanged();
                    }
                };
            }
        };

        etDestination.setAdapter(adapter);

        etDestination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() >= 2) {
                    searchPlaces(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        etDestination.setOnItemClickListener((parent, view, position, id) -> {
            PlaceResult selected = (PlaceResult) parent.getItemAtPosition(position);
            updateMap(selected);
        });
    }

    private void searchPlaces(String query) {
        executorService.execute(() -> {
            try {
                URL url = new URL("https://photon.komoot.io/api/?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&limit=10&lang=fr");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject json = new JSONObject(sb.toString());
                JSONArray features = json.getJSONArray("features");

                List<PlaceResult> results = new ArrayList<>();
                for (int i = 0; i < features.length(); i++) {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.getJSONObject("properties");
                    JSONArray coords = feature.getJSONObject("geometry").getJSONArray("coordinates");

                    String name = properties.optString("name", "");
                    String city = properties.optString("city", "");
                    String country = properties.optString("country", "");
                    String state = properties.optString("state", "");

                    StringBuilder fullName = new StringBuilder(name);
                    if (!city.isEmpty() && !city.equals(name)) fullName.append(", ").append(city);
                    if (!state.isEmpty()) fullName.append(", ").append(state);
                    if (!country.isEmpty()) fullName.append(", ").append(country);

                    results.add(new PlaceResult(fullName.toString(), coords.getDouble(1), coords.getDouble(0)));
                }

                mainHandler.post(() -> {
                    currentResults.clear();
                    currentResults.addAll(results);
                    if (!results.isEmpty()) {
                        etDestination.showDropDown();
                    }
                    ArrayAdapter<PlaceResult> adapterObj = (ArrayAdapter<PlaceResult>) etDestination.getAdapter();
                    adapterObj.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private double selectedLat, selectedLon;

    private void updateMap(PlaceResult place) {
        selectedLat = place.lat;
        selectedLon = place.lon;
        mapView.setVisibility(View.VISIBLE);
        GeoPoint point = new GeoPoint(place.lat, place.lon);
        mapView.getController().animateTo(point);

        mapView.getOverlays().clear();
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(place.name);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    private void generatePaths() {
        String destination = etDestination.getText().toString();
        if (destination.isEmpty()) {
            etDestination.setError("Destination requise");
            return;
        }

        List<String> activities = new ArrayList<>();
        for (int i = 0; i < cgActivities.getChildCount(); i++) {
            Chip chip = (Chip) cgActivities.getChildAt(i);
            if (chip.isChecked()) {
                activities.add(chip.getText().toString());
            }
        }

        if (activities.isEmpty()) {
            Toast.makeText(getContext(), "Veuillez choisir au moins une activité", Toast.LENGTH_SHORT).show();
            return;
        }

        String mandatoryStr = etMandatoryPlaces.getText().toString();
        List<String> mandatoryPlaces = new ArrayList<>();
        if (!mandatoryStr.isEmpty()) {
            mandatoryPlaces = Arrays.asList(mandatoryStr.split(","));
        }

        int budget = (int) sliderBudget.getValue();
        int duration = (int) sliderDuration.getValue();

        // Effort
        String effort = "MODERE";
        int effortId = tgEffort.getCheckedButtonId();
        if (effortId == R.id.btnEffortFaible) effort = "FAIBLE";
        else if (effortId == R.id.btnEffortEleve) effort = "ELEVE";

        // Weather
        String weather = null;
        int weatherId = tgWeather.getCheckedButtonId();
        if (weatherId == R.id.btnWeatherFroid) weather = "FROID";
        else if (weatherId == R.id.btnWeatherChaleur) weather = "CHALEUR";
        else if (weatherId == R.id.btnWeatherHumidite) weather = "HUMIDITE";

        // Navigate to Result Fragment with ALL parameters
        TravelPathResultFragment resultFragment = new TravelPathResultFragment();
        Bundle args = new Bundle();
        args.putString("destination", destination);
        args.putDouble("lat", selectedLat);
        args.putDouble("lon", selectedLon);
        args.putStringArrayList("activities", new ArrayList<>(activities));
        args.putInt("budget", budget);
        args.putInt("duration", duration);
        args.putString("effort", effort);
        if (weather != null) args.putString("weather", weather);
        args.putStringArrayList("mandatoryPlaces", new ArrayList<>(mandatoryPlaces));
        resultFragment.setArguments(args);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, resultFragment)
                .addToBackStack(null)
                .commit();
    }
}

package com.traveling.app.travelpath.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.io.File;
import java.io.FileOutputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.traveling.app.R;
import com.traveling.app.travelpath.adapters.TripStepAdapter;
import com.traveling.app.travelpath.models.TripStep;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;

public class TravelPathResultFragment extends Fragment {

    private RecyclerView rvTimeline;
    private TabLayout tabLayout;
    private MapView mapView;
    private List<TripStep> currentSteps = new ArrayList<>();
    private List<TripStep> allGeneratedSteps = new ArrayList<>();

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private String selectedDestination;
    private List<String> selectedActivities;
    private double baseLat, baseLon;
    private int userBudget = 1500;
    private int userDuration = 1;
    private String userEffort = "MODERE";
    private String userWeather = null;
    private List<String> userMandatoryPlaces = new ArrayList<>();

    // Weather data
    private String weatherDescription = "";
    private double weatherTemp = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Configuration.getInstance().load(getContext(), android.preference.PreferenceManager.getDefaultSharedPreferences(getContext()));
        Configuration.getInstance().setUserAgentValue(getContext().getPackageName());
        return inflater.inflate(R.layout.fragment_travel_path_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            selectedDestination = getArguments().getString("destination");
            selectedActivities = getArguments().getStringArrayList("activities");
            baseLat = getArguments().getDouble("lat");
            baseLon = getArguments().getDouble("lon");
            userBudget = getArguments().getInt("budget", 1500);
            userDuration = getArguments().getInt("duration", 1);
            userEffort = getArguments().getString("effort", "MODERE");
            userWeather = getArguments().getString("weather", null);
            userMandatoryPlaces = getArguments().getStringArrayList("mandatoryPlaces");
        }

        rvTimeline = view.findViewById(R.id.rvTimeline);
        tabLayout = view.findViewById(R.id.tabLayout);
        mapView = view.findViewById(R.id.mapView);

        // OSMDroid configuration
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.0);

        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));

        // Fetch weather first, then generate itinerary
        fetchWeather(() -> generateItinerary("Economique"));

        tabLayout.setVisibility(View.VISIBLE);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int selectedDay = tab.getPosition() + 1; // 0-based index
                filterStepsForDay(selectedDay);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        view.findViewById(R.id.btnRegenerate).setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        // Like / Dislike Logic
        ToggleButton btnLike = view.findViewById(R.id.btnLike);
        ToggleButton btnDislike = view.findViewById(R.id.btnDislike);

        btnLike.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnDislike.setChecked(false);
                saveItinerary();
                Toast.makeText(getContext(), "Parcours sauvegardé dans vos favoris !", Toast.LENGTH_SHORT).show();
            }
        });

        btnDislike.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                btnLike.setChecked(false);
                Toast.makeText(getContext(), "Vous n'aimez pas ce parcours.", Toast.LENGTH_SHORT).show();
            }
        });

        // Export PDF
        view.findViewById(R.id.btnExportPdf).setOnClickListener(v -> exportToPdf());

        // Share
        view.findViewById(R.id.btnShare).setOnClickListener(v -> shareItinerary());
    }

    // ───────────── Weather API (Open-Meteo, gratuit, pas de clé) ─────────────

    private void fetchWeather(Runnable onComplete) {
        executorService.execute(() -> {
            try {
                String urlStr = "https://api.open-meteo.com/v1/forecast?latitude=" + baseLat
                        + "&longitude=" + baseLon
                        + "&current=temperature_2m,weather_code"
                        + "&timezone=auto";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject json = new JSONObject(sb.toString());
                JSONObject current = json.getJSONObject("current");
                weatherTemp = current.getDouble("temperature_2m");
                int weatherCode = current.getInt("weather_code");
                weatherDescription = getWeatherDescription(weatherCode);

                mainHandler.post(() -> {
                    updateWeatherUI();
                    onComplete.run();
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Erreur API Météo: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                    weatherDescription = "Indisponible";
                    updateWeatherUI();
                    onComplete.run();
                });
            }
        });
    }

    private String getWeatherDescription(int code) {
        if (code == 0) return "☀️ Ciel dégagé";
        if (code <= 3) return "⛅ Partiellement nuageux";
        if (code <= 48) return "🌫️ Brouillard";
        if (code <= 57) return "🌧️ Bruine";
        if (code <= 67) return "🌧️ Pluie";
        if (code <= 77) return "🌨️ Neige";
        if (code <= 82) return "🌧️ Averses";
        if (code <= 86) return "🌨️ Averses de neige";
        if (code >= 95) return "⛈️ Orage";
        return "🌤️ Variable";
    }

    private void updateWeatherUI() {
        View view = getView();
        if (view == null) return;
        TextView tvWeather = view.findViewById(R.id.tvWeather);
        if (tvWeather != null) {
            tvWeather.setText(weatherDescription + " • " + String.format("%.1f", weatherTemp) + "°C");
        }
    }

    // ───────────── Itinerary Generation ─────────────

    private void generateItinerary(String planName) {
        Toast.makeText(getContext(), "Calcul de l'itinéraire optimal...", Toast.LENGTH_SHORT).show();
        currentSteps.clear();
        TripStepAdapter adapter = new TripStepAdapter(currentSteps);
        rvTimeline.setAdapter(adapter);

        executorService.execute(() -> {
            try {
                StringBuilder tags = new StringBuilder();
                if (selectedActivities != null) {
                    for (String act : selectedActivities) {
                        String actLower = act.toLowerCase();
                        if (actLower.contains("culture")) {
                            tags.append("node[\"tourism\"=\"museum\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"amenity\"=\"theatre\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                        }
                        if (actLower.contains("restauration")) {
                            tags.append("node[\"amenity\"=\"restaurant\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"amenity\"=\"cafe\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                        }
                        if (actLower.contains("nature")) {
                            tags.append("node[\"leisure\"=\"park\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"leisure\"=\"garden\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                        }
                        if (actLower.contains("loisirs")) {
                            tags.append("node[\"tourism\"=\"attraction\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"leisure\"=\"playground\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                        }
                        if (actLower.contains("couverte") || actLower.contains("découverte")) {
                            tags.append("node[\"tourism\"=\"attraction\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"historic\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"tourism\"=\"viewpoint\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                        }
                        if (actLower.contains("shopping")) {
                            tags.append("node[\"shop\"=\"mall\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                            tags.append("node[\"shop\"=\"department_store\"](around:5000,").append(baseLat).append(",").append(baseLon).append(");");
                        }
                    }
                }

                // Fallback
                if (tags.length() == 0) {
                    tags.append("node[\"tourism\"=\"attraction\"](around:10000,").append(baseLat).append(",").append(baseLon).append(");");
                }

                // Limit results based on plan type and duration
                int stepsPerDay = 6;
                if (planName.contains("quilibr")) stepsPerDay = 5;
                else if (planName.contains("onfort")) stepsPerDay = 4;
                
                int maxResults = stepsPerDay * userDuration;

                String query = "[out:json];(" + tags.toString() + ");out " + (maxResults + 5) + ";";
                URL url = new URL("https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8"));

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("User-Agent", "TravelingApp/1.0");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);

                JSONObject json = new JSONObject(sb.toString());
                JSONArray elements = json.getJSONArray("elements");

                List<TripStep> realPlaces = new ArrayList<>();
                int hour = 9;
                int currentDay = 1;

                // Duration per step based on effort
                int durationPerStep = 2; // hours
                if (userEffort.equals("FAIBLE")) durationPerStep = 2;
                else if (userEffort.equals("ELEVE")) durationPerStep = 3;

                double prevLat = baseLat, prevLon = baseLon;

                // Intégration des lieux impératifs saisis par l'utilisateur
                int stepIndex = 0;
                if (userMandatoryPlaces != null) {
                    for (String placeName : userMandatoryPlaces) {
                        String trimmed = placeName.trim();
                        if (trimmed.isEmpty()) continue;
                        
                        // Créer des coordonnées légèrement décalées autour de la destination
                        double lat = baseLat + (stepIndex * 0.003 - 0.001);
                        double lon = baseLon + (stepIndex * 0.003 + 0.001);

                        String timeSlot;
                        if (hour < 12) {
                            timeSlot = String.format("Jour %d • %02d:00 - %02d:00 (Matin)", currentDay, hour, hour + durationPerStep);
                        } else if (hour < 18) {
                            timeSlot = String.format("Jour %d • %02d:00 - %02d:00 (Après-midi)", currentDay, hour, hour + durationPerStep);
                        } else {
                            timeSlot = String.format("Jour %d • %02d:00 - %02d:00 (Soir)", currentDay, hour, hour + durationPerStep);
                        }

                        TripStep step = new TripStep(trimmed, "Visite impérative planifiée", timeSlot, getRealisticBudget("attraction", planName), lat, lon, "Découverte (Requis)", null);

                        if (!realPlaces.isEmpty()) {
                            double dist = haversineKm(prevLat, prevLon, lat, lon);
                            int walkMinutes = (int) (dist / 5.0 * 60);
                            String transportEmoji = walkMinutes <= 20 ? "🚶" : "🚌";
                            String transportText = transportEmoji + " ~" + Math.max(1, walkMinutes) + " min (" + String.format("%.1f", dist) + " km)";
                            step.setTransportInfo(transportText);
                        }

                        prevLat = lat;
                        prevLon = lon;
                        realPlaces.add(step);
                        hour += durationPerStep + 1;
                        stepIndex++;
                        if (hour >= 20) {
                            hour = 9;
                            currentDay++;
                        }
                        if (currentDay > userDuration) break;
                    }
                }

                for (int i = 0; i < elements.length() && realPlaces.size() < maxResults; i++) {
                    JSONObject el = elements.getJSONObject(i);
                    JSONObject tagsObj = el.optJSONObject("tags");
                    if (tagsObj == null) continue;

                    String name = tagsObj.optString("name", "");
                    if (name.isEmpty()) continue; // Skip unnamed places

                    String type = tagsObj.optString("amenity",
                            tagsObj.optString("tourism",
                                    tagsObj.optString("leisure",
                                            tagsObj.optString("historic",
                                                    tagsObj.optString("shop", "Visite")))));

                    // Capitalize type
                    String typeDisplay = type.substring(0, 1).toUpperCase() + type.substring(1).replace("_", " ");

                    String timeSlot;
                    if (hour < 12) {
                        timeSlot = String.format("Jour %d • %02d:00 - %02d:00 (Matin)", currentDay, hour, hour + durationPerStep);
                    } else if (hour < 18) {
                        timeSlot = String.format("Jour %d • %02d:00 - %02d:00 (Après-midi)", currentDay, hour, hour + durationPerStep);
                    } else {
                        timeSlot = String.format("Jour %d • %02d:00 - %02d:00 (Soir)", currentDay, hour, hour + durationPerStep);
                    }

                    double lat = el.getDouble("lat");
                    double lon = el.getDouble("lon");

                    // Build description from OSM tags
                    StringBuilder desc = new StringBuilder();
                    String osmDesc = tagsObj.optString("description", "");
                    if (!osmDesc.isEmpty()) {
                        desc.append(osmDesc);
                    } else {
                        desc.append("Lieu authentique (").append(typeDisplay).append(") à découvrir");
                    }
                    String openingHours = tagsObj.optString("opening_hours", "");
                    if (!openingHours.isEmpty()) {
                        desc.append(" • Horaires: ").append(openingHours);
                    }

                    TripStep step = new TripStep(name, desc.toString(), timeSlot, getRealisticBudget(type, planName), lat, lon, typeDisplay, null);

                    // Calculate realistic transport from previous step
                    if (!realPlaces.isEmpty()) {
                        double dist = haversineKm(prevLat, prevLon, lat, lon);
                        int walkMinutes = (int) (dist / 5.0 * 60); // 5 km/h walking speed
                        String transportEmoji = walkMinutes <= 20 ? "🚶" : "🚌";
                        String transportText = transportEmoji + " ~" + Math.max(1, walkMinutes) + " min (" + String.format("%.1f", dist) + " km)";
                        step.setTransportInfo(transportText);
                    }

                    prevLat = lat;
                    prevLon = lon;
                    realPlaces.add(step);
                    hour += durationPerStep + 1; // +1 for travel
                    if (hour >= 20) {
                        hour = 9;
                        currentDay++;
                    }
                    if (currentDay > userDuration) break;
                }

                mainHandler.post(() -> {
                    if (realPlaces.isEmpty()) {
                        Toast.makeText(getContext(), "Aucun lieu trouvé à proximité, affichage par défaut.", Toast.LENGTH_SHORT).show();
                        generateFallbackItinerary(planName);
                    } else {
                        allGeneratedSteps.clear();
                        allGeneratedSteps.addAll(realPlaces);
                        setupDayTabs();
                        filterStepsForDay(1); // Select first day by default
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), "Erreur API Trajet: " + e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
                    generateFallbackItinerary(planName);
                });
            }
        });
    }

    private void setupDayTabs() {
        tabLayout.removeAllTabs();
        for (int i = 1; i <= userDuration; i++) {
            tabLayout.addTab(tabLayout.newTab().setText("Jour " + i));
        }
    }

    private void filterStepsForDay(int day) {
        currentSteps.clear();
        String dayPrefix = "Jour " + day + " •";
        for (TripStep step : allGeneratedSteps) {
            if (step.getTimeSlot() != null && step.getTimeSlot().startsWith(dayPrefix)) {
                currentSteps.add(step);
            }
        }
        if (currentSteps.isEmpty() && day == 1) {
            // Si le formatage n'a pas matché pour une raison obscure, afficher tout
            currentSteps.addAll(allGeneratedSteps);
        }
        TripStepAdapter adapter = (TripStepAdapter) rvTimeline.getAdapter();
        if (adapter != null) adapter.notifyDataSetChanged();
        updateMapMarkers();
        updateMetricsUI(calculateTotalBudget(), calculateTotalHours(), "Economique");
    }

    // ───────────── Haversine Distance ─────────────

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ───────────── Metrics ─────────────

    private int calculateTotalBudget() {
        int total = 0;
        for (TripStep s : currentSteps) total += s.getBudget();
        return total;
    }

    private int calculateTotalHours() {
        return currentSteps.size() * 2;
    }

    private void generateFallbackItinerary(String planName) {
        allGeneratedSteps.clear();
        String prefix = selectedDestination != null ? selectedDestination.split(",")[0].trim() : "Lieu";
        
        int hour = 9;
        for (int day = 1; day <= userDuration; day++) {
            TripStep s1 = new TripStep("Exploration de " + prefix, "Découvrez les rues historiques.", String.format("Jour %d • %02d:00 - %02d:00 (Matin)", day, hour, hour+2), 0, baseLat, baseLon, "Découverte", null);
            TripStep s2 = new TripStep("Déjeuner local", "Cuisine locale typique.", String.format("Jour %d • %02d:00 - %02d:00 (Midi)", day, hour+3, hour+5), 25, baseLat + 0.002, baseLon + 0.001, "Restauration", null);
            s2.setTransportInfo("🚶 ~10 min (0.8 km)");
            TripStep s3 = new TripStep("Visite culturelle", "Musée ou monument.", String.format("Jour %d • %02d:00 - %02d:00 (Après-midi)", day, hour+6, hour+8), 15, baseLat - 0.001, baseLon + 0.002, "Culture", null);
            s3.setTransportInfo("🚶 ~12 min (1.0 km)");
            
            allGeneratedSteps.add(s1);
            allGeneratedSteps.add(s2);
            allGeneratedSteps.add(s3);
        }

        setupDayTabs();
        filterStepsForDay(1);
    }

    private void updateMetricsUI(int totalBudget, int totalHours, String planName) {
        View view = getView();
        if (view == null) return;

        TextView tvTotalBudget = view.findViewById(R.id.tvTotalBudget);
        TextView tvTotalTime = view.findViewById(R.id.tvTotalTime);
        TextView tvEffort = view.findViewById(R.id.tvEffort);

        if (tvTotalBudget != null) tvTotalBudget.setText("💰 " + totalBudget + " €");
        if (tvTotalTime != null) tvTotalTime.setText("⏱️ " + totalHours + " h");

        if (tvEffort != null) {
            if (planName.contains("onfort")) tvEffort.setText("💪 Faible");
            else if (planName.contains("quilibr")) tvEffort.setText("💪 Modéré");
            else tvEffort.setText("💪 Élevé");
        }
    }

    // ───────────── Map ─────────────

    private void updateMapMarkers() {
        if (mapView == null) return;

        mapView.getOverlays().clear();

        // Draw polyline connecting all steps
        if (currentSteps.size() > 1) {
            Polyline polyline = new Polyline();
            List<GeoPoint> points = new ArrayList<>();
            for (TripStep step : currentSteps) {
                points.add(new GeoPoint(step.getLatitude(), step.getLongitude()));
            }
            polyline.setPoints(points);
            polyline.getOutlinePaint().setColor(0xFFD81B60);
            polyline.getOutlinePaint().setStrokeWidth(5f);
            mapView.getOverlays().add(polyline);
        }

        // Add numbered markers
        for (int i = 0; i < currentSteps.size(); i++) {
            TripStep step = currentSteps.get(i);
            Marker marker = new Marker(mapView);
            GeoPoint point = new GeoPoint(step.getLatitude(), step.getLongitude());
            marker.setPosition(point);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle((i + 1) + ". " + step.getTitle());
            marker.setSnippet(step.getTimeSlot());
            mapView.getOverlays().add(marker);
        }

        if (!currentSteps.isEmpty()) {
            mapView.getController().setCenter(new GeoPoint(currentSteps.get(0).getLatitude(), currentSteps.get(0).getLongitude()));
        }

        mapView.invalidate();
    }

    // ───────────── Save Itinerary ─────────────

    private void saveItinerary() {
        try {
            SharedPreferences prefs = getContext().getSharedPreferences("saved_itineraries", Context.MODE_PRIVATE);
            Gson gson = new Gson();

            String existing = prefs.getString("itineraries_list", "[]");
            List<SavedItinerary> list = gson.fromJson(existing, new TypeToken<List<SavedItinerary>>() {}.getType());
            if (list == null) list = new ArrayList<>();

            SavedItinerary saved = new SavedItinerary();
            saved.destination = selectedDestination;
            saved.steps = currentSteps;
            saved.totalBudget = calculateTotalBudget();
            saved.totalHours = calculateTotalHours();
            saved.savedAt = System.currentTimeMillis();

            list.add(saved);
            prefs.edit().putString("itineraries_list", gson.toJson(list)).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class SavedItinerary {
        String destination;
        List<TripStep> steps;
        int totalBudget;
        int totalHours;
        long savedAt;
    }

    // ───────────── Share Itinerary ─────────────

    private void shareItinerary() {
        StringBuilder sb = new StringBuilder();
        sb.append("🗺️ Mon itinéraire TravelPath\n");
        sb.append("📍 Destination: ").append(selectedDestination).append("\n");
        sb.append("🌤️ Météo: ").append(weatherDescription).append(" ").append(String.format("%.1f", weatherTemp)).append("°C\n");
        sb.append("💰 Budget: ").append(calculateTotalBudget()).append(" €\n");
        sb.append("⏱️ Durée: ").append(calculateTotalHours()).append(" h\n\n");

        for (int i = 0; i < currentSteps.size(); i++) {
            TripStep step = currentSteps.get(i);
            sb.append((i + 1)).append(". ").append(step.getTitle()).append("\n");
            sb.append("   ").append(step.getTimeSlot()).append("\n");
            sb.append("   ").append(step.getDescription()).append("\n\n");
        }

        sb.append("Généré par TravelPath 🧭");

        String fullText = sb.toString();

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Partager l'itinéraire")
            .setItems(new CharSequence[]{"Publier sur TravelShare", "Partager via d'autres apps"}, (dialog, which) -> {
                if (which == 0) {
                    // Publier sur TravelShare
                    Intent pubIntent = new Intent(getActivity(), com.traveling.app.travelshare.ui.PublishActivity.class);
                    pubIntent.putExtra("PREFILL_DESC", fullText);
                    pubIntent.putExtra("PREFILL_LOCATION", selectedDestination);
                    startActivity(pubIntent);
                } else {
                    // Partager via le système
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/plain");
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Mon itinéraire TravelPath - " + selectedDestination);
                    shareIntent.putExtra(Intent.EXTRA_TEXT, fullText);
                    startActivity(Intent.createChooser(shareIntent, "Partager l'itinéraire"));
                }
            })
            .show();
    }

    // ───────────── Export PDF ─────────────

    private void exportToPdf() {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint titlePaint = new Paint();
        titlePaint.setTextSize(22);
        titlePaint.setFakeBoldText(true);
        titlePaint.setColor(0xFFD81B60);

        Paint headerPaint = new Paint();
        headerPaint.setTextSize(14);
        headerPaint.setFakeBoldText(true);

        Paint bodyPaint = new Paint();
        bodyPaint.setTextSize(12);

        Paint metricPaint = new Paint();
        metricPaint.setTextSize(13);
        metricPaint.setColor(0xFF666666);

        int y = 50;

        // Title
        canvas.drawText("Mon Itinéraire TravelPath", 50, y, titlePaint);
        y += 35;

        // Destination
        headerPaint.setColor(0xFF333333);
        canvas.drawText("Destination : " + (selectedDestination != null ? selectedDestination : ""), 50, y, headerPaint);
        y += 25;

        // Weather
        canvas.drawText("Météo : " + weatherDescription + " " + String.format("%.1f", weatherTemp) + "°C", 50, y, metricPaint);
        y += 25;

        // Metrics
        canvas.drawText("Budget total : " + calculateTotalBudget() + " € | Durée : " + calculateTotalHours() + " h | Effort : " + userEffort, 50, y, metricPaint);
        y += 15;

        // Separator line
        Paint linePaint = new Paint();
        linePaint.setColor(0xFFD81B60);
        linePaint.setStrokeWidth(2);
        canvas.drawLine(50, y, 545, y, linePaint);
        y += 25;

        // Steps
        for (int i = 0; i < currentSteps.size(); i++) {
            TripStep step = currentSteps.get(i);

            if (y > 780) {
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 50;
            }

            headerPaint.setColor(0xFFD81B60);
            canvas.drawText((i + 1) + ". " + step.getTitle(), 50, y, headerPaint);
            y += 18;

            bodyPaint.setColor(0xFF666666);
            canvas.drawText("    " + step.getTimeSlot(), 50, y, bodyPaint);
            y += 16;
            canvas.drawText("    " + step.getDescription(), 50, y, bodyPaint);
            y += 16;
            canvas.drawText("    Budget : " + step.getBudget() + "€ • " + step.getActivityType(), 50, y, bodyPaint);
            y += 16;

            if (step.getTransportInfo() != null) {
                canvas.drawText("    " + step.getTransportInfo(), 50, y, bodyPaint);
                y += 16;
            }

            y += 10;
        }

        pdfDocument.finishPage(page);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs();
        }
        
        String fileName = "Itineraire_TravelPath_" + System.currentTimeMillis() + ".pdf";
        File file = new File(downloadsDir, fileName);
        
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(getContext(), "PDF exporté dans Téléchargements !", Toast.LENGTH_SHORT).show();
            
            // Ouvrir le PDF automatiquement
            android.net.Uri pdfUri = androidx.core.content.FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            Intent chooser = Intent.createChooser(intent, "Ouvrir le PDF avec...");
            startActivity(chooser);
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erreur lors de l'exportation PDF", Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }

    private int getRealisticBudget(String rawType, String planName) {
        String plan = planName.toLowerCase();
        String type = rawType.toLowerCase();
        java.util.Random random = new java.util.Random();
        
        if (type.contains("restaurant") || type.contains("cafe") || type.contains("fast_food") || type.contains("bar") || type.contains("food")) {
            if (plan.contains("confort")) return 45 + random.nextInt(46); // 45 - 90
            if (plan.contains("quilibr")) return 20 + random.nextInt(16); // 20 - 35
            return 8 + random.nextInt(8); // 8 - 15
        }
        
        if (type.contains("shop") || type.contains("mall") || type.contains("boutique")) {
            if (plan.contains("confort")) return 100 + random.nextInt(151); // 100 - 250
            if (plan.contains("quilibr")) return 30 + random.nextInt(51); // 30 - 80
            return 10 + random.nextInt(16); // 10 - 25
        }
        
        if (type.contains("museum") || type.contains("theatre") || type.contains("cinema") || type.contains("historic") || type.contains("attraction")) {
            if (plan.contains("confort")) return 20 + random.nextInt(21); // 20 - 40
            if (plan.contains("quilibr")) return 8 + random.nextInt(11); // 8 - 18
            return 0 + random.nextInt(6); // 0 - 5 (often free)
        }
        
        if (type.contains("park") || type.contains("garden") || type.contains("playground") || type.contains("viewpoint")) {
            if (plan.contains("confort")) return 5 + random.nextInt(11); // 5 - 15
            return 0; // Free
        }
        
        // Default / General
        if (plan.contains("confort")) return 30 + random.nextInt(31); // 30 - 60
        if (plan.contains("quilibr")) return 15 + random.nextInt(16); // 15 - 30
        return 5 + random.nextInt(11); // 5 - 15
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
}

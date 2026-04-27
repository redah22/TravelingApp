package com.example.travelpath.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelpath.R;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Show logged-in user
        SharedPreferences prefs = getContext().getSharedPreferences("user_auth", Context.MODE_PRIVATE);
        String currentUser = prefs.getString("current_user", "anonymous");
        TextView tvWelcome = view.findViewById(R.id.tvWelcomeUser);
        if (tvWelcome != null) {
            if ("anonymous".equals(currentUser)) {
                tvWelcome.setText("Mode anonyme • Connectez-vous pour plus de fonctionnalités");
            } else {
                tvWelcome.setText("Connecté en tant que " + currentUser);
            }
        }

        view.findViewById(R.id.cardTravelShare).setOnClickListener(v -> {
            Toast.makeText(getContext(), "TravelShare — Partie de votre binôme", Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.cardTravelPath).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TravelPathCriteriaFragment())
                    .addToBackStack(null)
                    .commit();
        });
    }
}

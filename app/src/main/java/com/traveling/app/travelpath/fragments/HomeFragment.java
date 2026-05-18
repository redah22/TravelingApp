package com.traveling.app.travelpath.fragments;

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

import com.traveling.app.R;

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
            String displayName = currentUser;
            if (!"anonymous".equals(currentUser) && currentUser.contains("@")) {
                displayName = currentUser.split("@")[0];
            }
            if ("anonymous".equals(currentUser)) {
                tvWelcome.setText("Mode anonyme • Connectez-vous pour plus de fonctionnalités");
            } else {
                tvWelcome.setText("Connecté en tant que " + displayName);
            }
        }

        view.findViewById(R.id.cardTravelShare).setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getActivity(), com.traveling.app.travelshare.ui.FeedActivity.class);

            String displayName = currentUser;
            if (!"anonymous".equals(currentUser) && currentUser.contains("@")) {
                displayName = currentUser.split("@")[0];
            }

            intent.putExtra("CURRENT_USER_NAME", displayName);
            intent.putExtra("IS_ANONYMOUS", "anonymous".equals(currentUser));
            startActivity(intent);
        });

        view.findViewById(R.id.cardTravelPath).setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TravelPathCriteriaFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Animation d'entrée sur les cartes
        android.view.animation.Animation anim = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_fade);
        anim.setStartOffset(100);
        view.findViewById(R.id.cardTravelShare).startAnimation(anim);

        android.view.animation.Animation anim2 = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_fade);
        anim2.setStartOffset(200);
        view.findViewById(R.id.cardTravelPath).startAnimation(anim2);
    }
}

package com.example.travelpath.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.travelpath.R;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {

    private TextInputEditText etEmail, etPassword;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etEmail);
        etPassword = view.findViewById(R.id.etPassword);

        // Login button
        view.findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email requis");
                return;
            }
            if (password.isEmpty()) {
                etPassword.setError("Mot de passe requis");
                return;
            }

            // Check credentials from SharedPreferences
            SharedPreferences prefs = getContext().getSharedPreferences("user_auth", Context.MODE_PRIVATE);
            String savedPassword = prefs.getString("pass_" + email, null);

            if (savedPassword == null) {
                Toast.makeText(getContext(), "Aucun compte trouvé. Inscrivez-vous d'abord.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!savedPassword.equals(password)) {
                Toast.makeText(getContext(), "Mot de passe incorrect", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save logged-in state
            prefs.edit().putString("current_user", email).apply();
            Toast.makeText(getContext(), "Bienvenue, " + email + " !", Toast.LENGTH_SHORT).show();

            navigateToHome();
        });

        // Register link
        view.findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Email requis");
                return;
            }
            if (!email.contains("@")) {
                etEmail.setError("Email invalide");
                return;
            }
            if (password.isEmpty() || password.length() < 4) {
                etPassword.setError("Mot de passe requis (min. 4 caractères)");
                return;
            }

            SharedPreferences prefs = getContext().getSharedPreferences("user_auth", Context.MODE_PRIVATE);
            String existingPass = prefs.getString("pass_" + email, null);

            if (existingPass != null) {
                Toast.makeText(getContext(), "Ce compte existe déjà. Connectez-vous.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Register user
            prefs.edit()
                    .putString("pass_" + email, password)
                    .putString("current_user", email)
                    .apply();

            Toast.makeText(getContext(), "Compte créé ! Bienvenue !", Toast.LENGTH_SHORT).show();
            navigateToHome();
        });

        // Anonymous mode
        view.findViewById(R.id.btnAnonymous).setOnClickListener(v -> {
            SharedPreferences prefs = getContext().getSharedPreferences("user_auth", Context.MODE_PRIVATE);
            prefs.edit().putString("current_user", "anonymous").apply();
            navigateToHome();
        });
    }

    private void navigateToHome() {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }
}

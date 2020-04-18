package com.example.habr;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference preference = findPreference("theme");
            setPrefIcon(Objects.requireNonNull(preference));
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            setPrefIcon(preference);
            return super.onPreferenceTreeClick(preference);
        }
        private void setPrefIcon(Preference preference) {
            if(preference.getKey().equals("theme")) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Objects.requireNonNull(getContext()));
                if(!sharedPreferences.getBoolean("theme",true)) {
                    preference.setIcon(R.drawable.ic_wb_sunny_black_24dp);
                } else {
                    preference.setIcon(R.drawable.ic_brightness_4_black_24dp);
                }
            }
        }
    }

}
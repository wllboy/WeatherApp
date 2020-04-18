package com.example.habr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import com.snappydb.DB;
import com.snappydb.DBFactory;
import com.snappydb.SnappydbException;
import com.valdesekamdem.library.mdtoast.MDToast;

import org.jetbrains.annotations.NotNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import me.ibrahimsn.lib.OnItemReselectedListener;
import me.ibrahimsn.lib.SmoothBottomBar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "da97f82748130a72c467aa50dfffcda7";
    private static final String firstPartOfUrl = "https://api.openweathermap.org/data/2.5/weather?q=";
    private static final String secondPartOfUrl = "&appid=";
    private String country = "";
    RequestQueue mRequestQueue;
    TextView tempTextView, windTextView, celsiusTextView, cityNameTextView;
    EditText cityEditText;
    ImageView countryImageView;
    Button enterButton;
    RecyclerView recyclerView;
    SmoothBottomBar navigationView;
    Toolbar toolbar;
    ProgressBar progressBar;
    public double temp = 0, windSpeed = 0, latitude = 0, longitude = 0;

    private String cityName = "";

    private FusedLocationProviderClient fusedLocationClient;
    private List<Weather> weatherList;

    @Override
    protected void onResume() {
        getPref();
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        iniXml();

        mRequestQueue = Volley.newRequestQueue(this);

        weatherList = new ArrayList<>();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        getLocation();

        getPref();

        enterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = firstPartOfUrl + cityEditText.getText().toString().trim() + secondPartOfUrl + API_KEY;
                getWeather(url);

                String curCity = cityEditText.getText().toString().trim();
                getForecastOkHttp("https://api.openweathermap.org/data/2.5/forecast?q=" + curCity + "&appid=da97f82748130a72c467aa50dfffcda7");
            }
        });

        navigationView.setOnItemReselectedListener(new OnItemReselectedListener() {
            @Override
            public void onItemReselect(int i) {
                switch (i) {
                    case  0:
                        System.exit(0);
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this,SettingsActivity.class));
                        break;
                    case 2:
                }
            }
        });

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "id")
                .setSmallIcon(R.drawable.ic_wb_sunny_black_24dp)
                .setContentTitle("My notification")
                .setContentText("Much longer text that cannot fit one line...")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Much longer text that cannot fit one line..."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(1, builder.build());
    }

    private void getLocation() {
        try {
            DB weatherDB = DBFactory.open(this,"weather");
            if(weatherDB.exists("city")) {
                cityName = weatherDB.get("city");
                getForecastOkHttp("https://api.openweathermap.org/data/2.5/forecast?q=" + cityName + "&cnt=20&appid=da97f82748130a72c467aa50dfffcda7");
                setValuesByCity(cityName);
                cityNameTextView.setText(cityName);
                startRecyclerView();
                setUIThread();
            } else {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            longitude = location.getLongitude();
                            latitude = location.getLatitude();
                            latitude = round(latitude);
                            longitude = round(longitude);

                            cityName = getLocationName(latitude, longitude);
                            try {
                                DB weatherDB = DBFactory.open(MainActivity.this,"weather");
                                weatherDB.put("city",cityName);
                                weatherDB.putDouble("lat",latitude);
                                weatherDB.putDouble("lon",longitude);
                            } catch (SnappydbException e) {
                                e.printStackTrace();
                            }

                            cityNameTextView.setText(cityName);
                            getForecastOkHttp("https://api.openweathermap.org/data/2.5/forecast?q=" + cityName + "&cnt=20&appid=da97f82748130a72c467aa50dfffcda7");
                            setValuesByCity(cityName);
                            startRecyclerView();
                            setUIThread();
                        }
                    }
                });
            }
        } catch (SnappydbException e) {
            e.printStackTrace();
        }
    }

    private Runnable makeProgressBarInvisible  = new Runnable() {
        @Override
        public void run() {
            progressBar.setVisibility(View.GONE);
        }
    };

    private Runnable makeProgressBarVisible  = new Runnable() {
        @Override
        public void run() {
            progressBar.setVisibility(View.VISIBLE);
        }
    };

    private void setUIThread() {
        Thread t = new Thread(new Runnable() {
            public void run() {
                try {
                    TimeUnit.SECONDS.sleep(1);
                    if(Objects.requireNonNull(recyclerView.getAdapter()).getItemCount() > 0 || weatherList.size() > 0) {
                        runOnUiThread(makeProgressBarInvisible);
                    } else {
                        runOnUiThread(makeProgressBarVisible);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    private static double round(double value) {
        BigDecimal bd = new BigDecimal(Double.toString(value));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @SuppressLint("SetTextI18n")
    private void setValues() {
        tempTextView.setText(getResources().getString(R.string.weather) + Math.round((temp)));
        celsiusTextView.setText(getResources().getString(R.string.celcium) + Math.round((temp - 273.15)));
        windTextView.setText(getResources().getString(R.string.wind) + windSpeed);
        String flagUrl = "https://www.countryflags.io/" + country.toLowerCase() + "/flat/64.png";
        Glide.with(getApplicationContext()).load(flagUrl).into(countryImageView);
        if(cityEditText.getText().toString().trim().length() > 0) {
            cityNameTextView.setText(cityEditText.getText().toString().trim());
        }
        cityEditText.getText().clear();
    }

    public String getLocationName(double latitude, double longitude) {

        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude, longitude, 10);
            for (Address address : addresses) {
                if (address != null) {
                    String city = address.getLocality();
                    if (city != null && !city.equals("")) {
                        cityName = city;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cityName;
    }

    private void iniXml() {
        tempTextView = findViewById(R.id.feelsLikeTextView);
        celsiusTextView = findViewById(R.id.celciumTextView);
        windTextView = findViewById(R.id.windTextView);
        cityEditText = findViewById(R.id.cityEditText);
        countryImageView = findViewById(R.id.flagImageView);
        enterButton = findViewById(R.id.enterButton);
        navigationView = findViewById(R.id.bottomNavView);
        cityNameTextView = findViewById(R.id.cityNameTextView);
        progressBar = findViewById(R.id.progressBar);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void startRecyclerView() {
        recyclerView = findViewById(R.id.forecastRecyclerView);
        WeatherAdapter adapter = new WeatherAdapter(this,weatherList);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
    }

    private void getWeather(String url) {
        final JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET,
                url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject weather = response.getJSONObject("main"),wind = response.getJSONObject("wind"),sys = response.getJSONObject("sys");
                    temp = weather.getDouble("temp");
                    windSpeed = wind.getDouble("speed");
                    country = sys.getString("country");
                    setValues();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                assert wifiManager != null;
                if (!wifiManager.isWifiEnabled()) {
                    MDToast mdToast =
                            MDToast.makeText(MainActivity.this, "Проверьте ваше подключение к интернету",1, MDToast.TYPE_WARNING);
                    mdToast.show();
                } else {
                    MDToast mdToast =
                            MDToast.makeText(MainActivity.this, "Введите корректное название",1, MDToast.TYPE_INFO);
                    mdToast.show();
                }
                error.printStackTrace();
            }
        });

        mRequestQueue.add(request);
    }

    private void getPref() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        if(sp.getBoolean("theme",true) && !sp.getBoolean("personal_settings",true)) {
            setBlackTheme();
        } else {
            if(!sp.getBoolean("personal_settings",true)) {
                setWhiteTheme();
            }
        }

        if(sp.getBoolean("personal_settings",true)) {
            int background_color = sp.getInt("background_color_picker",Color.GRAY), bottom_view_color = sp.getInt("menu_color_picker",Color.GRAY);
            View mainView = findViewById(R.id.activityMain);
            mainView.setBackgroundColor(background_color);
            navigationView.setBackgroundColor(bottom_view_color);
        }
    }

    private void setWhiteTheme() {
        View mainView = findViewById(R.id.activityMain);
        mainView.setBackgroundColor(Color.WHITE);
        tempTextView.setTextColor(Color.BLACK);
        celsiusTextView.setTextColor(Color.BLACK);
        windTextView.setTextColor(Color.BLACK);
        cityNameTextView.setTextColor(Color.BLACK);
    }

    private void setBlackTheme() {
        View mainView = findViewById(R.id.activityMain);
        mainView.setBackgroundColor(Color.GRAY);
        tempTextView.setTextColor(Color.WHITE);
        celsiusTextView.setTextColor(Color.WHITE);
        windTextView.setTextColor(Color.WHITE);
        cityNameTextView.setTextColor(Color.WHITE);
    }

    private void setValuesByCity(String city) {
        getWeather(firstPartOfUrl + city + secondPartOfUrl + API_KEY);
    }

    private void getForecastOkHttp(String url) {
        OkHttpClient client = new OkHttpClient();
        okhttp3.Request request = new okhttp3.Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull okhttp3.Response response) throws IOException {
                if(response.isSuccessful()) {
                    final String mResponse = Objects.requireNonNull(response.body()).string();
                    try {
                        JSONObject mObj = new JSONObject(mResponse);

                        if(mObj.getInt("cod") != 401 && mObj.getInt("cod") != 400) {
                            weatherList.clear();
                            JSONArray array = mObj.getJSONArray("list");

                            weatherList.clear();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                JSONObject main = object.getJSONObject("main"), wind = object.getJSONObject("wind");
                                Weather weather = new Weather();
                                double temp = Math.round((main.getDouble("temp") - 273.15)), speed = Math.round(wind.getDouble("speed"));
                                String date = object.getString("dt_txt");
                                weather.setPic(R.drawable.weather);
                                weather.setTemp((int) temp);
                                weather.setWind((int) speed);
                                weather.setDate(date);

                                weatherList.add(weather);
                            }
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    startRecyclerView();
                                }
                            });

                            setUIThread();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

}

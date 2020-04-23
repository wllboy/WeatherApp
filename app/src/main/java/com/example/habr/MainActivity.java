package com.example.habr;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import android.annotation.SuppressLint;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import me.ibrahimsn.lib.OnItemReselectedListener;
import me.ibrahimsn.lib.SmoothBottomBar;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    private static final String API_KEY = "da97f82748130a72c467aa50dfffcda7";
    private static final String firstPartOfUrl = "https://api.openweathermap.org/data/2.5/weather?q=";
    private static final String secondPartOfUrl = "&units=metric&appid=";
    private static final String imageUrl = "https://openweathermap.org/img/wn/";
    private String country = "";
    TextView tempTextView, windTextView, celsiusTextView, cityNameTextView, humidityTextView;
    ImageView countryImageView, weatherStateImageView;
    RecyclerView recyclerView;
    SmoothBottomBar navigationView;
    Toolbar toolbar;
    ProgressBar progressBar;
    public double temp = 0, windSpeed = 0, latitude = 0, longitude = 0;
    private int humidity;

    private String cityName = "", imgUrlResult = "";

    private FusedLocationProviderClient fusedLocationClient;
    private List<Weather> weatherList;

    private HashMap <String,String> weatherMap;

    String weatherState = "";

    @Override
    protected void onResume() {
        getPref();
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setBackground();
            }
        });
        super.onResume();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weatherMap = new HashMap<>();
        weatherMap.put("Clear", "Ясно");
        weatherMap.put("Drizzle", "Изморось");
        weatherMap.put("Clouds", "Облачно");
        weatherMap.put("Rain", "Дождь");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        setTime();
                    }
                };
                Timer timer = new Timer();
                timer.schedule(timerTask, 1000, 5000);
            }
        });

        iniXml();

        weatherList = new ArrayList<>();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        
        getLocation();

        getPref();

        navigationView.setOnItemReselectedListener(new OnItemReselectedListener() {
            @Override
            public void onItemReselect(int i) {
                switch (i) {
                    case  0:
                        finish();
                        break;
                    case 1:
                        startActivity(new Intent(MainActivity.this,SettingsActivity.class));
                        break;
                    case 2:

                        break;
                }
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.search_bar);

        SearchManager searchManager = (SearchManager) MainActivity.this.getSystemService(Context.SEARCH_SERVICE);

        SearchView searchView = null;
        if (searchItem != null) {
            searchView = (SearchView) searchItem.getActionView();
            searchView.setQueryHint("Найти город...");
        }
        if (searchView != null) {
            searchView.setSearchableInfo(Objects.requireNonNull(searchManager).getSearchableInfo(MainActivity.this.getComponentName()));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    String url = firstPartOfUrl + query + secondPartOfUrl + API_KEY;
                    getWeather(url);
                    cityName = query;
                    getForecastOkHttp("https://api.openweathermap.org/data/2.5/forecast?q=" + query + "&units=metric&appid=da97f82748130a72c467aa50dfffcda7");
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    return false;
                }
            });
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void getLocation() {
        try {
            DB weatherDB = DBFactory.open(this,"weatherDB");
            if(weatherDB.exists("city")) {
                Log.d("tag", "exists");
                CityList cityList = weatherDB.getObject("city", CityList.class);
                ArrayList <City> cities = cityList.cities;
                City city = cities.get(0);
                if(city == null) {
                    Log.d("tag", "cities is null");
                }
                Log.d("tag", Objects.requireNonNull(city).cityName);
                cityName = city.cityName;
                latitude = city.latitudeValue;
                longitude = city.longitudeValue;
                getForecastOkHttp("https://api.openweathermap.org/data/2.5/forecast?q=" + cityName + "&units=metric&appid=da97f82748130a72c467aa50dfffcda7");
                setValuesByCity(cityName);
                cityNameTextView.setText(cityName);
                startRecyclerView();
                setUIThread();
            } else {
                Log.d("tag", "does not exist");
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
                                DB weatherDB = DBFactory.open(MainActivity.this,"weatherDB");
                                City city = new City(cityName, latitude, longitude);
                                CityList cityList = new CityList(new ArrayList<City>());
                                cityList.cities.add(city);
                                weatherDB.put("city", cityList);
                            } catch (SnappydbException e) {
                                e.printStackTrace();
                            }

                            cityNameTextView.setText(cityName);
                            getForecastOkHttp("https://api.openweathermap.org/data/2.5/forecast?q=" + cityName + "&units=metric&appid=da97f82748130a72c467aa50dfffcda7");
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
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tempTextView.setText(weatherMap.get(weatherState));
                celsiusTextView.setText("" + temp + " C°");
                windTextView.setText("" + windSpeed + " м/с");
                humidityTextView.setText("" + humidity + '%');
                String flagUrl = "https://www.countryflags.io/" + country.toLowerCase() + "/flat/64.png";
                Glide.with(getApplicationContext()).load(flagUrl).into(countryImageView);
                Glide.with(getApplicationContext()).load(imgUrlResult).into(weatherStateImageView);
                cityNameTextView.setText(cityName);
                setBackground();
            }
        });
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
        humidityTextView = findViewById(R.id.humidityTextView);
        countryImageView = findViewById(R.id.flagImageView);
        weatherStateImageView = findViewById(R.id.weatherStateImageView);
        navigationView = findViewById(R.id.bottomNavView);
        cityNameTextView = findViewById(R.id.cityNameTextView);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void startRecyclerView() {
        recyclerView = findViewById(R.id.forecastRecyclerView);
        WeatherAdapter adapter = new WeatherAdapter(this, weatherList, Glide.with(this), false);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.HORIZONTAL));
    }

    private void getWeather(String url) {
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
                        JSONObject weather = mObj.getJSONObject("main"),
                                wind = mObj.getJSONObject("wind"),
                                sys = mObj.getJSONObject("sys");

                        JSONArray main = mObj.getJSONArray("weather");
                        temp = weather.getDouble("temp");
                        humidity = weather.getInt("humidity");
                        windSpeed = wind.getDouble("speed");
                        country = sys.getString("country");
                        JSONObject object = main.getJSONObject(0);
                        weatherState = object.getString("main");
                        imgUrlResult = imageUrl + object.getString("icon") + "@2x.png";

                        MainActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setValues();
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    final WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    assert wifiManager != null;
                    if (!wifiManager.isWifiEnabled()) {
                        createMaterialToast("Проверьте ваше подключение к интернету", MDToast.TYPE_WARNING);
                    } else {
                        createMaterialToast("Введите корректное название", MDToast.TYPE_INFO);
                    }
                }
            }
        });
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

    private void getForecastOkHttp(final String url) {
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
                            JSONArray array = mObj.getJSONArray("list");
                            weatherList.clear();
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                JSONObject main = object.getJSONObject("main"), wind = object.getJSONObject("wind");
                                JSONArray weatherArray = object.getJSONArray("weather");
                                Weather weather = new Weather();
                                double temp = Math.round(main.getDouble("temp")), speed = Math.round(wind.getDouble("speed"));
                                String date = object.getString("dt_txt");
                                weather.setImagePath(imageUrl + weatherArray.getJSONObject(0).getString("icon") + "@2x.png");
                                weather.setTemp((int) temp);
                                weather.setWind((int) speed);
                                weather.setDate(date);

                                weatherList.add(weather);
                            }
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    List <Weather> list = refactorCurDayWeatherList(weatherList);
                                    startCurDatRecView(list);
                                    refactorWeatherList();
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

    public void addCity(View view) {
        final EditText editText = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Добавить город")
                .setMessage("Введите название города")
                .setView(editText)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });
        builder.show();
    }

    private void setBackground() {
        RelativeLayout layout = findViewById(R.id.bgRelativeLayout);
        int n = getTime().length();
        String time = getTime();
        if(weatherState.equals("Clear")) {
            if(time.charAt(n-2) == 'P' && time.charAt(0) >= '8' && !time.substring(0,2).equals("11")) {
                layout.setBackgroundResource(R.drawable.sunset);
            } else {
                layout.setBackgroundResource(R.drawable.landscape_sunny);
            }
        } else if(weatherState.equals("Rain") || weatherState.equals("Drizzle")) {
            layout.setBackgroundResource(R.drawable.rainy);
        } else {
            if(time.charAt(n-2) == 'P' && time.charAt(0) >= '8') {
                layout.setBackgroundResource(R.drawable.night);
            } else {
                layout.setBackgroundResource(R.drawable.landscape_sunny);
            }
        }
    }

    private void createMaterialToast(final String text, final int type) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MDToast mdToast = MDToast.makeText(MainActivity.this, text,1, type);
                mdToast.show();
            }
        });
    }

    private void startCurDatRecView(List <Weather> weatherList) {
        RecyclerView curDayRecyclerView = findViewById(R.id.currentDayRecyclerView);
        WeatherAdapter adapter = new WeatherAdapter(this, weatherList, Glide.with(this), true);
        curDayRecyclerView.setAdapter(adapter);
        curDayRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.HORIZONTAL));
    }

    private void refactorWeatherList() {
        List <Weather> newWeatherList = weatherList;
        for(int i = 0;i < weatherList.size();i++) {
            Weather weather1 = weatherList.get(i);
            for(int j = i;j < weatherList.size();j++) {
                Weather weather2 = weatherList.get(j);
                String s1 = weather1.getDate().substring(0,10), s2 = weather2.getDate().substring(0,10);
                Log.d("tag", s1 + " " + s2);
                if(s1.equals(s2)) {
                    Log.d("tag", "equals");
                    newWeatherList.remove(weather2);
                }
            }
        }
        weatherList = newWeatherList;
    }

    private List<Weather> refactorCurDayWeatherList(List <Weather> weatherList) {
        Date date = Calendar.getInstance().getTime();
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        String curDate = df.format(date).substring(0,10);
        List <Weather> res = new ArrayList<>();
        for(int i = 0;i < weatherList.size();i++) {
            if(weatherList.get(i).getDate().substring(0,10).equals(curDate)) {
                Weather weather = new Weather();
                weather.setDate(weatherList.get(i).getDate().substring(0,weatherList.get(i).getDate().length() - 3));
                weather.setImagePath(weatherList.get(i).getImagePath());
                weather.setTemp((int) weatherList.get(i).getTemp());
                weather.setWind((int) weatherList.get(i).getWind());
                weather.setDate(weather.getDate().substring(11));
                res.add(weather);
            } else {
                break;
            }
        }
        return res;
    }

    private void setTime() {
        Date date = Calendar.getInstance().getTime();
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("h:mm a");
        final String curDate = df.format(date);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = findViewById(R.id.timeTextView);
                textView.setText(curDate);
            }
        });
    }

    private String getTime() {
        Date date = Calendar.getInstance().getTime();
        @SuppressLint("SimpleDateFormat") DateFormat df = new SimpleDateFormat("h:mm a");
        return df.format(date);
    }
}

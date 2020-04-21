package com.example.habr;

import android.app.DownloadManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;

import java.util.List;

public class WeatherAdapter extends RecyclerView.Adapter<WeatherAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List <Weather> weatherList;
    private final RequestManager requestManager;

    WeatherAdapter(Context context, List<Weather> weatherList, RequestManager requestManager) {
        this.requestManager = requestManager;
        this.inflater = LayoutInflater.from(context);
        this.weatherList = weatherList;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        final CardView cardView;
        final ImageView imageView;
        final TextView temp,wind,date;
        public ViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.weatherCardView);
            imageView = itemView.findViewById(R.id.weatherImage);
            temp = itemView.findViewById(R.id.tempTextView);
            wind = itemView.findViewById(R.id.windTextView);
            date = itemView.findViewById(R.id.dateTextView);
        }
    }

    @Override
    public WeatherAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.weather_card_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(WeatherAdapter.ViewHolder holder, int position) {
        Weather weather = weatherList.get(position);
        loadImage(requestManager, weather.getImagePath(), holder.imageView);
        holder.temp.setText(String.valueOf(weather.getTemp()) +  '°');
        holder.wind.setText(String.valueOf(weather.getWind()) + "м/с");
        holder.date.setText(weather.getDate());
    }

    @Override
    public int getItemCount() {
        return weatherList.size();
    }

    static void loadImage(RequestManager glide, String url, ImageView view) {
        glide.load(url).into(view);
    }
}

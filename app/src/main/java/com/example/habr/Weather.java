package com.example.habr;

public class Weather {

    private String date;
    private int temp;
    private int wind;
    private String imagePath;

    public Weather() {

    }

    public Weather(String date, int temp, int wind, String imagePath) {
        this.date = date;
        this.temp = temp;
        this.wind = wind;
        this.imagePath = imagePath;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getTemp() {
        return temp;
    }

    public void setTemp(int temp) {
        this.temp = temp;
    }

    public void setWind(int wind) {
        this.wind = wind;
    }

    public double getWind() {
        return wind;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}

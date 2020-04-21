package com.example.habr;

public class City {
    public String cityName;
    public double latitudeValue;
    public double longitudeValue;

    public City() {

    }

    public City(String cityName, double latitudeValue, double longitudeValue) {
        this.cityName = cityName;
        this.latitudeValue = latitudeValue;
        this.longitudeValue = longitudeValue;
    }
}

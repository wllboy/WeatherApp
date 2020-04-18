package com.example.habr;

public class Weather {

    private String date;
    private int temp;
    private int wind;
    private int pic;

    public Weather() {

    }

    public Weather(String date, int temp, int wind, int pic) {
        this.date = date;
        this.temp = temp;
        this.wind = wind;
        this.pic = pic;
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


    public int getPic() {
        return pic;
    }

    public void setPic(int pic) {
        this.pic = pic;
    }
}

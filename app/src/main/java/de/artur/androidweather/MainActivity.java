package de.artur.androidweather;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainActivity extends AppCompatActivity {

    RecyclerView list;
    List<Item> items;
    MyAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list = findViewById(R.id.list);
        items = new ArrayList<>();
        adapter = new MyAdapter(items);
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getWeather();
            }
        });

        getWeather();
    }

    void getWeather() {
        items.clear();
        adapter.notifyDataSetChanged();

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getWeather();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double lat = location.getLatitude();
            double lng = location.getLongitude();

            String url =
                    "https://api.openweathermap.org/data/2.5/onecall?lat=" + lat + "&lon=" + lng + "&APPID=682baa9a0a0f2605d6402fdea1168837&units=metric";

            RequestQueue queue = Volley.newRequestQueue(MainActivity.this);


            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            try {
                                JSONObject res = new JSONObject(response);
                                JSONArray daily = res.getJSONArray("daily");
                                for (int i = 0; i < daily.length(); i++) {
                                    JSONObject item = daily.getJSONObject(i);
                                    items.add(new Item(new Date(item.getLong("dt") * 1000), new Date(item.getLong("sunrise") * 1000), new Date(item.getLong("sunset") * 1000), item.getJSONArray("weather").getJSONObject(0).getString("main"), item.getJSONObject("temp").getDouble("max"), item.getJSONObject("temp").getDouble("min")));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            adapter.notifyDataSetChanged();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    System.out.println(error.toString());
                }
            });
            queue.add(stringRequest);
        }

        @Override
        public void onProviderDisabled(String provider) {
            System.out.println("provider disabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
            System.out.println("provider enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        List<Item> items;
        SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd.MM", Locale.GERMAN);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.GERMAN);

        MyAdapter(List<Item> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_weather_item, parent, false);
                return new ViewHolder(view);

        }

        @Override
        public void onBindViewHolder(@NonNull MyAdapter.ViewHolder holder, int i) {
            holder.date.setText(dateFormat.format(items.get(i).getDate()));
            holder.sunrise.setText(timeFormat.format(items.get(i).getSunrise()));
            holder.sunset.setText(timeFormat.format(items.get(i).getSunset()));
            holder.tempHigh.setText(String.format("%s °C", String.valueOf(Math.round(items.get(i).getTempHigh()))));
            holder.tempLow.setText(String.format("%s °C", String.valueOf(Math.round(items.get(i).getTempLow()))));
            switch (items.get(i).getWeatherCondition().toLowerCase()) {
                case "rain":
                    holder.icon.setImageResource(R.drawable.ic_weather_pouring);
                    break;
                case "clear":
                    holder.icon.setImageResource(R.drawable.ic_refresh_black_24dp);
                    break;
                case "thunderstorm":
                    holder.icon.setImageResource(R.drawable.ic_weather_lightning);
                    break;
                case "clouds":
                    holder.icon.setImageResource(R.drawable.ic_weather_cloudy);
                    break;
                case "snow":
                    holder.icon.setImageResource(R.drawable.ic_weather_snowy);
                    break;
                case "drizzle":
                case "haze":
                    holder.icon.setImageResource(R.drawable.ic_weather_rainy);
                    break;
                case "mist":
                    holder.icon.setImageResource(R.drawable.ic_weather_fog);
                    break;
                default:
                    holder.icon.setImageResource(R.drawable.ic_error_outline_black_24dp);
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView date, sunrise, sunset, tempHigh, tempLow;
            ImageView icon;


            ViewHolder(@NonNull View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.date);
                sunrise = itemView.findViewById(R.id.sunrise);
                sunset = itemView.findViewById(R.id.sunset);
                tempHigh = itemView.findViewById(R.id.tempHigh);
                tempLow = itemView.findViewById(R.id.tempLow);
                icon = itemView.findViewById(R.id.icon);
            }
        }
    }

    private class Item {
        private Date date, sunrise, sunset;
        private String weatherCondition;
        private double tempHigh, tempLow;

        Item(Date date, Date sunrise, Date sunset, String weatherCondition, double tempHigh, double tempLow) {
            this.date = date;
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.weatherCondition = weatherCondition;
            this.tempHigh = tempHigh;
            this.tempLow = tempLow;
        }

        Date getDate() {
            return date;
        }

        Date getSunrise() {
            return sunrise;
        }

        Date getSunset() {
            return sunset;
        }

        String getWeatherCondition() {
            return weatherCondition;
        }

        double getTempHigh() {
            return tempHigh;
        }

        double getTempLow() {
            return tempLow;
        }
    }
}

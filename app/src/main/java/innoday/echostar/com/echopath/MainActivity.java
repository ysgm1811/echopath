package innoday.echostar.com.echopath;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.find);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Spinner fromSpinner = (Spinner) findViewById(R.id.from);
                Spinner toSpinner = (Spinner) findViewById(R.id.to);

                Location fromLocation = (Location) fromSpinner.getSelectedItem();
                Location toLocation = (Location) toSpinner.getSelectedItem();

                new ShortestDistanceTask(fromLocation, toLocation).execute();
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        new HttpRequestTask().execute();
    }

    public void onShowMap(View v){
        if (v.getId() == R.id.showMap){
            Intent i = new Intent(MainActivity.this, MapActivity.class);
            startActivity(i);
        }
    }

    public void setAdaptor(List<Location> items){

        Spinner from = (Spinner)findViewById(R.id.from);
        ArrayAdapter<Location> fromAdapter = new ArrayAdapter<Location>(this,android.R.layout.simple_spinner_dropdown_item,items);
        from.setAdapter(fromAdapter);
        Spinner to = (Spinner)findViewById(R.id.to);
        ArrayAdapter<Location> toAdapter = new ArrayAdapter<Location>(this,android.R.layout.simple_spinner_dropdown_item,items);
        to.setAdapter(toAdapter);
    }

    private class HttpRequestTask extends AsyncTask<Void, Void, LocationsDTO> {

        @Override
        protected LocationsDTO doInBackground(Void... params) {
            try {
                final String url = "http://10.79.85.86:8080/echopath/location/locations";
                RestTemplate restTemplate = new RestTemplate();
                restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
                LocationsDTO locationsDTO = restTemplate.getForObject(url, LocationsDTO.class);
                return locationsDTO;
            } catch (Throwable e) {
                Log.e("MainActivity", e.getMessage(), e);
                throw  new  RuntimeException(e);
            }
        }

        @Override
        protected void onPostExecute(LocationsDTO locationsDTO) {


            List<Location> items  = new ArrayList<Location>();

            for(Location location : locationsDTO.getLocations()){
                items.add(location);
            }
            setAdaptor(items);
        }

    }

    public class ShortestDistanceTask extends AsyncTask<Void, Void, ShortestPathDTO> {

        private static final String BASE_URL = "http://10.79.85.86:8080/echopath/location/";


        private Location fromLocation;

        private  Location toLocation;


        private ShortestPathDTO shortestPathDTO = new ShortestPathDTO();


        public ShortestDistanceTask(Location fromLocation, Location toLocation){
              this.fromLocation = fromLocation;
              this.toLocation = toLocation;
        }

        @Override
        protected ShortestPathDTO doInBackground(Void... params) {
            try {


                RestTemplate restOperations = new RestTemplate();
                restOperations.getMessageConverters().add(new MappingJackson2HttpMessageConverter());


                if (fromLocation != null && toLocation != null) {
                    shortestPathDTO = restOperations.getForObject(BASE_URL
                            + "shortestPath?fromID=" + fromLocation.getId() + "&toID="
                            + toLocation.getId(), ShortestPathDTO.class);

                    if(shortestPathDTO.getMinDistance() == Double.POSITIVE_INFINITY){
                        shortestPathDTO = restOperations.getForObject(BASE_URL
                                + "shortestPath?fromID=" + toLocation.getId() + "&toID="
                                + fromLocation.getId(), ShortestPathDTO.class);
                        Collections.reverse(shortestPathDTO.getLocations());

                        double sum = shortestPathDTO.getMinDistance();

                        for(Location location : shortestPathDTO.getLocations()){
                            location.setMinDistance(sum - location.getMinDistance());
                        }
                        if(shortestPathDTO.getLocations().size() > 0){
                            shortestPathDTO.getLocations().get(0).setMinDistance(0);
                        }
                    }
                }

                Location previousLocation = null;
                double tempSum = 0 ;

                for(Location location : shortestPathDTO.getLocations()){

                    if(previousLocation != null){
                        tempSum = tempSum + previousLocation.getMinDistance();
                        location.setMinDistance(location.getMinDistance() - tempSum);
                    }
                    previousLocation = location;
                }

            } catch (Throwable e) {
                Log.e("MainActivity", e.getMessage(), e);
                throw  new  RuntimeException(e);
            }

            return  shortestPathDTO;
        }

        @Override
        protected void onPostExecute(ShortestPathDTO shortestPathDTO) {

           setDistances(shortestPathDTO);

        }
    }

    public void setDistances(ShortestPathDTO shortestPathDTO){

        TableLayout tableLayout = (TableLayout) findViewById(R.id.table);

        tableLayout.removeAllViews();

        tableLayout.setPadding(15, 3, 15, 3);

        for(Location location : shortestPathDTO.getLocations()){
            TableRow row = new TableRow(this);
            TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);

            row.setPadding(15, 3, 15, 3);

            row.setBackgroundColor(Color.parseColor("#E4CC91"));

            TextView Values = new TextView(this);
            Values.setPadding(15, 0, 15, 0);
            Values.setGravity(Gravity.CENTER);
            Values.setTextSize(25.0f);
            Values.setTextColor(Color.parseColor("#FFFFFF"));
            Values.setTypeface(null, Typeface.BOLD);
            Values.setText(location.getName());
            row.addView(Values);

            TextView Values1 = new TextView(this);
            Values1.setPadding(15, 0, 15, 0);
            Values1.setGravity(Gravity.CENTER);
            Values1.setTextSize(25.0f);
            Values1.setTextColor(Color.parseColor("#FFFFFF"));
            Values1.setTypeface(null, Typeface.BOLD);
            Values1.setText(String.valueOf(location.getMinDistance()));
            row.addView(Values1);

            tableLayout.addView(row);
        }
    }

}

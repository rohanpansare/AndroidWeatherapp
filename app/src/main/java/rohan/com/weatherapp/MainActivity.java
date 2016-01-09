package rohan.com.weatherapp;

import android.content.Context;
import android.content.IntentSender;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.NetworkInterface;

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private double currentLatitude;
    private double currentLongitude;

    public static final String TAG = MainActivity.class.getSimpleName();
    CurrentWeatherConditions mCurrentWeatherConditions;
    //private TextView mTemperatureLabel;
    @Bind(R.id.timeLabel) TextView mTimelabel;
    @Bind(R.id.temperatureLabel) TextView mTemperatureLabel;
   // @Bind(R.id.humidityValue) TextView mHumidityValue;
    @Bind(R.id.precipValue) TextView mMPrecipValue;
    @Bind(R.id.summaryLabel) TextView mSummaryLabel;
    @Bind(R.id.iconImageView) ImageView mIconImageView;
    @Bind(R.id.humidityValue) TextView mHumidityValue;
    @Bind(R.id.refreshImageView) ImageView mRefreshImageView;
    @Bind((R.id.progressBar))ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        mGoogleApiClient = new GoogleApiClient.Builder(this)
                // The next two lines tell the new client that “this” current class will handle connection stuff
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                        //fourth line adds the LocationServices API endpoint from GooglePlayServices
                .addApi(LocationServices.API)
                .build();

        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds





        ButterKnife.bind(this);
        mProgressBar.setVisibility(View.INVISIBLE);
       // final double latitude = 37.8267;
        //final double longitude = -122.423;
        mRefreshImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getForecast(currentLatitude,currentLongitude);



            }
        });
        //mTemperatureLabel = (TextView)findViewById(R.id.temperatureLabel);


      //  getForecast(currentLatitude,currentLongitude);
    }

    private void getForecast(double latitude , double longitude) {
        String apiKey = "48c2396cf6743361ab181b57dbdbfbd6";

        if(isNetworkAvailable()) {
            String forecastUrl = "https://api.forecast.io/forecast/" + apiKey + "/" + latitude + "," + longitude;
            Log.d("Rohan",latitude+ ","+longitude + "["+currentLatitude+","+currentLongitude+"]");
            toggleRefresh();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(forecastUrl).build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Request request, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toggleRefresh();
                        }
                    });
                    alertUserAboutError();
                }

                @Override
                public void onResponse(Response response) throws IOException {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleRefresh();
                            }
                        });
                        //Response response =call.execute();
                        String jsondata = response.body().string();
                        if (response.isSuccessful()) {
                            mCurrentWeatherConditions = getCurrentDetails(jsondata);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateDisplay();
                                }
                            });


                            Log.v(TAG, jsondata);
                        } else {
                            alertUserAboutError();
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "EXception Caught", e);
                    } catch (JSONException e) {
                        Log.e(TAG, "EXception Caught", e);
                    }
                }

                private void alertUserAboutError() {
                    AlertDialogFragment dialog = new AlertDialogFragment();
                    dialog.show(getFragmentManager(), "error_dialog");

                }
            });
            Log.d(TAG, "Main UI code is running");
        }
        else
        {
            Toast.makeText(this, R.string.network_unavailable_message, Toast.LENGTH_LONG).show();
        }
    }

    private void toggleRefresh() {
        if(mProgressBar.getVisibility()==View.INVISIBLE) {
            mProgressBar.setVisibility(View.VISIBLE);
            mRefreshImageView.setVisibility(View.INVISIBLE);
        }
        else
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            mRefreshImageView.setVisibility(View.VISIBLE);
        }
    }

    private void updateDisplay() {
        mTemperatureLabel.setText((int)mCurrentWeatherConditions.getTemperature()+ "");
        mTimelabel.setText("At " + mCurrentWeatherConditions.getFormmatedTime() + " it will be ");
        mHumidityValue.setText(mCurrentWeatherConditions.getHumidity() + "");
        mMPrecipValue.setText(mCurrentWeatherConditions.getPrecipChance() + "%");
        mSummaryLabel.setText(mCurrentWeatherConditions.getSummary());
        Drawable drawable = getResources().getDrawable(mCurrentWeatherConditions.getIconId());
        mIconImageView.setImageDrawable(drawable);
    }

    private CurrentWeatherConditions getCurrentDetails(String jsondata) throws JSONException{
        JSONObject forecast = new JSONObject(jsondata);
        String timezone = forecast.getString("timezone");
        Log.i(TAG,"FROM JSON :" + timezone);
        JSONObject currently = forecast.getJSONObject("currently");
        CurrentWeatherConditions currentWeatherConditions = new CurrentWeatherConditions();
        currentWeatherConditions.setHumidity(currently.getDouble("humidity"));
        currentWeatherConditions.setTime(currently.getLong("time"));
        currentWeatherConditions.setIcon(currently.getString("icon"));
        currentWeatherConditions.setPrecipChance(currently.getDouble("precipProbability"));
        currentWeatherConditions.setSummary(currently.getString("summary"));
        currentWeatherConditions.setTemperature(currently.getDouble("temperature"));
        currentWeatherConditions.setTimeZone(timezone);
        Log.d(TAG, currentWeatherConditions.getFormmatedTime());
        return currentWeatherConditions;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE );
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo !=null && networkInfo.isConnected() )
        {
        isAvailable = true;
        }
        return isAvailable;
    }





    @Override
    protected void onResume() {
        super.onResume();
        //Now lets connect to the API
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.v(this.getClass().getSimpleName(), "onPause()");

        //Disconnect from API onPause()
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }


    }

    /**
     * If connected get lat and long
     *
     */
    @Override
    public void onConnected(Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        } else {
            //If everything went fine lets get latitude and longitude
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();

          //  Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
            getForecast(currentLatitude,currentLongitude);
        }
    }


    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
            /*
             * Google Play services can resolve some errors it detects.
             * If the error has a resolution, try sending an Intent to
             * start a Google Play services activity that can resolve
             * error.
             */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                    /*
                     * Thrown if Google Play services canceled the original
                     * PendingIntent
                     */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
                /*
                 * If no resolution is available, display a dialog to the
                 * user with the error.
                 */
            Log.e("Error", "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    /**
     * If locationChanges change lat and long
     *
     *
     * @param location
     */
    @Override
    public void onLocationChanged(Location location) {
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        Toast.makeText(this, currentLatitude + " WORKS " + currentLongitude + "", Toast.LENGTH_LONG).show();
    }




}
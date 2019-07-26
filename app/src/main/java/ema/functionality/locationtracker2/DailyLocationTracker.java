package ema.functionality.locationtracker2;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class DailyLocationTracker extends Worker {

    private static final String DEFAULT_START_TIME = "06:00";
    private static final String DEFAULT_END_TIME = "24:00";
    private static final SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    private static final DateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private static final String TAG = "DAILY TRACKER";

    private Context mContext;

    private static final int LOCATION_PERIOD = 10000;
    private static final int FASTEST_INTERVAL = LOCATION_PERIOD/2;

    private LocationCallback mLocationCallback;
    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private String ID;
    private SharedPreferences prefs;
    private String sharedPrefFile = "ema.functionality.locationtracker2";

    //FirebaseDatabase database = FirebaseDatabase.getInstance();
    //DatabaseReference myRef = database.getReference("message");

    private Intent intent;
    public static final String BROADCAST_TAG = BuildConfig.APPLICATION_ID + ".ACTION_BROADCAST_ADDRESS";

    public DailyLocationTracker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    /***************Location Tracker's Methods*************************/

    /**
     * Stop the location tracking by removing the thread on location updates.
     * Also reverses the Stop button back to Start.
     */

    private void updateDB(String adr) {
        Date date = Calendar.getInstance().getTime();
        Toast.makeText(getApplicationContext(), "Address updated !", Toast.LENGTH_LONG).show();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(ID + "/" + df.format(date) + "/" + dateFormat.format(date));
        ref.setValue(adr);
    }

    @NonNull
    @Override
    public Result doWork() {
        intent = new Intent(BROADCAST_TAG);

        Log.d(TAG, "doWork: Done");

        Log.d(TAG, "onStartJob: STARTING JOB..");

        /***Instantiate the Shared Preference and get the user ID from it***/
        prefs = getApplicationContext().getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        ID = prefs.getString("ID", ID);


        Calendar c = Calendar.getInstance();
        Date date = c.getTime();
        String formattedDate = dateFormat.format(date);

        try {
            Date currentDate = dateFormat.parse(formattedDate);
            Date startDate = dateFormat.parse(DEFAULT_START_TIME);
            Date endDate = dateFormat.parse(DEFAULT_END_TIME);

            if (currentDate.after(startDate) && currentDate.before(endDate)) {
                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
                mLocationCallback = new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        super.onLocationResult(locationResult);
                    }
                };

                final LocationRequest mLocationRequest = new LocationRequest();
                mLocationRequest.setInterval(LOCATION_PERIOD);
                mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                try {
                    mFusedLocationClient
                            .getLastLocation()
                            .addOnCompleteListener(new OnCompleteListener<Location>() {
                                @Override
                                public void onComplete(@NonNull Task<Location> task) {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        mLocation = task.getResult();
                                        Log.d(TAG, "Location : " + mLocation);
                                        String adr = getCompleteAddressString(mLocation.getLatitude(), mLocation.getLongitude());
                                        intent.putExtra("Address", adr);
                                        getApplicationContext().sendBroadcast(intent);
                                        updateDB(adr);
                                        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                    } else {
                                        Log.w(TAG, "Failed to get location.");
                                    }
                                }
                            });
                } catch (SecurityException unlikely) {
                    Log.e(TAG, "Lost location permission." + unlikely);
                }

                try {
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, null);
                } catch (SecurityException unlikely) {
                    //Utils.setRequestingLocationUpdates(this, false);
                    Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
                }
            } else {
                Log.d(TAG, "Time up to get location. Your time is : " + DEFAULT_START_TIME + " to " + DEFAULT_END_TIME);
            }
        } catch (ParseException ignored) {

        }

        return Result.success();
    }

    private String getCompleteAddressString(double LATITUDE, double LONGITUDE) {
        String strAdd = "";
        Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1);
            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();

                for (int i = 0; i <= returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n");
                }
                strAdd = strReturnedAddress.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return strAdd;
    }
}


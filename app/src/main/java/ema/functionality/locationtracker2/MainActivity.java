package ema.functionality.locationtracker2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.concurrent.TimeUnit;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 200;
    private static final String TAG = "LOCATION UPDATE";

    private WorkManager mWorkManager;
    private PeriodicWorkRequest workRequest;

    private SharedPreferences mPreferences;
    private String sharedPrefFile = "ema.functionality.locationtracker2";

    private TextView Status;
    private EditText ID;
    private Button btnTracker;
    private TextView Address;

    private String adr = "";
    private String id = "NOT_INITIALIZED";

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            adr = intent.getStringExtra("Address");
            Address.setText("Current address :" + adr);
        }
    };

    private boolean checkLocationPermission() {
        int result3 = ContextCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION);
        int result4 = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION);
        return result3 == PackageManager.PERMISSION_GRANTED &&
                result4 == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0) {
                boolean coarseLocation = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean fineLocation = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                if (coarseLocation && fineLocation)
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check whether this app has access to the location permission//

        if (!checkLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        mPreferences = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        id = mPreferences.getString("ID", id);


        Status = findViewById(R.id.status);
        Address = findViewById(R.id.address);
        ID = findViewById(R.id.id);
        btnTracker = findViewById(R.id.btn_daily);

        if (!id.equals("NOT_INITIALIZED"))
            ID.setText(id);

        /***WorkManager instantiated with a periodic work request that repeats everyday***/
        /*mWorkManager = WorkManager.getInstance(MainActivity.this);
        workRequest = new PeriodicWorkRequest.Builder(DailyLocationTracker.class, 2, TimeUnit.SECONDS).build();
        mWorkManager.getWorkInfoByIdLiveData(workRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null) {
                    WorkInfo.State state = workInfo.getState();
                    Status.append(state.toString() + "\n");
                }
            }
        });*/

        /***A text changed listener with afterTextChanged() method implemented in order to automatically save ID right after the ID field is modified***/
        ID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                id = ID.getText().toString();
                SharedPreferences.Editor preferencesEditor = mPreferences.edit();
                preferencesEditor.putString("ID",id);
                preferencesEditor.apply();
            }
        });

        /***The Broadcast Receiver is launched here***/
        registerReceiver(broadcastReceiver, new IntentFilter(DailyLocationTracker.BROADCAST_TAG));

        btnTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PeriodicWorkRequest periodicWork = new PeriodicWorkRequest.Builder(DailyLocationTracker.class, 45, TimeUnit.MINUTES)
                        .addTag(TAG)
                        .build();
                WorkManager.getInstance(MainActivity.this).enqueueUniquePeriodicWork("Location Tracker", ExistingPeriodicWorkPolicy.REPLACE,periodicWork);
                Toast.makeText(MainActivity.this, "Location Worker Started : " + periodicWork.getId(), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}

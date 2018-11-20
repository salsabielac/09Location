package com.example.salsa.location;

import android.Manifest;
import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.PlaceLikelihoodBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MainActivity extends AppCompatActivity implements DapatkanAlamatTask.onTaskSelesai{

    private int REQUEST_PICK_PLACE = 1;

    private Button mLocationButton,btnPilihLokasi;
    private TextView mLocationTextView;
    private ImageView mAndroidImageView;
    private AnimatorSet mRotateAnim;
    private Location mLastLocation;
    private boolean mTrackingLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private PlaceDetectionClient mPlaceDetectionClient;
    private String mPlaceName;


    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mLocationTextView = (TextView) findViewById(R.id.textview_location);
        mAndroidImageView = (ImageView) findViewById(R.id.imageview_android);
        mPlaceDetectionClient = Places.getPlaceDetectionClient(this);

        mRotateAnim = (AnimatorSet) AnimatorInflater.loadAnimator(this,R.animator.rotate);
        mRotateAnim.setTarget(mAndroidImageView);

        mLocationButton = (Button) findViewById(R.id.button_location);
        btnPilihLokasi = (Button) findViewById(R.id.button_pilih_lokasi);

        mLocationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!mTrackingLocation)
                {
                    mulaiTrackingLokasi();
                }
                else
                {
                    stopTrackingLokasi();
                }
            }
        });

        btnPilihLokasi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //membuat Intent untuk Place Picker
                PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
                try {
                    //menjalankan place picker
                    startActivityForResult(builder.build(MainActivity.this), REQUEST_PICK_PLACE);
                } catch (GooglePlayServicesRepairableException e) {
                    e.printStackTrace();
                } catch (GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        });

        mLocationCallback = new LocationCallback()
        {
            public void onLocationResult(LocationResult locationResult)
            {
                if (mTrackingLocation)
                {
                    new DapatkanAlamatTask(MainActivity.this,MainActivity.this).execute(locationResult.getLastLocation());
                }
            }
        };
    }

    private void mulaiTrackingLokasi()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_LOCATION_PERMISSION);
        }
        else
        {
            mFusedLocationClient.requestLocationUpdates(getLocationRequest(),mLocationCallback,null);
            mLocationTextView.setText(getString(R.string.alamat_text,"Sedang mencari nama tempat","Sedang mencari alamat", System.currentTimeMillis()));
            mTrackingLocation=true;
            mLocationButton.setText("Stop Traking Lokasi");
            mRotateAnim.start();
        }

    }

    private void stopTrackingLokasi()
    {
        if (mTrackingLocation)
        {
            mTrackingLocation=false;
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            mLocationButton.setText("Mulai Tracking Lokasi");
            mLocationTextView.setText("Tracking sedang dihetikan");
            mRotateAnim.end();
        }
    }

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    public void setRequestPermissionResult(int requestCode, @NonNull String[]permission, @NonNull int[]grantResult)
    {
        switch (requestCode)
        {
            case REQUEST_LOCATION_PERMISSION:
                if (grantResult.length>0 && grantResult[0] == PackageManager.PERMISSION_GRANTED)
                {
                    mulaiTrackingLokasi();
                }
                else
                {
                    Toast.makeText(this,"Permission huahaha gak dapet, kasian",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void onTaskCompleted(final String result) throws SecurityException{
        if (mTrackingLocation) {

            Task<PlaceLikelihoodBufferResponse> placeResult = mPlaceDetectionClient.getCurrentPlace(null);
            placeResult.addOnCompleteListener(new OnCompleteListener<PlaceLikelihoodBufferResponse>() {
                @Override
                public void onComplete(@NonNull Task<PlaceLikelihoodBufferResponse> task) {
                    if (task.isSuccessful()) {
                        PlaceLikelihoodBufferResponse likelyPlaces = task.getResult();
                        float maxLikelihood = 0;
                        Place currentPlace = null;
                        likelyPlaces.release();
                        for (PlaceLikelihood placeLikelihood : likelyPlaces){
                            if (maxLikelihood < placeLikelihood.getLikelihood()){
                                maxLikelihood = placeLikelihood.getLikelihood();
                                currentPlace = placeLikelihood.getPlace();
                                if (currentPlace != null){
                                    setTipeLokasi(currentPlace);
                                    mLocationTextView.setText(getString(R.string.alamat_text,currentPlace.getName(),result,System.currentTimeMillis()));
                                }
                            }
                        }

                    } else {
                        mLocationTextView.setText(getString(R.string.alamat_text,"Nama Lokasi Tidak Di Temukan!",result,System.currentTimeMillis()));
                    }
                }
            });
        }
    }

    private LocationRequest getLocationRequest()
    {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void setTipeLokasi (Place currentPlace) {
        int drawableID = -1;
        for (Integer placeType : currentPlace.getPlaceTypes()) {
            switch (placeType) {
                case Place.TYPE_UNIVERSITY:
                    drawableID = R.drawable.college;
                    break;
                case Place.TYPE_CAFE:
                    drawableID = R.drawable.shop;
                    break;
                case Place.TYPE_SHOPPING_MALL:
                    drawableID = R.drawable.mall;
                    break;
                case Place.TYPE_MOVIE_THEATER:
                    drawableID = R.drawable.cinema;
                    break;
                case Place.TYPE_BANK:
                    drawableID = R.drawable.bank;
                    break;
                case Place.TYPE_GAS_STATION:
                    drawableID = R.drawable.gas;
                    break;
                case Place.TYPE_AIRPORT:
                    drawableID = R.drawable.travel;
                    break;
            }
        }
        if (drawableID < 0) {
            drawableID = R.drawable.unknown;
        }

        mAndroidImageView.setImageResource(drawableID);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK)
        {
            Place place = PlacePicker.getPlace(this,data);
            setTipeLokasi(place);
            mLocationTextView.setText(getString(R.string.alamat_text,place.getName(),place.getAddress(), System.currentTimeMillis()));
        }
        else
        {
            mLocationTextView.setText("Location Not Selected");
        }
    }
}

package com.anx.application.jcustomer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sucho.placepicker.AddressData;
import com.sucho.placepicker.Constants;
import com.sucho.placepicker.MapType;
import com.sucho.placepicker.PlacePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerMapActivity extends AppCompatActivity implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, NavigationView.OnNavigationItemSelectedListener {

    Location mLastLocation;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;
    PlacesClient placesClient;
    Marker customMarker;

    private DrawerLayout drawer;

    private FusedLocationProviderClient mFusedLocationClient;

    private GoogleMap mMap;
    private LatLng pickUpLocation, destinationLatLng;
    private Marker pickupMarker;

    private Boolean requestBol = false;
    private String destination, requestService;

    private ImageView mDriverProfileImage, mActionBarImage;
    private LinearLayout mDriverInfo;
    private Button mLogout, mRequest, mSettings, mHistory, mBottomDrawerButton, mPlacePicker;
    private TextView mDriverName, mDriverPhone, mDriverCar, mFullName, mPhoneNumber;
    private RadioGroup mRadioGroup;
    private RatingBar mRatingBar;

    private LatLng pickLocationLatlng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        androidx.appcompat.widget.Toolbar toolbar =  findViewById(R.id.toolBar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        destinationLatLng = new LatLng(0.0, 0.0);

//        mHistory = findViewById(R.id.history);
        mBottomDrawerButton = findViewById(R.id.bottomDrawer);
        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverProfileImage = findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);
        mRadioGroup = findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.Bajjaj);
        mRatingBar = findViewById(R.id.ratingBar);
        mPlacePicker = findViewById(R.id.setLocationMap);

        final LayoutInflater factory = getLayoutInflater();

        final View textEntryView = factory.inflate(R.layout.nav_header, null);

        mActionBarImage = (ImageView) textEntryView.findViewById(R.id.profileImage);
        mFullName = (TextView) textEntryView.findViewById(R.id.nav_name);
        mPhoneNumber = (TextView) textEntryView.findViewById(R.id.nav_phone);
        String api_key = "AIzaSyBSVxzRAiYvKc-3-4AaKi8G0-tht285aHA";// for searching destination

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), api_key);
        }
        placesClient = Places.createClient(this);

        mPlacePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CustomerMapActivity.this, "The people", Toast.LENGTH_SHORT).show();
            }
        });



        //open service type chooser
        mBottomDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRadioGroup.getVisibility() == View.GONE){
                    mRadioGroup.setVisibility(View.VISIBLE);
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectId);
                    requestService = radioButton.getText().toString();
                    mBottomDrawerButton.setText("Service type: "+requestService);
                } else {
                    mRadioGroup.setVisibility(View.GONE);
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectId);
                    requestService = radioButton.getText().toString();
                    mBottomDrawerButton.setText("Service type: "+requestService);
                }
            }
        });

        // request button use and listen
        mRequest = findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                    endRide();
                } else {
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectId);

                    if (radioButton.getText() == null) {
                        Toast.makeText(CustomerMapActivity.this, "please check a vehicle type", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    requestService = radioButton.getText().toString();
                    mBottomDrawerButton.setText(requestService);
                    requestBol = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

                    GeoFire geoFire = new GeoFire(ref);
                    geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            Log.i("Last Location", "" + mLastLocation.getLatitude() + " : " + mLastLocation.getLongitude());
                        }
                    });

                    pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                    pickupMarker = mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pickup Here"));
                    Log.i("gettingDriver", "getting driver...");
                    mRequest.setText("Getting your driver...");

                    getClosestDriver();
                }
            }
        });

        AutocompleteSupportFragment autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        assert autocompleteSupportFragment != null;
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint("Where to...");
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                destination = place.getName();
                destinationLatLng = place.getLatLng();
                Log.i("placeName", place.getName());
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("ErrorOccurred", "an error occurred" + status);
            }
        });


        getUserInformation();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)){
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void getUserInformation(){
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0){
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null){
                        mFullName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null){
                        mPhoneNumber.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl")!= null){
                        StorageReference storageReference =  FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                Glide.with(getApplicationContext()).load(uri.toString()).error(R.drawable.ic_default_profile).into(mActionBarImage);
                            }
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;

    GeoQuery geoQuery;

    private void getClosestDriver() {
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference("driversAvailable");
        GeoFire geoFire = new GeoFire(driverLocation);
        geoQuery = geoFire.queryAtLocation(new GeoLocation(pickUpLocation.latitude, pickUpLocation.longitude), radius);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                Log.i("anwar", "" + geoQuery);
                if (!driverFound && requestBol) {
                    // checking for car service type
                    DatabaseReference mCustomerDatabaseReference = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(key);
                    mCustomerDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                                Map<String, Object> driverMap = (Map<String, Object>) dataSnapshot.getValue();
                                if (driverFound) {
                                    return;
                                }
                                if (driverMap.get("service").equals(requestService)) {
                                    driverFound = true;
                                    driverFoundId = dataSnapshot.getKey();
                                    Log.i("driverLocation", "Looking for driver location...");
                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideId", customerId);
                                    map.put("destination", destination);
                                    map.put("destinationLatitude", destinationLatLng.latitude);
                                    map.put("destinationLongitude", destinationLatLng.longitude);
                                    driverRef.updateChildren(map);
                                    getDriverLocation();
                                    getDriverInfo();
                                    getHasDriveEnded();
                                    mRequest.setText("Looking for driver location...");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });


                }
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if (!driverFound) {
                    radius++;
                    Log.i("anwar", "" + radius);
                    getClosestDriver();
                }
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;

    private void getDriverLocation() {
        driverLocationRef = FirebaseDatabase.getInstance().getReference().child("driversWorking").child(driverFoundId).child("l");

        driverLocationRefListener = driverLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && requestBol) {
                    List<Object> map = (List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    mRequest.setText("Driver found");

                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());
                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng driverLatLng = new LatLng(locationLat, locationLng);
                    if (mDriverMarker != null) {
                        mDriverMarker.remove();
                    }

                    Location locPickup = new Location("");
                    locPickup.setLatitude(pickUpLocation.latitude);
                    locPickup.setLongitude(pickUpLocation.longitude);

                    Location locDriver = new Location("");
                    locDriver.setLatitude(driverLatLng.latitude);
                    locDriver.setLongitude(driverLatLng.longitude);

                    float distance = locPickup.distanceTo(locDriver);
                    if (distance < 0.1) {
                        mRequest.setText("Your Driver is here");
                    } else {
                        mRequest.setText("Your driver is coming: " + (String.valueOf(distance / 1000)) + " kms away");
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    StorageReference storageReference;
    String imageURL;

    private void getDriverInfo() {
        Log.i("CustomerInfo", "Info called");
        mDriverInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (dataSnapshot.child("name").getValue() != null) {
                        mDriverName.setText("Driver Name: " + dataSnapshot.child("name").getValue());
                    }
                    if (dataSnapshot.child("phone").getValue() != null) {
                        mDriverPhone.setText("Driver Phone: " + dataSnapshot.child("phone").getValue());
                    }
                    if (dataSnapshot.child("car").getValue() != null) {
                        mDriverCar.setText("Car Type: " + dataSnapshot.child("car").getValue());
                    }
                    if (dataSnapshot.child("profileImageUrl").getValue() != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profileImage").child(driverFoundId);
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                //do your stuff- uri.toString() will give you download URL\\
                                Glide.with(getApplicationContext()).load(uri.toString()).error(R.drawable.ic_default_profile).into(mDriverProfileImage);
                            }
                        });
                    }
                    int ratingSum = 0;
                    float ratingTotal = 0;
                    float ratingAvg = 0;
                    for (DataSnapshot child : dataSnapshot.child("rating").getChildren()) {
                        ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                        ratingTotal++;
                    }

                    if (ratingTotal != 0) {
                        ratingAvg = ratingSum / ratingTotal;
                        mRatingBar.setRating(ratingAvg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;

    private void getHasDriveEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { // If there is a customer ride id

                } else { // If there is no customer ride id
                    endRide();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void endRide() { // customer cancels ride
        requestBol = false;

        geoQuery.removeAllListeners();
        Log.i("cancelRequest", "GeoQuery listeners removed");
        if (driverLocationRefListener != null) {
            driverLocationRef.removeEventListener(driverLocationRefListener);
        }
        Log.i("cancelRequest", "Driver location listeners removed");
        if (driverLocationRefListener != null) {
            driveHasEndedRef.removeEventListener(driveHasEndedRefListener);
        }
        Log.i("cancelRequest", "drive has ended");

        if (driverFoundId != null) {
            DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
            driverRef.removeValue();
            driverFoundId = null;
            Log.i("cancelRequestFound", "drive has ended");
        }
        driverFound = false;
        radius = 1;

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("customerRequest");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId, new GeoFire.CompletionListener() {
            @Override
            public void onComplete(String key, DatabaseError error) {
                Log.i("CustomerRequestRemoved", "Customer Request Removed");
            }
        });

        if (pickupMarker != null) {
            pickupMarker.remove();
            if (mDriverMarker != null) {
                mDriverMarker.remove();
            }
            Log.i("pickupMarker", "Marker removed");
        }

        mRequest.setText("Call Driver");


        mDriverInfo.setVisibility(View.GONE);
        mDriverPhone.setText("");
        mDriverName.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.drawable.ic_default_profile);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
//        try{
//            boolean success = googleMap.setMapStyle(
//                    MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));
//
//            if (!success){
//                Toast.makeText(this, "Style parsing failed", Toast.LENGTH_SHORT).show();
//            }
//        } catch (Resources.NotFoundException e){
//            Toast.makeText(this, ""+e, Toast.LENGTH_SHORT).show();
//        }

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }
        }

        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    mLastLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.i("Hello", "Location Changed");
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                    mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
//                    mMap.addMarker(new MarkerOptions()
//                        .position(latLng)
////                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin))
//                        .title("You"));

                    if (!getDriversAroundStarted) {
                        getDriversAround();
                    }
                }
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Request")
                        .setMessage("Access Current Location")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        })
                        .create()
                        .show();
            } else {
                ActivityCompat.requestPermissions(CustomerMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Please provide a permission", Toast.LENGTH_LONG).show();
                    Log.i("Hello", "Permission Denied");
                }
                break;
            }
        }
    }

    boolean getDriversAroundStarted = false;
    List<Marker> markerList = new ArrayList<Marker>();

    private void getDriversAround() {
        getDriversAroundStarted = true;
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driversLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 10000);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                for (Marker markerIt : markerList) {
                    if (markerIt.getTag().equals(key)) {
                        return;
                    }
                }

                LatLng driverLocation = new LatLng(location.latitude, location.longitude);
                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation));

                mDriverMarker.setTag(key);
                markerList.add(mDriverMarker);
            }

            @Override
            public void onKeyExited(String key) {
                for (Marker markerIt : markerList) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.remove();
                        markerList.remove(markerIt);
                        return;
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for (Marker markerIt : markerList) {
                    if (markerIt.getTag().equals(key)) {
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Intent intent;
        switch (menuItem.getItemId()){
            case R.id.nav_history:
                intent = new Intent(CustomerMapActivity.this, HistoryActivity.class);
                intent.putExtra("customerOrDriver", "Customers");
                startActivity(intent);
                break;
            case R.id.nav_profile:
                intent = new Intent(CustomerMapActivity.this, CustomerSettingActivity.class);
                startActivity(intent);
                break;
            case R.id.nav_logout:
                logOut();
                break;
            case R.id.nav_callus:
                Toast.makeText(this, "Call Us", Toast.LENGTH_SHORT).show();
                break;
            case R.id.nav_email:
                Toast.makeText(this, "Email Us", Toast.LENGTH_SHORT).show();
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    private void logOut() {

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        return;
    }
}

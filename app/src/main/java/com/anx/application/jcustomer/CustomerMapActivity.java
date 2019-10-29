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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.TimeUtils;
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
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.anx.application.jcustomer.HistoryRecyclerView.HistoryObject;
import com.anx.application.jcustomer.HistoryRecyclerView.HistoryViewHolder;
import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
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
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.LocationRestriction;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sucho.placepicker.AddressData;
import com.sucho.placepicker.Constants;
import com.sucho.placepicker.MapType;
import com.sucho.placepicker.PlacePicker;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static androidx.annotation.InspectableProperty.ValueType.COLOR;

public class CustomerMapActivity extends AppCompatActivity implements RoutingListener, OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback, NavigationView.OnNavigationItemSelectedListener {

    private static final int[] COLORS = new int[]{R.color.colorPrimaryDark};
    Location mLastLocation;
    LocationRequest mLocationRequest;
    SupportMapFragment mapFragment;
    PlacesClient placesClient;
    GeoQuery geoQuery;
    boolean getDriversAroundStarted = false;
    List<Marker> markerList = new ArrayList<Marker>();
    private DrawerLayout drawer;
    private List<Polyline> polylines;
    private FusedLocationProviderClient mFusedLocationClient;
    private GoogleMap mMap;
    private boolean cameraSet = false;
    private LinearLayout mRideInfo;

    private int BUTTON_STATUS;


    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            for (Location location : locationResult.getLocations()) {
                if (getApplicationContext() != null) {
                    mLastLocation = location;
                    final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    Log.i("Hello", "Location Changed");
                    if (!cameraSet){
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        cameraSet = true;
                    }

                    mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                        @Override
                        public boolean onMyLocationButtonClick() {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                            return true;
                        }
                    });

                    if (!getDriversAroundStarted) {
                        getDriversAround();
                    }
                }
            }
        }
    };
    private LatLng pickUpLocation, destinationLatLng;
    private Marker pickupMarker;
    private Boolean requestBol = false;
    private String destination, requestService;
    private ImageView mDriverProfileImage, mActionBarImage;
    private LinearLayout mDriverInfo;
    private Button mLogout, mRequest, mSettings, mHistory, mBottomDrawerButton, mPlacePicker;
    private TextView mDriverName, mDriverPhone, mDriverCar, mFullName, mPhoneNumber, mRideFare, mRideTime;
    private RadioGroup mRadioGroup;
    private RatingBar mRatingBar;
    private LatLng pickLocationLatlng;
    private int radius = 1;
    private Boolean driverFound = false;
    private String driverFoundId;
    private Marker mDriverMarker;
    private DatabaseReference driverLocationRef;
    private ValueEventListener driverLocationRefListener;
    private DatabaseReference driveHasEndedRef;
    private ValueEventListener driveHasEndedRefListener;
    private View mapView;
    double fare;

    AutocompleteSupportFragment autocompleteSupportFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_map);

        polylines = new ArrayList<>();


        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Build the alert dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Location Services Not Active");
            builder.setMessage("Please enable Location Services and GPS");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            Dialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
        }
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolBar);
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
        mapView = mapFragment.getView();
        mapFragment.getMapAsync(this);

        destinationLatLng = new LatLng(0.0, 0.0);

        mBottomDrawerButton = findViewById(R.id.bottomDrawer);
        mDriverInfo = findViewById(R.id.driverInfo);
        mDriverProfileImage = findViewById(R.id.driverProfileImage);
        mDriverName = (TextView) findViewById(R.id.driverName);
        mDriverPhone = (TextView) findViewById(R.id.driverPhone);
        mDriverCar = (TextView) findViewById(R.id.driverCar);
        mRadioGroup = findViewById(R.id.radioGroup);
        mRadioGroup.check(R.id.taxi);
        mRatingBar = findViewById(R.id.ratingBar);
        mPlacePicker = findViewById(R.id.setLocationMap);
        mRideFare = findViewById(R.id.ride_fare);
        mRideTime = findViewById(R.id.ride_time);
        mRideInfo = findViewById(R.id.ride_info);

        View v = navigationView.getHeaderView(0);


        mActionBarImage = v.findViewById(R.id.profileImage);
        mFullName = v.findViewById(R.id.nav_name);
        mPhoneNumber = v.findViewById(R.id.nav_phone);

        getUserInformation();



        String api_key = "AIzaSyBSVxzRAiYvKc-3-4AaKi8G0-tht285aHA";// for searching destination

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), api_key);
        }
        placesClient = Places.createClient(this);

        mPlacePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new PlacePicker.IntentBuilder()
                        .setLatLong(mLastLocation.getLatitude(), mLastLocation.getLongitude())
                        .showLatLong(true)
                        .build(CustomerMapActivity.this);
                startActivityForResult(intent, Constants.PLACE_PICKER_REQUEST);
            }
        });


        //open service type chooser
        mBottomDrawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRadioGroup.getVisibility() == View.GONE) {
                    mRadioGroup.setVisibility(View.VISIBLE);
                    mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(RadioGroup group, int checkedId) {
                            int selectId = mRadioGroup.getCheckedRadioButtonId();
                            final RadioButton radioButton = (RadioButton) findViewById(selectId);
                            requestService = radioButton.getText().toString();
                            if (requestService.equals("Bajjaj")) {
                                mBottomDrawerButton.setText("Service type: Bajjaj");
                            } else {
                                mBottomDrawerButton.setText("Service type: Taxi");
                            }
                            mRadioGroup.setVisibility(View.GONE);
                        }
                    });
                } else {
                    mRadioGroup.setVisibility(View.GONE);
                    int selectId = mRadioGroup.getCheckedRadioButtonId();
                    final RadioButton radioButton = (RadioButton) findViewById(selectId);
                    requestService = radioButton.getText().toString();
                    if (requestService.equals("Bajjaj")){
                        mBottomDrawerButton.setText("Service type: Bajjaj");
                    } else {
                        mBottomDrawerButton.setText("Service tpe: Taxi");
                    }
                }
            }
        });

        // request button use and listen
        mRequest = findViewById(R.id.request);
        mRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestBol) {
                    new AlertDialog.Builder(CustomerMapActivity.this)
                            .setTitle("Cancel Request.")
                            .setMessage("Are you sure you want to cancel drive?")
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    endRide();
                                }
                            })
                            .create()
                            .show();
                } else {
                    if (destinationLatLng.latitude == 0.0 && destinationLatLng.longitude == 0.0){
                        Toast.makeText(CustomerMapActivity.this, ""+destinationLatLng, Toast.LENGTH_SHORT).show();
                        new AlertDialog.Builder(CustomerMapActivity.this)
                                .setTitle("Destination Empty")
                                .setMessage("Please search for a destination or add it using 'Add location'")
                                .setPositiveButton(android.R.string.yes, null)
                                .create()
                                .show();
                    } else {
                        mRideInfo.setVisibility(View.GONE);
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
                        mRequest.setText("Requesting driver... | Tap to cancel");
                        BUTTON_STATUS = 1;
                        getClosestDriver();
                    }
                }
            }
        });

        autocompleteSupportFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        assert autocompleteSupportFragment != null;
        autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteSupportFragment.setHint("search drop off...");
        autocompleteSupportFragment.setCountry("ET");
        autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                mPlacePicker.setText("Add location");
                destination = place.getName();
                destinationLatLng = place.getLatLng();
                Toast.makeText(CustomerMapActivity.this, destination + " " + destinationLatLng, Toast.LENGTH_SHORT).show();
                getRouteToMarker(destinationLatLng);
                if (polylines.size() > 0) {
                    for (Polyline poly : polylines) {
                        poly.remove();
                    }
                }
                getRideInfo(destinationLatLng);
                Log.i("placeName", place.getName());
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("ErrorOccurred", "an error occurred" + status);
            }
        });
    }

    private void getRideInfo(LatLng destinationLatLng) {

        mRideInfo.setVisibility(View.VISIBLE);

        pickLocationLatlng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
        Location pickUp = new Location(LocationManager.GPS_PROVIDER);
        pickUp.setLatitude(pickLocationLatlng.latitude);
        pickUp.setLongitude(pickLocationLatlng.longitude);

        Location dropOff = new Location(LocationManager.GPS_PROVIDER);
        dropOff.setLatitude(destinationLatLng.latitude);
        dropOff.setLongitude(destinationLatLng.longitude);

        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);

        double rideDistance = Double.parseDouble(df.format(pickUp.distanceTo(dropOff)));

        Toast.makeText(this, rideDistance+"", Toast.LENGTH_SHORT).show();


        if (mBottomDrawerButton.getText().equals("Service type: Bajjaj")) {

            int time =  (int) (rideDistance/40) * 1000;
            fare = ((rideDistance/ 1000) * 13) + 15;
            mRideTime.setText("approx. arrival time: " + secToTime(time));
            mRideFare.setText("Fare: " + df.format(fare) + " Birr");

        } else {
            int time = (int) (rideDistance/60) * 1000;
            fare = ((rideDistance/ 1000) * 13) + 40;
            mRideTime.setText("approx. arrival time: " + secToTime(time));
            mRideFare.setText("Fare: " + df.format(fare) + " Birr");
        }
    }

    String secToTime(int sec) {
        int second = sec % 60;
        int minute = sec / 60;
        if (minute >= 60) {
            int hour = minute / 60;
            minute %= 60;
            return hour + ":" + (minute < 10 ? "0" + minute : minute) + ":" + (second < 10 ? "0" + second : second);
        }
        return minute + ":" + (second < 10 ? "0" + second : second);
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    private void getRouteToMarker(LatLng pickupLatLng) {
        if (pickupLatLng != null && mLastLocation != null) {
            Routing routing = new Routing.Builder()
                    .key("AIzaSyBSVxzRAiYvKc-3-4AaKi8G0-tht285aHA")
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(false)
                    .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), destinationLatLng)
                    .build();
            routing.execute();
        }
    }

    private void getUserInformation() {
        final String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference mCustomerDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Customers").child(userId);
        mCustomerDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.getChildrenCount() > 0) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();
                    if (map.get("name") != null) {
                        mFullName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        mPhoneNumber.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl") != null) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profileImage").child(userId);
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
                                if (driverMap.get("service").equals(requestService) && !(Double.parseDouble(driverMap.get("quota").toString()) < fare)) {
                                    driverFound = true;
                                    driverFoundId = dataSnapshot.getKey();
                                    Log.i("driverLocation", "Looking for driver location... | Tap to cancel");
                                    DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest");
                                    String customerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                    HashMap map = new HashMap();
                                    map.put("customerRideId", customerId);
                                    map.put("destination", destination);
                                    Toast.makeText(CustomerMapActivity.this, destination + " " + destinationLatLng, Toast.LENGTH_SHORT).show();
                                    map.put("destinationLatitude", destinationLatLng.latitude);
                                    map.put("destinationLongitude", destinationLatLng.longitude);
                                    driverRef.updateChildren(map);
                                    getDriverLocation();
                                    getDriverInfo();
                                    getHasDriveEnded();
                                    mRequest.setText("Looking for driver location... | Tap to cancel");
                                    BUTTON_STATUS = 2;
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

                    float distance = locPickup.distanceTo(locDriver)/1000;
                    if (distance < 0.1) {
                        mRequest.setText("Your Driver is here");
                        BUTTON_STATUS = 3;
                    } else {
                        DecimalFormat df = new DecimalFormat("#.#");
                        df.setRoundingMode(RoundingMode.CEILING);
                        mRequest.setText("Your driver is coming: " + (df.format(distance)) + " kms away");
                        BUTTON_STATUS = 4;
                    }
                    mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLatLng).title("Your Driver"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

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
                    if (dataSnapshot.child("cartype").getValue() != null) {
                        String carColor = "";
                        String carPlate = "";
                        if (dataSnapshot.child("color").getValue() != null){
                            carColor = dataSnapshot.child("color").getValue().toString();
                        }
                        if (dataSnapshot.child("plate").getValue() != null){
                            carPlate = dataSnapshot.child("plate").getValue().toString();
                        }
                        mDriverCar.setText("Vehicle : " + carColor + " " + dataSnapshot.child("cartype").getValue() + ", " + carPlate);
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

    private void getHasDriveEnded() {
        driveHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Drivers").child(driverFoundId).child("customerRequest").child("customerRideId");
        driveHasEndedRefListener = driveHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) { // If there is a customer ride id

                } else { // If there is no customer ride id
                    endRide();
                    getLastDriveInfo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void getLastDriveInfo() {


        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        Query lastQuery = databaseReference.child("Users").child("Customers").child(userId).child("history").getRef().orderByKey().limitToLast(1);
        lastQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String key = "";
                    for (DataSnapshot history : dataSnapshot.getChildren()){
                        key = history.getKey();
                    }
                    Intent intent = new Intent(CustomerMapActivity.this, HistorySingleActivity.class);
                    Bundle b = new Bundle();
                    Toast.makeText(CustomerMapActivity.this, ""+dataSnapshot.getChildren(), Toast.LENGTH_SHORT).show();
                    b.putString("rideId", key);
                    intent.putExtras(b);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
            }
        });

    }

    private void endRide() { // customer cancels ride
        requestBol = false;
        mPlacePicker.setText("set drop off location");

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

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        autocompleteSupportFragment.setText("");


        getDriversAround();

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
            Log.i("pickupMarker", "Marker removed");
        }
        if (mDriverMarker != null) {
            mDriverMarker.remove();
        }


        destinationLatLng = new LatLng(0.0, 0.0);

        mRequest.setText("Request Driver");


        mDriverInfo.setVisibility(View.GONE);
        mDriverPhone.setText("");
        mDriverName.setText("");
        mDriverCar.setText("");
        mDriverProfileImage.setImageResource(R.drawable.ic_default_profile);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mapView != null &&
                mapView.findViewById(Integer.parseInt("1")) != null) {
            // Get the button view
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            // and next place it, on bottom right (as Google Maps app)
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)
                    locationButton.getLayoutParams();
            // position on right bottom
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
            layoutParams.setMargins(0, 0, 600, 600);
        }


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

    private void getDriversAround() {
        getDriversAroundStarted = true;
        DatabaseReference driversLocation = FirebaseDatabase.getInstance().getReference().child("driversAvailable");
        GeoFire geoFire = new GeoFire(driversLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude()), 1000);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                for (Marker markerIt : markerList) {
                    if (markerIt.getTag().equals(key)) {
                        return;
                    }
                }

                LatLng driverLocation = new LatLng(location.latitude, location.longitude);
                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).icon(BitmapDescriptorFactory.fromResource(R.drawable.driver_around)));

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
        switch (menuItem.getItemId()) {
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

        new AlertDialog.Builder(CustomerMapActivity.this)
                .setTitle("Log-out")
                .setMessage("Are you sure you want to log-out?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(CustomerMapActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                        return;
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteindex) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        pickUpLocation = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

        builder.include(pickUpLocation);
        builder.include(destinationLatLng);
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width * 0.2);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cameraUpdate);

        mMap.addMarker(new MarkerOptions().position(pickUpLocation).title("Pickup Location"));
        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination")).remove();

        mMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Destination"));

        if (polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        polylines = new ArrayList<>();
        //add route(s) to the map.
        for (int i = 0; i < route.size(); i++) {

            //In case of more than 5 alternative routes
            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);

            Toast.makeText(getApplicationContext(), "Route " + (i + 1) + ": distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRoutingCancelled() {

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == Constants.PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                try {
                    AddressData addressData = data.getParcelableExtra(Constants.ADDRESS_INTENT);
                    destination = addressData.component3().get(0).getAddressLine(0);
                    destinationLatLng = new LatLng(addressData.getLatitude(), addressData.getLongitude());
                    getRouteToMarker(destinationLatLng);
                    getRideInfo(destinationLatLng);
                    mPlacePicker.setText(addressData.component3().get(0).getAddressLine(0) + "");
                    Toast.makeText(this, ""+destinationLatLng, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Log.e("CustomerMapActivity", e.getMessage());
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

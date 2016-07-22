package ua.in.badparking.ui.fragments;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import roboguice.inject.InjectView;
import ua.in.badparking.R;
import ua.in.badparking.model.Claim;
import ua.in.badparking.services.ClaimState;
import ua.in.badparking.ui.activities.MainActivity;

/**
 * @author Dima Kovalenko & Vladimir Dranik
 */
public class LocationFragment extends BaseFragment implements GoogleMap.OnMyLocationButtonClickListener, OnMapReadyCallback{

    private static final String TAG = LocationFragment.class.getName();
    private static final int WAITING_TIME_MILLIS = 1000;
    private static final int ACCURANCY_IN_METERS = 3;

    private GoogleMap mMap;
    private Location location;
    private LocationManager locationManager;
    private Geocoder geocoder;
    private Claim claim = ClaimState.INST.getClaim();
    @InjectView(R.id.positioning_text_view)
    private TextView positioningText;
    @InjectView(R.id.next_button)
    private Button nextButton;

    public static Fragment newInstance() {
        return new LocationFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_location, container, false);

        SupportMapFragment fragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        fragment.getMapAsync(this);

        locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        geocoder = new Geocoder(getContext(), Locale.getDefault());

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainActivity) getActivity()).moveToNext();
            }
        });
        nextButton.setVisibility(View.GONE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if(mMap != null) {
            UiSettings uiSettings = mMap.getUiSettings();
            mMap.setMyLocationEnabled(true);
            mMap.setOnMyLocationButtonClickListener(this);
            uiSettings.setMyLocationButtonEnabled(true);
            uiSettings.setTiltGesturesEnabled(false);
            uiSettings.setCompassEnabled(false);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, WAITING_TIME_MILLIS,
                ACCURANCY_IN_METERS, locationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            mapPositioning(location.getLatitude(), location.getLongitude());
            Address address = getAddress(location);

            if(address != null){
                DecimalFormat df = new DecimalFormat("#.######");
                claim.setCity(address.getLocality());
                claim.setAddress(address.getAddressLine(0));
                claim.setLatitude(df.format(address.getLatitude()).replace(",", "."));
                claim.setLongitude(df.format(address.getLongitude()).replace(",", "."));

                positioningText.setText(ClaimState.INST.getFullAddress());
                nextButton.setVisibility(View.VISIBLE);
            } else {
                positioningText.setText("Miсцезнаходження визначається …");
                nextButton.setVisibility(View.GONE);
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    private Address getAddress(Location location) {
        try {
            List<Address> addressList = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            if (addressList != null) {
                return addressList.get(0);
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Помилка геопозиціонування", Toast.LENGTH_SHORT).show();
        }

        return null;
    }

    private void mapPositioning(double latitude, double longitude){
        LatLng coordinates = new LatLng(latitude, longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates, 13));

        CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(coordinates)
                    .zoom(17)
                    .bearing(90)
                    .tilt(40)
                    .build();

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
}


package edu.uw.bn22.geopaint;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import petrov.kristiyan.colorpicker.ColorPicker;

import static android.content.pm.PackageManager.*;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = "Geo-Paint Application";
    private GoogleApiClient mGoogleApiClient;
    private GoogleMap mMap;
    private static final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    private PolylineOptions line = new PolylineOptions();
    private List<LatLng> lineList = new ArrayList<>();
    private List<Polyline> lineSave = new ArrayList<>();
    private int count = 0;
    private Boolean pen = true;
    private String selectedColor = "";
    private int selectColor = -16777216;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        //Creates the Google API Client that allows the location to be tracked
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        //Remembers the user's setting
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        pen = prefs.getBoolean("penDown", true);
        selectColor = prefs.getInt("color", 0);
        
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.setRetainInstance(true);
        mapFragment.setHasOptionsMenu(true);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Initializes a map with the zoom buttons apparant
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResult) {
        ///if the use doesn't allow access to location (GPS)
        switch(requestCode){
            case REQUEST_CODE_ASK_PERMISSIONS: { //if asked for location
                if(grantResult.length > 0 && grantResult[0] == PERMISSION_GRANTED){
                    onConnected(null); //should work :/
                }
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        //Checks for permission to access the location (GPS)
        LocationRequest request = new LocationRequest();
        request.setInterval(10000);
        request.setFastestInterval(5000);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission == PERMISSION_GRANTED){
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);

            //Creates the inital marker indicating the user's current point
            try {
                Log.v(TAG, "Got location");
                Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if(loc != null) {
                    Log.v(TAG, "Not Null");
                    LatLng startPoint = new LatLng(loc.getLatitude(), loc.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(startPoint).title("Starting Point"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(startPoint));
                    if (pen) {
                        lineList = line.getPoints();
                        line = line.add(startPoint);
                        Polyline path = mMap.addPolyline(line);
                        lineSave.add(path);
                        path.setPoints(lineList);
                    }
                }
            } catch (SecurityException e) {
                Log.v(TAG, "Security Exception");
            }
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    public void drawLine(Location current) {
        //Draws a line whenever a new location is passed
        if (current != null) {
            LatLng newPoint = new LatLng(current.getLatitude(), current.getLongitude());
            if (pen) {
                lineList = line.getPoints();
                line = line.add(newPoint);
                Polyline path = mMap.addPolyline(line);
                lineSave.add(path);
                path.setPoints(lineList);
            } else {
                mMap.addMarker(new MarkerOptions().position(newPoint).title("Marker " + count));
                count = count + 1;
            }
            mMap.moveCamera(CameraUpdateFactory.newLatLng(newPoint));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        drawLine(location);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        //Allows requests for location updates
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        //Stops requests for location updates
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Creates the Option menu
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Listens for clicks on the options menu
        int optionClickedId = item.getItemId();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = prefs.edit();
        switch(item.getItemId()){
            case R.id.menu_item1 :
                penSettings();
                editor.putBoolean("penDown", pen);
                editor.apply();
                return true;
            case R.id.menu_item2 :
                colorSettings();
                return true;
            case R.id.menu_item3 :
                saveData();
                return true;
            case R.id.menu_item4 :
                shareData();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void penSettings() {
        //Checks to see if the pen is enabled or disabled, Sends a toast to give the user an indication of the current setting
        if (pen) {
            pen = false;
            Toast.makeText(getBaseContext(), "Pen is not drawing", Toast.LENGTH_LONG).show();
        } else {
            pen = true;
            Toast.makeText(getBaseContext(), "Pen is now drawing", Toast.LENGTH_LONG).show();
        }
    }

    public void colorSettings() {
        //Allows the user to change the colors of their lines that they draw
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = prefs.edit();
        final ColorPicker colorPicker = new ColorPicker(MapsActivity.this);
        colorPicker.setFastChooser(new ColorPicker.OnFastChooseColorListener() {
            @Override
            public void setOnFastChooseColorListner(int position, int color) {
                //Changes the entire draw to the currently selected color
                selectColor = color;
                selectedColor = String.format("#%06X", (0xFFFFFF & selectColor));
                lineList = line.getPoints();
                line.color(selectColor);
                Polyline path = mMap.addPolyline(line);
                path.setPoints(lineList);
                Toast.makeText(getBaseContext(), selectedColor, Toast.LENGTH_LONG).show();
                editor.putInt("color", selectColor);
                editor.apply();
                colorPicker.dismissDialog();
            }
        }).setColumns(5).show();
    }

    public void saveData() {
        //Saves the image as a geojson file
        if (isExternalStorageWritable()) {
            try {
                File file = new File(this.getExternalFilesDir(null), "drawing.geojson");
                FileOutputStream outputStream = new FileOutputStream(file);
                String fileToSave = GeoJsonConverter.convertToGeoJson(lineSave);
                outputStream.write(fileToSave.getBytes());
                outputStream.close();
                Toast.makeText(getBaseContext(), "File Saved", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                Log.v(TAG, "error");
            }
        }
    }

    public boolean isExternalStorageWritable() {
        //Checks to see if it is possible to save the file
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public void shareData() {
        //Allows the user to share the file if they previously saved it
        Uri fileUri;

        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, "drawing.geojson");

        fileUri = Uri.fromFile(file);
        Log.v(TAG, "File is at: " + fileUri);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);

        Intent chooser = Intent.createChooser(shareIntent, "Share this file");
        startActivity(chooser);
    }
}

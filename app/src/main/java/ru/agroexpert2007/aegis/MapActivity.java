package ru.agroexpert2007.aegis;

import android.Manifest;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements FileSaveDialog.DataExchange {
    private MapView mMapView;
    private DialogFragment mDialogFragment;
    private List<Overlay> mOverlays;
    private List<GeoPoint> mGeoPoints;
    private KmlDocument mKmlDocumentToSave;
    private File mDirectoryToSave;
    private File mFileToSave;
    private IMapController mapController;
    LocationManager mLocationManager;
    LocationListener mLocationListener;

    /*
    тайл с гугла (нелицензионный)
     */
    public static final OnlineTileSourceBase GOOGLE_HYBRID = new XYTileSource("Google-Hybrid",
            0, 19, 256, ".png", new String[] {
            "http://mt0.google.com",
            "http://mt1.google.com",
            "http://mt2.google.com",
            "http://mt3.google.com",

    }) {
        @Override
        public String getTileURLString(final long pMapTileIndex) {
            return getBaseUrl() + "/vt/lyrs=y&x=" + MapTileIndex.getX(pMapTileIndex) + "&y=" + MapTileIndex.getY(pMapTileIndex) + "&z=" + MapTileIndex.getZoom(pMapTileIndex);
        }
    };

    OnlineTileSourceBase mGoogleOnlineTileSourceBase;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.finish_gps_track:
                mLocationManager.removeUpdates(mLocationListener);
                mMapView.invalidate();
                return true;
            case R.id.start_gps_track:
                startGPSTrack();
                return true;
            case R.id.delete_marker:
                OverlaysHandler.removeOverlay(mOverlays, mOverlays.size() - 1);
                mMapView.invalidate();
                return true;
            case R.id.GPS_coordinates:
                setLocationFromGPS();
                return true;
            case R.id.create_dirs:
                createContentDirsIfNotExist();
                return true;
            case R.id.open_kml:
                getKmlsFromDefaultDir();
                return true;
            case R.id.save:
                mDialogFragment.show(getFragmentManager(), "fsDlg");
                return true;
            case R.id.polyline:
                Polyline polyline = new Polyline(mMapView);
                polyline.setPoints(mGeoPoints);
                polyline.setWidth(2);
                polyline.setColor(Color.BLUE);
                polyline.setTitle("polyline " + mOverlays.size());
                mOverlays.add(polyline);
                mMapView.invalidate();
                mKmlDocumentToSave.mKmlRoot.addOverlay(polyline, mKmlDocumentToSave);
                mGeoPoints.clear();
                return true;
            case R.id.polygon:
                Polygon polygon = new Polygon(mMapView);
                polygon.setPoints(mGeoPoints);
                mOverlays.add(polygon);
                polygon.setFillColor(0x8000FFFF);
                polygon.setStrokeColor(Color.RED);
                polygon.setStrokeWidth(2);
                polygon.setTitle("polygon " + mOverlays.size());
                mMapView.invalidate();
                mKmlDocumentToSave.mKmlRoot.addOverlay(polygon, mKmlDocumentToSave);
                mGeoPoints.clear();
                return true;
            case R.id.clear_all_geo_points:
                mGeoPoints.clear();
                return true;
            case R.id.clear_last_geopoint:
                mGeoPoints.remove(mGeoPoints.size() - 1);
            case R.id.delete_all_markers:
                OverlaysHandler.removeAllMarkers(mOverlays);
                mMapView.invalidate();
                return  true;
            case R.id.MAPNIK:
                mMapView.setTileSource(TileSourceFactory.MAPNIK);
                return true;
            case R.id.GOOGLE_HYBRID:
                mMapView.setTileSource(GOOGLE_HYBRID);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        checkPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE});

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        mGeoPoints = new ArrayList<>();
        mKmlDocumentToSave = new KmlDocument();
        File sdDir = Environment.getExternalStorageDirectory();
        mDirectoryToSave = new File(sdDir, "gis/saved/");

        setContentView(R.layout.activity_map);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
       /* getSupportActionBar().setDisplayShowTitleEnabled(false);*/
        mDialogFragment = new FileSaveDialog();
        mMapView = (MapView) findViewById(R.id.map);

        mMapView.setTileSource(GOOGLE_HYBRID);
//        mMapView.setTileSource(TileSourceFactory.MAPNIK);
        mMapView.setBuiltInZoomControls(true);
        mMapView.setMultiTouchControls(true);

        mapController = mMapView.getController();
        mapController.setZoom(11.0);
        GeoPoint startPoint = new GeoPoint(56.8561, 41.3892);
        mapController.setCenter(startPoint);
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        mOverlays = mMapView.getOverlays();
        setTapEventHandler();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * Устанавливает обработку событий по тапу
     */
    private void setTapEventHandler() {
        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                mGeoPoints.add(p);
                Marker tapMarker = new Marker(mMapView);
                tapMarker.setPosition(p);
                tapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                tapMarker.setTitle("Marker " + mOverlays.size());
                mOverlays.add(tapMarker);
                mMapView.invalidate();
                return false;
            }
        });
        mOverlays.add(mapEventsOverlay);
    }

    /**
     * Создает необходимые для работы прилоежния директории, если они еще не созданы
     */
    private void createContentDirsIfNotExist() {
        File sdCardDirectory = Environment.getExternalStorageDirectory();
        File defaultDirectory = new File(sdCardDirectory, "gis/default");
        File saveDirectory = new File(sdCardDirectory, "gis/saved");
        if (!defaultDirectory.exists()) {
            defaultDirectory.mkdirs();
        }
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }

    }

    /**
     * загружает рекурсивно файлы kml и помещает их  на дефолтный слой
     */
    private void getKmlsFromDefaultDir() {
        File sdDir = Environment.getExternalStorageDirectory();
        KmlDocument kmlDocument = new KmlDocument();
        File file = new File(sdDir, "gis/default");
        ArrayList<File> files = FileProcess.getFileList(file);
        if (!files.isEmpty()) {
            for (File f : files) {
                kmlDocument.parseKMLFile(f);
                FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(mMapView, null, null, kmlDocument);
                mMapView.getOverlays().add(kmlOverlay);
            }
            mMapView.invalidate();
            BoundingBox bb = kmlDocument.mKmlRoot.getBoundingBox();
            mMapView.getController().setCenter(bb.getCenterWithDateLine());
        }
    }

    /**
     * Проверяет и запрашивает разрешения к опасным функциям ОС
     *
     * @param permissions разрения в формате Manifest.permission.ACCESS_FINE_LOCATION
     */
    private void checkPermission(String[] permissions) {
        for (String s : permissions) {
            if (ActivityCompat.checkSelfPermission(this, s) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{s}, 0);
            }
        }
    }

    private void setLocationFromGPS() {
        checkPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE});

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitudeFormGPS = location.getLatitude();
                double longitudeFromGPS = location.getLongitude();
                GeoPoint geoPointGPS = new GeoPoint(latitudeFormGPS, longitudeFromGPS);
                Marker markerGPS = new Marker(mMapView);
                mapController.setCenter(geoPointGPS);
                markerGPS.setPosition(geoPointGPS);
                markerGPS.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                markerGPS.setTitle("GPS Marker " + mOverlays.size());
                mOverlays.add(markerGPS);
                mMapView.invalidate();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
    }

    private void startGPSTrack() {
        checkPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET, Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE});

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                double latitudeFormGPS = location.getLatitude();
                double longitudeFromGPS = location.getLongitude();
                GeoPoint geoPointGPS = new GeoPoint(latitudeFormGPS, longitudeFromGPS);
                mGeoPoints.add(geoPointGPS);
                mapController.setCenter(geoPointGPS);
                Marker markerGPS = new Marker(mMapView);
                markerGPS.setPosition(geoPointGPS);
                markerGPS.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                markerGPS.setTitle("GPS Marker " + mOverlays.size());
                mOverlays.add(markerGPS);
                mMapView.invalidate();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        };
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, mLocationListener);

    }

    /**
     *
     * @param s возвращает значение из file FileToSaveDialog
     */
    @Override
    public void exchange(String s) {
        mFileToSave = new File(mDirectoryToSave, s + ".kml");
        mKmlDocumentToSave.saveAsKML(mFileToSave);
        Log.d("skhanov", s);
    }
}

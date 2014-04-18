
package org.baobab.baolizer;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SlidingDrawer;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import ch.hsr.geohash.GeoHash;

public class MapActivity  extends ActionBarActivity implements
        View.OnClickListener,
        LoaderCallbacks<Cursor>,
        OnCheckedChangeListener,
        OnInfoWindowClickListener,
        ListView.OnItemClickListener {

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;

    private GoogleMap map;
    private MyMapFragment mapFragment;
    private HashMap<String, String> podioId;
    private static final String TAG = "Baolizer";
    public static final String SUBMIT = "submit/";
    public static final String WEBVIEW = "webview/";
    private static final String PAGE_HTML = ".page.html";

    private LinearLayout types;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item,
                getResources().getStringArray(R.array.menu)));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // TODO: Drawer toggle don't work!!??
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                System.out.println("close");
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                System.out.println("open");
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);


        map = ((SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map)).getMap();
        map.setMyLocationEnabled(true);
        map.animateCamera(CameraUpdateFactory
                .newCameraPosition(new CameraPosition(
                        new LatLng(48.138790, 11.553338), 12, 90, 0)));
        map.setOnInfoWindowClickListener(this);
        /*types = (LinearLayout) findViewById(R.id.types);
        for (int i = 0; i < types.getChildCount(); i++) {
            ((CheckBox) types.getChildAt(i)).setOnCheckedChangeListener(this);
        }*/
        //findViewById(R.id.seed).setOnClickListener(this);
        getSupportLoaderManager().initLoader(0, null, this);
        startService(new Intent(this, RefreshService.class));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.map_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_filter:
                SlidingDrawer drawer = (SlidingDrawer) findViewById(R.id.drawer);
                if (drawer.isOpened()) {
                    drawer.animateClose();
                } else {
                    drawer.animateOpen();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
            System.out.println("CLIGG");
        }
    }


    // Drawer onItemClick
    private void selectItem(int position) {


        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (position) {
            case 0:
                if (mapFragment == null || !mapFragment.isAdded())
                    mapFragment = new MyMapFragment();
                fragmentManager.beginTransaction().replace(R.id.frame, mapFragment).commit();
                break;
            case 1:
            case 2:
                WebFragment fragment = new WebFragment();
                Bundle args = new Bundle();
                args.putInt("position", position);
                args.putString("url", "http://map.baobab.org/submit/");
                fragment.setArguments(args);
                fragmentManager.beginTransaction().replace(R.id.frame, fragment).commit();
                break;

        }
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        //setTitle(mMenuTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);


    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        getSupportLoaderManager().restartLoader(0, null, this);
    }

    ContentObserver onChange = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
          getSupportLoaderManager().restartLoader(0, null, MapActivity.this);
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
        String where = "types IS 'none'";
        //String where = "types IS NOT 'none'";
        /*for (int i = 0; i < types.getChildCount(); i++) {
            CheckBox typ = (CheckBox) types.getChildAt(i);
            if (typ.isChecked()) {
                where += " OR types LIKE '%" + typ.getText() + "%'";
            }
        }*/

        //if (arg1 != null && (String)filterView.getItemAtPosition(arg1.getInt("position", 1)) != " Alle" ) {



          //  Log.d(TAG, "filter type: " + (String)filterView.getItemAtPosition(arg1.getInt("position", 1)));
          //  where += " OR types LIKE '%" + (String)filterView.getItemAtPosition(arg1.getInt("position", 1)) + "%'";
        //} else {
          //  where += " OR types IS NOT 'none'";
        //}

        return new CursorLoader(this, Uri.parse(
                "content://org.baobab.baolizer"), null, where, null,  null);
    }
    
    @Override
    public void onLoadFinished(Loader<Cursor> l, final Cursor cursor) {
        Log.d(TAG, "load finished: " + cursor.getCount());
        if (cursor.getCount() == 0) return;
        map.clear();
        cursor.moveToFirst();
        podioId = new HashMap<String, String>();

        ArrayList<String> filter_array = new ArrayList<String>(); // TSC

        while (!cursor.isLast()) {
            cursor.moveToNext();
            GeoHash geohash = GeoHash
                    .fromGeohashString(cursor.getString(7));
            Marker marker = map.addMarker(
                        new MarkerOptions()
                    .title(cursor.getString(3))
                    .snippet(cursor.getString(1))
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.drawable.tree))
                    .position(new LatLng(
                           geohash.getPoint().getLatitude(),
                           geohash.getPoint().getLongitude())));

            filter_array.addAll(Arrays.asList(cursor.getString(1).split(","))); // TSC
            podioId.put(marker.getId(), cursor.getString(8));
        }

        // < TSC: Generate FitlerView from loaded data
        //if(filterView.getAdapter() == null) {
            filter_array = new ArrayList<String>(new HashSet<String>(filter_array));

            Collections.sort(filter_array);
            filter_array.set(filter_array.indexOf(""), " Alle");
          //  filterView.setAdapter(new ArrayAdapter<String>(this,
          //          R.layout.filter_list_item, filter_array));


            //filterView.setOnItemClickListener(this);


        //}
                // > TSC

        //cursor.registerContentObserver(onChange);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        Log.d(TAG, "filter position: " + position);

        //filterView.setItemChecked(position, true);
        SlidingDrawer drawer = (SlidingDrawer) findViewById(R.id.drawer);
        drawer.animateClose();


        Bundle bundle = new Bundle();
        bundle.putInt("position", position);

        getSupportLoaderManager().restartLoader(0, bundle, this);
    }



    @Override
    public void onInfoWindowClick(Marker marker) {
        Uri url = Uri.parse(RefreshService.BASE_URL + WEBVIEW +
                podioId.get(marker.getId()) + PAGE_HTML);
        System.out.println("clicked " + url);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame, new WebFragment())
                .addToBackStack("webview").commit();
        //startActivity(new Intent(this, WebActivity.class).setData(url));
    }


    @Override
    public void onLoaderReset(Loader<Cursor> l) {
        System.out.println("loader reset");
        map.clear();
    }

    @Override
    public void onClick(View v) {
        startActivity(new Intent(this, WebActivity.class).setData(
                Uri.parse("http://baolizer.baobab.org/submit")));
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            SlidingDrawer drawer = (SlidingDrawer) findViewById(R.id.drawer);
            if (drawer.isOpened()) {
                drawer.close();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }
}
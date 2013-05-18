/*
 * Copyright (C) 2013 Dan Morrill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dude.morrildl.weatherport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import dude.morrildl.weatherport.WeatherLoader.WeatherInfo;

public class MainActivity extends FragmentActivity implements ViewPager.OnPageChangeListener {

    private ArrayAdapter<CharSequence> adapter;
    private LatLng latLng = null;
    private SupportMapFragment mapFragment;
    private TwoFragmentPagerAdapter pagerAdapter;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // We store the list of airports the user has expressed interest in in
        // SharedPrefs, as a list (well, Set) of Strings
        SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);

        Set<String> currentSet = prefs.getStringSet("airports", null);
        if (currentSet == null || currentSet.size() < 1) {
            // i.e. first run -- hard-default KSFO to the list
            HashSet<String> defaultSet = new HashSet<String>();
            defaultSet.add("KSFO");
            prefs.edit().putStringSet("airports", defaultSet).commit();
            currentSet = defaultSet;
        }

        // enable the nav spinner, which we'll use to pick which airport to look
        // at
        ActionBar bar = getActionBar();
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayList<String> currentList = new ArrayList<String>();
        currentList.addAll(currentSet);
        Collections.sort(currentList);
        adapter = new ArrayAdapter<CharSequence>(bar.getThemedContext(),
                android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(currentList);

        bar.setListNavigationCallbacks(adapter, new OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int arg0, long arg1) {
                // this re-ups the data whenever the user changes the current
                // airport
                startAsyncFetch(adapter.getItem(arg0).toString());
                return true;
            }
        });

        // Let's set up a fancy new v2 MapView, for the lulz
        mapFragment = new SupportMapFragment();
        pagerAdapter = new TwoFragmentPagerAdapter(this, getSupportFragmentManager(),
                new DetailsFragment(), mapFragment);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOnPageChangeListener(this);
        // No placemarker on the map because I've always secretly hated that
        // glyph
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // this gets a little hairy, but each class is too trivial to bother
        // moving to its own file; but together they add up to be a little
        // irritating. So it goes.

        // first we need to inflate the View for the Action Bar operations, and
        // then set click handlers on them. This one is is the add-new-airport
        // dialog,
        // which cascades a bit
        getMenuInflater().inflate(R.menu.main, menu);
        menu.findItem(R.id.action_add).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        // yo dawg, I hurd you like callbacks
                        LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                        View v = inflater.inflate(R.layout.dialog, null);
                        final EditText et = (EditText) v.findViewById(R.id.dialog_input);

                        // this is the
                        // "enter a new airport code to add to the list" dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        final AlertDialog dialog = builder
                                .setTitle(R.string.dialog_title)
                                .setIcon(R.drawable.ic_launcher)
                                .setView(v)
                                .setPositiveButton(R.string.dialog_pos,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                String newAirport = et.getText().toString()
                                                        .toUpperCase();
                                                SharedPreferences prefs = MainActivity.this
                                                        .getSharedPreferences("prefs",
                                                                Context.MODE_PRIVATE);
                                                Set<String> currentList = prefs.getStringSet(
                                                        "airports", new HashSet<String>());
                                                HashSet<String> newSet = new HashSet<String>();
                                                newSet.addAll(currentList);
                                                newSet.add(newAirport);
                                                prefs.edit().putStringSet("airports", newSet)
                                                        .commit();
                                                ArrayList<String> newList = new ArrayList<String>();
                                                newList.addAll(newSet);
                                                Collections.sort(newList);
                                                adapter.clear();
                                                adapter.addAll(newList);
                                                // I N C E P T I O N
                                            }
                                        })
                                .setNegativeButton(R.string.dialog_neg,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        }).create();
                        et.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                            @Override
                            public void onFocusChange(View v, boolean focused) {
                                if (focused) {
                                    dialog.getWindow()
                                            .setSoftInputMode(
                                                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                                }
                            }
                        });
                        dialog.show();
                        return true;
                    }
                });

        // ...and this one launches our FOSS compliance screen.
        menu.findItem(R.id.action_settings).setOnMenuItemClickListener(
                new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        Intent intent = new Intent();
                        intent.setClass(MainActivity.this, AboutActivity.class);
                        MainActivity.this.startActivity(intent);
                        return true;
                    }
                });
        return true;
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    // this causes the MapView to pan to the currently-selected airport (if
    // possible) whenever you switch to the Map tab
    @Override
    public void onPageSelected(int position) {
        if (position == 1 && latLng != null) {
            GoogleMap map = mapFragment.getMap();
            if (map != null) {
                CameraPosition pos = new CameraPosition.Builder().target(latLng).zoom(12).build();
                map.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        ArrayList<String> airports = new ArrayList<String>();
        airports.addAll(prefs.getStringSet("airports", null));
        Collections.sort(airports);

        // So, we hit the network every time the screen reloads. This is all
        // kinds of evil, or rather would be in a real app; but this app is so
        // small and the JSON serves so far it's not a big deal. And at least
        // we're not doing it on the UI thread.
        startAsyncFetch(airports.get(0));
    }

    private void startAsyncFetch(String airport) {
        new AsyncTask<String, Integer, WeatherInfo>() {

            @Override
            protected WeatherInfo doInBackground(String... params) {
                // first fetch the data, and when we have it...
                WeatherLoader.WeatherInfo info = WeatherLoader.fetchDataFor(params[0]);
                return info;
            }

            @Override
            protected void onPostExecute(WeatherInfo info) {
                // ...we update the UI with it
                ((TextView) findViewById(R.id.location_code_text)).setText(info.ICAO);
                ((TextView) findViewById(R.id.location_name_text)).setText(info.name);
                ((TextView) findViewById(R.id.elevation_value)).setText(info.elevation);
                ((TextView) findViewById(R.id.pressure_value)).setText(info.pressure);
                ((TextView) findViewById(R.id.windspeed_value)).setText(info.windSpeed);
                ((TextView) findViewById(R.id.winddir_value)).setText(info.windDirection);
                ((TextView) findViewById(R.id.temp_value)).setText(info.temperature);
                ((TextView) findViewById(R.id.dew_value)).setText(info.dewPoint);
                ((TextView) findViewById(R.id.humidity_value)).setText(info.humidity);
                ((ImageView) findViewById(R.id.imageView1)).setImageResource(info.imageId);
                latLng = info.latLng;

                // this causes us to pan the MapView if you select a new airport
                // from the Action Bar spinner while looking at the map. The
                // other similar code path only fires when you switch to the map view.
                if (viewPager.getCurrentItem() == 1 && latLng != null) {
                    GoogleMap map = mapFragment.getMap();
                    if (map != null) {
                        CameraPosition pos = new CameraPosition.Builder().target(latLng).zoom(12)
                                .build();
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(pos));
                    }
                }
            }
        }.execute(airport);
    }
}

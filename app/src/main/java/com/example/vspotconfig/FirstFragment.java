package com.example.vspotconfig;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.stealthcopter.networktools.SubnetDevices;
import com.stealthcopter.networktools.subnet.Device;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FirstFragment extends Fragment {

    private static final String TAG = "FirstFragment";

    static View rootView;
    Boolean isScanning = false;
    SubnetDevices subnetDevices;

    View rootview;
    CardView card;
    @BindView(R.id.textview_first)
    MaterialTextView textviewFirst;
    @BindView(R.id.relative_layout)
    LinearLayout relativeLayout;
    @BindView(R.id.button_first)
    MaterialButton buttonFirst;
    @BindView(R.id.scrollView2)
    ScrollView scrollView2;
    @BindView(R.id.textview_devices_found)
    MaterialTextView textviewDevicesFound;
    @BindView(R.id.progressBar2)
    ProgressBar progressBar2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_first, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootview = view;

        // SET SCANNING SPEED
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        String scanningspeed = sharedPreferences.getString("scanningspeed", "3");

        int timeout, threads;
        switch (scanningspeed) {
            case "Slow":
                timeout = 5;
                threads = 10;
                break;
            case "Medium":
                timeout = 3;
                threads = 50;
                break;
            default:
                timeout = 2;
                threads = 250;
                break;
        }

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!isScanning) {
                    isScanning = true;
                    relativeLayout.removeAllViewsInLayout();
                    buttonFirst.setText("ABORT");
                    textviewDevicesFound.setText("Searching...");
                    progressBar2.setIndeterminate(true);

                    subnetDevices = SubnetDevices.fromLocalAddress().setNoThreads(threads).setTimeOutMillis(timeout).findDevices(new SubnetDevices.OnSubnetDeviceFound() {
                        @Override
                        public void onDeviceFound(Device device) {
                            addDevice(device);
                        }

                        @Override
                        public void onFinished(ArrayList<Device> devicesFound) {
                            buttonFirst.setText("SCAN");
                            isScanning = false;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    textviewDevicesFound.setText("We have found " + devicesFound.size() + " device(s). Click on one to connect");
                                    progressBar2.setIndeterminate(false);
                                    Log.d(TAG, devicesFound.toString());
                                }
                            });
                        }
                    });
                } else {
                    subnetDevices.cancel();
                    isScanning = false;
                    progressBar2.setIndeterminate(false);
                    buttonFirst.setText("SCAN");
                }
            }
        });
    }


    private void addDevice(Device device) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                card = new CardView(MainActivity.context);
                LinearLayout linearLayout = new LinearLayout(MainActivity.context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setDividerPadding(5);

                // Set the CardView layoutParams
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                );
                linearLayout.setLayoutParams(params);
                card.setLayoutParams(params);
                card.setUseCompatPadding(true);
                if (isVspotDevice(device.mac, device.hostname)) {
                    card.setCardBackgroundColor(Color.GREEN);
                    card.setSelected(true);
                }
                // Set CardView corner radius
                card.setRadius(9);
                // Set cardView content padding
                card.setContentPadding(15, 15, 15, 15);
                // Set the CardView maximum elevation
                card.setMaxCardElevation(15);
                // Set CardView elevation
                card.setCardElevation(9);
                // Initialize a new TextView to put in CardView
                card.animate();
                card.isClickable();

                // Hostname
                TextView hostname = new TextView(getContext());
                hostname.setLayoutParams(params);
                if (device.mac == null) {
                    hostname.setText("Hostname:\t\t\t\t\t\t\t" + "-");
                } else {
                    hostname.setText("Hostname:\t\t\t\t\t\t\t" + device.hostname);
                }
                hostname.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                linearLayout.addView(hostname);

                // IP
                TextView IP = new TextView(getContext());
                IP.setLayoutParams(params);
                if (device.ip == null) {
                    IP.setText("IP-Address:\t\t\t\t\t\t" + "-");
                } else {
                    IP.setText("IP-Address:\t\t\t\t\t\t" + device.ip);
                }
                IP.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                linearLayout.addView(IP);
                card.addView(linearLayout);

                // MAC
                TextView mac = new TextView(getContext());
                mac.setLayoutParams(params);
                if (device.mac == null) {
                    mac.setText("MAC-Address:\t\t\t" + "-");
                } else {
                    mac.setText("MAC-Address:\t\t\t" + device.mac);
                }
                mac.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                linearLayout.addView(mac);

                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        subnetDevices.cancel();
                        FirstFragmentDirections.ActionFirstFragmentToSecondFragment action = FirstFragmentDirections.actionFirstFragmentToSecondFragment()
                                .setHostNAme(device.hostname)
                                .setIP(device.ip)
                                .setMAC(device.mac);
                        NavHostFragment.findNavController(FirstFragment.this).navigate(action);
                    }
                });

                if (card.getParent() != null) {
                    ((ViewGroup) card.getParent()).removeView(card);
                }
                relativeLayout.addView(card);
            }
        });
    }


    private boolean isVspotDevice(String macaddress, String hostname) {
        try {
            assert macaddress != null;
            assert hostname != null;
            // Check MAC Range
            String raspberry_mac_range = "(B8:27:EB)|(B8-27-EB)|(B827.EB)|(DC:A6:32)|(DC-A6-32)|(DCA6.32)|(e8:4e:06:32:2b:d8)";
            Pattern mac_pattern = Pattern.compile(raspberry_mac_range, Pattern.CASE_INSENSITIVE);
            Matcher mac_matcher = mac_pattern.matcher(macaddress);

            // Check Hostname Range
            String hostname_range = "(vspot)|(raspberry)|(pi)|(raspberrypi)i";
            Pattern hostname_pattern = Pattern.compile(hostname_range, Pattern.CASE_INSENSITIVE);
            Matcher hostname_matcher = hostname_pattern.matcher(hostname);

            return mac_matcher.find() || hostname_matcher.find();
        } catch (Exception e) {
            return false;
        }
    }
}

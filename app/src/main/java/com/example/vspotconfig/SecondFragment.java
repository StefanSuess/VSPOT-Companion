package com.example.vspotconfig;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.stealthcopter.networktools.Ping;
import com.stealthcopter.networktools.PortScan;
import com.stealthcopter.networktools.ping.PingResult;
import com.stealthcopter.networktools.ping.PingStats;

import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.app.Activity.RESULT_OK;

public class SecondFragment extends Fragment {

    private static final String TAG = "SecondFragment";
    String ip, newhostname, user, password, ssid, psk, apikey;
    PortScan portscan;


    @BindView(R.id.ssh)
    Button ssh;
    @BindView(R.id.configure)
    TextView connect;
    @BindView(R.id.overview)
    TextView overview;
    @BindView(R.id.tvipaddress)
    TextView tvipaddress;
    @BindView(R.id.tvhostname)
    TextView tvhostname;
    @BindView(R.id.hostname)
    TextView hostname;
    @BindView(R.id.ipaddress)
    TextView ipaddress;
    @BindView(R.id.macaddress)
    TextView macaddress;
    @BindView(R.id.openports)
    TextView openports;
    @BindView(R.id.tvmacaddress)
    TextView tvmacaddress;
    @BindView(R.id.tvopenports)
    TextView tvopenports;
    @BindView(R.id.online)
    TextView online;
    @BindView(R.id.divider4)
    View divider4;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    //Views for sshalertDialog
    Button cancelButton, connectButton;
    EditText passwordAlert, usernameAlert, ssidalert, pskalert, hostnamealert;

    @BindView(R.id.connect2)
    TextView connect2;
    @BindView(R.id.factoryReset)
    Button factoryReset;
    @BindView(R.id.debugtextview)
    TextView debugtextview;
    @BindView(R.id.rebootButton)
    Button rebootButton;
    @BindView(R.id.sethostnameButton)
    Button sethostnameButton;
    @BindView(R.id.changeapikeyButton)
    Button changeapikeyButton;
    @BindView(R.id.updateButton)
    Button updateButton;
    @BindView(R.id.addwifiButton)
    Button addwifiButton;
    @BindView(R.id.showlogsButton)
    Button showlogsButton;
    @BindView(R.id.statustextviiew)
    TextView statustextviiew;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_second, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get passed arguments
        String host_name = SecondFragmentArgs.fromBundle(getArguments()).getHostNAme();
        if (host_name == null)
            host_name = "-";

        ip = SecondFragmentArgs.fromBundle(getArguments()).getIP();
        if (ip == null)
            ip = "_";

        String mac = SecondFragmentArgs.fromBundle(getArguments()).getMAC();
        if (mac == null)
            mac = "_";

        hostname.setText(host_name);
        ipaddress.setText(ip);
        macaddress.setText(mac);
        openports.setText("");

        // SET HEARTBEAT, PORTS, and portscanning
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);

        boolean heartbeat = sharedPreferences.getBoolean("heartbeat", true);
        if (heartbeat) {
            isOnline(ip);
        } else {
            online.setText("N/A");
        }

        boolean portscanning = sharedPreferences.getBoolean("portscanning", false);
        if (portscanning) {
            getPorts(ip);
        } else {
            progressBar.setVisibility(View.GONE);
            openports.setText("N/A");
        }
    }


    private void isOnline(String ip) {
        Ping.onAddress(ip).setTimeOutMillis(1000).setTimes(1).doPing(new Ping.PingListener() {
            @Override
            public void onResult(PingResult pingResult) {
                if (pingResult.isReachable()) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            online.setTextColor(Color.GREEN);
                            online.setText("Online");
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            online.setText("Offline");
                            online.setTextColor(Color.RED);
                        }
                    });
                }
            }

            @Override
            public void onFinished(PingStats pingStats) {
                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            Thread.sleep(4000);
                            isOnline(ip);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);

            }

            @Override
            public void onError(Exception e) {
                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            Thread.sleep(4000);
                            isOnline(ip);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);

            }
        });
    }


    private void getPorts(String ip) {
        try {
            portscan = PortScan.onAddress(ip).setPortsAll().setMethodTCP().doScan(new PortScan.PortListener() {
                @Override
                public void onResult(int portNo, boolean open) {
                    //if (open) // Stub: found open port
                }

                @Override
                public void onFinished(ArrayList<Integer> openPorts) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            openports.setText(openPorts.toString());
                        }
                    });
                }
            });
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    private void showSSHConnectDialog(View rootView) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Connect to SSH Session")
                .setView(R.layout.shhdialog)
                .show();

        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                String password = passwordAlert.getText().toString();

                if (portscan != null) {
                    portscan.cancel();
                }
                SecondFragmentDirections.ActionSecondFragmentToSSHFragment action = SecondFragmentDirections.actionSecondFragmentToSSHFragment(ip, user, password, false);
                Navigation.findNavController(rootView).navigate(action);
                alertDialog.dismiss();
            }
        });
    }


    private void doFactoryReset(View rootView) {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Do Factory Reset")
                .setView(R.layout.shhdialog)
                .show();
        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                alertDialog.dismiss();
                String password = passwordAlert.getText().toString();
                if (portscan != null) {
                    portscan.cancel();
                }
                SecondFragmentDirections.ActionSecondFragmentToFactoryResetFragment action = SecondFragmentDirections.actionSecondFragmentToFactoryResetFragment(ip, user, password);
                NavHostFragment.findNavController(SecondFragment.this).navigate(action);

                /*executeSSHcommand("sudo apt-get update && sudo apt-get upgrade -y", user, password);
                executeSSHcommand("sudo apt-get install --no-install-recommends xserver-xorg x11-xserver-utils xinit openbox -y", user, password);
                executeSSHcommand("sudo apt-get install --no-install-recommends chromium-browser -y", user, password);
                executeSSHcommand("echo\"# Disable any form of screen saver / screen blanking / power management\n xset s off\nxset s noblank\nxset -dpms\n\n# Allow quitting the X server with CTRL-ATL-Backspace\nsetxkbmap -option terminate:ctrl_alt_bksp\n\n# Start Chromium in kiosk mode\nsed -i \'s/\"exited_cleanly\":false/\"exited_cleanly\":true/\' ~/.config/chromium/\'Local State\'\nsed -i \'s/\"exited_cleanly\":false/\"exited_cleanly\":true/; s/\"exit_type\":\"[^\"]\\+\"/\"exit_type\":\"Normal\"/\' ~/.config/chromium/Default/Preferences\nchromium-browser --disable-infobars --kiosk \'http://your-url-here\'\">/etc/xdg/openbox/autostart", user, password);
                executeSSHcommand("echo\"[[ -z $DISPLAY && $XDG_VTNR -eq 1 ]] && startx -- -nocursor\">~.bash_profile", user, password);
                executeSSHcommand("sudo reboot now", user, password);*/
            }
        });
    }


    public String executeSSHcommand(String command, String user, String password) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        int port = Integer.parseInt(sharedPreferences.getString("sshport", "22"));
        String host = ip;
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(10000);
            session.connect();
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            ByteArrayOutputStream baose = new ByteArrayOutputStream();
            channel.setErrStream(baose);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            channel.setOutputStream(baos);
            channel.setCommand(command);
            channel.connect();
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            }
            channel.disconnect();
            if (baos.size() == 0) {
                return baose.toString();
            } else {
                return baos.toString();
            }
        } catch (JSchException e) {
            e.printStackTrace();
            Snackbar.make(getActivity().findViewById(android.R.id.content),
                    "Last command failed! Error: " + e.getMessage(),
                    Snackbar.LENGTH_LONG)
                    .setDuration(7000).show();
        }
        return "Something went wrong";
    }


    @OnClick({R.id.factoryReset, R.id.ssh, R.id.rebootButton, R.id.sethostnameButton, R.id.changeapikeyButton, R.id.updateButton, R.id.addwifiButton, R.id.showlogsButton})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.rebootButton:
                doReboot();
                break;
            case R.id.sethostnameButton:
                setHostname();
                break;
            case R.id.changeapikeyButton:
                changeApiKex();
                break;
            case R.id.updateButton:
                doUpdate();
                break;
            case R.id.addwifiButton:
                addWifi();
                break;
            case R.id.showlogsButton:
                showLogs();
                break;
            case R.id.ssh:
                showSSHConnectDialog(view);
                break;
            case R.id.factoryReset:
                doFactoryReset(view);
                break;
        }
    }

    private void showLogs() {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Show Most Recent Devicelogs")
                .setView(R.layout.shhdialog)
                .show();
        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                String password = passwordAlert.getText().toString();
                alertDialog.dismiss();
                if (portscan != null) {
                    portscan.cancel();
                }
                SecondFragmentDirections.ActionSecondFragmentToSSHFragment action = SecondFragmentDirections.actionSecondFragmentToSSHFragment(ip, user, password, true);
                NavHostFragment.findNavController(SecondFragment.this).navigate(action);
            }
        });
    }


    private void doReboot() {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Reboot Device")
                .setView(R.layout.shhdialog)
                .show();
        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                String password = passwordAlert.getText().toString();
                alertDialog.dismiss();
                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            executeSSHcommand("sudo reboot now", user, password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);
            }
        });
    }


    private void doUpdate() {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Update Device")
                .setView(R.layout.shhdialog)
                .show();
        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                String password = passwordAlert.getText().toString();
                alertDialog.dismiss();
                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            executeSSHcommand("sudo apt-get update && apt-get upgrade -y", user, password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);
            }
        });
    }


    private void addWifi() {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Add Wifi")
                .setView(R.layout.addwifidialog)
                .show();
        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);
        ssidalert = view.findViewById(R.id.username2);
        pskalert = view.findViewById(R.id.password2);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                String password = passwordAlert.getText().toString();
                String ssid = ssidalert.getText().toString();
                String psk = pskalert.getText().toString();
                alertDialog.dismiss();
                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            String wifistring = "network={\n\tssid=\"" + ssid + "\"\n\tpsk=\"" + psk + "\"\n}";
                            executeSSHcommand("sudo echo '" + wifistring + "'" + "| sudo tee -a /etc/wpa_supplicant/wpa_supplicant.conf", user, password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);
            }
        });
    }


    private void changeApiKex() {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE"); // "PRODUCT_MODE for bar codes
            startActivityForResult(intent, 0);
        } catch (Exception e) {
            Uri marketUri = Uri.parse("market://details?id=com.google.zxing.client.android");
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
            startActivity(marketIntent);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                        .setTitle("Set Api Key")
                        .setView(R.layout.shhdialog)
                        .show();
                View view = alertDialog.getWindow().getDecorView();
                cancelButton = view.findViewById(R.id.cancelbutton);
                connectButton = view.findViewById(R.id.connectButton);
                passwordAlert = view.findViewById(R.id.password);
                usernameAlert = view.findViewById(R.id.username);
                cancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        alertDialog.cancel();
                    }
                });
                connectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String user = usernameAlert.getText().toString();
                        if (user.isEmpty()) {
                            usernameAlert.setError("Username cannot be empty");
                            return;
                        }
                        apikey = data.getStringExtra("SCAN_RESULT");
                        String password = passwordAlert.getText().toString();
                        alertDialog.dismiss();
                        new AsyncTask<Integer, Void, Void>() {
                            @Override
                            protected Void doInBackground(Integer... params) {
                                try {
                                    executeSSHcommand("sudo sed -i '$ d' /etc/xdg/openbox/autostart && echo 'chromium-browser --incognito --disable-features=TranslateUI --disable-features=InfiniteSessionRestore --noerrdialogs --disable-infobars --kiosk --app=" + apikey + "' | sudo tee -a /etc/xdg/openbox/autostart && sudo reboot now", user, password);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return null;
                            }
                        }.execute(1);
                    }
                });
            }
        }
    }

    private void setHostname() {
        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle("Add Wifi")
                .setView(R.layout.sethostnamedialog)
                .show();
        View view = alertDialog.getWindow().getDecorView();
        cancelButton = view.findViewById(R.id.cancelbutton);
        connectButton = view.findViewById(R.id.connectButton);
        passwordAlert = view.findViewById(R.id.password);
        usernameAlert = view.findViewById(R.id.username);
        hostnamealert = view.findViewById(R.id.username2);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.cancel();
            }
        });
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = usernameAlert.getText().toString();
                if (user.isEmpty()) {
                    usernameAlert.setError("Username cannot be empty");
                    return;
                }
                String password = passwordAlert.getText().toString();
                String hostname = hostnamealert.getText().toString();
                if (hostname.isEmpty()) {
                    hostnamealert.setError("Hostname cannot be emtpy");
                    return;
                }
                alertDialog.dismiss();
                new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            executeSSHcommand("sudo hostnamectl set-hostname " + hostname + " && sudo reboot now", user, password);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);
            }
        });
    }


    private void connectVNC() {
        //TODO Implement VNC
    }

}

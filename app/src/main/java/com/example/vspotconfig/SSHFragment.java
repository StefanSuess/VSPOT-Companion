package com.example.vspotconfig;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SSHFragment extends Fragment {
    private static final String TAG = "SSHFragment";
    boolean firstCommand;
    @BindView(R.id.console)
    EditText console;
    @BindView(R.id.send)
    Button send;
    @BindView(R.id.textfielcommand)
    EditText textfielcommand;

    int sshport;
    String user, password, ip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_ssh, container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // send on enter press
        textfielcommand.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    send.performClick();
                    handled = true;
                }
                return handled;
            }
        });


        //get passed arguments
        ip = SSHFragmentArgs.fromBundle(getArguments()).getIP();
        user = SSHFragmentArgs.fromBundle(getArguments()).getUser();
        password = SSHFragmentArgs.fromBundle(getArguments()).getPassword();
        Boolean logs = SSHFragmentArgs.fromBundle(getArguments()).getShowlogs();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);

        sshport = Integer.parseInt(sharedPreferences.getString("sshport", "22"));

        if (logs) {
            new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... params) {
                    try {
                        executeSSHcommand("journalctl -xe");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(1);
        } else {


            console.append("Trying to connect to " + user + "@" + ip);

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (executeSSHcommand("echo Connected succesfully!")) {
                            send.setEnabled(true);
                            console.append(user + "@" + ip + ":~$ ");
                            textfielcommand.requestFocus();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    public Boolean executeSSHcommand(String command) {
        String host = ip;
        int port = sshport;
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
                    Thread.sleep(10);
                } catch (Exception ee) {
                }
            }
            if (baos.size() == 0) {
                console.append("\n" + baose.toString());
            } else {
                console.append("\n" + baos.toString());
            }

            channel.disconnect();
            console.append(user + "@" + host + ":~$ ");
            return true;
        } catch (JSchException e) {
            // show the error in the UI
            console.append("\nCONNECTION FAILED\nCheck Wifi or Server!\nError: " + e.getMessage());
            return false;
        }
    }


    @OnClick(R.id.send)
    public void onViewClicked() {
        String command = textfielcommand.getText().toString();

        if (firstCommand) {
            String defaultString = user + "@" + ip + ":~$ ";
            console.append(command);

            new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... params) {
                    try {
                        executeSSHcommand(command);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(1);
        } else {
            firstCommand = true;
            console.append(command);
            new AsyncTask<Integer, Void, Void>() {
                @Override
                protected Void doInBackground(Integer... params) {
                    try {
                        executeSSHcommand(command);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute(1);
        }
    }
}

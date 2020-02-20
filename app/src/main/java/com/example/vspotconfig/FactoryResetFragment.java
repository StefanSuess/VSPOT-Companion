package com.example.vspotconfig;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FactoryResetFragment extends Fragment {

    private static final String TAG = "FactoryResetFragment";
    static Context context;
    View rootview;
    @BindView(R.id.progressBar3)
    ProgressBar progressBar3;
    @BindView(R.id.editText)
    EditText editText;
    int port;
    int totalprogress = 0;
    private int i;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_factoryreset, container, false);
        ButterKnife.bind(this, rootView);
        context = getContext();
        return rootView;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rootview = view;
        editText.append("Starting Factory Reset\n\nDO NOT CLOSE THIS APP!");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.context);
        port = Integer.parseInt(sharedPreferences.getString("sshport", "22"));
        doFactoryReset();
    }


    @SuppressLint("StaticFieldLeak")
    private void doFactoryReset() {

        String ip = FactoryResetFragmentArgs.fromBundle(getArguments()).getIP();
        String password = FactoryResetFragmentArgs.fromBundle(getArguments()).getPassword();
        String user = FactoryResetFragmentArgs.fromBundle(getArguments()).getUser();

        final int progress = 100 / (getFileSize() / 2);
        totalprogress = 2 * progress;
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    int filesize = getFileSize();
                    Log.e(TAG, "filesize: " + filesize);
                    for (i = 0; i < filesize; i += 2) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                editText.append("\n\n" + readCommentsFromFile(i));
                                //Log.e(TAG, "I ="+ i );
                                //Log.e(TAG, "READ COMMENTS: " + readCommentsFromFile(i));
                                //Log.e(TAG, "READ COMMANDS " + readCommandsFromFile(i));
                                totalprogress += progress;
                                progressBar3.setProgress(totalprogress);
                            }
                        });
                        if (!executeSSfast(readCommandsFromFile(i), user, password, ip)) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    editText.append("\n\nFACTORY RESET ABORTED");
                                }
                            });
                            break;
                        } else {
                            //do nothing
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }


    public Boolean executeSSfast(String command, String user, String password, String ip) {
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
            channel.setCommand(command);
            //editText.append("\nSending the following command:" + command);
            channel.connect();
            while (!channel.isClosed()) {
                try {
                    Thread.sleep(1000);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            editText.append(".");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (baose.size() > 0) {
                editText.append("\n" + baose.toString());
                return false;
            }
            channel.disconnect();
            return true;
        } catch (JSchException e) {
            editText.append("\nCONNECTION FAILED\nCheck Wifi or Server!\nError: " + e.getMessage());
            return false;
        }
    }

    private int getFileSize() {
        String lineIWant = "";
        InputStream inputStream = getResources().openRawResource(R.raw.factoryreset_commands);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        int size = 0;
        try {
            while (br.readLine() != null) {
                size += 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        {
            return size;
        }
    }


    private String readCommandsFromFile(int n) {
        String lineIWant = "";
        InputStream inputStream = getResources().openRawResource(R.raw.factoryreset_commands);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        for (int i = 0; i <= n + 1; ++i)
            try {
                lineIWant = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return lineIWant;
    }

    private String readCommentsFromFile(int n) {
        String lineIWant = "";
        InputStream inputStream = getResources().openRawResource(R.raw.factoryreset_commands);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        for (int i = 0; i <= n; ++i)
            try {
                lineIWant = br.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        return lineIWant;
    }
}

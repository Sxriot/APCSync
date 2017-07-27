package com.example.gabrielwohlford.apcsync;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ContentFrameLayout;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;

public class MainActivity extends AppCompatActivity {
    ClientThread clientThread;
    uploadFile up= new uploadFile();
    int intFilesProcessedCount=0;
    public static int choice;
    List<String> subPath = new ArrayList<>();

    static List<String> staticLstStrFilePaths = new ArrayList<>();

    String GetSharedReferenceString(String ReferenceKey){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String val = settings.getString(ReferenceKey, "");
        return val;
    }

    private void toast(String str){
        Toast.makeText(this, str, Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView tvFeedback= (TextView)findViewById(R.id.FeedBack);
        final TextView tvCurrentFile= (TextView)findViewById(R.id.CurrentFileTextView);
        tvFeedback.setMovementMethod(new ScrollingMovementMethod());
        Button btnRfresh = (Button)findViewById(R.id.refreshConnection);
        btnRfresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    //This retrieves the saved references. "example_text" is has to be there and cannot be renamed in xml whenever i change it in xml the app crashes
                    //  If you decide you want to make another reference object be wise as to what you name the key value.. otherwise your stuck with it
                    //  If you can figure out how to change preferences key value without breaking app let me know. Also removing preference
                    //  objects can break things.. so if you find how to remove them saftey let me know.
                    String strServerIP = GetSharedReferenceString("example_text");
                    String intPort = GetSharedReferenceString("Default_port");
                    clientThread = new ClientThread(strServerIP, Integer.parseInt(intPort));
                    new Thread(clientThread).start();
                    Thread.sleep(1000);
                    if (clientThread.isConnected()) {
                        tvFeedback.setText("Connected To: "+strServerIP+":"+intPort);
                    } else {
                        tvFeedback.setText("Failed to Connect to:"+strServerIP+":"+intPort);
                    }
                }catch (Exception e){
                    Log.v("Error",e.getMessage());
                }
            }
        });

/*
        Button b = (Button)findViewById(R.id.Settings);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(intent);
            }
        });
*/

        Button b2 = (Button)findViewById(R.id.Sync);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!clientThread.isConnected()){
                    toast("Not Connected to Server");
                }else {
                    intFilesProcessedCount=0;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            staticLstStrFilePaths.clear();
                            if (isPreferenceChecked("DCIM_Camera")) {
                                subPath = GetFileListArrayFromDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
                                Log.w("Done uploading this", subPath.size() + "");
                                for (int i = 0; i < subPath.size(); i++) {
                                    try {

                                        up.upload(subPath.get(i), clientThread.client);
                                        Log.w("Done uploading this", subPath.get(i));
                                    } catch (Exception e) {
                                        Log.v("Error Loading File:" + subPath.get(i), e.getMessage());
                                    }
                                }
                            }
                            if (isPreferenceChecked("pref_key_auto_delete")) { //another bad naming i cant change without breaking it..
                                subPath = GetFileListArrayFromDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
                                Log.w("Done uploading this", subPath.size() + "");
                                for (int i = 0; i < subPath.size(); i++) {

                                    up.upload(subPath.get(i), clientThread.client);
                                    Log.w("Done uploading this", subPath.get(i));
                                }
                            }
                            if (isPreferenceChecked("pref_key_Pictures")) {
                                subPath = GetFileListArrayFromDirectory(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath());
                                Log.w("Done uploading this", subPath.size() + "");
                                for (int i = 0; i < subPath.size(); i++) {
                                    up.upload(subPath.get(i), clientThread.client);
                                    Log.w("Done uploading this", subPath.get(i));
                                }
                            }
                        }
                    }).start();
                }

            }
        });

        //This is a custom event listener..it exist in your upload class
        //This syntax is ugly but its working.. ill explain how events work to ya ^.^
        up.setHandlerListener(new MyHandlerInterface() {
            @Override
            public void downloadComplete(final String Message) {
                intFilesProcessedCount++;
                tvFeedback.post(new Runnable() {
                    public void run() {
                        tvFeedback.append("\n"+intFilesProcessedCount+" of "+ subPath.size()+" "+Message.substring(Message.lastIndexOf("/")));
                    }
                });
            }

            @Override
            public void downloadProgress(final String Message) {
                tvCurrentFile.post(new Runnable() {
                    public void run() {
                        tvCurrentFile.append(Message);
                    }
                });
            }

            @Override
            public void downloadStarted(final String Message) {
                tvCurrentFile.post(new Runnable() {
                    public void run() {
                        tvCurrentFile.setText(Message);
                    }
                });
            }

        });

        //I needed to ad these for API 23 >= development------------Koki
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageInfo.REQUESTED_PERMISSION_GRANTED);

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PackageInfo.REQUESTED_PERMISSION_GRANTED);
        //-------------------------------------------------------------


        try {
            //Try to connect again on launch
            final String strServerIP = GetSharedReferenceString("example_text");
            final String intPort = GetSharedReferenceString("Default_port");
            clientThread = new ClientThread(strServerIP, Integer.parseInt(intPort));
            new Thread(clientThread).start();
            Thread.sleep(1000);
            if (clientThread.isConnected()) {
                tvFeedback.setText("Connected Server at: "+strServerIP+":"+intPort);
            } else {
                tvFeedback.setText("Failed to Connect to:"+strServerIP+":"+intPort);
            }
        }catch (Exception e){
            Log.v("Error",e.getMessage());
        }
    }

    Boolean isPreferenceChecked(String ReferenceKey){
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean val = settings.getBoolean(ReferenceKey, false);
        return val;
    }

    // Mof started here

    // Adding the menu to the action bar
    /** How to make a menu
     **  RESOURCE: https://developer.android.com/training/appbar/actions.html
     * 1- create a resource directory with "Resource Type" = "menu"
     * 2- add a resource file, name it anything.
     * 3- add <item ></item> to it
     * 4- go to the Java file and put the code below
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        mofSettingTransaction(id);
        return super.onOptionsItemSelected(item);
    }


    //MMMMMMMMMMMMMM// Methods ///////////////////////////
    private void mofSettingTransaction(int id) {
        switch (id) {
            case R.id.action_APCSync:
                choice = 1;
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_Notification:
                choice = 2;
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_DataAndSync:
                choice = 3;
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            default:
                choice = -1;
        }
    }
    // Mof ended here

    //Koki version of Getfiles()
    private List<String> GetFileListArrayFromDirectory(String DirectoryPath) {
        //List<String> lstStrFilePaths = new ArrayList<>();
        File directory;
        File[] files;
        try {
            directory = new File(DirectoryPath);
            files = directory.listFiles();
            for (File f : directory.listFiles()) {
                String fullPath = DirectoryPath + "/" + f.getName();
                if(f.getName().charAt(0)!='.'){
                    if (f.isFile()) {
                        Log.w("koki-Mofo-fullPath", fullPath);
                        staticLstStrFilePaths.add(fullPath);
                    }else if (f.isDirectory()) {
                        GetFileListArrayFromDirectory(f.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            Log.v("kokiException", e.toString());
        }
        return staticLstStrFilePaths;
    }

    private class ClientThread implements Runnable {
        public Socket client;
        String serverIP;
        int serverPort;
        DataOutputStream send;
        private boolean connected = false;
        ClientThread(String serverIp, int serverPort){
            this.serverIP = serverIp;
            this.serverPort = serverPort;
        }
        public Boolean isConnected(){
            return connected;
        }

        @Override
        public void run() {
            try {
                if(connected){
                    send = new DataOutputStream(client.getOutputStream());
                    send.writeBytes("3");
                }
                client = new Socket(serverIP, serverPort);
                connected = true;
            } catch (UnknownHostException e) {
                connected = false;
                Log.w("Mofo-UnknownHost", e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                connected = false;
                Log.w("Mofo-Connecting", e.toString());
                e.printStackTrace();
            } catch (SecurityException e) {
                Log.w("Mofo-SecurityException", e.toString());
                e.printStackTrace();
            }catch (IllegalArgumentException e) {
                Log.w("Mofo-IllegalArgsExcep", e.toString());
                e.printStackTrace();
            }
        }
    }
}
package systems.movingdata.goremote;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;

/**
 * Created by asprayx on 11/11/2014.
 */
public class DataManager implements
        DataApi.DataListener,
        MessageApi.MessageListener,
        NodeApi.NodeListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    public GoogleApiClient mGoogleApiClient;

    private MyActivity ParentActivity;
    public Bundle DataBul;

    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_REQUEST_CODE = 1;



    TextView TestMode, RunTime, RecordingTime;
    ImageButton powerMode, Record, SlectMode;

    ImageView ImageMode;

    public DataManager(MyActivity Parent, Bundle ata ) {
        ParentActivity = Parent;
        DataBul = ata;
        mGoogleApiClient = new GoogleApiClient.Builder(ParentActivity)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        buildGui();

    }





    @Override
    public void onConnectionSuspended(int i) {
        Log.d(ParentActivity.TAG, "onConnectionSuspended: " + i);
    }



    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(ParentActivity.TAG, "ConnectionResult: " + result);
    }



    @Override //DataListener
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(ParentActivity.TAG, "onDataChanged: " + dataEvents);
    }

    @Override //NodeListener
    public void onPeerConnected(final Node peer) {
        Log.d(ParentActivity.TAG, "onPeerConnected: " + peer);


    }
    @Override
    public void onPeerDisconnected(Node peer) {
        Log.d(ParentActivity.TAG, "onPeerDisconnected: " + peer);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(ParentActivity.TAG, "Google API Client was connected");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    public void updateScreen(){
        ParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UpdateGui();
            }
        });
    }

    public String getRemoteNodeId() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodesResult =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        List<Node> nodes = nodesResult.getNodes();
        if (nodes.size() > 0) {
            return nodes.get(0).getId();
        }
        return null;
    }

    public void showNotification() {

        PendingIntent pi = PendingIntent.getActivity(ParentActivity, NOTIFICATION_REQUEST_CODE,
                new Intent(ParentActivity, Notafacation.class).putExtras(DataBul),
                PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent min = PendingIntent.getActivity(ParentActivity, NOTIFICATION_REQUEST_CODE,
                new Intent(ParentActivity, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT );

        Notification notification = new Notification.Builder(ParentActivity)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(min)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(pi)
                        .setCustomSizePreset(Notification.WearableExtender.SIZE_FULL_SCREEN))
                .build();

        NotificationManagerCompat.from(ParentActivity).notify(NOTIFICATION_ID, notification);

    }


    @Override //MessageListener
    public void onMessageReceived(final MessageEvent messageEvent) {

        Log.d(ParentActivity.TAG, "onMessageReceived() A message from watch was received:" + messageEvent
                .getRequestId() + " " + messageEvent.getPath());

        if (messageEvent.getPath().equals("/status")) {
            prossessStatus(new String(messageEvent.getData(), Charset.forName("UTF-8")));
        }

        if (messageEvent.getPath().equals("/gpControl")) {
            prossessControl(new String(messageEvent.getData(), Charset.forName("UTF-8")));
        }



        updateScreen();
    }
    String GetSettingMode(int mode, int setting, JSONObject node){
        try {
            int option = node.getInt("" + setting);
            switch (mode) {
                case 0:
                    switch (setting) {
                        case 2:
                            switch (option) {
                                case 1:
                                    return "4K";
                                case 2:
                                    return "4K S";
                                case 4:
                                    return "2.7K";
                                case 5:
                                    return "2.7K S";
                                case 6:
                                    return "2.7K 4:3";
                                case 7:
                                    return "1440";
                                case 8:
                                    return "1080";
                                case 9:
                                    return "1080 S";
                                case 10:
                                    return "960";
                                case 11:
                                    return "720 S";
                                case 12:
                                    return "720";
                                case 13:
                                    return "WVGA";
                            }
                        case 3:
                            switch (option) {
                                case 0:
                                    return "240";
                                case 1:
                                    return "120";
                                case 2:
                                    return "100";
                                case 3:
                                    return "90";
                                case 4:
                                    return "80";
                                case 5:
                                    return "60";
                                case 6:
                                    return "50";
                                case 7:
                                    return "48";
                                case 8:
                                    return "30";
                                case 9:
                                    return "25";
                                case 10:
                                    return "24";
                                case 11:
                                    return "15";
                                case 12:
                                    return "12.5";
                            }
                    }
            }
        }catch (Exception e) {
                Log.v(ParentActivity.TAG, "Oops: GetSettingMode\n"+e );
        }
        return "";

    }
    String GetSettingModes(int mode, int setting, JSONObject node){
        try {
            int option = node.getInt(""+setting);
            Log.v(ParentActivity.TAG, "option"+option);

            JSONArray modes = gpControl.getJSONArray("modes");
            Log.v(ParentActivity.TAG,"modes");

            for (int i = 0; i < modes.length(); i++) {
                JSONObject ModeJson = modes.getJSONObject(i);
                Log.v(ParentActivity.TAG,"ModeJson");
                if (ModeJson.getInt("value") == mode) {
                    JSONArray settings = ModeJson.getJSONArray("settings");
                    Log.v(ParentActivity.TAG,"settings");
                    for (int o = 0; o < settings.length(); o++) {
                        JSONObject settingJson = settings.getJSONObject(o);
                        Log.v(ParentActivity.TAG,"settingJson");
                        if (settingJson.getInt("id") == setting) {
                            JSONArray options = settingJson.getJSONArray("options");
                            Log.v(ParentActivity.TAG,"options");
                            for (int p = 0; p < options.length(); p++) {
                                JSONObject optionJson = options.getJSONObject(p);
                                Log.v(ParentActivity.TAG,"optionJson");
                                if (optionJson.getInt("value") == option) {
                                    return  optionJson.getString("display_name");
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception e) {
            Log.v(ParentActivity.TAG, "Oops: GetSettingMode\n"+e );
        }
        return "";
    }
    JSONObject gpControl;

    void prossessControl(String controlString ){
        try {
            gpControl = new JSONObject(controlString);


        }catch (Exception e) {
            Log.v(ParentActivity.TAG, "Oops: prossessControl\n" );
        }
    }


    void prossessStatus(String statusString){
        try {

            JSONObject gpStatus = new JSONObject(statusString);

            JSONObject jsonChildNode = gpStatus.getJSONObject("status");

            DataBul.putInt("mode", jsonChildNode.getInt("43"));
            DataBul.putInt("sub_mode", jsonChildNode.getInt("44"));

            DataBul.putInt("internal_battery_present", jsonChildNode.getInt("1"));
            DataBul.putInt("internal_battery_level", jsonChildNode.getInt("2"));
            DataBul.putInt("system_hot", jsonChildNode.getInt("6"));
            DataBul.putInt("system_busy", jsonChildNode.getInt("8"));


            DataBul.putInt("video_progress_counter", jsonChildNode.getInt("13"));

            DataBul.putInt("multi_shot_count_down", jsonChildNode.getInt("49"));

            DataBul.putInt("remaining_photos", jsonChildNode.getInt("34"));
            DataBul.putInt("remaining_video_time", jsonChildNode.getInt("35"));

            DataBul.putInt("num_total_videos", jsonChildNode.getInt("39"));
            DataBul.putInt("num_total_photos", jsonChildNode.getInt("38"));



            jsonChildNode = gpStatus.getJSONObject("settings");
            DataBul.putString("resolution", GetSettingMode(0,2,jsonChildNode));
            DataBul.putString("fps", GetSettingMode(0,3,jsonChildNode));
            /*
            DataBul.putString("fov", GetSettingMode(0,4,jsonChildNode));
            DataBul.putString("timelapse_rate", GetSettingMode(0,5,jsonChildNode));
            DataBul.putString("looping", GetSettingMode(0,6,jsonChildNode));
            DataBul.putString("piv", GetSettingMode(0,7,jsonChildNode));
            DataBul.putString("low_light", GetSettingMode(0,8,jsonChildNode));
            DataBul.putString("spot_meter", GetSettingMode(0,9,jsonChildNode));
            DataBul.putString("protune", GetSettingMode(0,10,jsonChildNode));
            */


        }catch (Exception e) {
            Log.v(ParentActivity.TAG, "Oops: prossessStatus\n" );
        }
    }
    public static String repeat(char c,int i)
    {
        String tst = "";
        for(int j = 0; j < i; j++)
        {
            tst = tst+c;
        }
        return tst;
    }

    public class SendData extends Thread {
        int button;
        public SendData(int button) {
            this.button = button;
        }

        @Override
        public void run() {
            Log.v(ParentActivity.TAG, "doing work in Thread");
            sendMessage(getRemoteNodeId(),this.button);
        }
    }
    private void sendMessage(String node,int button) {


        Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, "/remote/"+button, new byte[0]).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(ParentActivity.TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        } else {
                            Log.i(ParentActivity.TAG, "Sent Good");
                        }
                    }
                }
        );
        Log.i(ParentActivity.TAG,"sent");

    }

    void buildGui(){
        final WatchViewStub stub = (WatchViewStub) ParentActivity.findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {


            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                TestMode = (TextView) stub.findViewById(R.id.TestMode);
                RunTime = (TextView) stub.findViewById(R.id.RunTime);
                RecordingTime = (TextView) stub.findViewById(R.id.RecordingTime);

                powerMode = (ImageButton) stub.findViewById(R.id.PowerMode);
                powerMode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(1);
                        randomWork.start();

                    }
                });

                Record = (ImageButton) stub.findViewById(R.id.Record);
                Record.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(3);
                        randomWork.start();
                    }
                });

                SlectMode = (ImageButton) stub.findViewById(R.id.SlectMode);
                SlectMode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(2);
                        randomWork.start();
                    }
                });
                ImageMode = (ImageView)stub.findViewById(R.id.ImageMode);
                updateScreen();
            }
        });


    }
    void UpdateGui(){
        if(DataBul.containsKey("Buttons")){
            if (DataBul.getBoolean("Buttons")){
                //ButtonLayout.setVisibility(View.VISIBLE);
            }else{
                //ButtonLayout.setVisibility(View.GONE);
            }
        }

        if(DataBul.containsKey("mode")){
           int mode =  DataBul.getInt("mode");
           int sub_mode = DataBul.getInt("sub_mode");
            if (mode == 0){
                //Video
                if (sub_mode == 0) {
                    ImageMode.setImageResource(R.drawable.video);
                }else if( sub_mode == 2){
                    ImageMode.setImageResource(R.drawable.video_photo);
                }else if(sub_mode == 3){
                    ImageMode.setImageResource(R.drawable.looping);
                }
                TestMode.setText(DataBul.getString("resolution")+" - "+DataBul.getString("fps"));
                RecordingTime.setText(""+DataBul.getInt("remaining_video_time") + "s [" + repeat('#',  DataBul.getInt("internal_battery_level"))+ "]");
                if (DataBul.getInt("system_busy") == 1){
                    int time = DataBul.getInt("video_progress_counter");
                    if (time >= 60){
                        int min = time/60;
                        RunTime.setText(min+""+(time - (min * 60)));
                    }else{
                        RunTime.setText("00:"+time);
                    }

                }else {
                    RunTime.setText(""+DataBul.getInt("num_total_videos"));
                }


            }else if(mode == 1){
                //Photo
                if (sub_mode == 0) {
                    ImageMode.setImageResource(R.drawable.single);
                }else if( sub_mode == 1){
                    ImageMode.setImageResource(R.drawable.continuous);
                }else if(sub_mode == 2){
                    ImageMode.setImageResource(R.drawable.night);
                }


            }else if(mode == 2){
                //Multishot
                if (sub_mode == 0) {
                    ImageMode.setImageResource(R.drawable.burst);
                }else if( sub_mode == 1){
                    ImageMode.setImageResource(R.drawable.time_lapse);
                }else if(sub_mode == 2){
                    ImageMode.setImageResource(R.drawable.night_lapse);
                }


            }


        }


    }

}

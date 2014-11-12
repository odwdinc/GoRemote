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
import android.widget.LinearLayout;
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



    TextView TestMode;
    ImageButton powerMode;
    ImageButton Record;
    ImageButton SlectMode;
    LinearLayout ButtonLayout;


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

        DataBul.putString(messageEvent.getPath(), new String(messageEvent.getData(), Charset.forName("UTF-8")));
        updateScreen();
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
                ButtonLayout = (LinearLayout) stub.findViewById(R.id.ButtonLayout);

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

                updateScreen();
            }
        });


    }
    void UpdateGui(){
        if (DataBul.containsKey("TestMode")){
            TestMode.setText(DataBul.getString("TestMode"));
        }
        if(DataBul.containsKey("Buttons")){
            if (DataBul.getBoolean("Buttons")){
                ButtonLayout.setVisibility(View.VISIBLE);
            }else{
                ButtonLayout.setVisibility(View.GONE);
            }
        }
    }

}
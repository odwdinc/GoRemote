package systems.movingdata.goremote;

import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends MyActivity{


    private static final long TIME_OUT_MS = 100;
    DataManager myDataManager;
    TextView mTextView;
    private boolean mResolvingError = false;

    String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myDataManager = new DataManager(this);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);

        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.TestMode);
                ImageButton powerMode = (ImageButton) stub.findViewById(R.id.PowerMode);

                powerMode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(1);
                        randomWork.start();

                    }
                });

                ImageButton Record = (ImageButton) stub.findViewById(R.id.Record);
                Record.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(3);
                        randomWork.start();
                    }
                });

                ImageButton SlectMode = (ImageButton) stub.findViewById(R.id.SlectMode);
                SlectMode.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        SendData randomWork = new SendData(2);
                        randomWork.start();
                    }
                });
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            myDataManager.mGoogleApiClient.connect();
            myDataManager.showNotification();
        }
    }




    @Override
    protected void onStop() {
        if (!mResolvingError) {
            Wearable.MessageApi.removeListener(myDataManager.mGoogleApiClient, myDataManager);
            Wearable.NodeApi.removeListener(myDataManager.mGoogleApiClient, myDataManager);
            myDataManager.mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    private void sendMessage(String node,int button) {


        Wearable.MessageApi.sendMessage(
                myDataManager.mGoogleApiClient, node, "/remote/"+button, new byte[0]).setResultCallback(
                new ResultCallback<SendMessageResult>() {
                    @Override
                    public void onResult(SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to send message with status code: "
                                    + sendMessageResult.getStatus().getStatusCode());
                        }else{
                            Log.i(TAG,"Sent Good");
                        }
                    }
                }
        );
        Log.i(TAG,"sent");

    }



    public class SendData extends Thread {
        int button;
        public SendData(int button) {
            this.button = button;
        }

        @Override
        public void run() {
            Log.v(TAG, "doing work in Thread");
            sendMessage(myDataManager.getRemoteNodeId(),this.button);
        }
    }



    @Override
    void UpdateGui(Bundle data){
        if (data.containsKey("TestMode")){
            mTextView.setText(data.getString("TestMode"));
        }
    }

}

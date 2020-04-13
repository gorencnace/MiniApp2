package si.uni_lj.fri.pbd.miniapp2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private final static int MSG_SONG_TIME = 0;

    private MediaPlayerService mediaPlayerService;
    private boolean serviceBound;

    private TextView songTitle;
    private TextView songDuration;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service bound");

            MediaPlayerService.RunServiceBinder binder = (MediaPlayerService.RunServiceBinder) iBinder;
            mediaPlayerService = binder.getService();
            serviceBound = true;

            // Update the UI if the service is already running the timer
            if (mediaPlayerService.isPlaying()) {
                updateUIPlay();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnect");

            serviceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songTitle = (TextView) findViewById(R.id.text_song_info);
        songDuration = (TextView) findViewById(R.id.text_duration);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent i = new Intent(this, MediaPlayerService.class);
        startService(i);
        bindService(i, mConnection, 0);
        registerReceiver(exitReciver, new IntentFilter(MediaPlayerService.EXIT_ACTIVITY_BROADCAST));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(MediaPlayerService.TIMER_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUIStop();
        if (serviceBound) {
            unbindService(mConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        unregisterReceiver(exitReciver);
        System.exit(0);
    }

    public void playButtonClick(View v) {
        if (serviceBound) {
            mediaPlayerService.playButtonService();
            updateUIPlay();
        }
    }

    public void pauseButtonClick(View v) {
        if (serviceBound) {
            mediaPlayerService.pauseButtonService();
        }
    }

    public void stopButtonClick(View v) {
        if (serviceBound) {
            mediaPlayerService.stopButtonService();
            updateUIStop();
        }
    }

    public void exitButtonClick(View v) {
        if (serviceBound) {
            mediaPlayerService.exitButtonService();
        } else {
            finish();
        }
    }

    private void updateUIPlay() {
        if (serviceBound) {
            songTitle.setText(mediaPlayerService.getSong());
        }
    }

    private void updateUIStop() {
        if (serviceBound) {
            songTitle.setText(R.string.song_info);
            songDuration.setText(R.string.duration);
        }
    }

    private void updateUITimer() {
        if (serviceBound) {
            if (mediaPlayerService.isPlaying()) {
                songDuration.setText(mediaPlayerService.setTimerText());
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUITimer();
            updateUIPlay();
        }
    };

    private BroadcastReceiver exitReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
}

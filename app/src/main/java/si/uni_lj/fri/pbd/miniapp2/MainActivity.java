/*
 * MAIN ACTIVITY
 *
 * MainActivity communicates with MediaPlayerService
 *
 */

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
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    /*
     * FIELDS
     */

    private static final String TAG = MainActivity.class.getSimpleName();
    private MediaPlayerService mediaPlayerService;
    // like in lab5
    private boolean serviceBound;
    // TextViews of song title and duration
    private TextView songTitle;
    private TextView songDuration;

    /*
     * Bounding MediaPlayerService to MainActivity
     */

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "Service bound");

            MediaPlayerService.RunServiceBinder binder = (MediaPlayerService.RunServiceBinder) iBinder;
            mediaPlayerService = binder.getService();
            serviceBound = true;

            // Update the UI if song is already playing
            if (!mediaPlayerService.isMediaPlayerNull()) {
                updateUIPlay();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "Service disconnect");

            serviceBound = false;
        }
    };

    /*
     * ACTIVITY LIFECYCLE METHODS
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songTitle = (TextView) findViewById(R.id.text_song_info);
        songDuration = (TextView) findViewById(R.id.text_duration);
        updateUIPlay();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // binding service like in lab5
        Intent i = new Intent(this, MediaPlayerService.class);
        startService(i);
        bindService(i, mConnection, 0);
        // this receiver will help us exit the application from notification
        registerReceiver(exitReceiver, new IntentFilter(MediaPlayerService.EXIT_ACTIVITY_BROADCAST));
    }

    @Override
    protected void onResume() {
        super.onResume();
        // this receiver will get us a song duration from service
        registerReceiver(mReceiver, new IntentFilter(MediaPlayerService.TIMER_BROADCAST));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // we unregister receiver for song duration
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        updateUIStop();
        // unbinding service on stop
        if (serviceBound) {
            unbindService(mConnection);
            serviceBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        // we unregister receiver for exiting app from notification and exit app
        unregisterReceiver(exitReceiver);
        System.exit(0);
    }

    /*
     * BUTTONS
     * following methods are called, when we click a certain button
     */

    // PLAY
    public void playButtonClick(View v) {
        if (serviceBound) {
            // logic for playing is hidden in service
            mediaPlayerService.playButtonService();
            // we also update ui
            updateUIPlay();
        }
    }

    // PAUSE
    public void pauseButtonClick(View v) {
        if (serviceBound) {
            // logic for pausing is hidden in service
            mediaPlayerService.pauseButtonService();
        }
    }

    // STOP
    public void stopButtonClick(View v) {
        if (serviceBound) {
           // logic for stopping is hidden in service
            mediaPlayerService.stopButtonService();
            // we also update ui
            updateUIStop();
        }
    }

    // EXIT
    public void exitButtonClick(View v) {
        if (serviceBound) {
            // logic for exiting is hidden in service
            mediaPlayerService.exitButtonService();
        } else {
            // if service is not bound we just exit the app
            finish();
        }
    }

    // G-ON
    public void gestureOn(View v) {
        if (serviceBound) {
            // logic for g-on is hidden in service
            mediaPlayerService.gestureOnService();
        }
    }

    // G-OFF
    public void gestureOff(View v) {
        if (serviceBound) {
            // logic for g-off is hidden in service
            mediaPlayerService.gestureOffService();
        }
    }

    /*
     * UI UPDATE
     * following methods are used for updating ui
     */

    private void updateUIPlay() {
        if (serviceBound) {
            if (mediaPlayerService.isPlaying()) {
                // we update song title, if music is playing and service is bound
                songTitle.setText(mediaPlayerService.getSong());
            }
        }
    }

    private void updateUIStop() {
        if (serviceBound) {
            if (!mediaPlayerService.isPlaying()) {
                // we update song title and duration to default values if song is stopped
                songTitle.setText(R.string.song_info);
                songDuration.setText(R.string.duration);
            }
        }
    }

    private void updateUITimer() {
        if (serviceBound) {
            if (mediaPlayerService.isPlaying()) {
                // we update song duration
                songDuration.setText(mediaPlayerService.setTimerText());
            }
        }
    }

    /*
     * BROADCAST RECEIVERS
     * we use them to get commands from service
     */

    // this receiver is used to update song title and duration from service
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUITimer();
            updateUIPlay();
        }
    };

    // this receiver is used to exit application from notification
    private BroadcastReceiver exitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
}

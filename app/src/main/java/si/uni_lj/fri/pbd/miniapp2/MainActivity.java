package si.uni_lj.fri.pbd.miniapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    // Handler to update the UI
    private final Handler mUpdateHandler = new UIUpdateHandler(this);

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
                updateUIPlay(mediaPlayerService.getSongId());
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
        i.setAction(MediaPlayerService.ACTION_PLAY);
        startService(i);
        Log.d(TAG, "Starting and binding service");
        bindService(i, mConnection, 0);
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

    public void playButtonClick(View v) {
        Log.d(TAG, "in playButtonClick");
        if (serviceBound) {
            Log.d(TAG, "serviceBind == true");
            if (mediaPlayerService.isMediaPlayerNull() || mediaPlayerService.isPlaying()) {
                Log.d(TAG, "we are getting songid");
                int songId = mediaPlayerService.playButtonService();
                if (songId != -1) {
                    updateUIPlay(songId);
                }
            } else if (!mediaPlayerService.isPlaying()) {
                mediaPlayerService.getMediaPlayer().start();
            }
        }
    }

    public void pauseButtonClick(View v) {
        if (serviceBound) {
            if (!mediaPlayerService.isMediaPlayerNull() && mediaPlayerService.isPlaying()) {
                mediaPlayerService.getMediaPlayer().pause();
            }
        }
    }

    public void stopButtonClick(View v) {
        if (serviceBound) {
            if (!mediaPlayerService.isMediaPlayerNull()) {
                mediaPlayerService.stopButtonService();
                updateUIStop();
            }
        }
    }

    public void exitButtonClick(View v) {
        if (serviceBound) {
            if (!mediaPlayerService.isMediaPlayerNull()) {
                mediaPlayerService.exitButtonService();
            }
        }
        finish();
        System.exit(0);
    }

    private void updateUIPlay(int id) {
        if (serviceBound) {
            mUpdateHandler.sendEmptyMessage(MSG_SONG_TIME);
            songTitle.setText(mediaPlayerService.getSongById(id));
        }
    }

    private void updateUIStop() {
        if (serviceBound) {
            mUpdateHandler.removeMessages(MSG_SONG_TIME);
            songTitle.setText(R.string.song_info);
            songDuration.setText(R.string.duration);
        }
    }

    private void updateUITimer() {
        if (serviceBound) {
            Log.d(TAG, "update ui time");
            if (mediaPlayerService.isPlaying()) {
                songDuration.setText(mediaPlayerService.setTimerText());
            }
        }
    }

    static class UIUpdateHandler extends Handler {
        private final static int UPDATE_RATE_MS = 1000;
        private final WeakReference<MainActivity> activity;

        UIUpdateHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SONG_TIME) {
                Log.d(TAG, "update time");
                activity.get().updateUITimer();
                sendEmptyMessageDelayed(MSG_SONG_TIME, UPDATE_RATE_MS);
            }
        }
    }
}

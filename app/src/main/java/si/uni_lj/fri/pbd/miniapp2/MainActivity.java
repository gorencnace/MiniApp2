package si.uni_lj.fri.pbd.miniapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
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

    // initializing buttons
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Button gesturesOnButton;
    private Button gesturesOffButton;
    private Button exitButton;
    // Handler to update the UI
    private final Handler mUpdateHandler = new UIUpdateHandler(this);

    private final static int MSG_SONG_TIME = 0;

    private MediaPlayer mediaPlayer;

    // media files in assets
    private AssetManager assetManager;
    private String[] songs;
    private List<String> it;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playButton = (Button) findViewById(R.id.button_play);
        pauseButton = (Button) findViewById(R.id.button_pause);
        stopButton = (Button) findViewById(R.id.button_stop);
        gesturesOnButton = (Button) findViewById(R.id.button_g_on);
        gesturesOffButton = (Button) findViewById(R.id.button_g_off);
        exitButton = (Button) findViewById(R.id.button_exit);

        assetManager = getAssets();
        try {
            songs = assetManager.list("songs");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void playButtonClick(View v) {
        if (mediaPlayer == null || isPlaying()) {
            Random rand = new Random();
            int randN = rand.nextInt(songs.length);
            if (mediaPlayer != null && isPlaying()) {
                mediaPlayer.stop();
            }
            // https://stackoverflow.com/questions/3289038/play-audio-file-from-the-assets-directory
            try {
                AssetFileDescriptor assetFileDescriptor = getAssets().openFd("songs/" + songs[randN]);
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(assetFileDescriptor.getFileDescriptor(),
                        assetFileDescriptor.getStartOffset(),
                        assetFileDescriptor.getLength());
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.d("MUSIC", e.toString());
            }
            updateUIPlay(randN);
        } else if (!isPlaying()) {
            mediaPlayer.start();
        }
    }

    public void pauseButtonClick(View v) {
        if (mediaPlayer != null && isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void stopButtonClick(View v) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            updateUIStop();
        }
    }

    public void exitButtonClick(View v) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            finish();
            System.exit(0);
        }
    }

    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    private void updateUIPlay(int n) {
        mUpdateHandler.sendEmptyMessage(MSG_SONG_TIME);
        TextView title = (TextView) findViewById(R.id.text_song_info);
        title.setText(songs[n]);
    }

    private void updateUIStop() {
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        TextView duration = (TextView) findViewById(R.id.text_duration);
        TextView title = (TextView) findViewById(R.id.text_song_info);
        title.setText(R.string.song_info);
        duration.setText(R.string.duration);
    }

    private void updateUITimer() {
        TextView duration = (TextView) findViewById(R.id.text_duration);
        if (isPlaying()) {
            duration.setText(durationToSting(mediaPlayer.getCurrentPosition()) + "/" + durationToSting(mediaPlayer.getDuration()));
        }
    }

    private String durationToSting(int d) {
        // we add 500 ms to round up
        d += 500;
        d = d / 1000;
        int h = d / 3600;
        d %= 3600;
        int min = d / 60;
        int sec = d % 60;
        return String.format("%d:%02d:%02d", h,  min, sec);
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
                activity.get().updateUITimer();
                sendEmptyMessageDelayed(MSG_SONG_TIME, UPDATE_RATE_MS);
            }
        }
    }
}

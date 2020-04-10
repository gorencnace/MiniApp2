package si.uni_lj.fri.pbd.miniapp2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.Button;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {

    // initializing buttons
    private Button playButton;
    private Button pauseButton;
    private Button stopButton;
    private Button gesturesOnButton;
    private Button gesturesOffButton;
    private Button exitButton;
    // Handler to update the UI
    private final Handler mUpdateSongHandler = new UIUpdateHandler(this);

    private final static int MSG_SONG_DURATION = 0;

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
    }

    private void updateUIPlay() {
        // TODO: sendMessage AtFrontOfQueue()???
        Message msg = Message.obtain();
        msg.obj = "Name of the song";
        mUpdateSongHandler.sendMessage(msg);
        mUpdateSongHandler.sendEmptyMessage(MSG_SONG_DURATION);
    }

    private void updateUIPause() {

    }

    private void updateUIStop() {

    }

    static class UIUpdateHandler extends Handler {
        private final static int UPDATE_RATE_MS = 1000;
        private final WeakReference<MainActivity> activity;

        UIUpdateHandler(MainActivity activity) {
            this.activity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            // TODO: song and time
            super.handleMessage(msg);
        }
    }
}

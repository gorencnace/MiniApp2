package si.uni_lj.fri.pbd.miniapp2;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.Random;

public class MediaPlayerService extends Service {

    private static final String TAG = MediaPlayerService.class.getSimpleName();

    public static final String ACTION_STOP = "stop_service";

    public static final String ACTION_START = "start_service";

    // media files in assets
    private AssetManager assetManager;
    private String[] songs;
    private MediaPlayer mediaPlayer;
    private int songId;

    // Define serviceBinder and instantiate it to RunServiceBinder
    public class RunServiceBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    private final IBinder serviceBinder = new RunServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding service");
        return this.serviceBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");
        assetManager = getAssets();
        try {
            songs = assetManager.list("songs");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying service");
    }

    public int playButtonService() {
        Random rand = new Random();
        int randN = rand.nextInt(songs.length);
        if (isPlaying()) {
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
            songId = randN;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("MUSIC", e.toString());
            songId = -1;
        }
        return songId;
    }

    public void stopButtonService() {
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

    public void exitButtonService() {
        mediaPlayer.stop();
        mediaPlayer.release();
    }

    public String setTimerText() {
        return durationToSting(mediaPlayer.getCurrentPosition()) + "/" + durationToSting(mediaPlayer.getDuration());
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

    public boolean isPlaying() {
        if (mediaPlayer == null) {
            return false;
        }
        return mediaPlayer.isPlaying();
    }

    public boolean isMediaPlayerNull() {
        return mediaPlayer == null;
    }

    public MediaPlayer getMediaPlayer() {
        return this.mediaPlayer;
    }

    public String getSongById(int id) {
        return songs[id];
    }

    public int getSongId() {
        return songId;
    }
}
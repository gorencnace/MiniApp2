package si.uni_lj.fri.pbd.miniapp2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Random;

public class MediaPlayerService extends Service {

    private static final String TAG = MediaPlayerService.class.getSimpleName();

    public static final String ACTION_STOP = "stop_service";
    public static final String ACTION_PAUSE = "pause_service";
    public static final String ACTION_PLAY = "start_service";
    public static final String ACTION_EXIT = "exit_service";

    private static final String channelID = "background_timer";

    private static final int NOTIFICATION_ID = 69;

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
        // create notification channel
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Starting service");
        createNotification();
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

    private void createNotification() {
        // add code to define a notification action
        Intent stopIntent = new Intent(this, MediaPlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(this, MediaPlayerService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0,
                playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent pauseIntent = new Intent(this, MediaPlayerService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0,
                pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent exitIntent = new Intent(this, MediaPlayerService.class);
        exitIntent.setAction(ACTION_EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0,
                exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        if (isMediaPlayerNull()) {
            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);
        } else if (isPlaying()) {
            builder.setContentTitle(songs[songId])
                    //.setContentText(setTimerText())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);

        } else {
            builder.setContentTitle(songs[songId])
                    //.setContentText(setTimerText())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);// create an action button in the notification
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    // Uncomment for creating a notification channel for the foreground service
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT < 26) {
            return;
        } else {

            NotificationChannel channel = new NotificationChannel(MediaPlayerService.channelID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.channel_desc));
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            channel.enableVibration(true);

            NotificationManager managerCompat = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            managerCompat.createNotificationChannel(channel);
        }
    }
}
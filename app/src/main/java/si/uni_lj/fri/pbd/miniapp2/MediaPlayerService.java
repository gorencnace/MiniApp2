package si.uni_lj.fri.pbd.miniapp2;

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
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Random;

public class MediaPlayerService extends Service {

    private static final String TAG = MediaPlayerService.class.getSimpleName();

    public static final String ACTION_STOP = "stop_service";
    public static final String ACTION_PAUSE = "pause_service";
    public static final String ACTION_PLAY = "start_service";
    public static final String ACTION_EXIT = "exit_service";

    public final static String EXIT_ACTIVITY_BROADCAST = "si.uni_lj.fri.pbd.miniapp2.EXIT_ACTIVITY_BROADCAST";

    private final Handler mUpdateHandler = new UIUpdateHandler(this);

    private final static int MSG_SONG_TIME = 0;

    public static final String TIMER_BROADCAST = "si.uni_lj.fri.pbd.miniapp2.TIMER_BROADCAST";

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
        if (intent.getAction() != null) {
            if (intent.getAction().equals((ACTION_EXIT))) {
                exitButtonService();
            } else if (intent.getAction().equals(ACTION_PLAY)) {
                playButtonService();
            } else if (intent.getAction().equals(ACTION_PAUSE)) {
                pauseButtonService();
            } else if (intent.getAction().equals(ACTION_STOP)) {
                stopButtonService();
            }
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Destroying service");
    }

    public void playButtonService() {
        if (isPlaying()) {
            stopButtonService();
        }
        if (isMediaPlayerNull()) {
            Random rand = new Random();
            int randN = rand.nextInt(songs.length);

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
        } else {
            mediaPlayer.start();
            mUpdateHandler.sendEmptyMessage(MSG_SONG_TIME);
            return;
        }
        mUpdateHandler.sendEmptyMessage(MSG_SONG_TIME);
        createNotification();
    }

    public void pauseButtonService() {
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        if (!isMediaPlayerNull() && isPlaying()) {
            mUpdateHandler.removeMessages(MSG_SONG_TIME);
            mediaPlayer.pause();
        }
        createNotification();
    }

    public void stopButtonService() {
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        if (!isMediaPlayerNull()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        createNotification();
    }

    public void exitButtonService() {
        if (!isMediaPlayerNull()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        Intent exitIntent = new Intent(EXIT_ACTIVITY_BROADCAST);
        deleteNotification();
        stopSelf();
        sendBroadcast(exitIntent);
    }

    public String setTimerText() {
        return durationToSting(mediaPlayer.getCurrentPosition()) + "/" + durationToSting(mediaPlayer.getDuration());
    }

    private String durationToSting(int d) {
        // we add 500 ms to round up
        d += 200;
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

    public String getSong() {
        if (songId < 0) {
            return null;
        }
        return songs[songId];
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
                    .setContentText(setTimerText())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);

        } else {
            builder.setContentTitle(songs[songId])
                    .setContentText(setTimerText())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);
        }

        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void deleteNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(MediaPlayerService.channelID);
        }
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

    static class UIUpdateHandler extends Handler {
        private final static int UPDATE_RATE_MS = 500;
        private final WeakReference<MediaPlayerService> service;

        UIUpdateHandler(MediaPlayerService service) {
            this.service = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_SONG_TIME) {
                service.get().updateUITimerService();
                sendEmptyMessageDelayed(MSG_SONG_TIME, UPDATE_RATE_MS);
            }
        }
    }

    private void updateUITimerService() {
        Intent intent = new Intent(TIMER_BROADCAST);
        sendBroadcast(intent);
        createNotification();
    }
}
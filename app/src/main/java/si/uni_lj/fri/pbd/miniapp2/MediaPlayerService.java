/*
 * MEDIA PLAYER SERVICE
 *
 * here we handle playing music
 *
 */

package si.uni_lj.fri.pbd.miniapp2;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Random;

public class MediaPlayerService extends Service {

    /*
     * FIELDS
     */

    private static final String TAG = MediaPlayerService.class.getSimpleName();
    // action fields
    public static final String ACTION_STOP = "stop_service";
    public static final String ACTION_PAUSE = "pause_service";
    public static final String ACTION_PLAY = "start_service";
    public static final String ACTION_EXIT = "exit_service";
    // broadcast fields
    public static final String EXIT_ACTIVITY_BROADCAST = "si.uni_lj.fri.pbd.miniapp2.EXIT_ACTIVITY_BROADCAST";
    public static final String TIMER_BROADCAST = "si.uni_lj.fri.pbd.miniapp2.TIMER_BROADCAST";
    // handler fields
    private final Handler mUpdateHandler = new UIUpdateHandler(this);
    private static final int MSG_SONG_TIME = 0;
    // notification fields
    private static final String channelID = "si.uni_lj.fri.pbd.miniapp2.NOTIFICATION";
    private static final int NOTIFICATION_ID = 69;
    // media files in assets
    private AssetManager assetManager;
    private String[] songs;
    private MediaPlayer mediaPlayer;
    private int songId;

    // define serviceBinder and instantiate it to RunServiceBinder
    public class RunServiceBinder extends Binder {
        MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }

    /*
     * BINDING
     */

    private final IBinder serviceBinder = new RunServiceBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Binding service");
        return this.serviceBinder;
    }

    /*
     * SERVICE LIFECYCLE METHODS
     */

    @Override
    public void onCreate() {
        Log.d(TAG, "Creating service");
        // we try to get list of songs from assets
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
        // create notification
        createNotification();
        // getting commands from notification and executing methods in service
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

    /*
     * BUTTONS
     * here we handle all logic for buttons from MainActivity
     */

    // PLAY
    public void playButtonService() {
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        // if user presses play button while song is already playing, we stop playing it
        if (isPlaying()) {
            stopButtonService();
        }
        // if song was not on pause we play new song ...
        if (isMediaPlayerNull()) {
            // we choose random song
            Random rand = new Random();
            int randN = rand.nextInt(songs.length);
            // but not the same as previous
            while (randN == songId) {
                randN = rand.nextInt(songs.length);
            }
            // then we try to prepare song and play it, based on:
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
            // ... else we unpause it
            mediaPlayer.start();
            mUpdateHandler.sendEmptyMessage(MSG_SONG_TIME);
            return;
        }
        // finally, we send empty message so the handler starts the timer
        mUpdateHandler.sendEmptyMessage(MSG_SONG_TIME);
        // and we create new notification
        createNotification();
    }

    // PAUSE
    public void pauseButtonService() {
        // we pause the song if song was playing
        if (isPlaying()) {
            // we stop updating handler
            mUpdateHandler.removeMessages(MSG_SONG_TIME);
            mediaPlayer.pause();
            createNotification();
        }
    }

    // STOP
    public void stopButtonService() {
        // we remove messages from handler
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        if (!isMediaPlayerNull()) {
            // stop playing
            mediaPlayer.stop();
            // release song
            mediaPlayer.release();
            mediaPlayer = null;
        }
        createNotification();
    }

    // EXIT
    public void exitButtonService() {
        // we stop mediaPlayer
        if (!isMediaPlayerNull()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        //  we remove messages from handler
        mUpdateHandler.removeMessages(MSG_SONG_TIME);
        // we set intent to exit app, delete notification, stop service and lastly send broadcast
        Intent exitIntent = new Intent(EXIT_ACTIVITY_BROADCAST);
        //deleteNotification();
        stopForeground(true);
        stopSelf();
        sendBroadcast(exitIntent);
    }

    /*
     * UTILITY METHODS
     */

    // getting string for song duration
    public String setTimerText() {
        return durationToSting(mediaPlayer.getCurrentPosition()) + "/" + durationToSting(mediaPlayer.getDuration());
    }

    // getting duration in string
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

    // checking if song is playing
    public boolean isPlaying() {
        if (mediaPlayer == null) {
            return false;
        }
        return mediaPlayer.isPlaying();
    }

    // checking if mediaPlayer is null
    public boolean isMediaPlayerNull() {
        return mediaPlayer == null;
    }

    // getting song based on id
    public String getSong() {
        if (songId < 0) {
            return null;
        }
        return songs[songId];
    }

    /*
     * NOTIFICATION
     */

    private Notification createNotification() {
        /* NOTIFICATION ACTIONS */
        // STOP
        Intent stopIntent = new Intent(this, MediaPlayerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0,
                stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // PLAY
        Intent playIntent = new Intent(this, MediaPlayerService.class);
        playIntent.setAction(ACTION_PLAY);
        PendingIntent playPendingIntent = PendingIntent.getService(this, 0,
                playIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // PAUSE
        Intent pauseIntent = new Intent(this, MediaPlayerService.class);
        pauseIntent.setAction(ACTION_PAUSE);
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 0,
                pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // EXIT
        Intent exitIntent = new Intent(this, MediaPlayerService.class);
        exitIntent.setAction(ACTION_EXIT);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0,
                exitIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        /* BUILDING */
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID);
        // if we don't have any song (we just started app or we previously pressed STOP)
        if (isMediaPlayerNull()) {
            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);
        }
        // if song is playing, we update notification
        else if (isPlaying()) {
            builder.setContentTitle(songs[songId])
                    .setContentText(setTimerText())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_pause, "Pause", pausePendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);

        }
        // if song is paused, we update notification
        else {
            builder.setContentTitle(songs[songId])
                    .setContentText(setTimerText())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setChannelId(channelID)
                    .addAction(android.R.drawable.ic_media_play, "Play", playPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
                    .addAction(android.R.drawable.ic_media_pause, "Exit", exitPendingIntent);
        }

        // finalizing
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        // we show notification
        //NotificationManager manager = getSystemService(NotificationManager.class);
        //manager.notify(NOTIFICATION_ID, builder.build());
        return builder.build();
    }

    // here we dismiss notification
    private void deleteNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel(MediaPlayerService.channelID);
        }
    }

    // we create a notification channel
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

    /*
     * HANDLER
     * for updating song duration, like in lab5
     */

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

    // we call this function in handler in which we send broadcast to MainActivity and
    // create new notification, both of them update song title and song duration
    private void updateUITimerService() {
        Intent intent = new Intent(TIMER_BROADCAST);
        sendBroadcast(intent);
        startForeground(NOTIFICATION_ID, createNotification());
    }

    /*
     * GESTURE SERVICE
     * here we have fields and methods for communication with AccelerationService
     */

    private AccelerationService accelerationService;
    private boolean serviceBound = false;

    // Bounding AccelerationService to MediaPlayerService
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "AccelerationService bound");

            AccelerationService.RunServiceBinder binder = (AccelerationService.RunServiceBinder) iBinder;
            accelerationService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "AccelerationService disconnect");
            serviceBound = false;
        }
    };

    // we receive broadcast if we should pause or play song
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra(AccelerationService.MOVEMENT);
            if (command.equals(AccelerationService.HORIZONTAL)) {
                pauseButtonService();
            } else if (command.equals(AccelerationService.VERTICAL)) {
                playButtonService();
            }
        }
    };

    // BUTTONS from MainActivity

    public void gestureOnService() {
        if (!serviceBound) {
            Toast.makeText(this, "Gestures activated", Toast.LENGTH_LONG).show();
            Log.d(TAG, "gestureOnService");
            Intent i = new Intent(this, AccelerationService.class);
            startService(i);
            bindService(i, mConnection, 0);
            registerReceiver(mReceiver, new IntentFilter(AccelerationService.GESTURE_BROADCAST));
        }
    }

    public void gestureOffService() {
        if (serviceBound) {
            Toast.makeText(this, "Gestures deactivated", Toast.LENGTH_LONG).show();
            unregisterReceiver(mReceiver);
            unbindService(mConnection);
            Intent i = new Intent(this, AccelerationService.class);
            stopService(i);
            serviceBound = false;
        }
    }
}
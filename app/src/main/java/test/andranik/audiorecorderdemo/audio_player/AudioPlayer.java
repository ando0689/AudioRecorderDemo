package test.andranik.audiorecorderdemo.audio_player;

import android.media.MediaPlayer;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import test.andranik.audiorecorderdemo.utils.ExceptionTracker;


/**
 * Created by andranik on 7/11/16.
 */
public class AudioPlayer {
    public static final String TAG = "AudioPlayer";

    public static final int PLAY_STATUS_STOPPED = 0;
    public static final int PLAY_STATUS_PAUSED = 1;
    public static final int PLAY_STATUS_PLAYING = 2;

    private MediaPlayer mediaPlayer;

    private AudioPlayListener playListener;

    private Subscription playProgressSubscription;

    private int playStatus;

    private String currentPlayingFileName;

    private String fileName;

    public AudioPlayer() {
        playStatus = PLAY_STATUS_STOPPED;
    }

    public void startPlaying(String fileName) {
        this.fileName = fileName;
        if (isPlaying()) {
            if (fileName.equals(currentPlayingFileName)) {
                return;
            } else {
                stopPlaying();
            }
        }

        Log.d(TAG, "startPlaying: " + fileName);

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(fileName);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();

                playStatus = PLAY_STATUS_PLAYING;
                currentPlayingFileName = fileName;

                notifyPlayStarted();
                startPlayProgress();
            });
        } catch (Exception e) {
            ExceptionTracker.trackException(e);
            Log.e(TAG, "prepare() failed");
            notifyPlayError();
        }
    }

    private void notifyPlayError() {
        if (playListener != null) playListener.onAudioPlayError();
    }

    public void stopPlaying() {
        if (mediaPlayer == null) {
            playStatus = PLAY_STATUS_STOPPED;
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        mediaPlayer.release();
        mediaPlayer = null;

        playStatus = PLAY_STATUS_STOPPED;

        notifyPlayStopped();
        stopPlayProgress();
    }


    public void pausePlaying() {
        //TODO need more checks
        mediaPlayer.pause();

        playStatus = PLAY_STATUS_PAUSED;

        notifyPlayPaused();
        pausePlayProgress();
    }

    public void resumePlaying() {
        //TODO need more checks
        mediaPlayer.start();

        playStatus = PLAY_STATUS_PLAYING;

        notifyPlayResumed();
        resumePlayProgress();
    }

    private void startPlayProgress() {
        playProgressSubscription = Observable
                .interval(100, TimeUnit.MILLISECONDS)
                .takeUntil(aLong -> aLong > mediaPlayer.getDuration() / 100)
                .map(aLong1 -> {
                    int[] result = new int[3];

                    final int time = mediaPlayer.getCurrentPosition();
                    final int maxTime = mediaPlayer.getDuration();

                    result[0] = time / 1000;
                    result[1] = (maxTime - time) / 1000;
                    result[2] = (time * 100) / maxTime;

                    return result;
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<int[]>() {
                    @Override
                    public void onCompleted() {
                        stopPlaying();
                    }

                    @Override
                    public void onError(Throwable e) {
                        stopPlaying();
                    }

                    @Override
                    public void onNext(int[] ints) {
                        notifyPlayProgress(ints[0], ints[1], ints[2]);
                    }
                });
    }

    private void stopPlayProgress() {
        if (playProgressSubscription != null && !playProgressSubscription.isUnsubscribed()) {
            playProgressSubscription.unsubscribe();
        }
    }

    private void pausePlayProgress() {
        stopPlayProgress(); //TODO test
    }

    private void resumePlayProgress() {
        startPlayProgress(); // TODO test
    }

    public boolean isPlaying() {
        return playStatus == PLAY_STATUS_PLAYING;
    }

    public boolean isPaused() {
        return playStatus == PLAY_STATUS_PAUSED;
    }

    public boolean isStopped() {
        return playStatus == PLAY_STATUS_STOPPED;
    }

    private void notifyPlayStarted() {
        if (playListener != null) playListener.onAudioPlayStarted(fileName);
    }

    private void notifyPlayStopped() {
        if (playListener != null) playListener.onAudioPlayStopped(fileName);
    }

    private void notifyPlayPaused() {
        if (playListener != null) playListener.onAudioPlayPaused(fileName);
    }

    private void notifyPlayResumed() {
        if (playListener != null) playListener.onAudioPlayResumed(fileName);
    }

    private void notifyPlayProgress(int timeElapsed, int timeLeft, int percentPlayed) {
        if (playListener != null)
            playListener.onAudioPlayProgress(timeElapsed, timeLeft, percentPlayed);
        if (percentPlayed == 100) {
            stopPlaying();
        }
    }

    public void setPlayListener(AudioPlayListener playListener) {
        this.playListener = playListener;
    }

    public interface AudioPlayListener {
        void onAudioPlayStarted(String path);

        void onAudioPlayStopped(String path);

        void onAudioPlayPaused(String path);

        void onAudioPlayResumed(String path);

        void onAudioPlayProgress(int timeElapsed, int timeLeft, int percentPlayed);

        void onAudioPlayError();
    }
}

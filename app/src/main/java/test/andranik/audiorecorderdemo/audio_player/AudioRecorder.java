package test.andranik.audiorecorderdemo.audio_player;

import android.media.MediaRecorder;
import android.support.annotation.IntDef;
import android.util.Base64;
import android.util.Log;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import test.andranik.audiorecorderdemo.App;
import test.andranik.audiorecorderdemo.utils.ExceptionTracker;


/**
 * Created by andranik on 7/8/16.
 */
public class AudioRecorder {

    public static final String TAG = "AudioRecorder";

    @IntDef({DURATION_SHORT, DURATION_LONG})
    @Retention(RetentionPolicy.SOURCE)
    @interface Duration {
    }

    public static final int DURATION_SHORT = 12 * 1000;
    public static final int DURATION_LONG = 30 * 1000;

    private int maxDuration = 12 * 1000; // 12 seconds by default

    private String fileName;

    private MediaRecorder mediaRecorder;

    private AudioRecordListener recordListener;

    private Subscription recordProgressSubscription;

    private boolean isRecording;


    public AudioRecorder(@Duration int maxDuration) {
        this.maxDuration = maxDuration;

        fileName = App.getInstance().getFilesDir().getAbsolutePath() + "/current.m4a";
    }

    void startRecording() {
        App.getInstance().getUiHandler().removeCallbacks(stopRecordRunnable);

        if (isRecording) {
            return;
        }

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(fileName);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            notifyRecordStarted();
            startRecordProgress();
        } catch (IOException e) {
            ExceptionTracker.trackException(e);
            Log.e(TAG, "prepare() failed");
        }
    }

    void stopRecording() {
        if (mediaRecorder == null) {
            return;
        }

        App.getInstance().getUiHandler().postDelayed(stopRecordRunnable, 200);
    }

    private Runnable stopRecordRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (mediaRecorder != null) {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                }
                notifyRecordStopped(true);
                stopRecordProgress();
            } catch (RuntimeException e) {
                // meaning user just tapped on the recorder and released without passing even a second.Catch that and behave as nothing happened.
                stopRecordProgress();
                notifyRecordStopped(false);
            }

            isRecording = false;
        }
    };

    private void notifyRecordStarted() {
        if (recordListener != null) recordListener.onAudioRecordStarted();
    }

    private void notifyRecordStopped(boolean success) {
        if (recordListener != null) recordListener.onAudioRecordStopped(success);
    }

    private void notifyRecordProgress(int timeElapsed, int timeLeft, int percentPlayed) {
        if (recordListener != null)
            recordListener.onAudioRecordProgress(timeElapsed, timeLeft, percentPlayed);
    }

    public void setRecordListener(AudioRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    public void setMaxDuration(@Duration int maxDuration) {
        this.maxDuration = maxDuration;
    }

    private void startRecordProgress() {
        recordProgressSubscription = Observable
                .interval(100, TimeUnit.MILLISECONDS)
                .takeUntil(aLong -> aLong > maxDuration / 100)
                .map(aLong1 -> {
                    int time = aLong1.intValue() / 10;
                    int maxTime = maxDuration / 1000;
                    int[] result = new int[3];

                    result[0] = time;
                    result[1] = (maxTime - time);
                    result[2] = (int) ((aLong1 * 10) / maxTime);

                    return result;
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<int[]>() {
                    @Override
                    public void onCompleted() {
                        stopRecording();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        stopRecording();
                    }

                    @Override
                    public void onNext(int[] ints) {
                        notifyRecordProgress(ints[0], ints[1], ints[2]);
                    }
                });
    }

    private void stopRecordProgress() {
        if (recordProgressSubscription != null && !recordProgressSubscription.isUnsubscribed()) {
            recordProgressSubscription.unsubscribe();
        }
    }

    public boolean isRecording() {
        return isRecording;
    }

    boolean delete() {
        File file = new File(fileName);
        return file.delete();
    }

    String getBase64() throws IOException {
        File file = new File(fileName);
        if (!file.exists()) {
            throw new FileNotFoundException("The audio file was not found");
        }

        String rawBase64 = Base64.encodeToString(
                FileUtils.readFileToByteArray(file),
                Base64.DEFAULT);

        return rawBase64; // TODO may be will need to format it here and add metadata
    }

    public String getFileName() {
        return fileName;
    }

    public interface AudioRecordListener {
        void onAudioRecordStarted();

        void onAudioRecordStopped(boolean successfullyStopped);

        void onAudioRecordProgress(int timeElapsed, int timeLeft, int percentPlayed);
    }
}

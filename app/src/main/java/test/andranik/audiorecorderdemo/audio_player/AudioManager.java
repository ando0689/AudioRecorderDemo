package test.andranik.audiorecorderdemo.audio_player;

import java.io.IOException;

/**
 * Created by andranik on 7/11/16.
 */
public class AudioManager {

    private AudioRecorder audioRecorder;

    private AudioPlayer audioPlayer;

    private AudioDownloader audioDownloader;

    public AudioManager() {
    }

    public AudioManager initAudioPlayer(AudioPlayer.AudioPlayListener listener) {
        audioPlayer = new AudioPlayer();
        audioPlayer.setPlayListener(listener);

        return this;
    }

    public AudioManager initAudioRecorder(AudioRecorder.AudioRecordListener listener) {
        audioRecorder = new AudioRecorder(AudioRecorder.DURATION_SHORT); // defaults to short
        audioRecorder.setRecordListener(listener);

        return this;
    }

    public AudioManager initAudioDownloader() {
        audioDownloader = new AudioDownloader();

        return this;
    }

    public AudioRecorder getAudioRecorder() {
        return audioRecorder;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public AudioDownloader getAudioDownloader() {
        return audioDownloader;
    }


    public void startDownloadFile(String url, AudioDownloader.AudioDownloadListener listener) {
        if (audioDownloader == null) {
            throw new AudioManagerException("need to call initAudioDownloader() first");
        }

        audioDownloader.startDownload(url, listener);
    }

    public void startRecording() {
        if (audioRecorder == null) {
            throw new AudioManagerException("need to call initAudioRecorder() first");
        }

        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.stopPlaying();
        }

        audioRecorder.startRecording();
    }

    public void stopRecording() {
        if (audioRecorder == null) {
            throw new AudioManagerException("need to call initAudioPlayer() first");
        }
        try{
            audioRecorder.stopRecording();
        }catch (RuntimeException e){
            e.printStackTrace();
        }
    }

    public void startPlaying(String path) {
        if (audioPlayer == null) {
            return;
        }

        audioPlayer.startPlaying(path);
    }

    public void startPlayingRecordedFile() {
        if (audioRecorder == null || audioPlayer == null) {
            return;
        }

        if (audioRecorder.isRecording()) {
            stopRecording();
        }

        startPlaying(audioRecorder.getFileName());
    }

    public void stopPlaying() {
        if (audioPlayer == null) {
            return;
        }

        audioPlayer.stopPlaying();
    }

    public void pausePlaying() {
        if (audioPlayer == null) {
            return;
        }

        audioPlayer.pausePlaying();
    }

    public void resumePlaying() {
        if (audioPlayer == null) {
            return;
        }

        audioPlayer.resumePlaying();
    }

    public void deleteRecordedFile() {
        if (audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.stopPlaying();
        }

        if (audioRecorder != null && audioRecorder.isRecording()) {
            stopRecording();
        }

        if (audioRecorder != null) {
            audioRecorder.delete();
        }
    }

    public String getBase64() {
        if (audioRecorder == null) {
            return null;
        }

        try {
            return audioRecorder.getBase64();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}

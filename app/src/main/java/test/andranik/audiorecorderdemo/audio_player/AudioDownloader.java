package test.andranik.audiorecorderdemo.audio_player;

import java.io.File;
import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import test.andranik.audiorecorderdemo.utils.ExceptionTracker;


/**
 * Created by andranik on 7/11/16.
 */
public class AudioDownloader {

    public static final int DOWNLOAD_CHUNK_SIZE = 2048; //Same as Okio Segment.SIZE
    private OkHttpClient client;

    public AudioDownloader() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        //TODO experiment with options
        client = builder.build();
    }

    public void startDownload(String url, AudioDownloadListener listener){
        listener.onAudioDownloadStarted(AudioUtils.getAudioFilePath(url));

        if(AudioUtils.checkIfFileExists(url)){
            listener.onAudioDownloadFinished(AudioUtils.getAudioFilePath(url), true);
            return;
        }

        getDownloadObservable(url)
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<DownloadStatus>() {
                    @Override
                    public void onCompleted() {
                        listener.onAudioDownloadComplete(url);
                    }

                    @Override
                    public void onError(Throwable e) {
                        listener.onAudioDownloadError(e);
                        ExceptionTracker.trackException(e);
                    }

                    @Override
                    public void onNext(DownloadStatus downloadStatus) {
                        if (downloadStatus.isFinished()){
                            listener.onAudioDownloadFinished(downloadStatus.getPath(), downloadStatus.isSuccessful());
                        } else if(downloadStatus.isSuccessful()){
                            listener.onAudioDownloadProgress(downloadStatus.getPath(), downloadStatus.getProgress());
                        }
                    }
                });
    }

    private Observable<DownloadStatus> getDownloadObservable(String url){
        return Observable.create(subscriber -> {
            try {
                Request request = new Request.Builder().url(url).build();

                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();
                long contentLength = body.contentLength();
                BufferedSource source = body.source();

                String path = AudioUtils.getAudioFilePath(url);
                File file = new File(path);
                BufferedSink sink = Okio.buffer(Okio.sink(file));

                long totalRead = 0;
                long read = 0;
                int lastUpdatedProgress = 0;
                while ((read = (source.read(sink.buffer(), DOWNLOAD_CHUNK_SIZE))) != -1) {
                    totalRead += read;
                    int progress = (int) ((totalRead * 100) / contentLength);

                    if(progress > lastUpdatedProgress){
                        lastUpdatedProgress = progress;
                        subscriber.onNext(DownloadStatus.create(path, progress));
                    }
                }
                sink.writeAll(source);
                sink.flush();
                sink.close();

                subscriber.onNext(DownloadStatus.create(path, 100, true, true));
                subscriber.onCompleted();
            } catch (IOException e) {
                subscriber.onNext(DownloadStatus.create(AudioUtils.getAudioFilePath(url), -1, true, false));
                subscriber.onError(e);
                ExceptionTracker.trackException(e);
            }
        });
    }


    private static class DownloadStatus{
        private String path;
        private int progress;
        private boolean finished;
        private boolean successful;

        public static DownloadStatus create(String path, int progress) {
            return create(path, progress, false, true);
        }

        public static DownloadStatus create(String path, int progress, boolean finished, boolean successful) {
            DownloadStatus status = new DownloadStatus();

            status.setPath(path);
            status.setProgress(progress);
            status.setFinished(finished);
            status.setSuccessful(successful);

            return status;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getProgress() {
            return progress;
        }

        public void setProgress(int progress) {
            this.progress = progress;
        }

        public boolean isFinished() {
            return finished;
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }
    }

    public interface AudioDownloadListener{
        void onAudioDownloadStarted(String path);
        void onAudioDownloadFinished(String path, boolean successful);
        void onAudioDownloadProgress(String path, int percentPlayed);
        void onAudioDownloadError(Throwable e);
        void onAudioDownloadComplete(String url);
    }

}

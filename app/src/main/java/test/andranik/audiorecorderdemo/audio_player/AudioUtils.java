package test.andranik.audiorecorderdemo.audio_player;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.apache.commons.io.FilenameUtils;

import java.io.File;

/**
 * Created by andranik on 7/11/16.
 */
public class AudioUtils {

    private static final long DAY_MILIS = 86400000;

    private static final String APP_DIR_NAME = "AudioDemo";
    private static final String AUDIO_FILES_DIR = "audio";

    public static String getAudioDirPath() {
        String dirPath = String.format("%s/%s/%s",
                Environment.getExternalStorageDirectory().getAbsolutePath(),
                APP_DIR_NAME,
                AUDIO_FILES_DIR);

        File dir = new File(dirPath);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dirPath;
    }

    public static String getAudioFilePath(String url) {
        return String.format("%s/%s", getAudioDirPath(), getFileNameFromUri(url));
    }

    public static boolean checkIfFileExists(String url) {
        File file = new File(getAudioFilePath(url));

        return file.exists();
    }

    public static String getFileNameFromUri(String url) {
        return FilenameUtils.getBaseName(url);
    }

    public static void makeCleanUp(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String dirPath = getAudioDirPath();
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files.length <= 0) {
            Log.d("time", "no files to delete...");
            return;
        }
        for (File file : files) {
            Log.d("time", "file - " + file.getName());

            long diff = System.currentTimeMillis() - file.lastModified();
            if (diff > DAY_MILIS) {
                Log.d("time", "difference" + diff);
                file.delete();
            }
        }
    }

}

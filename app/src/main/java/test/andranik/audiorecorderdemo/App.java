package test.andranik.audiorecorderdemo;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

/**
 * Created by andranik on 1/6/18.
 */

public class App extends Application {

    private static App instance;

    public App() {
        super();
    }

    public static App getInstance() {
        return instance;
    }

    private Handler uiHandler;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        uiHandler = new Handler(Looper.getMainLooper());
    }

    public Handler getUiHandler() {
        return uiHandler;
    }
}

package connectServer;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

/**
 * Created by atul on 12/4/16.
 */
@SuppressLint("ParcelCreator")
public class DownloadResultReceiver extends ResultReceiver {
    private Receiver mReceiver;
    private Listener listener;
    public DownloadResultReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    public interface Receiver {
        public void onReceiveResult(int resultCode, Bundle resultData);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
    public static interface Listener {
        void onReceiveResult(int resultCode, Bundle resultData);
    }
}

package ListenerClasses;

/**
 * Created by atul on 14/4/16.
 */
public interface DownloadListeners {
    public void onDataUpdate(boolean flag);
    public void donotNeedtoDownload(String path);
}

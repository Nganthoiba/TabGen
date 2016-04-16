package connectServer;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by atul on 12/4/16.
 */
public class FileInfoService extends IntentService {
    String token,webApi;
    InputStream isr=null;
    public int responseCode;
    public String responseMessage, errorMessage,TokenId=null;
    URL api_url=null;
    public HttpURLConnection conn=null;
    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_FINISHED = 1;
    public static final int STATUS_ERROR = 2;
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
//     * @param name Used to name the worker thread, important only for debugging.
     */
    public FileInfoService() {
        super(FileInfoService.class.getName());

    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle bundle = new Bundle();
        final ResultReceiver receiver = intent.getParcelableExtra("receiver");
        Bundle bundle1=intent.getExtras();
        if(bundle!=null){
            TokenId=bundle1.getString("TOKEN");
            webApi=bundle1.getString("WEBAPI");

            Log.v("WEBAPI","WEBAPI:::"+webApi+"::TOKEN::"+TokenId);
        }

        try{
            api_url =new URL(webApi);

            if (!TextUtils.isEmpty(webApi)) {
            /* Update UI: Download Service is Running */
                receiver.send(STATUS_RUNNING, Bundle.EMPTY);

                try {
                    InputStream inputStream=getData(api_url,TokenId);
                    String results = convertInputStreamToString(inputStream);
                /* Sending result back to activity */
                    if (null != results && !results.equals("")) {
                        Log.v("WEBAPI","RESULT:::"+results);
                        bundle.putString("result", results);
                        receiver.send(STATUS_FINISHED, bundle);
                    }
                } catch (Exception e) {

                /* Sending error message back to activity */
                    bundle.putString(Intent.EXTRA_TEXT, e.toString());
                    receiver.send(STATUS_ERROR, bundle);
                }
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        Log.d("SERVICE", "Service Stopping!");
        this.stopSelf();
    }
    public String convertInputStreamToString(InputStream inputStream){
        String result=null;
        if(inputStream!=null){
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"iso-8859-1"),8);
                StringBuilder sb = new StringBuilder();
                String line;
                while((line=reader.readLine())!=null){
                    sb.append(line +"\n");
                }
                inputStream.close();
                result = sb.toString();
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("We have found an exception: \n"+e.toString());
            }
        }
        return result;
    }
    public InputStream getData(URL api_url,String TokenId){
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try{
            conn = (HttpURLConnection) api_url.openConnection();
            //conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + TokenId);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            responseCode = conn.getResponseCode();
            responseMessage = conn.getResponseMessage();
            System.out.println("Response Code: " + responseCode + "\nResponse message: " + responseMessage);
            if(responseCode == 200/*HttpURLConnection.HTTP_OK*/){
                isr = new BufferedInputStream(conn.getInputStream());
            }
            else {
                isr = new BufferedInputStream(conn.getErrorStream());
            }
        }catch(Exception e){
            e.printStackTrace();
            errorMessage = e.toString();
            responseCode=-1;
            System.out.println("Exception occurs here: " + e.toString());
        }
        return isr;
    }//end of getData function


}

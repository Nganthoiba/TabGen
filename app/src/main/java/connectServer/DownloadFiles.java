package connectServer;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import ListenerClasses.DownloadListeners;
import readData.ReadFile;

/**
 * Created by SALAI on 2/17/2016.
 */
public class DownloadFiles extends AsyncTask<String,String,String> implements DownloadListeners{
    URL api_url;
    HttpURLConnection conn;
    public Context context;
    public int responseCode;
    public String responseMessage,errorMessage;
    String TokenId;
    private ProgressDialog mProgressDialog;
    public String file_name=null;
    DownloadListeners downloadListeners;

    public DownloadFiles(String url_path,Context _context,String token_id,DownloadListeners downloadListeners){
        try{
            this.downloadListeners=downloadListeners;
            api_url = new URL(url_path);
            context = _context;
            TokenId = token_id;
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage("Downloading file..");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
        }catch(Exception e){
            System.out.println("Unable to set URL in DownloadFiles constructor: "+e.toString());
        }
    }


    public DownloadFiles(String url_path,Context _context,String token_id){
        try{
            api_url = new URL(url_path);
            context = _context;
            TokenId = token_id;
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            mProgressDialog = new ProgressDialog(context);
            mProgressDialog.setMessage("Downloading file..");
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
        }catch(Exception e){
            System.out.println("Unable to set URL in DownloadFiles constructor: "+e.toString());
        }
    }

    @Override
    protected void onPreExecute(){mProgressDialog.show();}
    @Override
    protected String doInBackground(String... filename){
        InputStream isr;
        int count;
        file_name = filename[0];
        try{
            conn = (HttpURLConnection) api_url.openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + TokenId);
            //conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            responseCode = conn.getResponseCode();
            responseMessage = conn.getResponseMessage();
            System.out.println("Response Code: " + responseCode + "\nResponse message: " + responseMessage);
            if(responseCode == 200/*HttpURLConnection.HTTP_OK*/){
                int lenghtOfFile = conn.getContentLength();
                //isr = new BufferedInputStream(api_url.openStream());
                isr = conn.getInputStream();
                /*
                //For accessing internal data storage
                File mydir = context.getDir("TabGen", Context.MODE_PRIVATE); //Creating an internal dir;
                File fileWithinMyDir = new File(mydir, filename[0]); //Getting a file within the dir.
                FileOutputStream output = new FileOutputStream(fileWithinMyDir); //Use the stream as usual to write into the file.
                */
                //For accessing external data storage
                File SDCardRoot = new File(Environment.getExternalStorageDirectory()+"/HCircle");
                if(!SDCardRoot.isDirectory())//if directory does not exists
                    SDCardRoot.mkdirs();//then make the directory
                File file = new File(SDCardRoot,file_name);
                FileOutputStream output = new FileOutputStream(file);

                byte data[] = new byte[1024];

                long total = 0;
                while ((count = isr.read(data)) != -1) {
                    total += count;
                    publishProgress(""+(int)((total*100)/lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
            }
            else {
                //isr = new BufferedInputStream(conn.getErrorStream());
                //Toast.makeText(context,"Failed to download file",Toast.LENGTH_LONG).show();
            }
        }catch(Exception e){
            e.printStackTrace();
            errorMessage = e.toString();
            responseCode=-1;
            System.out.println("Exception occurs here: " + e.toString());
            //Toast.makeText(context,"Oops! Failed to download file",Toast.LENGTH_LONG).show();
            return errorMessage;
        }
        return responseMessage;
    }

    protected void onProgressUpdate(String... progress) {
        Log.d("ANDRO_ASYNC", progress[0]);
        downloadListeners.onDataUpdate(true);
        mProgressDialog.setProgress(Integer.parseInt(progress[0]));
        //mProgressDialog.show();
    }
    @Override
    protected void onPostExecute(String res){
        if(responseCode==200) {
            mProgressDialog.dismiss();
            String destination_path = Environment.getExternalStorageDirectory()+"/HCircle";
            openFolder(context);
        }else{
            mProgressDialog.setMessage(res);
            mProgressDialog.setCancelable(true);
        }
    }

    public void openFolder(final Context context)
    {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        Uri uri = Uri.parse(Environment.getExternalStorageDirectory().getPath()
                + "/HCircle/");
        intent.setDataAndType(uri, "*/*");
        Activity my_activity = new Activity(){
            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data){
                if(data==null) return;
                Uri fileUri = data.getData();
                //ReadFile readFile = new ReadFile();
                switch(requestCode){
                    case 1: //file_path = readFile.getFilePath(fileUri,context);
                        String file_path = ReadFile.getPath(fileUri, context);
                        if(file_path!=null){

                        }
                        break;
                    default:
                        Toast.makeText(context, "Invalid request code. You haven't selected any file", Toast.LENGTH_SHORT).show();
                }
            }
        };
        context.startActivity(Intent.createChooser(intent, "Open folder"));
    }

    @Override
    public void onDataUpdate(boolean flag) {

    }

    @Override
    public void donotNeedtoDownload(String path) {

    }
}

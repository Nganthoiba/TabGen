package chattingEngine;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nganthoi.salai.tabgen.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import Utils.InpuStreamConversion;
import connectServer.ConnectAPIs;
import connectServer.DownloadFiles;
import sharePreference.SharedPreference;

/**
 * Created by SALAI on 2/23/2016.
 */
public class FileAdapter extends BaseAdapter{
    private List<String> fileList;
    private Activity context;
    String file_info;
    String only_filename;
    String ip;
    String token;

    public FileAdapter(List<String> list,Activity _context){
        //this.fileList = new ArrayList<String>();
        this.fileList = list;
        this.context = _context;
        SharedPreference sp = new SharedPreference();
        ip = sp.getServerIP_Preference(context);
        token = sp.getTokenPreference(context);
    }
    @Override
    public int getCount() {
        //return 0;
        return fileList.size();
    }

    @Override
    public String getItem(int position) {
        if(fileList!=null)
        {
            return fileList.get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FileViewHolder holder;
        file_info = getItem(position);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if(convertView==null){
            convertView = vi.inflate(R.layout.file_display_layout,null);
            holder=createViewHolder(convertView);
            convertView.setTag(holder);
        }else{
            holder = (FileViewHolder) convertView.getTag();
        }
        if(file_info!=null) {

            ConnectAPIs connectAPIs = new ConnectAPIs("http://" + ip + ":8065/api/v1/files/get_info/" + file_info, token);
            InputStream isr = connectAPIs.getData();
            String file_detail_Info;
            try {
//                Log.v("VALUE"+)
                //System.out.println("File Information: "+convertInputStreamToString(isr));
                JSONObject jsonfileInfo = new JSONObject(connectAPIs.convertInputStreamToString(isr));
                Log.v("JSONFILENAME", "JSONFILENAME::::" + jsonfileInfo);
                String file_path = jsonfileInfo.getString("filename");
                int lastOccurance = file_path.lastIndexOf('/');
                only_filename = file_path.substring(lastOccurance + 1);
                file_detail_Info = only_filename + " \n" + InpuStreamConversion.humanReadableByteCount(Long.parseLong(jsonfileInfo.getString("size")),true);
                holder.file_details.setText(file_detail_Info);
                Picasso.with(context).load("http://"+ip+":8065/api/v1/files/get/"+file_info+"?session_token_index=1").resize(200, 200).into(holder.imgDetails);
            } catch (JSONException e) {
                System.out.println("JSON Exception in FileAdapter: " + e.toString());
                holder.file_details.setText(null);
            }
            holder.downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Log.e("VALUE","TOKEN:::"+token);
                    String filePath = "http://"+ip+":8065/api/v1/files/get/"+file_info+"?session_token_index=0";

                    DownloadFiles downloadFiles = new DownloadFiles(filePath,context,token);
                    downloadFiles.execute(only_filename);
                }
            });
        }
        return convertView;
    }
    private FileViewHolder createViewHolder(View view){
        FileViewHolder fileViewHolder = new FileViewHolder();
        fileViewHolder.imgDetails=(ImageView)view.findViewById(R.id.imgDetails);
//        http://128.199.111.18:8065/api/v1/files/get/8uq5js8z6tdjmnwk7mw55uuhko/j7r9nopox7rrtfqdbcr3w66zih/9hixmccmsjnkdeoy6f3yo7kxbo/IMG_20160402_201804.jpg
        Log.v("IMAGE","IMAGE::"+"http://"+ip+":8065/api/v1/files/get"+fileList.get(0));
//        http://"+ip+"8065/api/v1/files/get"+fileList.get(0)+"?session_token_index=0
//        http://128.199.111.18:8065/api/v1/files/get/8uq5js8z6tdjmnwk7mw55uuhko/j7r9nopox7rrtfqdbcr3w66zih/9hixmccmsjnkdeoy6f3yo7kxbo/IMG_20160402_201804.jpg?session_token_index=1
//        Picasso.with(context).load("http://128.199.111.18:8065/api/v1/files/get/8uq5js8z6tdjmnwk7mw55uuhko/j7r9nopox7rrtfqdbcr3w66zih/9hixmccmsjnkdeoy6f3yo7kxbo/IMG_20160402_201804.jpg?session_token_index=1").resize(200,200).into(fileViewHolder.imgDetails);
//        File imgFile1 = new  File(fileList.get(0));
//
//        if(imgFile1.exists()){
//
//            Bitmap myBitmap = BitmapFactory.decodeFile(imgFile1.getAbsolutePath());
//            fileViewHolder.imgDetails.setImageBitmap(myBitmap);
//        }
        fileViewHolder.file_details = (TextView) view.findViewById(R.id.fileInformation);
        fileViewHolder.downloadButton = (ImageView) view.findViewById(R.id.downloadButton);
        return fileViewHolder;
    }
    public static class FileViewHolder{
        ImageView imgDetails;
        ImageView downloadButton;
        TextView file_details;
    }

}

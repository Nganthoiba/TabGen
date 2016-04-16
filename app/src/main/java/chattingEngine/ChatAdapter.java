package chattingEngine;

import android.app.Activity;
import android.content.Context;
/*import android.graphics.Color;
import android.support.annotation.NonNull;*/
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
//import android.widget.Toast;

import com.android.volley.toolbox.NetworkImageView;
import com.nganthoi.salai.tabgen.AppController;
import com.nganthoi.salai.tabgen.BuildConfig;
import com.nganthoi.salai.tabgen.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;


/*import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;*/

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import Utils.InpuStreamConversion;
import Utils.LruBitmapCache;
import connectServer.ConnectAPIs;
import connectServer.DownloadFiles;
import connectServer.DownloadResultReceiver;
import connectServer.FileInfoService;
import sharePreference.SharedPreference;
//import java.util.ListIterator;

/**
 * Created by SALAI on 1/26/2016.
 */
public class ChatAdapter extends BaseAdapter implements View.OnClickListener{
    DownloadResultReceiver downloadResultReceiver;
    private final List<ChatMessage> chatMessages;
    String file_info_Result;
    private Activity context;
    String ip,token,only_filename;
    private SparseBooleanArray selectedItemIds;
//    public ImageLoader imageLoader;
    DisplayImageOptions options;
    com.android.volley.toolbox.ImageLoader imageLoader;
    public ChatAdapter(Activity context, List<ChatMessage> chatMessages) {
        this.context = context;
        this.chatMessages = chatMessages;
        imageLoader= AppController.getInstance().getImageLoader();
        selectedItemIds = new SparseBooleanArray();
        SharedPreference sp = new SharedPreference();
        ip = sp.getServerIP_Preference(context);
        token = sp.getTokenPreference(context);
    }
    public void setChatResult(String result){
        file_info_Result=result;
    }
    @Override
    public int getCount() {
        if (chatMessages != null) {
            return chatMessages.size();
        } else {
            return 0;
        }
    }

    @Override
    public ChatMessage getItem(int position) {
        if (chatMessages != null) {
            return chatMessages.get(position);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        final ChatMessage chatMessage = getItem(position);
        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Log.v("TOKEN","TOKEN::"+token);
        if (convertView == null) {
            convertView = vi.inflate(R.layout.chat_list_layout, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.sender_name.setText(chatMessage.getSenderName());
        holder.txtMessage.setText(chatMessage.getMessage());
        String fileInfo=null;
        SharedPreference sp = new SharedPreference();
//      final List<String> files = chatMessage.getFileList();
        if(chatMessage.getFileList()!=null) {
            ConnectAPIs connectAPIs = new ConnectAPIs("http://" + ip + ":8065/api/v1/files/get_info/" + chatMessage.getFileList(), token);
            InputStream isr = connectAPIs.getData();
//            Log.v("ISR", "ISR:::" + isr);
//            try {
//                holder.txtAttachmentDetails.setVisibility(View.VISIBLE);
//                holder.imgDownloads.setVisibility(View.VISIBLE);
//                holder.imgImages.setVisibility(View.VISIBLE);
//                if(file_info_Result!=null){
//                    Log.v("FILE_INFO_RESULT","FILE_INFO_RESULT::"+file_info_Result);
//                    String file_info[]=chatMessage.getFileList().split("/");
//                    JSONObject jsonfileInfo = new JSONObject(InpuStreamConversion.convertInputStreamToString(isr));
//                    String file_name = jsonfileInfo.getString("filename");
//                    int lastOccurance = file_name.lastIndexOf('/');
//                    only_filename = file_name.substring(lastOccurance + 1);
//                    fileInfo = only_filename + " \n" + InpuStreamConversion.humanReadableByteCount(Long.parseLong(jsonfileInfo.getString("size")), true);
//                    holder.txtAttachmentDetails.setText(""+file_info[4]);
//
//                }
                OkHttpClient picassoClient = new OkHttpClient();
                picassoClient.networkInterceptors().add(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request newRequest = chain.request().newBuilder()
                                .addHeader("Content-Type", "application/json")
                                .addHeader("Accept", "application/json")
                                .addHeader("Authorization", "Bearer " + token)
                                .build();
                        return chain.proceed(newRequest);
                    }
                });

                Picasso.with(context).setIndicatorsEnabled(true);
                new Picasso.Builder(context).memoryCache(new LruCache(context)).loggingEnabled(BuildConfig.DEBUG)
                        .indicatorsEnabled(BuildConfig.DEBUG)
                        .downloader(new OkHttpDownloader(picassoClient)).build()
                        .load("http://"+ip+":8065/api/v1/files/get"+chatMessage.getFileList())
                        .resize(600,300).error(R.drawable.place_holder)
                        .into(holder.imgImages);
//
//            } catch (Exception e) {
//                System.out.println("JSON Exception in FileAdapter: " + e.toString());
//                holder.txtAttachmentDetails.setText(null);
//            }
            holder.imgDownloads.setTag(fileInfo);
            holder.imgDownloads.setOnClickListener(this);
        }else{
            holder.txtAttachmentDetails.setVisibility(View.GONE);
            holder.imgDownloads.setVisibility(View.GONE);
            holder.imgImages.setVisibility(View.GONE);
        }

        holder.dateInfo.setText(chatMessage.getDate());
        convertView.setBackgroundColor(selectedItemIds.get(position) ? 0x9934B5E4 : Color.TRANSPARENT);
        return convertView;
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
    public static Bitmap showThumbnail(Bitmap bitmap){
        Bitmap resized = ThumbnailUtils.extractThumbnail(bitmap, 200, 200);
        return resized;
    }
    public static Bitmap getResizedBitmap(Bitmap image, int maxSize) {
        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float)width / (float) height;
        if (bitmapRatio > 0) {
            width = maxSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxSize;
            width = (int) (height * bitmapRatio);
        }
        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public static Bitmap getBitmapFromURL(String src,String token) {

        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setUseCaches(true);
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
//            Bitmap newBitmap=Bitmap.createScaledBitmap(myBitmap,myBitmap.getWidth(),myBitmap.getHeight(),false);

//            newBitmap=getResizedBitmap(myBitmap,100);
            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }



    public void add(ChatMessage message) {
        chatMessages.add(message);
    }

    public void add(List<ChatMessage> messages) {
        chatMessages.addAll(messages);
    }

    public void remove(int position){
        chatMessages.remove(position);
        notifyDataSetChanged();
    }
    /***********************************************************/
    public void toggleSelection(int position) {
        selectView(position, !selectedItemIds.get(position));
    }

    public void selectView(int position, boolean value) {
        if (value)
            selectedItemIds.put(position, value);
        else
            selectedItemIds.delete(position);

        notifyDataSetChanged();
    }

    public void removeSelection() {
        selectedItemIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }

    public int getSelectedCount() {
        return selectedItemIds.size();
    }

    public SparseBooleanArray getSelectedIds() {
        return selectedItemIds;
    }

    private ViewHolder createViewHolder(View v) {
        ViewHolder holder = new ViewHolder();
        holder.txtMessage = (TextView) v.findViewById(R.id.txtMessage);
        holder.sender_name = (TextView) v.findViewById(R.id.sender);
        holder.content = (LinearLayout) v.findViewById(R.id.content);
        holder.imgDownloads=(ImageView)v.findViewById(R.id.imgDownloads);
//        holder.txtAttachmentDetails=(TextView)v.findViewById(R.id.txtAttachmentDetails);
        holder.contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBackground);
        holder.dateInfo = (TextView) v.findViewById(R.id.dateInfo);
        holder.imgImages=(ImageView)v.findViewById(R.id.imgImages);
//        holder.fileList = (ListView) v.findViewById(R.id.fileList);
        return holder;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imgDownloads:
                Log.e("VALUE","TOKEN:::"+token);
                String filePath = "http://"+ip+":8065/api/v1/files/get/"+v.getTag()+"?session_token_index=0";

                DownloadFiles downloadFiles = new DownloadFiles(filePath,context,token);
                downloadFiles.execute(only_filename);
                break;
        }

    }

    private static class ViewHolder {
        public TextView txtMessage;
        public TextView sender_name;
        public TextView dateInfo,txtAttachmentDetails;
        public LinearLayout contentWithBG;
        public LinearLayout content;
        public ImageView imgDownloads;
        public ImageView imgImages;
        public ListView fileList;
    }

}

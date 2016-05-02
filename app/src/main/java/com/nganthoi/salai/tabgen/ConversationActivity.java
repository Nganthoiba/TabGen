package com.nganthoi.salai.tabgen;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
//import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import Channel.Channel;
import Channel.GetChannelDetails;
import ListenerClasses.ListviewListeners;
import Utils.FileUtils;
import Utils.Methods;
import Utils.SimpleDividerItemDecoration;
import chattingEngine.ChatAdapter;
import chattingEngine.ChatConversationAdapter;
import chattingEngine.ChatMessage;
import connectServer.DownloadResultReceiver;
import connectServer.FileInfoService;
import customDialogManager.CustomDialogManager;
import io.codetail.animation.SupportAnimator;
import io.codetail.animation.ViewAnimationUtils;
import io.codetail.widget.RevealFrameLayout;
import readData.ReadFile;
import sharePreference.SharedPreference;
import connectServer.ConnectAPIs;


public class ConversationActivity extends AppCompatActivity implements View.OnClickListener,View.OnLongClickListener,ListviewListeners{
    private static final int REQUEST_CODE=111;
    public static final int UPLOAD_REQUEST_CODE=112;
    public static final int RESULT_CODE=113;
    public static final int REQUEST_CODE_CAMERA=114;
    SupportAnimator animator_reverse;
    RevealFrameLayout revealFrameLayout;
    LinearLayout reveal_items,llAudios,llVideos,llPhotos;
    private Toolbar toolbar;
    private ImageView writeImageButton,imgSentMessages;
    private TextView channel_label;
    private ImageView backButton,pickImageFile,imgPhotos,imgVideos,imgAudios,imgDocuments,imgCamera,cameraImageButton;//,imgCamera,conv_Icon;
    private RecyclerView messagesContainerRecyclerview;
    private EditText messageEditText;
//    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;
    private SharedPreference sharedPreference;
    private Context context=this;
    private String channel_id="",user_id,token,last_timetamp="000000000",extra_info,users_info,copied_msg=null,channel_title;
    private String file_path=null;
    private String ip;
    private HttpURLConnection conn=null;
    Uri cameraUri;
    private Thread currentMessageTaskThread;
    public Boolean interrupt=false;
    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy' at 'h:mm a");
    private JSONArray filenames=null;// A JSON variable that contains list of file names returned from the mattermost APIs
    private ProgressDialog progressDialog;
    private JSONObject extraInfoObj,all_users;
    //private JSONArray members;
    private Activity activity=this;
    private ActionMode mActionMode;
    ChatConversationAdapter adapter;
    boolean hidden=true;
    int visibleItemCount,totalItemCount,pastVisiblesItems;
    int firstVisibleInListview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        toolbar = (Toolbar) findViewById(R.id.toolbarConversation);
        setSupportActionBar(toolbar);
        Intent intent = getIntent();
        channel_title = intent.getStringExtra(ChatFragment.CHANNEL_NAME);
        String team_name = intent.getStringExtra(ChatFragment.TEAM_NAME);
        initComponent();
        sharedPreference = new SharedPreference();
        token=sharedPreference.getTokenPreference(context);
        GetChannelDetails channelDetails = new GetChannelDetails();
        Channel channel = channelDetails.getChannel(team_name,channel_title,context);
        channel_id=channel.getChannel_id();
        System.out.println("Team Name: "+team_name+" Channel Title: "+channel_title+" ---> Channel Id: "+channel_id+"\nToken Id: "+token);
        String user_details=sharedPreference.getPreference(context);
        try{
            JSONObject jObj = new JSONObject(user_details);
            user_id=jObj.getString("id");
        }catch(Exception e){
            System.out.println("Unable to read user ID: "+e.toString());
        }
        ip = sharedPreference.getServerIP_Preference(context);//getting ip
        //last_timetamp="1456185600000";
        Thread loadHistory = new Thread(){
            @Override
            public void run(){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /*Joining the Channel for the first time*/
                        ConnectAPIs joinChannel = new ConnectAPIs("http://"+ip+":8065/api/v1/channels/"+channel_id+"/join",token);
                        String joinChannelInfo = convertInputStreamToString(joinChannel.getData());
                        System.out.println("Join Channel Result: "+joinChannelInfo);
                        /*******************************************************************/
                        /*** Getting extra information about the current channel ***/
                        /*ConnectAPIs connApis = new ConnectAPIs("http://"+ip+":8065//api/v1/channels/"+channel_id+"/extra_info",token);
                        extra_info = convertInputStreamToString(connApis.getData());
                        System.out.println("Extra Information: "+extra_info);
                        try{
                            extraInfoObj = new JSONObject(extra_info);
                            members=extraInfoObj.getJSONArray("members");
                        }catch(Exception e){
                            System.out.println("unable to get user extra information");
                        }*/

                        /********************************************************************/
                        new GetMessageHistoryTask().execute("http://"+ip+
                                ":8065//api/v1/channels/"+channel_id+
                                "/posts/0/60");
                    }
                });
            }
        };
        //loadHistory.start();
        currentMessageTaskThread = new Thread(){
            @Override
            public void run(){
                try{
                    while(!isInterrupted() || !interrupt){
                        Thread.sleep(6000);
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){

                                if(last_timetamp!=null||last_timetamp=="000000000") {
                                    System.out.println("Last timestamp: "+last_timetamp);
                                    new GetCurrentMessageTask().execute("http://"+ip+
                                            ":8065//api/v1/channels/"+channel_id+
                                            "/posts/"+last_timetamp);
                                }else
                                    System.out.println("latest timestamp is null, no chat history for this channel");
                            }
                        });
                    }
                }catch(InterruptedException e){
                    System.out.println("Interrupted Exception: "+e.toString());
                }
            }
        };
        currentMessageTaskThread.start();
        loadHistory.start();
    }

//-----initializing the xml components
    public void initComponent(){
        /*** labels in the action abar of the activity_conversation ***/
        imgSentMessages=(ImageView)findViewById(R.id.imgSentMessages);
        imgSentMessages.setOnClickListener(this);
        reveal_items=(LinearLayout)findViewById(R.id.reveal_items);
        channel_label = (TextView) findViewById(R.id.channel_name);
        channel_label.setText(channel_title);
        messageEditText = (EditText) findViewById(R.id.messageEditText);
        progressDialog = new ProgressDialog(context);
        backButton = (ImageView) toolbar.findViewById(R.id.backButton);
        messagesContainerRecyclerview = (RecyclerView) findViewById(R.id.messagesContainerRecyclerview);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(context);
//        layoutManager.setReverseLayout(true);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        messagesContainerRecyclerview.setLayoutManager(layoutManager);
//        messagesContainerRecyclerview.addItemDecoration(new SimpleDividerItemDecoration(this));
        writeImageButton = (ImageView) findViewById(R.id.writeImageButton);
        writeImageButton.setOnClickListener(this);
        cameraImageButton=(ImageView)findViewById(R.id.cameraImageButton);
        cameraImageButton.setOnClickListener(this);
        //setting Chat adapter
        adapter = new ChatConversationAdapter(ConversationActivity.this, new ArrayList<ChatMessage>());
        messagesContainerRecyclerview.setAdapter(adapter);
        messageEditText.setOnLongClickListener(this);
        pickImageFile = (ImageView) findViewById(R.id.pickImageFile);
        pickImageFile.setOnClickListener(this);

        backButton.setOnClickListener(this);
        imgPhotos=(ImageView)findViewById(R.id.imgPhotos);
        imgPhotos.setOnClickListener(this);
        imgAudios=(ImageView)findViewById(R.id.imgAudios);
        imgAudios.setOnClickListener(this);
        imgVideos=(ImageView)findViewById(R.id.imgVideos);
        imgVideos.setOnClickListener(this);
        imgDocuments=(ImageView)findViewById(R.id.imgDocuments);
        imgDocuments.setOnClickListener(this);
        imgCamera=(ImageView)findViewById(R.id.imgCamera);
        imgCamera.setOnClickListener(this);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length()>0){
                    imgSentMessages.setVisibility(View.VISIBLE);
                    writeImageButton.setVisibility(View.GONE);
                }else{
                    writeImageButton.setVisibility(View.VISIBLE);
                    imgSentMessages.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }


//----OnClick Listeners
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.imgSentMessages:
                Methods.toastShort("CLICKED",this);
                String messageText = messageEditText.getText().toString();
                if (TextUtils.isEmpty(messageText)||messageText.trim().length()==0) {
                    return;
                }
                try {
                    JSONObject jsonObject = new JSONObject();

                    if(filenames!=null && filenames.length()>0){
                        jsonObject.put("filenames",filenames);
                    }
                    jsonObject.put("channel_id", channel_id);
                    jsonObject.put("root_id", "");
                    jsonObject.put("parent_id","");
                    jsonObject.put("Message", messageText);
                    sendMyMessage(jsonObject);
                    messageEditText.setText(" ");
                    filenames=null;
                } catch (Exception e) {
                    System.out.print("Message Sending failed: " + e.toString());
                    Snackbar.make(v, "Oops! Message Sending failed", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
                break;
            case R.id.backButton:
                onBackPressed();
                break;
            case R.id.writeImageButton:

                break;
            case R.id.cameraImageButton:
                hidePopupWindow();
                Intent intent5 = new Intent("android.media.action.IMAGE_CAPTURE");
                File file1 =FileUtils.getImageFile();
                cameraUri=Uri.fromFile(file1);
                intent5.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file1));
                startActivityForResult(intent5, REQUEST_CODE_CAMERA);
                break;
            case R.id.pickImageFile:
                if (hidden) {
                    reveal_items.setVisibility(View.VISIBLE);
                    hidden=false;
                }else{
                    reveal_items.setVisibility(View.GONE);
                    hidden=true;
                }
//                opentheAttachmentMenu();
                break;
            case R.id.imgAudios:
                hidePopupWindow();
                Intent intent1 = new Intent(Intent.ACTION_PICK);
                intent1.setType("audio/*");
                intent1.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent1.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent1, "Select a file from music player"), REQUEST_CODE);
                break;
            case R.id.imgVideos:
                hidePopupWindow();
                Intent intent3 = new Intent(Intent.ACTION_PICK,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent3.setType("video/*");
                startActivityForResult(Intent.createChooser(intent3, "Select a file from vidoes"), REQUEST_CODE);
                break;
            case R.id.imgPhotos:
                hidePopupWindow();
                Intent intent2 = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent2.setType("image/*");
                startActivityForResult(Intent.createChooser(intent2, "Select a file photos"), REQUEST_CODE);
                break;
            case R.id.imgDocuments:
                hidePopupWindow();
                Intent intent4 = new Intent(Intent.ACTION_PICK,
                        MediaStore.Files.getContentUri("application/*"));
                intent4.setType("application/*");
                intent4.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent4, "Select a file from documents"), REQUEST_CODE);
                break;
            case R.id.imgCamera:
                hidePopupWindow();
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                File file = FileUtils.getImageFile();
                cameraUri=Uri.fromFile(file);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                startActivityForResult(intent, REQUEST_CODE_CAMERA);
                break;
        }
    }

//-----OnLongClick Listener for EditText
    @Override
    public boolean onLongClick(View v) {

        switch (v.getId()){
            case R.id.messageEditText:
                if(copied_msg!=null){
                    messageEditText.setText(copied_msg);
                    Toast.makeText(context, "message pasted", Toast.LENGTH_SHORT).show();
                    return true;
                }
                break;
        }
        return false;
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.conversation_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id){
            case R.id.aeroplane:

//                int cy = (reveal_items.getTop() + reveal_items.getBottom())/2;
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    public void opentheAttachmentMenu(){

        int cx = (reveal_items.getLeft() + reveal_items.getRight());
        int cy = reveal_items.getBottom();
        int radius = Math.max(reveal_items.getWidth(), reveal_items.getHeight());
        SupportAnimator animator =
                ViewAnimationUtils.createCircularReveal(reveal_items, cx, cy, radius, 0);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setDuration(1000);
        animator_reverse = animator.reverse();
        if (hidden) {
            reveal_items.setVisibility(View.VISIBLE);
            animator.start();
            hidden = false;
        } else {
            animator_reverse.addListener(new SupportAnimator.AnimatorListener() {

                @Override
                public void onAnimationStart() {
                    Methods.toastShort("ANIMATION START", ConversationActivity.this);
                }

                @Override
                public void onAnimationEnd() {
                    Methods.toastShort("ANIMATION END", ConversationActivity.this);
                    reveal_items.setVisibility(View.INVISIBLE);
                    hidden = true;
                }

                @Override
                public void onAnimationCancel() {
                    Methods.toastShort("ANIMATION CANCEL", ConversationActivity.this);
                }

                @Override
                public void onAnimationRepeat() {
                    Methods.toastShort("ANIMATION REPEAT",ConversationActivity.this);
                }
            });
            animator_reverse.start();
        }

    }
    public void hidePopupWindow(){
        if(!hidden) {
                    reveal_items.setVisibility(View.GONE);
            hidden=false;
        }else{
            reveal_items.setVisibility(View.VISIBLE);
            hidden=true;
        }
    }
    @Override
    public void onBackPressed(){
        progressDialog.setCancelable(true);
        try{
            if(currentMessageTaskThread!=null){
                currentMessageTaskThread.interrupt();
                interrupt=true;
            }
        }catch(Exception e){
            System.out.println("Interrupt Exception: "+e.toString());
        }
        super.onBackPressed();
        finish();
    }
    private static String getMimeType(String fileUrl) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl);
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){

//        if(resultCode!=RESULT_OK || data==null) return;
        Uri fileUri;
        //ReadFile readFile = new ReadFile();
        switch(requestCode){

            case REQUEST_CODE: //file_path = readFile.getFilePath(fileUri,context);
                if(data.getData()!=null) {
                    fileUri = data.getData();
                    file_path = ReadFile.getPath(fileUri, context);
                    String mimetype = getMimeType(file_path);

                    Methods.toastShort(mimetype, this);
                    if (file_path != null) {
                        //System.out.println("File has been selected: "+file_path);
//                    Toast.makeText(context, "You have selected: "+file_path, Toast.LENGTH_SHORT).show();
                        if (file_path.contains(".mp3")) {
                            new Thread(new Runnable() {
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            UploadFile uploadFile = new UploadFile(file_path, "http://" + ip + ":8065/api/v1/files/upload");
                                            uploadFile.execute();

                                        }
                                    });
                                }
                            }).start();

                        } else if (mimetype.contains("application/")) {
                            new Thread(new Runnable() {
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            UploadFile uploadFile = new UploadFile(file_path, "http://" + ip + ":8065/api/v1/files/upload");
                                            uploadFile.execute();

                                        }
                                    });
                                }
                            }).start();
                        } else if (mimetype.contains("image/")) {

                            Intent intent = new Intent(ConversationActivity.this, UploadActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("IP_VALUE", ip);
                            bundle.putString("FILE_PATH", file_path);
                            bundle.putString("TOKEN", token);
                            bundle.putString("CHANNEL_ID", channel_id);
                            bundle.putString("TYPE", "IMAGE");
                            intent.putExtras(bundle);
                            startActivityForResult(intent, UPLOAD_REQUEST_CODE);
                        } else if (mimetype.contains("video/")) {
//
                            Intent intent = new Intent(ConversationActivity.this, UploadActivity.class);
                            Bundle bundle = new Bundle();
                            bundle.putString("IP_VALUE", ip);
                            bundle.putString("FILE_PATH", file_path);
                            bundle.putString("TOKEN", token);
                            bundle.putString("CHANNEL_ID", channel_id);
                            bundle.putString("TYPE", "VIDEO");
                            intent.putExtras(bundle);
                            startActivityForResult(intent, UPLOAD_REQUEST_CODE);
                        }
                        Log.e("CONVERSATION", "ONACTIVITY_RESULT.");
                    }
                }
                break;
            case REQUEST_CODE_CAMERA:
                if(cameraUri!=null) {
                    Intent intent = new Intent(ConversationActivity.this, UploadActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("IP_VALUE", ip);
                    bundle.putString("FILE_PATH", cameraUri.getPath());
                    bundle.putString("TOKEN", token);
                    bundle.putString("CHANNEL_ID", channel_id);
                    bundle.putString("TYPE", "CAMERA");
                    intent.putExtras(bundle);
                    startActivityForResult(intent, UPLOAD_REQUEST_CODE);
                }
                break;
            case UPLOAD_REQUEST_CODE:
                try{
                String result=data.getStringExtra("RESULT_STRING");
                String caption=data.getStringExtra("CAPTION");
                JSONObject fileObject = new JSONObject(result);
                filenames=fileObject.getJSONArray("filenames");
//                String messageText = messageEditText.getText().toString();
                    JSONObject jsonObject = new JSONObject();

                    if(filenames!=null && filenames.length()>0){
                        jsonObject.put("filenames",filenames);
                    }
                    jsonObject.put("channel_id", channel_id);
                    jsonObject.put("root_id", "");
                    jsonObject.put("parent_id","");
                    jsonObject.put("Message", caption);
                    sendMyMessage(jsonObject);
                    filenames=null;
                } catch (Exception e) {
                    System.out.print("Message Sending failed: " + e.toString());
                }
                break;
            default:
                Toast.makeText(context, "Invalid request code. You haven't selected any file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getUsernameById(String user_id){
        String username=null;
        /*if(members!=null){
            try{
                for(int i=0;i<members.length();i++){
                    JSONObject users = members.getJSONObject(i);
                    if(user_id.equals(users.getString("id"))){
                        username = users.getString("username");
                        break;
                    }
                }
            }catch(JSONException e){
                System.out.println("Unable to get Username in getUsernameById: "+e.toString());
                username=null;
            }
        }*/
        if(all_users!=null){
            try{
                JSONObject jobj = all_users.getJSONObject(user_id);
                if(jobj.getString("first_name").length()!=0 && jobj.getString("first_name")!=null){
                    username = jobj.getString("first_name");
                }
                else{
                    username = jobj.getString("username");
                }
            }
            catch(Exception e){
                username = null;
            }
        }
        else username="Unknown";
        return username;
    }
    private void sendMyMessage(JSONObject jsonMsg) {
        String link = "http://"+ip+":8065/api/v1/channels/"+channel_id+"/create";
        String response;
        try{
            ConnectAPIs messageAPI = new ConnectAPIs(link,token);
            response=convertInputStreamToString(messageAPI.sendData(jsonMsg));
            if(response!=null ){
                if(messageAPI.responseCode==200){
                    ChatMessage chatMessage = new ChatMessage();
                    //chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                    chatMessage.setMe(true);
                    chatMessage.setSenderName("Me");
                    //messageEditText.setText("");
                    System.out.println("Sending result: " + response);
                    Log.v("MESSAGE","RESPONSE::"+response);
                    try{
                        JSONObject json_obj= new JSONObject(response);
                        chatMessage.setId(json_obj.getString("id"));
                        chatMessage.setMessage(json_obj.getString("message"));
                        last_timetamp = json_obj.getString("create_at");
                        Long timestamp = Long.parseLong(last_timetamp);
                        Date date = new Date(timestamp);
                        chatMessage.setDate(simpleDateFormat.format(date));

                        JSONArray files = json_obj.getJSONArray("filenames");
                        if(files.length()!=0){
                            chatMessage.setFileList(files.getString(0));
                        }

                        displayMessage(chatMessage);
                    }catch(Exception e){
                        System.out.print("Chat Exception: "+e.toString());
                    }
                    file_path=null;
                }
                else{
                    try{
                        JSONObject json_obj= new JSONObject(response);
                        Toast.makeText(context,""+json_obj.get("message"),Toast.LENGTH_LONG).show();
                    }catch(Exception e){
                        System.out.print("Chat Exception: "+e.toString());
                    }
                }

            }else
                Toast.makeText(context,"Failed to send message",Toast.LENGTH_LONG).show();

        }catch(Exception e){
            System.out.println("Sending error: "+e.toString());
        }
    }

    public void displayMessage(ChatMessage message) {
        adapter.add(message);
        adapter.notifyDataSetChanged();
        scroll();
    }

    private void scroll() {
        messagesContainerRecyclerview.scrollToPosition(adapter.getItemCount()-1);

    }

    @Override
    public void updateListview(String filename, boolean flag) {
        if(flag){
            Methods.toastShort("CALLBACK",this);
            try{
                JSONObject fileObject = new JSONObject(filename);
//                if(serverRespCode==200) {
                    //assign the list of filenames in the global JSON array filename
                    filenames=fileObject.getJSONArray("filenames");
                    for (int i = 0; i < filenames.length(); i++) {
                        Log.e("FILE::::", "file name: " + filenames.getString(i));

//                    }
                }
                sendMyMessage(fileObject);
                //end if statement
//                else{
////
//                }
            }catch(Exception e){
//
            }
        }
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


    public class UploadFile extends AsyncTask<Void, String, String>{
        URL connectURL;
        String serverRespMsg,file_upload_uri=null;
        HttpURLConnection httpURLConn = null;
        DataOutputStream dos = null;

        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        // "----------------------------13820696122345";
        int bytesRead, bytesAvailable, bufferSize;
        int serverRespCode;
        byte[] buffer;
        int maxBufferSize = 1024*1024;
        String fileLocation=null;
        InputStream isr=null;

        public UploadFile(String sourceFileUri,String serverUploadPath){
        Log.e("CONVERSATION","UPLOAD FILE");
            fileLocation = sourceFileUri;
            file_upload_uri = serverUploadPath;
        }
        @Override
        protected void onPreExecute(){
            Methods.showProgressDialog(ConversationActivity.this,"Please wait..");
        }
        @Override
        protected String doInBackground(Void... v){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            progressDialog.setCancelable(true);
            File sourceFile = new File(fileLocation);
            if(!sourceFile.isFile()){
                Toast.makeText(context, "Source file does not exist", Toast.LENGTH_SHORT).show();
                return null;
            }
            else{
                try{
                    FileInputStream fis = new FileInputStream(sourceFile);
                    connectURL = new URL(file_upload_uri);
                    httpURLConn = (HttpURLConnection) connectURL.openConnection();
                    httpURLConn.setDoInput(true);
                    httpURLConn.setDoOutput(true);
                    httpURLConn.setRequestMethod("POST");
                    httpURLConn.setRequestProperty("Connection", "Keep-Alive");
                    httpURLConn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    httpURLConn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" +boundary);
                    httpURLConn.setRequestProperty("Authorization", "Bearer " + token);
                    httpURLConn.setRequestProperty("files", fileLocation);
                    httpURLConn.setRequestProperty("channel_id", channel_id);
                    System.setProperty("http.keepAlive", "false");
                    httpURLConn.connect();
                    OutputStreamWriter osw = new OutputStreamWriter(httpURLConn.getOutputStream());
                    osw.write("files=" + fileLocation + "&channel_id=" + channel_id);
                    dos = new DataOutputStream(httpURLConn.getOutputStream());

                    dos.writeBytes(twoHyphens+boundary+lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"channel_id\""+lineEnd+lineEnd);
                    dos.writeBytes(channel_id+lineEnd);

                    dos.writeBytes(twoHyphens+boundary+lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"files\";filename=\""+fileLocation + "\"" + lineEnd);
                    dos.writeBytes(lineEnd);

                    /*
                    // Send parameter #1
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"param1\"" + lineEnd + lineEnd);
                    dos.writeBytes("foo1" + lineEnd);
                    // Send parameter #2
                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"param2\"" + lineEnd + lineEnd);
                    dos.writeBytes("foo2" + lineEnd);*/

                    //create a buffer of maximum size
                    bytesAvailable = fis.available();
                    bufferSize = Math.min(bytesAvailable,maxBufferSize);
                    buffer=new byte[bufferSize];

                    bytesRead = fis.read(buffer,0,bufferSize);
                    int total = bytesAvailable;
                    while(bytesRead>0){
                        publishProgress(""+(int)((total-bytesAvailable)*100)/total);
                        dos.write(buffer,0,bufferSize);
                        bytesAvailable = fis.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = fis.read(buffer,0,bufferSize);
                    }
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
                    osw.flush();
                    dos.flush();
                    serverRespCode = httpURLConn.getResponseCode();
                    serverRespMsg = httpURLConn.getResponseMessage();
                    System.out.println("File Upload Response: " + serverRespCode + " " + serverRespMsg);

                    if(serverRespCode==200){
                        //Toast.makeText(context,"Your file upload is successfully completed",Toast.LENGTH_LONG).show();
                        System.out.println("Your file upload is successfully completed");
                        isr = new BufferedInputStream(httpURLConn.getInputStream());
                        progressDialog.setCancelable(true);
                    }
                    else{
                        System.out.println("Oops! Your file upload is failed");
                        isr = new BufferedInputStream(httpURLConn.getErrorStream());
                        progressDialog.setCancelable(true);
                    }
                    fis.close();
                    dos.close();
                    osw.close();

                }catch(Exception e){
                    e.printStackTrace();
                    System.out.println("File Upload Exception here: " + e.toString());
                    progressDialog.setCancelable(true);
                    return null;
                }//end try catch
            }
            return convertInputStreamToString(isr);
        }
        protected void onProgressUpdate(String... progress){

        }
        @Override
        protected void onPostExecute(String result){
            Methods.closeProgressDialog();
            if(result!=null){
                try{
                    JSONObject fileObject = new JSONObject(result);
                    if(serverRespCode==200) {
                        filenames=fileObject.getJSONArray("filenames");
                        if(file_path.contains(".mp3")){
                            try{
                                filenames=fileObject.getJSONArray("filenames");
                                JSONObject jsonObject = new JSONObject();

                                if(filenames!=null && filenames.length()>0){
                                    jsonObject.put("filenames",filenames);
                                }
                                jsonObject.put("channel_id", channel_id);
                                jsonObject.put("root_id", "");
                                jsonObject.put("parent_id","");
                                jsonObject.put("Message", " ");
                                sendMyMessage(jsonObject);
                                filenames=null;
                            } catch (Exception e) {
                                Log.v("MESSAGE","Message Sending failed");
                                System.out.print("Message Sending failed: " + e.toString());
                            }
                        }
                        for (int i = 0; i < filenames.length(); i++) {

                            Log.e("FILE::::","file name: " + filenames.getString(i));
                        }

                    }else {
                        Methods.toastShort("Sorry! File is too large..",context);
                    }
                }catch(Exception e){
                    System.out.println("Unable to read file details: " + e.toString());

                }
            }
            else {
                System.out.println("Response is null");

            }
            //Toast.makeText(context,result,Toast.LENGTH_LONG).show();
        }
    }//end of class UploadFile

    //class for getting instant message
    class GetCurrentMessageTask extends AsyncTask<String,Void,String>{
        InputStream isr=null;
        HttpURLConnection conn;
        URL api_url;
        int responseCode=-1;
        String respMsg;
        String resp=null;
        @Override
        protected String doInBackground(String... messageUrl){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try{
                api_url = new URL(messageUrl[0]);
                conn = (HttpURLConnection) api_url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.connect();
                responseCode = conn.getResponseCode();
                respMsg = conn.getResponseMessage();
                System.out.println("Response Code: " + responseCode + "\nResponse message: " + respMsg);
                if(responseCode == 200)/*HttpURLConnection.HTTP_OK*/{
                    isr = new BufferedInputStream(conn.getInputStream());
                }
                else {
                    isr = new BufferedInputStream(conn.getErrorStream());
                }
                resp = convertInputStreamToString(isr);
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("Exception in getMessage(): " + e.toString());
                return null;
            }
            System.out.println(resp);
            return resp;
        }
        @Override
        protected void onPostExecute(String resp){
            if(resp!=null && responseCode==200) {
                try {

                    JSONObject jObj1 = new JSONObject(resp);
                    JSONArray jsonArray = jObj1.getJSONArray("order");
                    JSONObject jObj2;
                    if (jsonArray.length() > 0) {
                        jObj2 = jObj1.getJSONObject("posts");
                        int i = 0;
                        String messageDate;
                        while (i < jsonArray.length()) {
                            //System.out.println(jsonArray.getString(i));
                            JSONObject jObj3 = jObj2.getJSONObject(jsonArray.getString(i));
                            System.out.println("Id: " + jObj3.getString("id") + " Message: " + jObj3.getString("message"));
                            messageDate = "" + jObj3.getString("create_at");
                            System.out.println("Message Date: " + messageDate);
                            //!messageDate.equals(last_timetamp)
                            if(Long.parseLong(messageDate)>Long.parseLong(last_timetamp) && jObj3.getLong("delete_at")==0) {//it means if the message is new, which is indicated by the last timestamp
                                ChatMessage currentMsg = new ChatMessage();
                                currentMsg.setId(jObj3.getString("id"));
                                currentMsg.setMessage("" + jObj3.getString("message"));
                                Long timeStamp = Long.parseLong(messageDate);
                                Date date = new Date(timeStamp);
                                currentMsg.setDate(simpleDateFormat.format(date));

                                /*If the post contains files*/
                                JSONArray files = jObj3.getJSONArray("filenames");
                                if(files.length()>0)
                                    currentMsg.setFileList(files.getString(0));
//                                currentMsg.setFileList(files);

                                if (user_id.equals("" + jObj3.getString("user_id"))) {
                                    currentMsg.setMe(true);
                                    currentMsg.setSenderName("Me");
                                } else {
                                    currentMsg.setMe(false);
                                    getUsernameById(jObj3.getString("user_id"));
                                    currentMsg.setSenderName(getUsernameById(jObj3.getString("user_id")));
                                }
                                displayMessage(currentMsg);
                            }//otherwise dont create the message
                            if(Long.parseLong(last_timetamp)< Long.parseLong(messageDate))
                                last_timetamp = messageDate;
                            i++;
                        }//end while loop
                    }
                } catch (Exception e) {
                    System.out.println("Error in parsing JSON: " + e.toString());
                }
            }//end if
        }//end on post execution
    }//end of GetCurrentMessageTask class
    //class for getting instant message
    class GetMessageHistoryTask extends AsyncTask<String,Void,String>{
        InputStream isr=null;
        HttpURLConnection conn;
        URL api_url;
        int responseCode=-1;
        String respMsg;
        String resp=null;

        @Override
        protected void onPreExecute(){
            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... messageUrl){
            /****New Code for Getting all user information from server********/
            ConnectAPIs getUsers = new ConnectAPIs("http://"+ip+":8065//api/v1/users/profiles",token);
            users_info = convertInputStreamToString(getUsers.getData());
            System.out.println("Users: " + users_info);
            try{
                all_users = new JSONObject(users_info);
            }
            catch(Exception e){
                System.out.println("unable to get user information");
            }
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try{
                api_url = new URL(messageUrl[0]);
                conn = (HttpURLConnection) api_url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.connect();
                responseCode = conn.getResponseCode();
                respMsg = conn.getResponseMessage();
                System.out.println("Response Code: " + responseCode + "\nResponse message: " + respMsg);
                if(responseCode == 200)/*HttpURLConnection.HTTP_OK*/{
                    isr = new BufferedInputStream(conn.getInputStream());
                }
                else {
                    isr = new BufferedInputStream(conn.getErrorStream());
                }
                resp = convertInputStreamToString(isr);
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("Exception in getMessage(): " + e.toString());
                return null;
            }
            System.out.println(resp);
            return resp;
        }
        @Override
        protected void onPostExecute(String resp){
            Log.v("RESPONSE", "RESPONSE CONVERSATION:::" + resp);
            if(resp!=null && responseCode==200) {
                try {
                    JSONObject jObj1 = new JSONObject(resp);
                    JSONArray jsonArray = jObj1.getJSONArray("order");
                    JSONObject jObj2;
                    if (jsonArray.length() > 0) {

                        jObj2 = jObj1.getJSONObject("posts");
                        int i = jsonArray.length()-1;
                        String messageDate;
                        System.out.print("length : "+i);
                        while (i>=0) {
                            //System.out.println(jsonArray.getString(i));
                            JSONObject jObj3 = jObj2.getJSONObject(jsonArray.getString(i));
                            System.out.println("Id: " + jObj3.getString("id") + " Message: " + jObj3.getString("message"));
                            messageDate = "" + jObj3.getString("create_at");
                            System.out.println("Message Date: " + messageDate);
                            //!messageDate.equals(last_timetamp)
                            if(jObj3.getLong("delete_at")==0) {//it means if the message is new, which is indicated by the last timestamp
                                ChatMessage currentMsg = new ChatMessage();
                                currentMsg.setId(jObj3.getString("id"));
                                currentMsg.setMessage("" + jObj3.getString("message"));
                                Long timeStamp = Long.parseLong(messageDate);
                                Date date = new Date(timeStamp);
                                currentMsg.setDate(simpleDateFormat.format(date));

                                /*If the post contains files*/
                                JSONArray files = jObj3.getJSONArray("filenames");
                                if(files.length()>0) {
                                    Log.v("FILE","FILE::"+files.getString(0));
                                    currentMsg.setFileList(files.getString(0));
                                }

                                if (user_id.equals("" + jObj3.getString("user_id"))) {
                                    currentMsg.setMe(true);
                                    currentMsg.setSenderName("Me");
                                } else {
                                    currentMsg.setMe(false);
                                    getUsernameById(jObj3.getString("user_id"));
                                    currentMsg.setSenderName(getUsernameById(jObj3.getString("user_id")));
                                }
                                displayMessage(currentMsg);
                                scroll();
                            }//otherwise dont create the message
                            if(Long.parseLong(last_timetamp)< Long.parseLong(messageDate))
                                last_timetamp = messageDate;
                            i--;
                        }//end while loop
                    }
                } catch (Exception e) {
                    System.out.println("Something is wrong error in parsing JSON: " + e.toString());
                }
            }//end if
            progressDialog.dismiss();
        }//end on post execution
    }//end of GetMessageHistoryTask class

    //class for contextual action bar
    private class ActionModeCallback implements ActionMode.Callback{
        ChatMessage msg;
        int position;
        private ActionModeCallback(int msgPosition){
            msg=adapter.getItem(msgPosition);
            position = msgPosition;
        }
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu,menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch(item.getItemId()){
                case R.id.copy: copied_msg = msg.getMessage();
                    if(copied_msg!=null) Toast.makeText(getBaseContext(),"Message copied",Toast.LENGTH_SHORT).show();
                    mActionMode.finish();
                    break;
                case R.id.delete:ConnectAPIs deleteMsg =
                        new ConnectAPIs("http://"+ip+":8065/api/v1/channels/"+channel_id+"/post/"+msg.getId()+"/delete",token);
                    InputStream isr = deleteMsg.getData();
                    String result = deleteMsg.convertInputStreamToString(isr);
                    if(deleteMsg.responseCode==200){
                        Toast.makeText(getApplicationContext(),"Message deleted",Toast.LENGTH_LONG).show();
                        adapter.remove(position);
                    }
                    else{
                        try{
                            JSONObject jobj = new JSONObject(result);
                            Toast.makeText(getApplicationContext(),jobj.getString("message"),Toast.LENGTH_LONG).show();
                        }catch(Exception e){
                            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
                        }
                    }
                    mActionMode.finish();
                    break;
                case R.id.forward:
                default:
                    CustomDialogManager customDialogManager = new CustomDialogManager(context,"Under Development",
                            "We are developing the appropriate action for this button",false);
                    customDialogManager.showCustomDialog();
                    mActionMode.finish();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            adapter.removeSelection();
            mActionMode=null;
        }
    }

    public class SampleScrollListener extends RecyclerView.OnScrollListener {
        private final Context context;

        public SampleScrollListener(Context context) {
            this.context = context;
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
        }

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            final Picasso picasso = Picasso.with(context);
            if(recyclerView.getScrollState()==newState){
                picasso.resumeTag(context);
            }else{
                picasso.pauseTag(context);
            }
//            if (newState == SCROLL_STATE_IDLE || newState == SCROLL_STATE_TOUCH_SCROLL) {
//                picasso.resumeTag(context);
//            } else {
//                picasso.pauseTag(context);
//            }
        }

    }


}


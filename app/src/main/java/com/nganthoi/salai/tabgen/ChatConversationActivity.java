//package com.nganthoi.salai.tabgen;
//
//import android.app.Activity;
//import android.app.ProgressDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.RecyclerView;
//import android.support.v7.widget.Toolbar;
//import android.view.ActionMode;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//
//import java.net.HttpURLConnection;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//
//import Channel.Channel;
//import Channel.GetChannelDetails;
//import Utils.InpuStreamConversion;
//import chattingEngine.ChatAdapter;
//import chattingEngine.ChatConversationAdapter;
//import chattingEngine.ChatMessage;
//import connectServer.ConnectAPIs;
//import io.codetail.animation.SupportAnimator;
//import io.codetail.widget.RevealFrameLayout;
//import sharePreference.SharedPreference;
//
///**
// * Created by atul on 13/4/16.
// */
//public class ChatConversationActivity extends AppCompatActivity implements View.OnClickListener {
//    private static final int REQUEST_CODE=111;
//    public static final int UPLOAD_REQUEST_CODE=112;
//    public static final int RESULT_CODE=113;
//    public static final int REQUEST_CODE_CAMERA=114;
//    SupportAnimator animator_reverse;
//    RevealFrameLayout revealFrameLayout;
//    LinearLayout reveal_items,llAudios,llVideos,llPhotos;
//    private Toolbar toolbar;
//    private ImageView writeImageButton;
//    private TextView channel_label;
//    private ImageView backButton,pickImageFile,imgPhotos,imgVideos,imgAudios,imgDocuments,imgCamera,cameraImageButton;//,imgCamera,conv_Icon;
//    private RecyclerView messagesContainerRecyclerview;
//    private EditText messageEditText;
//    private ChatConversationAdapter adapter;
//    private ArrayList<ChatMessage> chatHistory;
//    private SharedPreference sharedPreference;
//    private Context context=this;
//    private String channel_id="",user_id,token,last_timetamp="000000000",extra_info,copied_msg=null,channel_title;
//    private String file_path=null;
//    private String ip;
//    private HttpURLConnection conn=null;
//    Uri cameraUri;
//    private Thread currentMessageTaskThread;
//    public Boolean interrupt=false;
//    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy' at 'h:mm a");
//    private JSONArray filenames=null;// A JSON variable that contains list of file names returned from the mattermost APIs
//    private ProgressDialog progressDialog;
//    private JSONObject extraInfoObj;
//    private JSONArray members;
//    private Activity activity=this;
//    private ActionMode mActionMode;
//    boolean hidden=true;
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_conversation);
//        toolbar = (Toolbar) findViewById(R.id.toolbarConversation);
//        setSupportActionBar(toolbar);
//        Intent intent = getIntent();
//        channel_title = intent.getStringExtra(ChatFragment.CHANNEL_NAME);
//        String team_name = intent.getStringExtra(ChatFragment.TEAM_NAME);
//        initComponent();
//        sharedPreference = new SharedPreference();
//        token=sharedPreference.getTokenPreference(context);
//        GetChannelDetails channelDetails = new GetChannelDetails();
//        Channel channel = channelDetails.getChannel(team_name,channel_title,context);
//        channel_id=channel.getChannel_id();
//        System.out.println("Team Name: "+team_name+" Channel Title: "+channel_title+" ---> Channel Id: "+channel_id+"\nToken Id: "+token);
//        String user_details=sharedPreference.getPreference(context);
//        try{
//            JSONObject jObj = new JSONObject(user_details);
//            user_id=jObj.getString("id");
//        }catch(Exception e){
//            System.out.println("Unable to read user ID: "+e.toString());
//        }
//        ip = sharedPreference.getServerIP_Preference(context);//getting ip
//        //last_timetamp="1456185600000";
//        Thread loadHistory = new Thread(){
//            @Override
//            public void run(){
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        /*** Getting extra information about the current channel ***/
//                        ConnectAPIs connApis = new ConnectAPIs("http://"+ip+":8065//api/v1/channels/"+channel_id+"/extra_info",token);
//                        extra_info = InpuStreamConversion.convertInputStreamToString(connApis.getData());
//                        System.out.println("Extra Information: "+extra_info);
//                        try{
//                            extraInfoObj = new JSONObject(extra_info);
//                            members=extraInfoObj.getJSONArray("members");
//                        }catch(Exception e){
//                            System.out.println("unable to get user extra information");
//                        }
//                        new GetMessageHistoryTask().execute("http://"+ip+
//                                ":8065//api/v1/channels/"+channel_id+
//                                "/posts/0/60");
//                    }
//                });
//            }
//        };
//        //loadHistory.start();
//        currentMessageTaskThread = new Thread(){
//            @Override
//            public void run(){
//                try{
//                    while(!isInterrupted() || !interrupt){
//                        Thread.sleep(6000);
//                        runOnUiThread(new Runnable(){
//                            @Override
//                            public void run(){
//
//                                if(last_timetamp!=null||last_timetamp=="000000000") {
//                                    System.out.println("Last timestamp: "+last_timetamp);
//                                    new GetCurrentMessageTask().execute("http://"+ip+
//                                            ":8065//api/v1/channels/"+channel_id+
//                                            "/posts/"+last_timetamp);
//                                }else
//                                    System.out.println("latest timestamp is null, no chat history for this channel");
//                            }
//                        });
//                    }
//                }catch(InterruptedException e){
//                    System.out.println("Interrupted Exception: "+e.toString());
//                }
//            }
//        };
//        currentMessageTaskThread.start();
//        loadHistory.start();
//    }
//
//    //-----initializing the xml components
//    public void initComponent(){
//        /*** labels in the action abar of the activity_conversation ***/
//
//        reveal_items=(LinearLayout)findViewById(R.id.reveal_items);
//        channel_label = (TextView) toolbar.findViewById(R.id.channel_name);
//        channel_label.setText(channel_title);
//        messageEditText = (EditText) findViewById(R.id.messageEditText);
//        progressDialog = new ProgressDialog(context);
//        backButton = (ImageView) toolbar.findViewById(R.id.backButton);
//        messagesContainerRecyclerview = (RecyclerView) findViewById(R.id.messagesContainerRecyclerview);
//
//        writeImageButton = (ImageView) findViewById(R.id.writeImageButton);
//        writeImageButton.setOnClickListener(this);
//        cameraImageButton=(ImageView)findViewById(R.id.cameraImageButton);
//        cameraImageButton.setOnClickListener(this);
//        //setting Chat adapter
//        adapter = new ChatConversationAdapter(ChatConversationActivity.this, new ArrayList<ChatMessage>());
//        messagesContainerRecyclerview.setAdapter(adapter);
////        messagesContainerListview.setOnScrollListener(new SampleScrollListener(this));
////        messageEditText.setOnLongClickListener(this);
////        messagesContainerRecyclerview.setOnItemLongClickListener(this);
////        messagesContainerListview.setOnItemClickListener(this);
//        pickImageFile = (ImageView) findViewById(R.id.pickImageFile);
//        pickImageFile.setOnClickListener(this);
//
//        backButton.setOnClickListener(this);
//        imgPhotos=(ImageView)findViewById(R.id.imgPhotos);
//        imgPhotos.setOnClickListener(this);
//        imgAudios=(ImageView)findViewById(R.id.imgAudios);
//        imgAudios.setOnClickListener(this);
//        imgVideos=(ImageView)findViewById(R.id.imgVideos);
//        imgVideos.setOnClickListener(this);
//        imgDocuments=(ImageView)findViewById(R.id.imgDocuments);
//        imgDocuments.setOnClickListener(this);
//        imgCamera=(ImageView)findViewById(R.id.imgCamera);
//        imgCamera.setOnClickListener(this);
//
//    }
//    @Override
//    public void onClick(View v) {
//
//    }
//}

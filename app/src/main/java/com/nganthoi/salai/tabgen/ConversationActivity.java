package com.nganthoi.salai.tabgen;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import chattingEngine.ChatAdapter;
import chattingEngine.ChatMessage;
import readData.ReadFile;
import sharePreference.SharedPreference;

public class ConversationActivity extends AppCompatActivity {
    //ImageButton sendMessage;
    ImageView backButton,conv_Icon;
    ListView messagesContainer;
    EditText messageEditText;
    ImageButton sendMessage;
    private ChatAdapter adapter;
    private ArrayList<ChatMessage> chatHistory;
    SharedPreference sp;
    Context context=this;
    String channel_id="",user_id,token,last_timetamp=null;
    String channelDetails=null,file_path=null;
    int sender_responseCode=0,receiver_responseCode;
    String ip,responseMessage,errorMessage;
    HttpURLConnection conn=null;
    URL api_url=null;
    Thread thread;
    public Boolean interrupt=false;
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy' at 'h:mm a");
    //JSONArray filenames=null;
    // A JSON variable that contains list of file names returned from the mattermost APIs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarConversation);
        setSupportActionBar(toolbar);

        messagesContainer = (ListView) findViewById(R.id.chatListView);
        messageEditText = (EditText) findViewById(R.id.messageEditText);

        //setting Chat adapter
        adapter = new ChatAdapter(ConversationActivity.this, new ArrayList<ChatMessage>());
        messagesContainer.setAdapter(adapter);
        /********************************************/

        backButton = (ImageView) toolbar.findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    if(thread!=null){
                        thread.interrupt();
                        interrupt=true;
                    }
                }catch(Exception e){
                    System.out.println("Interrupt Exception: "+e.toString());
                }
                onBackPressed();
            }
        });
        Intent intent = getIntent();
        String title = intent.getStringExtra(ChatFragment.TITLE);
        TextView conversationLabel = (TextView) toolbar.findViewById(R.id.conversation_Label);
        conversationLabel.setText(title);
        conv_Icon = (ImageView) toolbar.findViewById(R.id.conv_icon);
        if(title.equals("Laboratory Group")){
            conv_Icon.setImageResource(R.drawable.laboratory_group);
        }else if(title.equals("Cardiology Dept")){
            conv_Icon.setImageResource(R.drawable.cardiology_dept);
        }
        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/
        sp = new SharedPreference();
        channelDetails = sp.getChannelPreference(context);
        token=sp.getTokenPreference(context);
        if(channelDetails!=null) System.out.println("Channel is not null: "+channelDetails);
        try{
            JSONArray jsonArray = new JSONArray(channelDetails);
            JSONObject jsonObject;
            for(int i=0;i<jsonArray.length();i++){
               jsonObject = jsonArray.getJSONObject(i);
               /*System.out.println("Title: "+title+"------->Channel name: "+jsonObject.getString("Channel_name")+" ---->ID: "+
                        jsonObject.getString("Channel_ID"));*/
               if(title.equals(jsonObject.getString("Channel_name"))) {
                   channel_id = jsonObject.getString("Channel_ID");// setting channel id
                   break;
               }//channel_id = jsonObject.getString("Channel_ID");
            }
            System.out.println("Title: "+title+" ---> Channel Id: "+channel_id+"\nToken Id: "+token);

        }catch(Exception e){
            System.out.println(e.toString());
        }
        String user_details=sp.getPreference(context);
        try{
            JSONObject jObj = new JSONObject(user_details);
            user_id=jObj.getString("id");
        }catch(Exception e){
            System.out.println("Unable to read user ID: "+e.toString());
        }
        ip = sp.getServerIP_Preference(context);//getting ip
        loadHistory();
        thread = new Thread(){
            @Override
            public void run(){
                try{
                    while(!isInterrupted() || !interrupt){
                        Thread.sleep(4000);
                        runOnUiThread(new Runnable(){
                            @Override
                            public void run(){

                                if(last_timetamp!=null) {
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
        thread.start();
        sendMessage = (ImageButton) findViewById(R.id.chatSendButton);
        sendMessage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String messageText = messageEditText.getText().toString();
                    if (TextUtils.isEmpty(messageText)) {
                        return;
                    }
                    try {
                        JSONObject jsonObject = new JSONObject();
                        /*
                        if(filenames!=null && filenames.length()>0){
                            jsonObject.put("filenames",filenames);
                        }*/
                        jsonObject.put("channel_id", channel_id);
                        jsonObject.put("root_id", "");
                        jsonObject.put("parent_id","");
                        jsonObject.put("Message", messageText);
                        sendMyMessage(jsonObject);
                    } catch (Exception e) {
                        System.out.print("Message Sending failed: " + e.toString());
                        Snackbar.make(v, "Oops! Message Sending failed", Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }
            });
        }
        //initControls();

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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.attach_file){
            Intent intent = new Intent();
            //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select a file from the gallary"),1);
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if(resultCode!=RESULT_OK || data==null) return;
        Uri fileUri = data.getData();
        //ReadFile readFile = new ReadFile();
        switch(requestCode){
            case 1: //file_path = readFile.getFilePath(fileUri,context);
                file_path = ReadFile.getPath(fileUri,context);
                if(file_path!=null){
                    //System.out.println("File has been selected: "+file_path);
                    Toast.makeText(context, "You have selected: "+file_path, Toast.LENGTH_SHORT).show();
                        new Thread(new Runnable(){
                            public void run(){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        System.out.println("Uploading your file....: "+file_path);
                                        Toast.makeText(context,"Sending your file now...",Toast.LENGTH_LONG).show();
                                        UploadFile uploadFile = new UploadFile(file_path,"http://"+ip+":8065/api/v1/files/upload");
                                        uploadFile.execute();
                                    }
                                });
                            }
                        }).start();
                }
                break;
            default:
                Toast.makeText(context, "Invalid request code. You haven't selected any file", Toast.LENGTH_SHORT).show();
        }
    }
    private void sendMyMessage(JSONObject jsonMsg) {
        String link = "http://"+ip+":8065/api/v1/channels/"+channel_id+"/create";
        String response=null;
        try{
            ConnectAPIs messageAPI = new ConnectAPIs(link,token);
            response=convertInputStreamToString(messageAPI.sendData(jsonMsg));
            if(response!=null ){
                if(messageAPI.responseCode==200){
                    ChatMessage chatMessage = new ChatMessage();
                    //chatMessage.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                    chatMessage.setMe(true);
                    chatMessage.setSenderName("Me");
                    messageEditText.setText("");
                    System.out.println("Sending result: "+response);

                    try{
                        JSONObject json_obj= new JSONObject(response);
                        chatMessage.setMessage(json_obj.getString("message"));
                        last_timetamp = json_obj.getString("create_at");
                        Long timestamp = Long.parseLong(last_timetamp);
                        Date date = new Date(timestamp);
                        chatMessage.setDate(simpleDateFormat.format(date));
                        //+json_obj.get("id")
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
        //messagesContainer.setSelection(messagesContainer.getCount() - 1);
        messagesContainer.setSelection(messagesContainer.getCount() - 1);
    }

    private void loadHistory(){
        InputStream inputStream = getData("http://"+ip+"/TabGen/getPost.php?channel_id="+channel_id);
        String res = convertInputStreamToString(inputStream);
        if(receiver_responseCode==200 && res!=null) {
            chatHistory = new ArrayList<ChatMessage>();
            //ChatMessage[] msg=new ChatMessage[100];
            try {
                JSONArray jsonArray = new JSONArray(res);
                JSONObject jsonObject;
                int i;
                for (i = 0; i < jsonArray.length(); i++) {
                    jsonObject = jsonArray.getJSONObject(i);
                    ChatMessage msg = new ChatMessage();
                    msg.setId(i);
                    if (user_id.equals(jsonObject.getString("UserId"))) {
                        msg.setMe(true);
                        msg.setSenderName("Me");
                    } else {
                        msg.setMe(false);
                        msg.setSenderName(jsonObject.getString("messaged_by"));
                    }
                    msg.setMessage(jsonObject.getString("Message"));
                    Long chatTime = Long.parseLong(jsonObject.getString("CreateAt"));
                    Date date = new Date(chatTime);
                    msg.setDate(simpleDateFormat.format(date));
                    //msg.setDate(DateFormat.getDateTimeInstance().format(new Date()));
                    last_timetamp = jsonObject.getString("LastPostAt");
                    chatHistory.add(msg);
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
            }

            adapter.add(chatHistory);
            adapter.notifyDataSetChanged();
            scroll();
            /*for (int i = 0; i < chatHistory.size(); i++) {
                ChatMessage message = chatHistory.get(i);
                adapter.add(message);
                adapter.notifyDataSetChanged();
                scroll();
            }*/
        }
    }

    public InputStream getData(String api_link){
        InputStream isr=null;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        try{
            api_url = new URL(api_link);
            conn = (HttpURLConnection) api_url.openConnection();
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
            receiver_responseCode = conn.getResponseCode();
            responseMessage = conn.getResponseMessage();
            System.out.println("Response Code: " + receiver_responseCode + "\nResponse message: " + responseMessage);
            if(receiver_responseCode == 200/*HttpURLConnection.HTTP_OK*/){
                isr = new BufferedInputStream(conn.getInputStream());
            }
            else {
                isr = new BufferedInputStream(conn.getErrorStream());
            }
        }catch(Exception e){
            e.printStackTrace();
            errorMessage = e.toString();
            receiver_responseCode=-1;
            System.out.println("Exception occurs here: " + e.toString());
        }
        return isr;
    }

    public String convertInputStreamToString(InputStream inputStream){
        String result=null;
        if(inputStream!=null){
            try{
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,"iso-8859-1"),8);
                StringBuilder sb = new StringBuilder();
                String line=null;
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


    public class UploadFile extends AsyncTask<Void,Void,String>{
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
        int maxBufferSize = 1*1024*1024;
        String fileLocation=null;
        InputStream isr=null;

        public UploadFile(String sourceFileUri,String serverUploadPath){
            fileLocation = sourceFileUri;
            file_upload_uri = serverUploadPath;
        }
        @Override
        protected void onPreExecute(){
            Toast.makeText(context,"Sending your file now...",Toast.LENGTH_LONG).show();
        }
        @Override
        protected String doInBackground(Void... v){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
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
                    while(bytesRead>0){
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
                    }
                    else{
                        System.out.println("Oops! Your file upload is failed");
                        isr = new BufferedInputStream(httpURLConn.getErrorStream());
                    }
                    fis.close();
                    dos.close();
                    osw.close();

                }catch(Exception e){
                    e.printStackTrace();
                    System.out.println("File Upload Exception here: "+e.toString());
                    return null;
                }//end try catch
            }
            return convertInputStreamToString(isr);
        }
        @Override
        protected void onPostExecute(String result){
            if(result!=null){
                System.out.println("Result: "+result);//printing out the server results
                try{
                    JSONObject fileObject = new JSONObject(result);
                    //assign the list of filenames in the global JSON array filename
                    JSONArray filenames = fileObject.getJSONArray("filenames");
                    for(int i=0;i<filenames.length();i++){
                        System.out.println("file name: "+filenames.getString(i));
                        ConnectAPIs connAPIs = new ConnectAPIs("http://"+ip+":8065/api/v1/files/get_info/"
                                +filenames.getString(i),token);
                        InputStream isr = connAPIs.getData();
                        System.out.println("File Info: "+convertInputStreamToString(isr));
                    }

                    if(filenames.length()>0) {
                        Toast.makeText(context,"File uploaded",Toast.LENGTH_SHORT).show();
                        try {
                            JSONObject msgObject = new JSONObject();
                            msgObject.put("filenames", filenames);
                            msgObject.put("channel_id", channel_id);
                            msgObject.put("root_id", "");
                            msgObject.put("parent_id", "");
                            msgObject.put("Message", "File Sent");
                            /*
                            ConnectAPIs msgAPI = new ConnectAPIs("http://"+ip+":8065/api/v1/channels/"+channel_id+"/create",token);
                            InputStream is = msgAPI.sendData(msgObject);
                            System.out.println(convertInputStreamToString(is));*/
                            sendMyMessage(msgObject);
                        } catch (JSONException e) {
                            System.out.println("Something goes wrong: "+e.toString());
                        }
                    }
                    //end if statement
                }catch(Exception e){
                    System.out.println("Unable to read file details: "+e.toString());
                }
            }
            else System.out.println("Response is null");
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
                        String messageDate = null;
                        while (i < jsonArray.length()) {
                            //System.out.println(jsonArray.getString(i));
                            JSONObject jObj3 = jObj2.getJSONObject(jsonArray.getString(i));
                            System.out.println("Id: " + jObj3.getString("id") + " Message: " + jObj3.getString("message"));
                            messageDate = "" + jObj3.getString("create_at");
                            System.out.println("Message Date: " + messageDate);
                            ChatMessage currentMsg = new ChatMessage();
                            //currentMsg.setId(777);
                            currentMsg.setMessage("" + jObj3.getString("message"));
                            Long timeStamp = Long.parseLong(messageDate);
                            Date date = new Date(timeStamp);
                            currentMsg.setDate(simpleDateFormat.format(date));

                            if (user_id.equals("" + jObj3.getString("user_id"))) {
                                currentMsg.setMe(true);
                                currentMsg.setSenderName("Me");
                            } else {
                                currentMsg.setMe(false);
                                currentMsg.setSenderName(""+jObj3.getString("user_id"));
                            }
                            if(!messageDate.equals(last_timetamp))
                                displayMessage(currentMsg);
                            if (messageDate != null)
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

    //class for connecting APIs
    class ConnectAPIs {
        InputStream isr=null;
        public int responseCode;
        public String responseMessage, errorMessage,TokenId=null;
        URL api_url=null;
        public HttpURLConnection conn=null;
        public ConnectAPIs(String web_api,String token){
            try{
                api_url =new URL(web_api);
                TokenId = token;
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }

        public InputStream getData(){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            try{
                conn = (HttpURLConnection) api_url.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + TokenId);
                conn.setRequestMethod("GET");
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
        public InputStream sendData(JSONObject parameters){
            OutputStream os;
            OutputStreamWriter osw;
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            InputStream isr;
            try{
                //api_url = new URL(api_link);
                conn = (HttpURLConnection) api_url.openConnection();
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Authorization", "Bearer "+TokenId);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.connect();
                os = conn.getOutputStream();
                osw = new OutputStreamWriter(os);
                osw.write(parameters.toString());
                osw.flush();
                responseCode = conn.getResponseCode(); //it only the code 200
                responseMessage = conn.getResponseMessage();// it is the json response from the mattermost api
                System.out.println("Response Code: "+responseCode+"\nResponse message: "+responseMessage);
                if(responseCode == 200) {
                    isr = new BufferedInputStream(conn.getInputStream());
                }
                else{
                    isr = new BufferedInputStream(conn.getErrorStream());
                }
                osw.close();
            }catch(Exception e){
                e.printStackTrace();
                errorMessage = e.toString();
                responseCode=-1;
                System.out.println("Unable to send Exception occurs here: " + e.toString());
                isr = null;
            }
            return isr;
        }
    }//end of connectAPIs class
}


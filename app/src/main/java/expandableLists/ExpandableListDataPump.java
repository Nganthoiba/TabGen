package expandableLists;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import connectServer.ConnectAPIs;
import connectServer.ConnectServer;
import sharePreference.SharedPreference;

public class ExpandableListDataPump {
    static String user_id="";
    static String team_name;
    static List<String> channelList;


    public static HashMap<String, List<String>> getData(Context context) {
        HashMap<String, List<String>> expandableListDetail = new HashMap<String, List<String>>();
        SharedPreference sp = new SharedPreference();
        String user_details = sp.getPreference(context);

        try{
            JSONObject jObj = new JSONObject(user_details);
            user_id=jObj.getString("id");
            System.out.println("User ID: "+user_id);
            //username = jObj.getString("username");
        }catch(Exception e){
            System.out.println("Unable to read user ID: "+e.toString());
        }

        //getting list of Channel lists
        //channelList = OrganisationDetails.getListOfChannel(user_id);

        try {
            ConnectServer channelIdList = new ConnectServer("http://"+sp.getServerIP_Preference(context)+"/TabGenAdmin/getChannels.php"+
                    "?user_id="+user_id);
            String jsonStr = channelIdList.convertInputStreamToString(channelIdList.getData());
            //saving channel details in share preference
            sp.saveChannelPreference(context,jsonStr);
            System.out.println(jsonStr);
            if(jsonStr!=null){
                try {
                    JSONObject jsonObj1 = new JSONObject(jsonStr);
                    JSONArray jsonArray1 = jsonObj1.getJSONArray("team_list");
                    JSONArray jsonArray2 = jsonObj1.getJSONArray("channels");
                    for(int i=0;i<jsonArray1.length();i++){//for every item(team) in the team list
                        //JSONObject jsonObj2 = jsonArray1.getJSONObject(i);
                        team_name = jsonArray1.getString(i);//getting the team name
                        System.out.println("Team name--> " + team_name);
                        channelList = new ArrayList<String>();

                        JSONObject jsonObj3 = jsonArray2.getJSONObject(i);//getting json objects for channels

                        try {
                            JSONArray jsonArray3 = jsonObj3.getJSONArray(team_name);

                            if (jsonArray3.length() != 0 && jsonArray3 != null) {
                                for (int j = 0; j < jsonArray3.length(); j++) {
                                    JSONObject jsonObj4 = jsonArray3.getJSONObject(j);
                                    if(jsonObj4!=null) {
                                        channelList.add(jsonObj4.getString("Channel_name"));
                                        System.out.println("Channel Name: " + jsonObj4.getString("Channel_name"));
                                    }
                                    else {
                                        channelList.add("NO CHANNEL AVAILABLE");
                                        continue;
                                    }
                                }
                                expandableListDetail.put(team_name, channelList);
                            } else channelList.add("No channel available");
                        } catch (Exception e) {
                            System.out.println("System crashes here. Unable to parse channel: \n" + e.toString());
                            continue;
                        }

                    }
                }catch(Exception e){
                    System.out.print("An Exception occurs here: "+e.toString());
                }
            }
        }
        catch(Exception e){
            System.out.println("Exception here: " + e.toString());
        }
        return expandableListDetail;
    }
}

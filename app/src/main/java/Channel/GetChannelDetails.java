package Channel;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import sharePreference.SharedPreference;

/**
 * Created by SALAI on 3/25/2016.
 */
public class GetChannelDetails {
    public Channel getChannel(String team_name,String channel_name,Context context){
        String channel_id=null;
        Channel channel=new Channel();
        SharedPreference sp = new SharedPreference();
        String channelDetails = sp.getChannelPreference(context);
        if(channelDetails!=null) {
            System.out.println("Channel is not null: " + channelDetails);
            try {
                JSONObject jsonObj1 = new JSONObject(channelDetails);
                JSONArray jsonArray1 = jsonObj1.getJSONArray("team_list");
                JSONArray jsonArray2 = jsonObj1.getJSONArray("channels");
                for(int i=0;i<jsonArray1.length();i++){//for every item(team) in the team list
                    //JSONObject jsonObj2 = jsonArray1.getJSONObject(i);
                    String teamName = jsonArray1.getString(i);//getting the team name

                    JSONObject jsonObj3 = jsonArray2.getJSONObject(i);//getting json objects for channels
                    try {
                        JSONArray jsonArray3 = jsonObj3.getJSONArray(teamName);
                        //List<String> channelList = new ArrayList<String>();
                        for (int j = 0; j < jsonArray3.length(); j++) {
                            JSONObject jsonObj4 = jsonArray3.getJSONObject(j);
                            if (channel_name.equals(jsonObj4.getString("Channel_name")) && team_name.equals(teamName)) {
                                channel_id = jsonObj4.getString("Channel_ID");// setting channel id
                                channel.setChannel(channel_id, channel_name, Integer.parseInt(jsonObj4.getString("members_count")), teamName);
                                break;
                            }
                        }
                    }catch(Exception e){
                        System.out.println("Exception occurs in getting channel id in GetChannelDetails.java: "+e.toString());
                    }
                    if(channel_id!=null) break;
                }
            }catch(Exception e){
                System.out.print("Channel ID Exception occurs here: " + e.toString());
            }
        }
        return channel;
    }
}

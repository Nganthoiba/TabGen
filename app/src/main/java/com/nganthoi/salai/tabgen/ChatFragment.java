package com.nganthoi.salai.tabgen;

/**
 * Created by SALAI on 1/15/2016.
 */

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import expandableLists.ExpandableListAdapter;
import expandableLists.ExpandableListDataPump;

public class ChatFragment extends Fragment {

    /*For Chatting List View*/
    ExpandableListView expandableListView;
    ExpandableListAdapter expandableListAdapter;
    List<String> expandableListTitle;
    HashMap<String, List<String>> expandableListDetail;
    View chatView;//,layoutGroupHeader;

    public final static String CHANNEL_NAME = "com.nganthoi.salai.tabgen.MESSAGE";
    public final static String TEAM_NAME="team_name";
    //Context _context=this;
    public ChatFragment(){

    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater layoutInflater,ViewGroup container,Bundle savedInstanceState){
        chatView = layoutInflater.inflate(R.layout.chat_layout,container,false);
        expandableListView = (ExpandableListView) chatView.findViewById(R.id.chatExpandableListView);

        DisplayMetrics metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        expandableListView.setIndicatorBounds(width-120, width);
        //expandableListView.setIndicatorBounds(expandableListView.getRight()-150, expandableListView.getWidth()-GetDipsFromPixel(10));
        //this code for adjusting the group indicator into right side of the view
        //expandableListView.setIndicatorBounds(width - UIUtils.getPxFromDp(getActivity(), 40), width - UIUtils.getPxFromDp(getActivity(),20));
        //.setIndicatorBounds(width - UIUtils.getPxFromDp(getActivity(), 40), width - UIUtils.getPxFromDp(getActivity(),20));

        showChatLists();
        return chatView;
    }

    public void showChatLists() {
        /*Setting chat list View*/
        expandableListDetail = ExpandableListDataPump.getData(chatView.getContext());
        expandableListTitle = new ArrayList<String>(expandableListDetail.keySet());
        expandableListAdapter= new ExpandableListAdapter(chatView.getContext(),expandableListTitle,expandableListDetail);
        expandableListView.setAdapter(expandableListAdapter);
        //layoutGroupHeader = layoutInflater.inflate(R.layout.list_group, null);
        expandableListView.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                /*
                Toast.makeText(chatView.getContext(),
                        expandableListTitle.get(groupPosition) + " List Expanded ",
                        Toast.LENGTH_SHORT).show();*/
            }
        });

        expandableListView.setOnGroupCollapseListener(new ExpandableListView.OnGroupCollapseListener() {

            @Override
            public void onGroupCollapse(int groupPosition) {
                /*
                Toast.makeText(chatView.getContext(),
                        expandableListTitle.get(groupPosition) + " List Collapsed",
                        Toast.LENGTH_SHORT).show();*/
            }
        });

        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v,
                                        int groupPosition, int childPosition, long id) {

                /*Toast.makeText(
                        chatView.getContext(),
                        expandableListTitle.get(groupPosition)
                                + " -> "
                                + expandableListDetail.get(
                                expandableListTitle.get(groupPosition)).get(childPosition), Toast.LENGTH_SHORT).show();*/
                Intent intent = new Intent(chatView.getContext(), ConversationActivity.class);
                intent.putExtra(TEAM_NAME,expandableListTitle.get(groupPosition));
                intent.putExtra(CHANNEL_NAME, expandableListDetail.get(expandableListTitle.get(groupPosition)).get(childPosition));
                startActivity(intent);
                return false;
            }
        });
    }
    public int GetDipsFromPixel(float pixels)
    {
        // Get the screen's density scale
        final float scale = getResources().getDisplayMetrics().density;
        // Convert the dps to pixels, based on density scale
        return (int) (pixels * scale + 0.5f);
    }

    /*public class LoadChannels extends AsyncTask<View,Void,HashMap<String, List<String>>>{
        @Override
        protected HashMap<String, List<String>> doInBackground(View... chatView){
            expandableListDetail = ExpandableListDataPump.getData(chatView.getContext());
        }
    }*/

}

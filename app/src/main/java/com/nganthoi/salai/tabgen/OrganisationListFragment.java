package com.nganthoi.salai.tabgen;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import sharePreference.SharedPreference;

/**
 * Created by SALAI on 1/18/2016.
 */
public class OrganisationListFragment extends Fragment{
    View org_layout;
    SharedPreference sp;
    List<String> list;
    ListView orgList;
    ArrayAdapter<String> arrayAdapter;
    public OrganisationListFragment(){

    }
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater layoutInflater,ViewGroup container,Bundle savedInstanceState){
        org_layout = layoutInflater.inflate(R.layout.organisation_list,container,false);
        sp = new SharedPreference();
        list = new ArrayList<String>();
        String user_details = sp.getPreference(org_layout.getContext());
        try {
            JSONObject jsonObject = new JSONObject(user_details);
            list = OrganisationDetails.getListOfOrganisations(jsonObject.getString("username"),org_layout.getContext());
        } catch (JSONException e) {
            System.out.println("Exception :" + e.toString());
        }
        orgList = (ListView) org_layout.findViewById(R.id.OrgListView);
        arrayAdapter = new ArrayAdapter<String>(org_layout.getContext(),android.R.layout.simple_list_item_1, list);
        orgList.setAdapter(arrayAdapter);
        return org_layout;
    }
}

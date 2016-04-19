package com.nganthoi.salai.tabgen;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import sharePreference.SharedPreference;

public class UserActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    Context _context=this;
    ProgressDialog progressDialog;
    String role;//role of the user
    SharedPreference sp;
    List<String> list;
    ArrayList<String> template_list;
    TextView chatView,refView,cmeView,newsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        chatView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        refView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        cmeView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        newsView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        */
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /* Getting textView IDs from the drawer */
        TextView username = (TextView) findViewById(R.id.username);
        TextView usermail = (TextView) findViewById(R.id.user_email);
        TextView userrole = (TextView) findViewById(R.id.user_role);
        /* Getting user details from the shared preference */
        sp = new SharedPreference();

        String user_details = sp.getPreference(_context);
        try {
            JSONObject jsonObject = new JSONObject(user_details);
            username.setText(jsonObject.getString("username"));
            usermail.setText(jsonObject.getString("email"));
            role = jsonObject.getString("roles");
            userrole.setText(role);
        } catch (JSONException e) {
            System.out.println("Exception :" + e.toString());
        }
        progressDialog = new ProgressDialog(_context);
        progressDialog.setMessage("Loading Your Templates....");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        mViewPager = (ViewPager) findViewById(R.id.container);
        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        Intent intent = getIntent();
        //gettting list of templates from the previous activity
        template_list = intent.getStringArrayListExtra(UserLandingActivity.templateListExtra);

        new GetTabList().execute(template_list);
        //tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        //mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        //tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#e5d600"));
        tabLayout.setSelectedTabIndicatorHeight((int) (0 * getResources().getDisplayMetrics().density));


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            //logout();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.user, menu);
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

        return super.onOptionsItemSelected(item);
    }
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.logout){
            logout();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    class GetTabList extends AsyncTask<ArrayList<String>,Void,List<String>> {
        @Override
        protected void onPreExecute(){
            progressDialog.show();
        }
        @Override
        protected List<String> doInBackground(ArrayList<String>... template_list){
            /*Getting list of Templates for a particular role */
            //list=OrganisationDetails.getListOfTemplates(_context,userRole[0]);
            list = new ArrayList<String>();
            for(int i=0;i<template_list[0].size();i++){
                list.add(template_list[0].get(i));
            }
            onProgressUpdate();
            return list;
        }

        protected void onProgressUpdate(){
            progressDialog.show();
        }
        @Override
        protected void onPostExecute(List<String> list){
            setupViewPager(mViewPager, list);
            tabLayout.setupWithViewPager(mViewPager);
            try{
                setTabLayoutIcons(tabLayout,list);
            }catch(Exception e){
                System.out.println("Layout Exception: "+e.toString());
            }
            /*tabLayout.getTabAt(0).setIcon(R.drawable.icon_chat);
            tabLayout.getTabAt(1).setIcon(R.drawable.icon_reference);
            tabLayout.getTabAt(2).setIcon(R.drawable.icon_cme);
            tabLayout.getTabAt(3).setIcon(R.drawable.icon_news);*/

            Intent mIntent = getIntent();
            int tab_position = mIntent.getIntExtra(UserLandingActivity.tabPosition,0);
            tabLayout.getTabAt(tab_position).select();
            progressDialog.dismiss();

        }
    }

    private void setupViewPager(ViewPager viewPager,List<String> list) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        Boolean chat_available=false,cme_available=false,ref_available=false,news_available=false;
        for(int i=0;i<list.size();i++) {
            /*Adding tabs and fragments according to the Templates from the server*/
            switch (list.get(i)) {
                case "Chat Template"://check if Chat template exist
                    chat_available = true;
                    break;
                case "Reference Template":
                    ref_available = true;
                    break;
                case "CME Template":
                    cme_available = true;
                    break;
                case "Latest News Template":
                    news_available = true;
                    break;
            }
        }
        if(chat_available){
            adapter.addFragment(new ChatFragment(), "");
        }else{
            adapter.addFragment(new UnauthorisedFragment(), "");
        }
        if(ref_available) {
            adapter.addFragment(new ReferenceFragment(), "");
        }else{
            adapter.addFragment(new UnauthorisedFragment(), "");
        }
        if(cme_available) {
            adapter.addFragment(new CmeFragment(),"");
        }else{
            adapter.addFragment(new UnauthorisedFragment(),"");
        }
        if(news_available){
            adapter.addFragment(new LatestNewsFragment(),"");
        }else{
            adapter.addFragment(new UnauthorisedFragment(),"");
        }
        viewPager.setAdapter(adapter);
        tabLayout.setOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(viewPager));
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
    }

    private void setTabLayoutIcons(TabLayout tabLayout,List<String> list) throws Exception{

        try {
            /*for chat*/

            chatView.setText(" ");//chatView.setText("CHAT");
            chatView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.chat, 0, 0);
            tabLayout.getTabAt(0).setCustomView(chatView);

            refView.setText(" ");//refView.setText("REFERENCE");
            refView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.reference, 0, 0);
            tabLayout.getTabAt(1).setCustomView(refView);

            cmeView.setText(" ");//cmeView.setText("CME");
            cmeView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.cme, 0, 0);
            tabLayout.getTabAt(2).setCustomView(cmeView);

            newsView.setText(" ");//newsView.setText("LATEST NEWS");
            newsView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.news, 0, 0);
            tabLayout.getTabAt(3).setCustomView(newsView);

        }catch(Exception e){
            System.out.println("Exception: "+e.toString());
        }
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    /* function for logout */
    public void logout(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(_context);
        alertDialogBuilder.setTitle("Logout ?");
        alertDialogBuilder.setMessage("Are you sure to logout?");
        alertDialogBuilder.setIcon(R.drawable.failure_icon);
        alertDialogBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(_context, MainActivity.class));
                sp.clearPreference(_context);
                finish();
            }
        });
        alertDialogBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

}

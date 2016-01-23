package com.nganthoi.salai.tabgen;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import connectServer.ConnectServer;
import sharePreference.SharedPreference;

public class UserActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private ViewPager mViewPager;
    private TabLayout tabLayout;
    private Toolbar toolbar;
    Context _context=this;
    String role;//role of the user
    String[] templateArray;
    SharedPreference sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
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

        /*Code for getting list of templates*/
        ConnectServer templateList = new ConnectServer("http://188.166.210.24/getTemplateListByRole.php");
        String jsonStr = templateList.convertInputStreamToString(templateList.putData("Role="+role));
        if(jsonStr!=null){
            try {
                JSONArray jsonArray = new JSONArray(jsonStr);
                if (templateList.responseCode == 200) {
                    JSONObject jsonObject;
                    for(int i=0;i<jsonArray.length();i++){
                        jsonObject = jsonArray.getJSONObject(i);
                        templateArray[i]=jsonObject.getString("Template_Name");
                    }
                }else templateArray=null;
            }catch(Exception e){
                System.out.print("An Exception occurs here: "+e.toString());
            }
        }else templateArray=null;
        /***************************************/


        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(mViewPager);
        try{
            setTabLayoutIcons(tabLayout);
        }catch(Exception e){
            System.out.print("TabLayout Exception: "+e.toString());
        }
        /*
        tabLayout.getTabAt(0).setIcon(R.drawable.chat);
        tabLayout.getTabAt(1).setIcon(R.drawable.reference);
        tabLayout.getTabAt(2).setIcon(R.drawable.cme);
        tabLayout.getTabAt(3).setIcon(R.drawable.latest_news);*/
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            //super.onBackPressed();
            logout();
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

        if (id == R.id.nav_camara) {
            // Handle the camera action
        }  else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.logout){
            logout();
        } else if(id == R.id.showTemplates){
            //startActivity(new Intent(_context,ShowTemplateActivity.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(new ChatFragment(), "\n CHATS");
        adapter.addFragment(new ReferenceFragment(), "\n REFERENCES");
        adapter.addFragment(new CmeFragment(), "\n CME");
        adapter.addFragment(new LatestNewsFragment(), "\n LATEST NEWS");
        viewPager.setAdapter(adapter);
    }

    private void setTabLayoutIcons(TabLayout tabLayout) throws NullPointerException{
        try{
            TextView chatView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab,null);
            chatView.setText("CHAT");
            chatView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.chat_icon, 0, 0);
            tabLayout.getTabAt(0).setCustomView(chatView);
        }catch(Exception e){
            System.out.println("Unable to view chat layout: "+e.toString());
        }
        try {
            TextView refView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
            refView.setText("REFERENCE");
            refView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.refer_icon, 0, 0);
            tabLayout.getTabAt(1).setCustomView(refView);
        }catch(Exception e){
            System.out.println("Unable to view reference layout: "+e.toString());
        }
        try {
            TextView cmeView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
            cmeView.setText("CME");
            cmeView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.cms, 0, 0);
            tabLayout.getTabAt(2).setCustomView(cmeView);
        }catch(Exception e){
            System.out.println("Unable to view CME layout: "+e.toString());
        }
        try {
            TextView newsView = (TextView) LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
            newsView.setText("LATEST NEWS");
            newsView.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.news_icon, 0, 0);
            tabLayout.getTabAt(3).setCustomView(newsView);
        }catch(Exception e){
            System.out.println("Unable to view latest news Layout: "+e.toString());
        }
        /*
        tabLayout.getTabAt(1).setIcon(R.drawable.reference);
        tabLayout.getTabAt(2).setIcon(R.drawable.cme);
        tabLayout.getTabAt(3).setIcon(R.drawable.latest_news);
        */
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

    public void displayTemplates(){

    }
}

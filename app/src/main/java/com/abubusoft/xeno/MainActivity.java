package com.abubusoft.xeno;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.android.sqlite.TransactionResult;
import com.abubusoft.xeno.android.SectionsPagerAdapter;
import com.abubusoft.xeno.events.EventConfigSave;
import com.abubusoft.xeno.events.EventEnablePrefix;
import com.abubusoft.xeno.events.EventNoPermission;
import com.abubusoft.xeno.persistence.BindXenoDaoFactory;
import com.abubusoft.xeno.persistence.BindXenoDataSource;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import com.abubusoft.xeno.model.PrefixConfig;

import butterknife.BindView;
import butterknife.ButterKnife;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    EventBus bus= EventBus.getDefault();

    @Override
    protected void onPause() {
        super.onPause();

        bus.unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        bus.register(this);
    }

    @OnPermissionDenied({Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.READ_CONTACTS,  Manifest.permission.SYSTEM_ALERT_WINDOW})
    public void onEventNoPermission() {
        Logger.info("onEventNoPermission");
        BindXenoDataSource.instance().execute((BindXenoDaoFactory daoFactory) -> {
                PrefixConfig config = daoFactory.getPrefixConfigDao().selectOne();
                config.enabled=false;
                daoFactory.getPrefixConfigDao().update(config);

                bus.post(new EventNoPermission());
                return TransactionResult.COMMIT;
        });

    }

    private SectionsPagerAdapter mSectionsPagerAdapter;

    @BindView(R.id.container)
    ViewPager mViewPager;

    @BindView(R.id.tabs)
    TabLayout tabLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mSectionsPagerAdapter.addFragment(new PhoneListFragment(), getString(R.string.tab_header_phone_list));
        mSectionsPagerAdapter.addFragment(new ConfigFragment(), getString(R.string.tab_header_config));

        ButterKnife.bind(this);

        mViewPager.setAdapter(mSectionsPagerAdapter);
        tabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        MainActivityPermissionsDispatcher.onActivityResult(this, requestCode);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventEnablePrefix(EventEnablePrefix event)
    {
        Logger.info("onEventEnablePrefix");

        MainActivityPermissionsDispatcher.enablePermissionsWithCheck(this);

        MainActivityPermissionsDispatcher.enableSystemAlertWindowWithCheck(this);
    }

    @NeedsPermission(Manifest.permission.SYSTEM_ALERT_WINDOW)
    public void enableSystemAlertWindow()
    {
        Logger.info("enableSystemAlertWindow");
    }


    @NeedsPermission({Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.READ_CONTACTS})
    public void enablePermissions()
    {
        Logger.info("enableProcessOutgoindCalls");
    }





//    @OnShowRationale({Manifest.permission.PROCESS_OUTGOING_CALLS, Manifest.permission.READ_CONTACTS})
//    public void showRationaleForCamera(final PermissionRequest request) {
//        new AlertDialog.Builder(this)
//                .setMessage(R.string.permission_phone_rationale)
//                .setPositiveButton(R.string.button_allow, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        request.proceed();
//                    }
//                })
//                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        request.cancel();
//                    }
//                })
//                .create()
//                .show();
//    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id)
        {
          //  case R.id.action_settings:
            //    return true;
            case R.id.action_privacy_policy: {
                String url = "http://www.abubusoft.com/privacy/default/privacy.html";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                this.startActivity(intent);
                return true;
            }
            case R.id.action_rate_me:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id)
        {
            case R.id.nav_contacts_list:
              // showPhoneListFragment();
                break;
            case R.id.nav_configuration:
                //showConfigFragment();
                break;
            case R.id.nav_privacy_policy: {
                String url = "http://www.abubusoft.com/privacy/default/privacy.html";
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                this.startActivity(intent);
            }
                break;
            case R.id.nav_rate_me:
                Uri uri = Uri.parse("market://details?id=" + getPackageName());
                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                // To count with Play market backstack, After pressing back button,
                // to taken back to our application, we need to add following flags to intent.
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                try {
                    startActivity(goToMarket);
                } catch (ActivityNotFoundException e) {
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
                }
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND )
    public void onConfigSave(EventConfigSave event)
    {
        Logger.info("selected " + event);
    }
}

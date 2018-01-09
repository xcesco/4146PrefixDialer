package com.abubusoft.xeno;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import com.abubusoft.kripton.KriptonBinder;
import com.abubusoft.kripton.android.KriptonLibrary;
import com.abubusoft.kripton.android.sqlite.DataSourceOptions;
import com.abubusoft.kripton.android.sqlite.DatabaseLifecycleHandler;
import com.abubusoft.xeno.model.Country;
import com.abubusoft.xeno.persistence.BindXenoDataSource;
import com.abubusoft.xeno.persistence.CountryDaoImpl;
import com.abubusoft.xeno.persistence.PrefixConfigDaoImpl;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.abubusoft.xeno.model.PrefixConfig;

public class XenoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        KriptonLibrary.init(this);

        final BindXenoDataSource dataSource = BindXenoDataSource.instance();
        dataSource.setOptions(DataSourceOptions.builder().databaseLifecycleHandler(new DatabaseLifecycleHandler() {
            @Override
            public void onUpdate(SQLiteDatabase db, int oldVersion, int newVersion, boolean upgrade) {

            }

            @Override
            public void onCreate(SQLiteDatabase database) {
                // valorizziamo le country
                InputStream input = getResources().openRawResource(R.raw.iso_countries);
                PhoneNumberUtil util = PhoneNumberUtil.getInstance();
                Set<String> supportedCodes = util.getSupportedRegions();

                CountryDaoImpl dao = dataSource.getCountryDao();
                List<Country> list = KriptonBinder.jsonBind().parseList(input, Country.class);

                for (Country item : list) {
                    if (supportedCodes.contains(item.code))
                        dao.insert(item);
                }

                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                PrefixConfigDaoImpl daoConfig = dataSource.getPrefixConfigDao();
                PrefixConfig config = daoConfig.selectOne();
                if (config == null) {
                    config = new PrefixConfig();
                    config.defaultCountry = Locale.ITALY.getCountry();
                    config.dualBillingPrefix = "4146";
                    config.dialogTimeout = 50;
                    config.enabled = true;
                   // config.skipMessage=true;

                    daoConfig.insert(config);
                }
            }

            @Override
            public void onConfigure(SQLiteDatabase database) {

            }
        }).build());

        EventBus.builder().addIndex(new XenoEventBusIndex());
    }
}

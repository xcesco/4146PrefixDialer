package com.abubusoft.xeno;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.abubusoft.kripton.KriptonBinder;
import com.abubusoft.kripton.android.KriptonLibrary;
import com.abubusoft.kripton.android.sqlite.DataSourceOptions;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTask;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTaskHelper;
import com.abubusoft.kripton.exception.KriptonRuntimeException;
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

import cat.ereza.customactivityoncrash.config.CaocConfig;

public class XenoApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // https://github.com/Ereza/CustomActivityOnCrash
        CaocConfig.Builder.create()
//                .backgroundMode(CaocConfig.BACKGROUND_MODE_SILENT) //default: CaocConfig.BACKGROUND_MODE_SHOW_CUSTOM
//                .enabled(false) //default: true
                .showErrorDetails(false) //default: true
//                .showRestartButton(false) //default: true
//                .logErrorOnRestart(false) //default: true
//                .trackActivities(true) //default: false
//                .minTimeBetweenCrashesMs(2000) //default: 3000
                //.errorDrawable(R.drawable.ic_custom_drawable) //default: bug image
                //.restartActivity(YourCustomActivity.class) //default: null (your app's launch activity)
                //.errorActivity(ErrorActivity.class) //default: null (default error activity)
                //.eventListener(new YourCustomEventListener()) //default: null
                .apply();

        KriptonLibrary.init(this);

        final BindXenoDataSource dataSource = BindXenoDataSource.build(DataSourceOptions.builder()
                .addUpdateTask(2, (database, previousVersion, currentVersion) -> migrationVersion2(this, database))
                .populator(database -> fillCountryCodes(this)).build());

        EventBus.builder().addIndex(new XenoEventBusIndex());
    }

    /**
     * populate country codes
     *
     * @param context
     */
    public static void fillCountryCodes(Context context) {
        // fill country
        InputStream input = context.getResources().openRawResource(R.raw.iso_countries);
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        Set<String> supportedCodes = util.getSupportedRegions();

        BindXenoDataSource.getInstance().executeBatch(daoFactory -> {
            CountryDaoImpl dao = daoFactory.getCountryDao();
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

            PrefixConfigDaoImpl daoConfig = daoFactory.getPrefixConfigDao();
            PrefixConfig config = daoConfig.selectOne();
            if (config == null) {
                config = new PrefixConfig();
                config.defaultCountry = Locale.ITALY.getCountry();
                config.dualBillingPrefix = "4146";
                config.dialogTimeout = 50;
                config.enabled = true;
                //config.skipMessage=true;

                daoConfig.insert(config);
            }


            return null;
        });
    }

    public static void migrationVersion2(Context context, SQLiteDatabase datasource) {
        SQLiteUpdateTaskHelper.renameTablesWithPrefix(datasource, "tmp_");

        try {
            SQLiteUpdateTaskHelper.executeSQL(datasource,  context.getAssets().open("xeno_schema_2.sql"));
        } catch (IOException e) {
            e.printStackTrace();
            throw new KriptonRuntimeException(e);
        }
        SQLiteUpdateTaskHelper.executeSQL(datasource, "INSERT INTO prefix_config SELECT * FROM tmp_prefix_config;");
        SQLiteUpdateTaskHelper.executeSQL(datasource, "INSERT INTO country SELECT * FROM tmp_country;");
        SQLiteUpdateTaskHelper.executeSQL(datasource, "INSERT INTO phone_number SELECT * FROM tmp_phone_number;");

        SQLiteUpdateTaskHelper.dropTablesWithPrefix(datasource, "tmp_");
    }
}

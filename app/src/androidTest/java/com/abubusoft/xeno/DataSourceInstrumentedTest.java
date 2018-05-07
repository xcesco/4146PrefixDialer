package com.abubusoft.xeno;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.abubusoft.kripton.android.KriptonLibrary;
import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.android.sqlite.SQLiteSchemaVerifierHelper;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTask;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTaskHelper;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTestDatabase;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTestHelper;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class DataSourceInstrumentedTest {

    @Test
    public void testMigration() throws Exception {
        // Context of the app under test.
        Context testContext = InstrumentationRegistry.getContext();
        Context context = InstrumentationRegistry.getTargetContext();

        KriptonLibrary.init(context);
        InputStream schema1 = testContext.getAssets().open("xeno_schema_1.sql");
        InputStream schema2 = testContext.getAssets().open("xeno_schema_2.sql");

        InputStream finalSchema1 = testContext.getAssets().open("xeno_schema_1.sql");
        InputStream finalSchema2 = testContext.getAssets().open("xeno_schema_2.sql");

        SQLiteSchemaVerifierHelper.clearDatabase(context);
        SQLiteUpdateTestDatabase database = SQLiteUpdateTestDatabase.builder(1, schema1)
                .addPopulator(datasource -> {
                    Logger.info("execute populator");
                    XenoApplication.fillCountryCodes(context);
                })
                .addVersionUpdateTask(2, (datasource, previousVersion, currentVersion) ->
                        XenoApplication.migrationVersion2(context, datasource)
                )
                .build();

        database.updateAndVerify(1, finalSchema1);
        database.updateAndVerify(2, finalSchema2);
    }

    @Test
    public void testVersion2() throws Exception {
        // Context of the app under test.
        Context testContext = InstrumentationRegistry.getContext();
        Context context = InstrumentationRegistry.getTargetContext();

        KriptonLibrary.init(context);
        InputStream schema2 = testContext.getAssets().open("xeno_schema_2.sql");
        InputStream finalSchema2 = testContext.getAssets().open("xeno_schema_2.sql");

        SQLiteSchemaVerifierHelper.clearDatabase(context);
        SQLiteUpdateTestDatabase database = SQLiteUpdateTestDatabase.builder(2, schema2)
                .addPopulator(datasource -> {
                    Logger.info("execute populator");
                    XenoApplication.fillCountryCodes(context);
                })
                .addVersionUpdateTask(2, (datasource, previousVersion, currentVersion) ->
                        XenoApplication.migrationVersion2(context, datasource)
                )
                .build();

        database.updateAndVerify(2, finalSchema2);
    }
}

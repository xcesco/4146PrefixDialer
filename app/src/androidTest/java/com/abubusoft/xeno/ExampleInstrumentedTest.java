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
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context testContext = InstrumentationRegistry.getContext();
        Context context = InstrumentationRegistry.getTargetContext();

        KriptonLibrary.init(context);
       // assertEquals("abubusoft.com.xeno", appContext.getPackageName());
        InputStream schema1 = testContext.getAssets().open("xeno_schema_1.sql");
        InputStream schema2 = testContext.getAssets().open("xeno_schema_2.sql");

       // String a=IOUtils.readText(schema1);
        //Logger.info(a);

        SQLiteSchemaVerifierHelper.clearDatabase(context);
        SQLiteUpdateTestDatabase database = SQLiteUpdateTestDatabase.builder(1, schema1)
                .addVersionUpdateTask(2, (SQLiteDatabase datasource, int previousVersion, int currentVersion) -> {
                        SQLiteUpdateTaskHelper.renameTablesWithPrefix(datasource, "tmp_");

                        SQLiteUpdateTaskHelper.executeSQL(datasource, schema2);
                        SQLiteUpdateTaskHelper.executeSQL(datasource, "INSERT INTO phone_number SELECT * FROM tmp_phone_number;");

                        // SQLiteUpdateTaskHelper.dropTablesWithPrefix(datasource, "tmp_");
                }).build();

        database.updateAndVerify(2, schema2);
    }
}

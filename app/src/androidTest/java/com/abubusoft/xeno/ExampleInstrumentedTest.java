package com.abubusoft.xeno;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.abubusoft.kripton.android.KriptonLibrary;
import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.android.commons.IOUtils;
import com.abubusoft.kripton.android.sqlite.SQLiteSchemaVerifierHelper;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTask;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTaskHelper;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTestDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.abubusoft.xeno.test.R;

import java.io.File;
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

        SQLiteSchemaVerifierHelper.clearDatabase(context);

        SQLiteUpdateTestDatabase database = SQLiteUpdateTestDatabase.builder(1, testContext, R.raw.xeno_schema_1)
                .addVersionUpdateTask(new SQLiteUpdateTask(2) {
                    @Override
                    public void execute(SQLiteDatabase database) {
                        SQLiteUpdateTaskHelper.renameTablesWithPrefix(database, "tmp_");
                        SQLiteUpdateTaskHelper.executeSQL(database, testContext, R.raw.xeno_schema_2);
                        SQLiteUpdateTaskHelper.dropTablesWithPrefix(database, "tmp_");
                    }
                }).build();

        database.updateAndVerify(2, testContext, R.raw.xeno_schema_2);
    }

    @Test
    public void schoolTest() throws Exception {
        // Context of the app under test.
        Context testContext = InstrumentationRegistry.getContext();
        Context context = InstrumentationRegistry.getTargetContext();

        KriptonLibrary.init(context);
        // assertEquals("abubusoft.com.xeno", appContext.getPackageName());
        InputStream schema1 = testContext
                .getResources()
                .openRawResource(R.raw.school_schema_1);
        SQLiteSchemaVerifierHelper.clearDatabase(context);

        SQLiteUpdateTestDatabase database = SQLiteUpdateTestDatabase.builder(1, schema1)
                .addVersionUpdateTask(new SQLiteUpdateTask(2) {
                    @Override
                    public void execute(SQLiteDatabase database) {
                        SQLiteUpdateTaskHelper.executeSQL(database, testContext, R.raw.school_update_1_2);
                    }
                }).build();

        database.updateAndVerify(2, testContext
                .getResources()
                .openRawResource(R.raw.school_schema_2));
    }
}

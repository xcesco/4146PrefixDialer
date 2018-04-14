package com.abubusoft.xeno;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.abubusoft.kripton.android.KriptonLibrary;
import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.android.commons.IOUtils;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTask;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTaskHelper;
import com.abubusoft.kripton.android.sqlite.SQLiteUpdateTestDatabase;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.abubusoft.xeno.test.R;

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
        InputStream schema1 = testContext
                .getResources()
                .openRawResource(R.raw.xeno_schema_1);

        InputStream schema2 = testContext
                .getResources()
                .openRawResource(R.raw.xeno_schema_2);

       // String a=IOUtils.readText(schema1);
        //Logger.info(a);

        SQLiteUpdateTestDatabase database = SQLiteUpdateTestDatabase.builder(1, context, schema1)
                .addVersionUpdateTask(new SQLiteUpdateTask(2) {
                    @Override
                    public void execute(SQLiteDatabase database) {
                        SQLiteUpdateTaskHelper.renameTablesWithPrefix(database, "tmp_");
                        SQLiteUpdateTaskHelper.executeSQL(database, schema2);
                        SQLiteUpdateTaskHelper.dropTablesWithPrefix(database, "tmp_");
                    }
                }).build();

        database.updateAndVerify(2, schema2);
    }
}

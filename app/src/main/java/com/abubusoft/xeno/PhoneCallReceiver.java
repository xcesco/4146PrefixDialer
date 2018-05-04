package com.abubusoft.xeno;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.android.sqlite.TransactionResult;
import com.abubusoft.kripton.common.One;
import com.abubusoft.kripton.common.Pair;
import com.abubusoft.xeno.events.EventPhoneNumberAdded;
import com.abubusoft.xeno.model.ActionType;
import com.abubusoft.xeno.model.Country;
import com.abubusoft.xeno.model.PhoneNumber;
import com.abubusoft.xeno.persistence.BindXenoDaoFactory;
import com.abubusoft.xeno.persistence.BindXenoDataSource;
import com.abubusoft.xeno.persistence.CountryDaoImpl;
import com.abubusoft.xeno.persistence.PhoneDaoImpl;
import com.abubusoft.xeno.persistence.PrefixConfigDaoImpl;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.lang.reflect.Method;
import java.util.Date;

import com.abubusoft.xeno.model.PrefixConfig;

import org.greenrobot.eventbus.EventBus;

import static android.content.Context.WINDOW_SERVICE;
import static com.abubusoft.xeno.model.ActionType.ADD_PREFIX;

public class PhoneCallReceiver extends BroadcastReceiver {

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;

    private static Date callStartTime;

    public static Pair<String, String> getContactName(Context context, String phoneNumber) {
        Pair<String, String> result = new Pair<>();

        if (context.checkCallingOrSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return result;
        }

        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        String contactId = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

            result.value0 = contactName;
            result.value1 = contactId;
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return result;
    }

    public enum ResultType {
        SKIP,
        PROCEED,
        ASK,
        ERROR
    }

    public static class ExecutionResult {
        public ExecutionResult(ResultType type) {
            this.result = type;
        }

        public ExecutionResult(ResultType type, String phoneNumber) {
            this.result = type;
            this.phoneNumber = phoneNumber;
        }

        public ResultType result;
        public String phoneNumber;
        public Country country;
        public PrefixConfig prefixConfig;

        public ExecutionResult(ResultType type, String phoneNumber, PrefixConfig prefixConfig) {
            this.result = type;
            this.phoneNumber = phoneNumber;
            this.prefixConfig = prefixConfig;
        }

        public ExecutionResult(ResultType type, String phoneNumber, PrefixConfig prefixConfig, Country country) {
            this.result = type;
            this.phoneNumber = phoneNumber;
            this.prefixConfig = prefixConfig;
            this.country = country;
        }
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            final One<String> currentNumber = new One<>(intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER));
            final Pair<String, String> contact = getContactName(context, currentNumber.value0);

            ExecutionResult result = BindXenoDataSource.instance().executeBatch((BindXenoDaoFactory daoFactory) -> {
                    final PhoneDaoImpl daoPhone = daoFactory.getPhoneDao();
                    PrefixConfigDaoImpl daoPrefix = daoFactory.getPrefixConfigDao();
                    CountryDaoImpl daoCountry = daoFactory.getCountryDao();

                    final PrefixConfig config = daoPrefix.selectOne();
                    if (!config.enabled) {
                        Logger.info("Eseguo skip!");
                        return new ExecutionResult(ResultType.SKIP);
                    }

                    if (currentNumber.value0.startsWith(config.dualBillingPrefix)) {
                        Logger.info("PHONE already inserted, we do nothing");

                        currentNumber.value0 += ",,1";

                        Logger.info("Call " + currentNumber.value0);
                        return new ExecutionResult(ResultType.PROCEED, currentNumber.value0);
                    } else {
                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        Phonenumber.PhoneNumber temp = null;
                        try {
                            temp = phoneUtil.parse(currentNumber.value0, config.defaultCountry);
                        } catch (NumberParseException e) {
                            e.printStackTrace();
                        }
                        final String number = phoneUtil.format(temp, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

                        Country tempCountry;
                        tempCountry = daoCountry.selectByCountry(config.defaultCountry);
                        final Country country = tempCountry;

                        PhoneNumber phone = daoPhone.selectByNumber(number);
                        if (phone != null) {
                            if (phone.action==ADD_PREFIX) {
                                //this.setResultData(config.dualBillingPrefix + number + (config.dualBillingAddSuffix ? "pp1": ""));
                                String phoneNumber = config.dualBillingPrefix + number.replace("+", "00");

                                phoneNumber += ",,1";

                                Logger.info("Call " + phoneNumber);
                                return new ExecutionResult(ResultType.PROCEED, phoneNumber, config);
                            }else {
                                    return new ExecutionResult(ResultType.PROCEED, phone.number);
                            }

                        } else {
                            return new ExecutionResult(ResultType.ASK, number, config, country);
                        }
                    }
            });

            switch (result.result) {
                case SKIP:
                    // do nothing
                    return;
                case PROCEED:
                    Logger.info("Call " + result.phoneNumber);
                    this.setResultData(result.phoneNumber);
                    return;
                case ASK:
                    displayWindow(context, result, contact);
                    break;
                case ERROR:
                    break;
            }


        }
    }

    @SuppressLint("NewApi")
    public static boolean canDrawOverlayViews(Context con) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return true;

        try {
            return Settings.canDrawOverlays(con);
        } catch (NoSuchMethodError e) {
            return canDrawOverlaysUsingReflection(con);
        }

    }

    View myView;
    WindowManager windowManager;

    public void displayWindow(Context context, final ExecutionResult result, Pair<String, String> contact) {
        final One<Boolean> windowOpened = new One<Boolean>(false);

        if (!canDrawOverlayViews(context)) {
            //Toast.makeText(context,"")
            Logger.warn("Permission " + Manifest.permission.SYSTEM_ALERT_WINDOW + " is not enabled");
            // non possiamo visualizzare nulla
            return;
        }

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                //WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                //WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                LAYOUT_FLAG,
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                PixelFormat.RGBA_8888);
        params.gravity = Gravity.TOP | Gravity.LEFT;

        final Pair<Boolean, String> result0 = new Pair<>();

        windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        myView = inflater.inflate(R.layout.layout_prefix_dialog, null);

        myView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(final View v) {
                windowOpened.value0 = true;

                new CountDownTimer(result.prefixConfig.dialogTimeout * 1000, 400) { // adjust the milli seconds here

                    public void onTick(long millisUntilFinished) {
                        TextView txtCountDown = (TextView) v.findViewById(R.id.prefix_dialog_counter);
                        txtCountDown.setText(context.getString(R.string.prefix_countdown_label, millisUntilFinished / 1000));
                    }

                    public void onFinish() {
                        Logger.info("Timer is finished");
                        if (windowOpened.value0) {
                            windowManager.removeView(myView);
                        }
                    }
                }.start();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                windowOpened.value0 = false;
            }
        });

        ((TextView) myView.findViewById(R.id.prefix_dialog_name)).setText(contact.value0);
        ((TextView) myView.findViewById(R.id.prefix_dialog_number)).setText(result.phoneNumber);
        ((TextView) myView.findViewById(R.id.prefix_dialog_ask)).setText(context.getString(R.string.prefix_dialog_question, result.prefixConfig.dualBillingPrefix));

        myView.findViewById(R.id.prefix_dialog_prefix_add).setOnClickListener((View v) -> {
            manageOnClick(context, ADD_PREFIX, result, contact, windowManager, myView);
            //windowManager.removeView(myView);

        });

        myView.findViewById(R.id.prefix_dialog_prefix_none).setOnClickListener((View v) -> {
            manageOnClick(context, ActionType.DO_NOTHING, result, contact, windowManager, myView);

        });

        myView.findViewById(R.id.prefix_dialog_call_end).setOnClickListener((View v) -> {
            // non fa niente e chiude la telefonata
            windowManager.removeView(myView);
        });

        windowManager.addView(myView, params);

        // chiudiamo la telefonata
        setResultCode(Activity.RESULT_CANCELED);
        setResultData(null);
        abortBroadcast();
    }

    private void manageOnClick(final Context context, ActionType action, final ExecutionResult result, Pair<String, String> contact, WindowManager windowManager, View myView) {
        final PhoneNumber phone = new PhoneNumber();
        BindXenoDataSource.instance().execute((BindXenoDaoFactory daoFactory) -> {
            Logger.info("subscribe " + Thread.currentThread().getName());


            phone.action = action;
            phone.number = result.phoneNumber;
            phone.countryCode = result.country.code;
            phone.contactName = contact.value0;
            phone.contactId = contact.value1;

            daoFactory.getPhoneDao().insert(phone);

            return TransactionResult.COMMIT;
        });
        Logger.info("observe on " + Thread.currentThread().getName());

        EventBus.getDefault().post(new EventPhoneNumberAdded(phone));

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        //Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + config.dualBillingPrefix + number+ (config.dualBillingAddSuffix ? "pp1": "")));
        //String phoneNumber = config.dualBillingPrefix + number.replace("+", "00")  /*+ (config.dualBillingAddSuffix ? ",1,2,3,4,5,6": "")*/;
        String phoneNumberString = result.prefixConfig.dualBillingPrefix + result.phoneNumber.replace("+", "00");

        Logger.info("Redirect to " + phone);
        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumberString));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

        windowManager.removeView(myView);
    }


    public static boolean canDrawOverlaysUsingReflection(Context context) {
        try {
            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            Class clazz = AppOpsManager.class;
            Method dispatchMethod = clazz.getMethod("checkOp", new Class[]{int.class, int.class, String.class});
            //AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
            int mode = (Integer) dispatchMethod.invoke(manager, new Object[]{24, Binder.getCallingUid(), context.getApplicationContext().getPackageName()});

            return AppOpsManager.MODE_ALLOWED == mode;

        } catch (Exception e) {
            return false;
        }

    }

}
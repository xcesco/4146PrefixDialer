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
import android.widget.Toast;

import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.common.Pair;
import com.abubusoft.xeno.events.EventPhoneNumberAdded;
import com.abubusoft.xeno.model.ActionType;
import com.abubusoft.xeno.model.Country;
import com.abubusoft.xeno.model.PhoneNumber;
import com.abubusoft.xeno.persistence.BindXenoDataSource;
import com.abubusoft.xeno.persistence.CountryDaoImpl;
import com.abubusoft.xeno.persistence.PhoneDaoImpl;
import com.abubusoft.xeno.persistence.PrefixConfigDaoImpl;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Locale;

import com.abubusoft.xeno.model.PrefixConfig;

import org.greenrobot.eventbus.EventBus;

import static android.content.Context.WINDOW_SERVICE;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.abubusoft.xeno.R.string.prefix_dialog_question;
import static com.google.i18n.phonenumbers.Phonenumber.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY;

public class PhoneCallReceiver extends BroadcastReceiver {

    private static int lastState = TelephonyManager.CALL_STATE_IDLE;

    private static Date callStartTime;

    private static boolean isIncoming;

    private static String currentNumber;  //because the passed incoming is only valid in ringing

    public static Pair<String, String> getContactName(Context context, String phoneNumber) {
        Pair<String, String> result=new Pair<>();

        if (context.checkCallingOrSelfPermission(Manifest.permission.READ_CONTACTS)!=PackageManager.PERMISSION_GRANTED)
        {
            return result;
        }

        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        String contactId=null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
            contactId= cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup._ID));

            result.value0=contactName;
            result.value1=contactId;
        }

        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }

        return result;
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            currentNumber = intent.getExtras().getString(Intent.EXTRA_PHONE_NUMBER);
            final Pair<String, String> contact=getContactName(context, currentNumber);

            BindXenoDataSource dataSource = BindXenoDataSource.open();
            try {
                final PhoneDaoImpl daoPhone = dataSource.getPhoneDao();
                PrefixConfigDaoImpl daoPrefix = dataSource.getPrefixConfigDao();
                CountryDaoImpl daoCountry = dataSource.getCountryDao();
                final PrefixConfig config = daoPrefix.selectOne();

                if (!config.enabled)
                {
                    Logger.info("Eseguo skip!");
                    return;
                }

                if (currentNumber.startsWith(config.dualBillingPrefix)) {
                    Logger.info("PHONE already inserted, we do nothing");

                    currentNumber+=",,1";

                    Logger.info("Call "+currentNumber);
                    this.setResultData(currentNumber);

                } else {
                    try {
                        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
                        Phonenumber.PhoneNumber temp = phoneUtil.parse(currentNumber, config.defaultCountry);
                        final String number = phoneUtil.format(temp, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);

                        Country tempCountry;
                        tempCountry=daoCountry.selectByCountry(config.defaultCountry);
                        final Country country=tempCountry;

                        PhoneNumber phone = daoPhone.selectByNumber(number);
                        if (phone != null) {
                            switch (phone.action) {
                                case ADD_PREFIX:
                                    //this.setResultData(config.dualBillingPrefix + number + (config.dualBillingAddSuffix ? "pp1": ""));
                                    String phoneNumber=config.dualBillingPrefix + number.replace("+","00");

                                    phoneNumber+=",1";

                                    Logger.info("Call "+phoneNumber);
                                    this.setResultData(phoneNumber);
                                    break;
                                case DO_NOTHING:
                                    break;
                            }
                        } else {
                            if (!canDrawOverlayViews(context))
                            {
                                //Toast.makeText(context,"")
                                Logger.warn("Permission "+Manifest.permission.SYSTEM_ALERT_WINDOW +" is not enabled");
                                // non possiamo visualizzare nulla
                                return;
                            }


                            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                    WindowManager.LayoutParams.MATCH_PARENT,
                                    WindowManager.LayoutParams.MATCH_PARENT,
                                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                                    //WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                    PixelFormat.RGBA_8888);

                            final Pair<Boolean, String> result = new Pair<>();

                            final WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);

                            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            final View myView = inflater.inflate(R.layout.layout_prefix_dialog, null);

                            params.gravity = Gravity.TOP | Gravity.LEFT;
                            myView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                                @Override
                                public void onViewAttachedToWindow(final View v) {
                                    result.value0 = true;

                                    new CountDownTimer(config.dialogTimeout*1000, 400) { // adjust the milli seconds here

                                        public void onTick(long millisUntilFinished) {
                                            TextView txtCountDown = (TextView) v.findViewById(R.id.prefix_dialog_counter);
                                            txtCountDown.setText(context.getString(R.string.prefix_countdown_label, millisUntilFinished / 1000));
                                        }

                                        public void onFinish() {
                                            if (result.value0) {
                                                windowManager.removeView(myView);
                                            }
                                        }
                                    }.start();
                                }

                                @Override
                                public void onViewDetachedFromWindow(View v) {
                                    result.value0 = false;
                                }
                            });

                            ((TextView) myView.findViewById(R.id.prefix_dialog_name)).setText(contact.value0);
                            ((TextView) myView.findViewById(R.id.prefix_dialog_number)).setText(number);
                            ((TextView) myView.findViewById(R.id.prefix_dialog_ask)).setText(context.getString(R.string.prefix_dialog_question, config.dualBillingPrefix));


                            myView.findViewById(R.id.prefix_dialog_prefix_add).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    BindXenoDataSource dataSource = BindXenoDataSource.open();
                                    try {
                                        PhoneNumber phone = new PhoneNumber();

                                        phone.action = ActionType.ADD_PREFIX;
                                        phone.number = number;
                                        phone.countryCode=country.code;
                                        phone.contactName=contact.value0;
                                        phone.contactId=contact.value1;

                                        dataSource.getPhoneDao().insert(phone);

                                        EventBus.getDefault().post(new EventPhoneNumberAdded(phone));

                                    } finally {
                                        dataSource.close();

                                        if (result.value0) {
                                            windowManager.removeView(myView);
                                        }

                                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            return;
                                        }
                                        //Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + config.dualBillingPrefix + number+ (config.dualBillingAddSuffix ? "pp1": "")));
                                        String phoneNumber=config.dualBillingPrefix + number.replace("+","00")  /*+ (config.dualBillingAddSuffix ? ",1,2,3,4,5,6": "")*/;
                                        Logger.info("Redirect to "+phoneNumber);
                                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intent);
                                    }

                                }
                            });

                            myView.findViewById(R.id.prefix_dialog_prefix_none).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    BindXenoDataSource dataSource = BindXenoDataSource.open();
                                    try {
                                        PhoneNumber phone = new PhoneNumber();

                                        phone.action = ActionType.DO_NOTHING;
                                        phone.number = number;
                                        phone.countryCode=country.code;
                                        phone.contactName=contact.value0;
                                        phone.contactId=contact.value1;

                                        dataSource.getPhoneDao().insert(phone);

                                        EventBus.getDefault().post(new EventPhoneNumberAdded(phone));
                                    } finally {
                                        dataSource.close();

                                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                            return;
                                        }
                                        Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        context.startActivity(intent);

                                        if (result.value0) {
                                            windowManager.removeView(myView);
                                        }


                                    }
                                }
                            });

                            myView.findViewById(R.id.prefix_dialog_call_end).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    // non fa niente e chiude la telefonata
                                    if (result.value0) {
                                        windowManager.removeView(myView);
                                    }
                                }
                            });

                            windowManager.addView(myView, params);


                            // chiudiamo la telefonata
                            setResultCode(Activity.RESULT_CANCELED);
                            setResultData(null);
                            abortBroadcast();
                        }
                        //  }

                    } catch (Exception e) {
                        e.printStackTrace();
                        System.err.println("NumberParseException was thrown: " + e.toString());
                    }
                }
            } finally {
                dataSource.close();
            }

        } else {
            String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
            String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
            int state = 0;
            if (stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                state = TelephonyManager.CALL_STATE_IDLE;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                state = TelephonyManager.CALL_STATE_OFFHOOK;
            } else if (stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                state = TelephonyManager.CALL_STATE_RINGING;
            }


            onCallStateChanged(context, state, number);
        }
    }

    @SuppressLint("NewApi")
    public static boolean canDrawOverlayViews(Context con){
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.LOLLIPOP)
            return true;

        try {
            return Settings.canDrawOverlays(con);
        }
        catch(NoSuchMethodError e){
            return canDrawOverlaysUsingReflection(con);
        }

    }



    public static boolean canDrawOverlaysUsingReflection(Context context) {

        try {

            AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            Class clazz = AppOpsManager.class;
            Method dispatchMethod = clazz.getMethod("checkOp", new Class[] { int.class, int.class, String.class });
            //AppOpsManager.OP_SYSTEM_ALERT_WINDOW = 24
            int mode = (Integer) dispatchMethod.invoke(manager, new Object[] { 24, Binder.getCallingUid(), context.getApplicationContext().getPackageName() });

            return AppOpsManager.MODE_ALLOWED == mode;

        } catch (Exception e) {  return false;  }

    }

    //Derived classes should override these to respond to specific events of interest
    //protected abstract void onIncomingCallReceived(Context ctx, String number, Date start);
    //protected abstract void onIncomingCallAnswered(Context ctx, String number, Date start);
    //protected abstract void onIncomingCallEnded(Context ctx, String number, Date start, Date end);

    //protected abstract void onOutgoingCallStarted(Context ctx, String number, Date start);
    // protected abstract void onOutgoingCallEnded(Context ctx, String number, Date start, Date end);

    //   protected abstract void onMissedCall(Context ctx, String number, Date start);

    //Deals with actual events

    //Incoming call-  goes from IDLE to RINGING when it rings, to OFFHOOK when it's answered, to IDLE when its hung up
    //Outgoing call-  goes from IDLE to OFFHOOK when it dials out, to IDLE when hung up
    public void onCallStateChanged(Context context, int state, String number) {
        if (lastState == state) {
            //No change, debounce extras
            return;
        }
        switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                isIncoming = true;
                callStartTime = new Date();
                currentNumber = number;
                // onIncomingCallReceived(context, number, callStartTime);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                //Transition of ringing->offhook are pickups of incoming calls.  Nothing done on them
                if (lastState != TelephonyManager.CALL_STATE_RINGING) {
                    isIncoming = false;
                    callStartTime = new Date();
                    //   onOutgoingCallStarted(context, currentNumber, callStartTime);
                } else {
                    isIncoming = true;
                    callStartTime = new Date();
                    // onIncomingCallAnswered(context, currentNumber, callStartTime);
                }

                break;
            case TelephonyManager.CALL_STATE_IDLE:
                //Went to idle-  this is the end of a call.  What type depends on previous state(s)
                if (lastState == TelephonyManager.CALL_STATE_RINGING) {
                    //Ring but no pickup-  a miss
                    //onMissedCall(context, currentNumber, callStartTime);
                } else if (isIncoming) {
                    //onIncomingCallEnded(context, currentNumber, callStartTime, new Date());
                } else {
                    //onOutgoingCallEnded(context, currentNumber, callStartTime, new Date());
                }
                break;
        }
        lastState = state;
    }
}
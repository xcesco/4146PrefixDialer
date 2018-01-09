package com.abubusoft.xeno;

import android.content.Context;

import com.abubusoft.kripton.android.Logger;

import java.util.Date;

public class CallReceiver extends PhoneCallReceiver {

    protected void onIncomingCallReceived(Context ctx, String number, Date start)
    {
        Logger.info("onIncomingCallReceived");
    }

    protected void onIncomingCallAnswered(Context ctx, String number, Date start)
    {
        Logger.info("onIncomingCallAnswered");
    }

    protected void onIncomingCallEnded(Context ctx, String number, Date start, Date end)
    {
        Logger.info("onIncomingCallEnded");
    }

    protected void onOutgoingCallStarted(Context ctx, String number, Date start)
    {
        Logger.info("onOutgoingCallStarted");
    } 

    protected void onOutgoingCallEnded(Context ctx, String number, Date start, Date end)
    {
        Logger.info("onOutgoingCallEnded");
    }

    protected void onMissedCall(Context ctx, String number, Date start)
    {
        Logger.info("onMissedCall");
    }

}
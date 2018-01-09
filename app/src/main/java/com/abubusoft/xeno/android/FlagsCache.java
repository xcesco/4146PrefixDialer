package com.abubusoft.xeno.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

public class FlagsCache {

    protected static final FlagsCache instance = new FlagsCache();

    public static FlagsCache instance() {
        return instance;
    }

    protected Map<String, Bitmap> flags= new HashMap<>();

    protected boolean initialized = false;

    public Bitmap getFlagBitmap(Context context, String countryCode) {
       if (!flags.containsKey(countryCode))
       {
           flags.put(countryCode, getFlagBitmapInternal(context, countryCode));
       }

        return flags.get(countryCode);
    }

    public static Bitmap getFlagBitmapInternal(Context context, String countryCode) {
        Bitmap image;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inScaled = false;
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;// important

        String valueName = "ic_flag_"+countryCode.toLowerCase();
        int resourceId = context.getResources().getIdentifier(valueName, "drawable", context.getPackageName());

        if (resourceId!=0) {
            image = BitmapFactory.decodeResource(context.getResources(), resourceId);
            return image;
        }

        return null;
    }


}

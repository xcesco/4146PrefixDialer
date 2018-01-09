package com.abubusoft.xeno.android;

import android.content.Context;
import android.widget.EditText;

import com.abubusoft.xeno.R;

import java.util.regex.Pattern;

/**
 * Created by xcesco on 27/02/2017.
 */

public abstract class Validation {

    // Regular Expression
    // you can change the expression based on your need
    private static final String EMAIL_REGEX = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

//    // call this method when you need to check email validation
//    public static boolean isEmailAddress(EditText editText, boolean required) {
//        return isValid(editText, EMAIL_REGEX, EMAIL_MSG, required);
//    }
//
//    // call this method when you need to check phone number validation
//    public static boolean isPhoneNumber(EditText editText, boolean required) {
//        return isValid(editText, PHONE_REGEX, PHONE_MSG, required);
//    }

    // return true if the input field is valid, based on the parameter passed
//    public static boolean isValid(Context context, EditText editText, String regex, String errMsg, boolean required) {
//
//        String text = editText.getText().toString().trim();
//        // clearing the error, if it was previously set by some other values
//        editText.setError(null);
//
//        // text required and editText is blank, so return false
//        if ( required && !hasText(editText) ) return false;
//
//        // pattern doesn't match so returning false
//        if (required && !Pattern.matches(regex, text)) {
//            editText.setError(errMsg);
//            return false;
//        };
//
//        return true;
//    }

    // check the input field has any text or not
    // return true if it contains text otherwise false
    public static boolean hasText(Context context, EditText editText) {
        String text = editText.getText().toString().trim();
        editText.setError(null);

        // length 0 means there is no text
        if (text.length() == 0) {
            editText.setError(context.getString(R.string.validation_required));
            return false;
        }

        return true;
    }

    public static boolean isIntegerGreaterEqualThan(Context context, EditText editText, int minLimit) {
        String text = editText.getText().toString().trim();
        editText.setError(null);

        // length 0 means there is no text
        if (text.length() == 0) {
            editText.setError(context.getString(R.string.validation_required));
            return false;
        }

        if (Integer.parseInt(text)<minLimit)
        {
            editText.setError(context.getString(R.string.validation_integer_greater_eq_than, minLimit));
            return false;
        }

        return true;
    }

    public static boolean isIntegerBetween(Context context, EditText editText, int minLimit, int maxLimit) {
        String text = editText.getText().toString().trim();
        editText.setError(null);

        // length 0 means there is no text
        if (text.length() == 0) {
            editText.setError(context.getString(R.string.validation_required));
            return false;
        }

        if (Integer.parseInt(text)<minLimit)
        {
            editText.setError(context.getString(R.string.validation_integer_greater_eq_than, minLimit));
            return false;
        }

        if (Integer.parseInt(text)>maxLimit)
        {
            editText.setError(context.getString(R.string.validation_integer_less_eq_than, minLimit));
            return false;
        }

        return true;
    }
}

package com.abubusoft.xeno.events;

import com.abubusoft.xeno.model.PhoneNumber;

/**
 * Created by xcesco on 28/02/2017.
 */
public class EventPhoneNumberAdded {
    public final PhoneNumber item;

    public EventPhoneNumberAdded(PhoneNumber item) {
        this.item=item;
    }
}

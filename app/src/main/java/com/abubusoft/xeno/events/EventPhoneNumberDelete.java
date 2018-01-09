package com.abubusoft.xeno.events;

import com.abubusoft.xeno.model.PhoneNumber;

/**
 * Created by xcesco on 28/02/2017.
 */
public class EventPhoneNumberDelete {
    public final PhoneNumber item;

    public EventPhoneNumberDelete(PhoneNumber item) {
        this.item=item;
    }
}

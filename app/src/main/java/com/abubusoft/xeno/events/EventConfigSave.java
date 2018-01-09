package com.abubusoft.xeno.events;

import com.abubusoft.xeno.model.PrefixConfig;

public class EventConfigSave {

    public EventConfigSave(PrefixConfig config)
    {
        this.config=config;
    }

    public PrefixConfig config;
}

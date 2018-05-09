package com.abubusoft.xeno.model;

import com.abubusoft.kripton.android.annotation.BindSqlType;

@BindSqlType
public class PrefixConfig {

    public long id;

    public String defaultCountry;

    public String dualBillingPrefix;

    public boolean enabled;

    public long dialogTimeout;

    //public boolean skipMessage;
}

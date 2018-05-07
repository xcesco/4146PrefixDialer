package com.abubusoft.xeno.model;

import com.abubusoft.kripton.android.ColumnType;
import com.abubusoft.kripton.android.annotation.BindSqlColumn;
import com.abubusoft.kripton.annotation.BindType;

@BindType
public class PhoneNumber {

    public long id;

    public ActionType action;

    @BindSqlColumn(nullable = false, columnType = ColumnType.UNIQUE)
    public String number;

    public String countryCode;

    public String contactName;

    public String contactId;
}

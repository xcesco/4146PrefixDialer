package com.abubusoft.xeno.persistence;

import com.abubusoft.kripton.android.annotation.BindDataSource;

@BindDataSource(daoSet = {PhoneDao.class, PrefixConfigDao.class, CountryDao.class}, fileName = "xeno.db", asyncTask = true, schema = true, version = 2)
public interface XenoDataSource {
}

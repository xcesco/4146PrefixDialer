package com.abubusoft.xeno.persistence;

import com.abubusoft.kripton.android.annotation.BindDao;
import com.abubusoft.kripton.android.annotation.BindSqlSelect;
import com.abubusoft.xeno.model.Country;

import java.util.List;

/**
 * Created by xcesco on 26/02/2017.
 */
@BindDao(Country.class)
public interface CountryDao extends AbstractDao<Country> {

    @BindSqlSelect(orderBy = "name asc")
    List<Country> selectAll();

    @BindSqlSelect(where ="callingCode = ${callingCode}")
    Country selectByCallingCode(String callingCode);

    @BindSqlSelect(where ="code = ${code}")
    Country selectByCountry(String code);
}

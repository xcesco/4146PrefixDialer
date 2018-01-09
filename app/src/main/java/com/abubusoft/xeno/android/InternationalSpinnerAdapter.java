package com.abubusoft.xeno.android;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.abubusoft.xeno.R;
import com.abubusoft.xeno.model.Country;

import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by xcesco on 27/02/2017.
 */

public class InternationalSpinnerAdapter extends SpinnerAdapter<Country> {
    private final Locale locale;

    public InternationalSpinnerAdapter(Context context, int layoutId, List<Country> list, Locale locale) {
        super(context, layoutId, list);
        this.locale=locale;
    }

    @BindView(R.id.spinner_prefix_text)
    public TextView tvPrefixText;

    @BindView(R.id.spinner_prefix_number)
    public TextView tvPrefixNumber;

    @BindView(R.id.spinner_prefix_image)
    public ImageView ivPrefixImage;

    @Override
    protected void bindView(View view, Country item) {
        ButterKnife.bind(this, view);

        item.bitmap = FlagsCache.instance().getFlagBitmap(getContext(), item.code);
        if (item.bitmap!=null) ivPrefixImage.setImageBitmap(item.bitmap);

        tvPrefixText.setText(item.getTranslatedName(locale.getLanguage()));
        tvPrefixNumber.setText("( +"+item.callingCode+")");
    }
}

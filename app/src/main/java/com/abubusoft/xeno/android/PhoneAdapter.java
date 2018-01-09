package com.abubusoft.xeno.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.abubusoft.kripton.common.StringUtils;
import com.abubusoft.xeno.R;
import com.abubusoft.xeno.model.ActionType;
import com.abubusoft.xeno.model.PhoneNumber;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PhoneAdapter extends AbstractRecyclerViewAdapter<PhoneNumber, PhoneAdapter.ViewHolder> {

    private final PhoneActions listener;
    private final PhoneNumberUtil phoneUtil;

    public interface PhoneActions {
        void onDeletePhoneConfiguration(PhoneNumber item);

        void onSelectPhoneConfiguration(PhoneNumber item);

        /**
         * Invia un smss
         * @param phone
         */
        void onSendSms(PhoneNumber phone);
    }

    private final Context context;
    Bitmap image1;
    Bitmap image2;

    public PhoneAdapter(Context context, PhoneActions listener) {
        this.context = context;
        this.listener = listener;
        phoneUtil = PhoneNumberUtil.getInstance();
        image1 = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_call_prefix_blue_36dp);
        image2 = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_phone_orange_a700_36dp);
    }

    @Override
    public ViewHolder createViewHolder(View view) {
        final ViewHolder holder = new ViewHolder(view);

        ButterKnife.bind(holder, view);

        return holder;
    }

    @Override
    public int getViewLayoutResourceId() {
        return R.layout.layout_phone;
    }

    @Override
    public void onBindItem(final ViewHolder holder, final PhoneNumber item) {
        holder.phone = item;

        holder.imgPhoneDualBillingPrefix.setImageBitmap(item.action == ActionType.ADD_PREFIX ? image1 : image2);
        holder.imgPhoneInternationalPrefix.setImageBitmap(FlagsCache.instance().getFlagBitmap(context, item.countryCode));

        try {
            Phonenumber.PhoneNumber temp = phoneUtil.parse(item.number, null);
            String formattedNumber = phoneUtil.format(temp, PhoneNumberUtil.PhoneNumberFormat.NATIONAL);

            if (StringUtils.hasText(item.contactName)) {
                holder.tvTxtPhoneName.setText(item.contactName);

                holder.tvTxtPhoneNumber.setText(formattedNumber);
                holder.tvTxtPhoneNumber.setVisibility(View.VISIBLE);

            } else {
                holder.tvTxtPhoneName.setText(formattedNumber);

                holder.tvTxtPhoneNumber.setText(null);
                holder.tvTxtPhoneNumber.setVisibility(View.GONE);
            }

            holder.imgPhoneDualBillingPrefix.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onSelectPhoneConfiguration(holder.phone);
                    }
                }
            });

        } catch (NumberParseException e) {
            e.printStackTrace();
        }



        holder.btnPhoneActionSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSendSms(holder.phone);
                }
            }
        });
    }

    public static class ViewHolder extends AbstractRecyclerViewAdapter.ViewHolder {

        ViewHolder(View v) {
            super(v);
        }

        @BindView(R.id.phone_dual_billing_prefix_image)
        ImageView imgPhoneDualBillingPrefix;

        @BindView(R.id.phone_international_prefix_image)
        ImageView imgPhoneInternationalPrefix;

        @BindView(R.id.phone_number)
        TextView tvTxtPhoneNumber;

        @BindView(R.id.phone_name)
        TextView tvTxtPhoneName;

        @BindView(R.id.phone_action_sms)
        ImageButton btnPhoneActionSms;

        PhoneNumber phone;

    }
}

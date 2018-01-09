package com.abubusoft.xeno;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Spinner;

import com.abubusoft.kripton.android.BindAsyncTaskType;
import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.common.Triple;
import com.abubusoft.xeno.android.InternationalSpinnerAdapter;
import com.abubusoft.xeno.android.Validation;
import com.abubusoft.xeno.events.EventEnablePrefix;
import com.abubusoft.xeno.events.EventNoPermission;
import com.abubusoft.xeno.model.Country;
import com.abubusoft.xeno.persistence.BindXenoAsyncTask;
import com.abubusoft.xeno.persistence.BindXenoDaoFactory;
import com.abubusoft.xeno.persistence.BindXenoDataSource;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.util.Locale;

import com.abubusoft.xeno.model.PrefixConfig;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import butterknife.OnItemSelected;
import butterknife.Unbinder;

/**
 * A fragment representing a list of Items.
 * <p/>
 * interface.
 */
public class ConfigFragment extends Fragment {

    private EventBus bus = EventBus.getDefault();

    private PrefixConfig config;

    private Unbinder unbinder;

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @OnItemSelected(R.id.config_default_international_prefix_spinner)
    public void onItemSelected(Spinner spinner, int position) {
        if (config==null) return;

        if (adapter!=null)
        {
            Country country=adapter.getItem(position);
            config.defaultCountry=country.code;
            updateConfig();
        }
    }

    @OnTextChanged(value = {R.id.config_dual_billing_prefix_edit, R.id.config_timeout_prefix_dialog}, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onTextChanged1(CharSequence text) {
        if (config==null) return;

        if (isValidValues() ) {
            config.dualBillingPrefix = txtDualBilling.getText().toString();
            config.dialogTimeout = Long.parseLong(txtTimeOut.getText().toString());

            updateConfig();
        }
    }

    public boolean isValidValues()
    {
        boolean valid = true;
        valid = valid && Validation.hasText(getContext(), txtDualBilling);
        valid = valid && Validation.isIntegerGreaterEqualThan(getContext(), txtTimeOut, 30);

        return valid;
    }

    @OnClick(R.id.config_enable)
    public void onConfiEnabledClick(SwitchCompat view) {
        if (config==null) return;

        config.enabled = view.isChecked();
        if (config.enabled) {
            Logger.info("Enabled!!");
            bus.post(new EventEnablePrefix());
        }

        updateConfig();
    }

    private void updateConfig() {
        /*BindXenoDataSource.instance().executeBatch(new BindXenoDataSource.Batch<Boolean>() {
            @Override
            public Boolean onExecute(BindXenoDaoFactory daoFactory) {
                daoFactory.getPrefixConfigDao().update(config);
                return true;
            }
        });*/

        BindXenoDataSource.instance().executeBatch(daoFactory -> {
                daoFactory.getPrefixConfigDao().update(config);
                return true;
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onNoPermission(EventNoPermission event) {
        // per il momento è l'unica cosa che può cambiare
        config.enabled = false;
        swEnabled.setChecked(config.enabled);
    }

    BindXenoAsyncTask.Simple<Triple<PrefixConfig, List<Country>, Integer>> asyncLoadTask = new BindXenoAsyncTask.Simple<Triple<PrefixConfig, List<Country>, Integer>>(BindAsyncTaskType.READ) {

        @Override
        public Triple<PrefixConfig, List<Country>, Integer> onExecute(BindXenoDataSource dataSource) throws Throwable {
            Triple<PrefixConfig, List<Country>, Integer> result = new Triple<>();

            PrefixConfig config = dataSource.getPrefixConfigDao().selectOne();
            List<Country> list = dataSource.getCountryDao().selectAll();

            int i = 0;
            result.value2 = 0;
            for (Country item : list) {
                if (config.defaultCountry.equals(item.code)) {
                    result.value2 = i;
                }

                i++;
            }

            result.value0 = config;
            result.value1 = list;

            return result;
        }

        @Override
        public void onFinish(Triple<PrefixConfig, List<Country>, Integer> result) {
            adapter = new InternationalSpinnerAdapter(getContext(), R.layout.layout_spinner_prefix, result.value1, Locale.getDefault());
            config = result.value0;

            spSpinner.setAdapter(adapter);
            spSpinner.setSelection(result.value2);

            swEnabled.setChecked(config.enabled);

            if (config.enabled) bus.post(new EventEnablePrefix());

            txtDualBilling.setText(config.dualBillingPrefix);
            txtTimeOut.setText("" + config.dialogTimeout);
        }
    };


    private InternationalSpinnerAdapter adapter;

    @BindView(R.id.config_default_international_prefix_spinner)
    Spinner spSpinner;

    @BindView(R.id.config_enable)
    SwitchCompat swEnabled;

    @BindView(R.id.config_dual_billing_prefix_edit)
    EditText txtDualBilling;

    @BindView(R.id.config_timeout_prefix_dialog)
    EditText txtTimeOut;

    public ConfigFragment() {
    }

    @SuppressWarnings("unused")
    public static ConfigFragment newInstance() {
        ConfigFragment fragment = new ConfigFragment();
        Bundle args = new Bundle();
        //  args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container, false);
        ButterKnife.setDebug(true);
        unbinder = ButterKnife.bind(this, view);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        asyncLoadTask.execute();
        bus.register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        bus.unregister(this);
    }

}

package com.abubusoft.xeno;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.abubusoft.kripton.android.BindAsyncTaskType;
import com.abubusoft.kripton.android.Logger;
import com.abubusoft.kripton.annotation.Bind;
import com.abubusoft.xeno.android.EmptyRecyclerView;
import com.abubusoft.xeno.android.PhoneAdapter;
import com.abubusoft.xeno.events.EventPhoneNumberAdded;
import com.abubusoft.xeno.events.EventPhoneNumberDelete;
import com.abubusoft.xeno.model.ActionType;
import com.abubusoft.xeno.model.PhoneNumber;
import com.abubusoft.xeno.model.PrefixConfig;
import com.abubusoft.xeno.persistence.BindXenoAsyncTask;
import com.abubusoft.xeno.persistence.BindXenoDaoFactory;
import com.abubusoft.xeno.persistence.BindXenoDataSource;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A fragment representing a list of Items.
 * <p/>
 * interface.
 */
public class PhoneListFragment extends Fragment {

    private EventBus bus = EventBus.getDefault();

    BindXenoAsyncTask.Simple<List<PhoneNumber>> asyncTask = new BindXenoAsyncTask.Simple<List<PhoneNumber>>(BindAsyncTaskType.READ) {

        @Override
        public List<PhoneNumber> onExecute(BindXenoDataSource dataSource) throws Throwable {
            return dataSource.getPhoneDao().selectAll();
        }

        @Override
        public void onFinish(List<PhoneNumber> result) {
            adapter.update(result);
        }
    };

    private PhoneAdapter adapter;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public PhoneListFragment() {
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static PhoneListFragment newInstance(int columnCount) {
        PhoneListFragment fragment = new PhoneListFragment();
        Bundle args = new Bundle();
        // args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            // mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @BindView(R.id.phone_list_recycler_view)
    EmptyRecyclerView recyclerView;

    @BindView(R.id.phone_list_empty_view)
    View emptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_phone_list, container, false);

        adapter = new PhoneAdapter(getContext(), new PhoneAdapter.PhoneActions() {
            @Override
            public void onDeletePhoneConfiguration(PhoneNumber item) {
                adapter.remove(item);
                bus.post(new EventPhoneNumberDelete(item));
            }

            @Override
            public void onSelectPhoneConfiguration(PhoneNumber item) {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + item.number));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }

            @Override
            public void onSendSms(PhoneNumber phone) {
                PrefixConfig config = BindXenoDataSource.open().getPrefixConfigDao().selectOne();
                BindXenoDataSource.getInstance().close();

                String number = (phone.action == ActionType.ADD_PREFIX ? config.dualBillingPrefix : "") + phone.number;
                Logger.info("SMS to " + number);
                Uri sms_uri = Uri.parse("smsto:" + number);
                Intent sms_intent = new Intent(Intent.ACTION_SENDTO, sms_uri);
                sms_intent.putExtra("sms_body", "");
                startActivity(sms_intent);

//                Intent smsIntent = new Intent(Intent.ACTION_VIEW);
//                smsIntent.setType("vnd.android-dir/mms-sms");
//                smsIntent.putExtra("address", (phone.action== ActionType.ADD_PREFIX? config.dualBillingPrefix : "")+ phone.number);
//                smsIntent.putExtra("sms_body","");
//                startActivity(smsIntent);
            }
        });

        ButterKnife.bind(this, view);

        Context context = view.getContext();

        LinearLayoutManager layoutManager;
        layoutManager = new LinearLayoutManager(context);
        recyclerView.setEmptyView(emptyView);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(), layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(final RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition(); //get position which is swipe

                if (direction == ItemTouchHelper.LEFT) {    //if swipe left
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext()); //alert for confirm to delete
                    builder.setMessage(R.string.phone_remove_confirmation);

                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() { //when click on DELETE
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.notifyItemRemoved(position);    //item removed from recylcerview
                            PhoneNumber item = adapter.get(position);
                            adapter.remove(item);
                            bus.post(new EventPhoneNumberDelete(item));

                            return;
                        }
                    }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {  //not removing items if cancel is done
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.notifyItemRemoved(position + 1);    //notifies the RecyclerView Adapter that data in adapter has been removed at a particular position.
                            adapter.notifyItemRangeChanged(position, adapter.getItemCount());   //notifies the RecyclerView Adapter that positions of element in adapter has been changed from position(removed element index to end of list), please update it.
                            return;
                        }
                    }).show();  //show alert dialog
                }
            }
        };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView); //set swipe to recylcerview

        return view;
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEventPhoneNumberAdded(EventPhoneNumberAdded event) {
        asyncTask.execute();
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onPhoneNumberDelete(final EventPhoneNumberDelete event) {
        BindXenoDataSource.getInstance().executeBatch(daoFactory -> {
            daoFactory.getPhoneDao().deleteById(event.item.id);
            return null;
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        bus.register(this);
        asyncTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        bus.unregister(this);
    }

}

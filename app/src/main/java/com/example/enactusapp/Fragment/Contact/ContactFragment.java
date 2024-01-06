package com.example.enactusapp.Fragment.Contact;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.enactusapp.Adapter.ContactAdapter;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Constants.SpUtilValueConstants;
import com.example.enactusapp.Entity.Selection;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlLeftEvents;
import com.example.enactusapp.Event.MuscleControlEvent.MuscleControlRightEvents;
import com.example.enactusapp.Fragment.Dialog.DialogFragment;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.CalculateUtils;
import com.example.enactusapp.Utils.ContextUtils;
import com.example.enactusapp.Utils.ToastUtils;

import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;
import pl.droidsonroids.gif.GifImageView;

public class ContactFragment extends SupportFragment implements OnItemClickListener, OnTaskCompleted {

    private static final String TAG = ContactFragment.class.getSimpleName();

    private static final int GET_USERS = 1;
    private static final int SEND_MESSAGE = 2;

    private TextView mTvSelection;
    private SwipeRefreshLayout mSrlRefresh;
    private GifImageView mGivLoading;
    private ContactAdapter mContactAdapter;

    private final List<User> users = new ArrayList<>();
    private final List<Selection> selections = new ArrayList<>();
    private int muscleControlRightCount = 0;

    public static ContactFragment newInstance() {
        ContactFragment fragment = new ContactFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact, container, false);
        EventBusActivityScope.getDefault(_mActivity).register(this);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mTvSelection = (TextView) view.findViewById(R.id.tv_selection);
        if (Config.sControlMode == SpUtilValueConstants.EYE_TRACKING_MODE) {
            mTvSelection.setVisibility(View.GONE);
        } else {
            mTvSelection.setVisibility(View.VISIBLE);
        }
        mSrlRefresh = (SwipeRefreshLayout) view.findViewById(R.id.srl_refresh);
        RecyclerView mContactRecyclerView = (RecyclerView) view.findViewById(R.id.contact_recycler_view);
        mGivLoading = (GifImageView) view.findViewById(R.id.giv_loading);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(ContextUtils.getContext());
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContactRecyclerView.getContext(), 0);
        mContactRecyclerView.setLayoutManager(linearLayoutManager);
        mContactRecyclerView.addItemDecoration(dividerItemDecoration);
        mContactAdapter = new ContactAdapter(ContextUtils.getContext(), users);
        mContactRecyclerView.setAdapter(mContactAdapter);
        mContactAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
        mGivLoading.setVisibility(View.VISIBLE);
        HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, GET_USERS);
        String jsonData = convertToJSONGetUsers(Config.sUserId);
        task.execute(Constants.IP_ADDRESS + "api/Account/Users" + (Constants.SERVER.equals("PHP") ? ".php" : ""), jsonData, null);
    }

    private void initDelayView() {
        mSrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, GET_USERS);
                String jsonData = convertToJSONGetUsers(Config.sUserId);
                task.execute(Constants.IP_ADDRESS + "api/Account/Users" + (Constants.SERVER.equals("PHP") ? ".php" : ""), jsonData, null);
            }
        });
    }

    @Override
    public void onDestroyView() {
        EventBusActivityScope.getDefault(_mActivity).unregister(this);
        super.onDestroyView();
    }

    private String convertToJSONGetUsers(int userId) {
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("Id", userId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONGetUsers(String response) {
        try {
            users.clear();
            JSONArray jsonArray = new JSONArray(response);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = new JSONObject(jsonArray.getString(i));
                int id = -1;
                String username = "";
                String name = "";
                String thumbnail = "";
                String firebaseToken = "";
                double longitude = 9999;
                double latitude = 9999;
                if (jsonObject.has("id")) {
                    id = jsonObject.getInt("id");
                    thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + id + ".jpg";
                }
                if (jsonObject.has("username")) {
                    username = jsonObject.getString("username");
                }
                if (jsonObject.has("name")) {
                    name = jsonObject.getString("name");
                }
                if (jsonObject.has("firebaseToken")) {
                    firebaseToken = jsonObject.getString("firebaseToken");
                }
                if (jsonObject.has("longitude")) {
                    longitude = jsonObject.getDouble("longitude");
                }
                if (jsonObject.has("latitude")) {
                    latitude = jsonObject.getDouble("latitude");
                }
                users.add(new User(id, username, name, thumbnail, firebaseToken, longitude, latitude));
                users.sort(new Comparator<User>() {
                    @Override
                    public int compare(User user1, User user2) {
                        double distance1 = CalculateUtils.getDistance(Config.sLatitude, Config.sLongitude, user1.getLatitude(), user1.getLongitude());
                        double distance2 = CalculateUtils.getDistance(Config.sLatitude, Config.sLongitude, user2.getLatitude(), user2.getLongitude());
                        return (int) (distance1 - distance2);
                    }
                });
            }
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mContactAdapter.notifyDataSetChanged();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            try {
                JSONObject jsonObject = new JSONObject(response);
                String message = jsonObject.getString("message");
                ToastUtils.showShortSafe(message);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String convertToJSONSendMessage(String message, String firebaseToken) {
        JSONObject jsonMsg = new JSONObject();
        JSONObject content = new JSONObject();
        JSONObject body = new JSONObject();
        JSONObject from = new JSONObject();
        try {
            from.put("id", Config.sUserId);
            from.put("username", Config.sUsername);
            from.put("name", Config.sName);
            from.put("firebaseToken", Config.sFirebaseToken);
            from.put("longitude", Config.sLongitude);
            from.put("latitude", Config.sLatitude);
            body.put("from", from);
            body.put("message", message);
            content.put("title", MessageType.GREETING.getValue());
            content.put("body", body);
            jsonMsg.put("to", firebaseToken);
            jsonMsg.put("notification", content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONSendMessage(String response, int id) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int code = jsonObject.getInt("success");
            if (code == 1) {
                ToastUtils.showShortSafe("Sent");
                if (getParentFragment() != null) {
                    ((MainFragment) getParentFragment()).startBrotherFragment(DialogFragment.newInstance(id));
                }
            } else {
                String results = jsonObject.getString("results");
                JSONArray jsonArray = new JSONArray(results);
                JSONObject result = new JSONObject(jsonArray.getString(0));
                ToastUtils.showShortSafe(result.getString("error"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            ToastUtils.showShortSafe("System error");
        }
    }

    private void addSelections() {
        muscleControlRightCount = 0;
        selections.clear();
        for (int i = 0; i < users.size(); i++) {
            selections.add(new Selection(users.get(i).getId(), users.get(i).getName()));
        }
        if (selections.size() > 0) {
            mTvSelection.setText(selections.get(0).getName());
        }
    }

    @Override
    public void onTaskCompleted(String response, int requestId, String... others) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == GET_USERS) {
            mSrlRefresh.setRefreshing(false);
            retrieveFromJSONGetUsers(response);
            addSelections();
        }
        if (requestId == SEND_MESSAGE) {
            int id = Integer.parseInt(others[0]);
            retrieveFromJSONSendMessage(response, id);
        }
    }

    @Override
    public void onItemClick(int position) {
        User user = users.get(position);
        if (!TextUtils.isEmpty(user.getFirebaseToken())) {
            mGivLoading.setVisibility(View.VISIBLE);
            String firebaseToken = users.get(position).getFirebaseToken();
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, SEND_MESSAGE, String.valueOf(user.getId()));
            task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(Config.sName + " says hello to you", firebaseToken), Constants.SERVER_KEY);
        } else {
            ToastUtils.showShortSafe("Firebase Token Empty");
        }
    }

    @Subscribe
    public void onMuscleControlLeftEvents(MuscleControlLeftEvents event) {
        if (event != null && event.getFragmentId() == Constants.CONTACT_FRAGMENT_ID) {
            if (selections.size() > 0 && !TextUtils.isEmpty(users.get(muscleControlRightCount).getFirebaseToken())) {
                _mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mGivLoading.setVisibility(View.VISIBLE);
                    }
                });
                String firebaseToken = users.get(muscleControlRightCount).getFirebaseToken();
                HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, SEND_MESSAGE);
                task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(Config.sName + " says hello to you", firebaseToken), Constants.SERVER_KEY);
            } else {
                ToastUtils.showShortSafe("Firebase Token Empty");
            }
        }
    }

    @Subscribe
    public void onMuscleControlRightEvents(MuscleControlRightEvents event) {
        if (event != null && event.getFragmentId() == Constants.CONTACT_FRAGMENT_ID) {
            muscleControlRightCount++;
            if (muscleControlRightCount == selections.size()) {
                muscleControlRightCount = 0;
            }
            _mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvSelection.setText(selections.get(muscleControlRightCount).getName());
                }
            });
        }
    }
}

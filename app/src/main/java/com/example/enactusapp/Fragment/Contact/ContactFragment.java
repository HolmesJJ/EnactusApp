package com.example.enactusapp.Fragment.Contact;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.example.enactusapp.Adapter.ContactAdapter;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Constants.MessageType;
import com.example.enactusapp.Entity.User;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.CalculateUtils;
import com.example.enactusapp.Utils.ToastUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class ContactFragment extends SupportFragment implements OnItemClickListener, OnTaskCompleted {

    private static final int GET_USERS = 1;
    private static final int SEND_MESSAGE = 2;

    private Toolbar mToolbar;
    private ProgressBar mPbLoading;
    private SwipeRefreshLayout mSrlRefresh;
    private RecyclerView mContactRecyclerView;
    private ContactAdapter mContactAdapter;

    private List<User> users = new ArrayList<>();

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
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.contact);
        mPbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
        mSrlRefresh = (SwipeRefreshLayout) view.findViewById(R.id.srl_refresh);
        mContactRecyclerView = (RecyclerView) view.findViewById(R.id.contact_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContactRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mContactRecyclerView.setLayoutManager(linearLayoutManager);
        mContactRecyclerView.addItemDecoration(dividerItemDecoration);
        mContactAdapter = new ContactAdapter(_mActivity, users);
        mContactRecyclerView.setAdapter(mContactAdapter);
        mContactAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        mSrlRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, GET_USERS);
                String jsonData = convertToJSONGetUsers(Config.sUserId);
                task.execute(Constants.IP_ADDRESS + "get_users.php", jsonData, null);
            }
        });
        showProgress(true);
        HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, GET_USERS);
        String jsonData = convertToJSONGetUsers(Config.sUserId);
        task.execute(Constants.IP_ADDRESS + "get_users.php", jsonData, null);
    }

    @Override
    public void onDestroyView() {
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
                    thumbnail = Constants.IP_ADDRESS + "img" + File.separator + id + ".jpg";
                }
                if (jsonObject.has("username")) {
                    username = jsonObject.getString("username");
                }
                if (jsonObject.has("name")) {
                    name = jsonObject.getString("name");
                }
                if (jsonObject.has("firebase_token")) {
                    firebaseToken = jsonObject.getString("firebase_token");
                }
                if (jsonObject.has("longitude")) {
                    longitude = jsonObject.getDouble("longitude");
                }
                if (jsonObject.has("latitude")) {
                    latitude = jsonObject.getDouble("latitude");
                }
                users.add(new User(id, username, name, thumbnail, firebaseToken, longitude, latitude));
                Collections.sort(users, new Comparator<User>() {
                    @Override
                    public int compare(User user1, User user2) {
                        double distance1 = CalculateUtils.getDistance(Config.sLatitude, Config.sLongitude, user1.getLatitude(), user1.getLongitude());
                        double distance2 = CalculateUtils.getDistance(Config.sLatitude, Config.sLongitude, user2.getLatitude(), user2.getLongitude());
                        return (int) (distance1 - distance2);
                    }
                });
                mContactAdapter.notifyDataSetChanged();
            }
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

    private void retrieveFromJSONSendMessage(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int id = jsonObject.getInt("success");
            if (id == 1) {
                ToastUtils.showShortSafe("Sent");
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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            mPbLoading.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mPbLoading.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onTaskCompleted(String response, int requestId) {
        showProgress(false);
        if (requestId == GET_USERS) {
            mSrlRefresh.setRefreshing(false);
            retrieveFromJSONGetUsers(response);
        }
        if (requestId == SEND_MESSAGE) {
            retrieveFromJSONSendMessage(response);
        }
    }

    @Override
    public void onItemClick(int position) {
        showProgress(true);
        if (!TextUtils.isEmpty(users.get(position).getFirebaseToken())) {
            String firebaseToken = users.get(position).getFirebaseToken();
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this, SEND_MESSAGE);
            task.execute(Constants.FIREBASE_ADDRESS, convertToJSONSendMessage(Config.sName + " says hello to you", firebaseToken), Constants.SERVER_KEY);
        } else {
            ToastUtils.showShortSafe("Firebase Token Empty");
        }
    }
}

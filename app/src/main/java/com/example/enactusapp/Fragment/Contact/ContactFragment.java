package com.example.enactusapp.Fragment.Contact;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.enactusapp.Adapter.ContactAdapter;
import com.example.enactusapp.CustomView.CustomToast;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnItemClickListener;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.config.Config;

import org.json.JSONObject;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import me.yokeyword.fragmentation.SupportFragment;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class ContactFragment extends SupportFragment implements OnItemClickListener, OnTaskCompleted {

    private Toolbar mToolbar;
    private RecyclerView mContactRecyclerView;
    private ContactAdapter mContactAdapter;

    private List<String> contactNameList = new ArrayList<>();
    private List<String> contactThumbnailList = new ArrayList<>();

    private Handler handler = new Handler();

    private Mat frame;
    private Mat mGray;

    private int isSucceeded = 0;
    private static final String USER1_FIREBASE_TOKEN = "emq4XVdGkfs:APA91bF0xqRrdB0REku_hR_VsnuGnw_xufRMcGVSv3Lx_-kv7e0m41HsMPUrOkbbxj02gJwvvhuhI5VZec2A7ar6j1UwXRh6Wjt3buF48prghlT2iP2YMR5f3IQZ6qDwmGdOEJbYyVXA";
    private static final String USER2_FIREBASE_TOKEN = "e1tXvYDdr9M:APA91bE8x-VWP0QzInyLJ92_pD4KO96csJbnh5QaiQ1pxe2uiOBwaj8NQgtRs9ogdqdhgZrT6_ydEWe-VTQKvhpZLOeiUB8BMfZpe9gD2SD90hBdzJ569NLF9ClhXvM2aYPkluYe8i9T";

    public static ContactFragment newInstance(){
        ContactFragment fragment = new ContactFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact,container,false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.contact);
        mContactRecyclerView = (RecyclerView) view.findViewById(R.id.contact_recycler_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(_mActivity);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mContactRecyclerView.getContext(), linearLayoutManager.getOrientation());
        mContactRecyclerView.setLayoutManager(linearLayoutManager);
        mContactRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {

        if (Config.sIsLogin && Config.sUserId.equals("A1234567B")) {
            contactNameList.add("Mr.Chai");
            contactNameList.add("Ms.Cheng");
            contactNameList.add("Mr.Liaw");
            contactNameList.add("Ms.Lim");
            contactThumbnailList.add("user2");
            contactThumbnailList.add("user3");
            contactThumbnailList.add("user4");
            contactThumbnailList.add("user5");
        } else {
            contactNameList.add("Mr.Wong");
            contactNameList.add("Ms.Cheng");
            contactNameList.add("Mr.Liaw");
            contactNameList.add("Ms.Lim");
            contactThumbnailList.add("user1");
            contactThumbnailList.add("user3");
            contactThumbnailList.add("user4");
            contactThumbnailList.add("user5");
        }

        mContactAdapter = new ContactAdapter(_mActivity, contactNameList, contactThumbnailList);
        mContactRecyclerView.setAdapter(mContactAdapter);

        mContactAdapter.setOnItemClickListener(this);
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroyView();
    }

    public String convertToJSON(String message, String user_firebase_token) {
        JSONObject jsonMsg = new JSONObject();
        JSONObject content = new JSONObject();
        try {
            content.put("title", "message");
            content.put("body", message);
            jsonMsg.put("to", user_firebase_token);
            jsonMsg.put("notification", content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    public void retrieveFromJSON(String message) {
        try {
            JSONObject jsonObject = new JSONObject(message);
            isSucceeded = jsonObject.getInt("success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onTaskCompleted(String response) {
        retrieveFromJSON(response);

        // if response is from upload request
        if (isSucceeded == 1){
            CustomToast.show(_mActivity, "Sent!");
        }
        else {
            CustomToast.show(_mActivity, "Failed to send!");
        }

        isSucceeded = 0;
    }

    @Override
    public void onItemClick(int position) {
        if(contactThumbnailList.get(position).equals("user1")) {
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this);
            task.execute("https://fcm.googleapis.com/fcm/send", convertToJSON("user2ChatWithYou", USER1_FIREBASE_TOKEN));
        }
        else if(contactThumbnailList.get(position).equals("user2")) {
            HttpAsyncTaskPost task = new HttpAsyncTaskPost(ContactFragment.this);
            task.execute("https://fcm.googleapis.com/fcm/send", convertToJSON("user1ChatWithYou", USER2_FIREBASE_TOKEN));
        }
    }
}

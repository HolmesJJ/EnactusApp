package com.example.enactusapp.Fragment.Profile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.enactusapp.Constants.Constants;
import com.example.enactusapp.Event.CalibrationEvent;
import com.example.enactusapp.Fragment.Bluetooth.BluetoothFragment;
import com.example.enactusapp.Fragment.MainFragment;
import com.example.enactusapp.Http.HttpAsyncTaskPost;
import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.Config.Config;
import com.example.enactusapp.Utils.ToastUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

import java.io.File;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;

import static com.example.enactusapp.Config.Config.resetConfig;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class ProfileFragment extends SupportFragment implements OnTaskCompleted {

    private static final int UPDATE_USER = 1;

    private Toolbar mToolbar;
    private ProgressBar mPbLoading;
    private ImageButton profileImageBtn;
    private ImageButton profileEditBtn;
    private ImageButton profileConfirmBtn;
    private TextView profileNameTv;
    private EditText profileNameEt;
    private Button startCalibrationBtn;
    private Button muscleSensorBtn;
    private Button logoutBtn;

    public static ProfileFragment newInstance() {
        ProfileFragment fragment = new ProfileFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initView(view);
        return view;
    }

    private void initView(View view) {
        mToolbar = (Toolbar) view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.profile);
        mPbLoading = (ProgressBar) view.findViewById(R.id.pb_loading);
        profileImageBtn = (ImageButton) view.findViewById(R.id.profile_image_btn);
        profileEditBtn = (ImageButton) view.findViewById(R.id.profile_edit_btn);
        profileConfirmBtn = (ImageButton) view.findViewById(R.id.profile_confirm_btn);
        profileConfirmBtn.setVisibility(View.GONE);
        profileNameTv = (TextView) view.findViewById(R.id.profile_name_tv);
        profileNameEt = (EditText) view.findViewById(R.id.profile_name_et);
        profileNameEt.setVisibility(View.GONE);
        startCalibrationBtn = (Button) view.findViewById(R.id.start_calibration_btn);
        muscleSensorBtn = (Button) view.findViewById(R.id.muscle_sensor_btn);
        logoutBtn = (Button) view.findViewById(R.id.logout_btn);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        String thumbnail = Constants.IP_ADDRESS + "img" + File.separator + Config.sUserId + ".jpg";
        Glide.with(this).load(thumbnail).into(profileImageBtn);
        profileNameTv.setText(Config.sName);

        profileEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                profileEditBtn.setVisibility(View.GONE);
                profileConfirmBtn.setVisibility(View.VISIBLE);
                profileNameTv.setVisibility(View.GONE);
                profileNameEt.setVisibility(View.VISIBLE);
                profileNameEt.setText(Config.sName);
            }
        });

        profileConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(profileNameEt.getText().toString())) {
                    showProgress(true);
                    HttpAsyncTaskPost task = new HttpAsyncTaskPost(ProfileFragment.this, UPDATE_USER);
                    String jsonData = convertToJSONUpdateUser(Config.sUserId, profileNameEt.getText().toString());
                    task.execute(Constants.IP_ADDRESS + "update_user.php", jsonData, null);
                } else {
                    ToastUtils.showShortSafe("Please enter valid name");
                }
            }
        });

        startCalibrationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EventBusActivityScope.getDefault(_mActivity).post(new CalibrationEvent(true));
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetConfig();
                _mActivity.finish();
            }
        });

        muscleSensorBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((MainFragment) getParentFragment()).startBrotherFragment(BluetoothFragment.newInstance());
            }
        });
    }

    private String convertToJSONUpdateUser(int userId, String name) {
        JSONObject jsonMsg = new JSONObject();
        try {
            jsonMsg.put("Id", userId);
            jsonMsg.put("Name", name);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonMsg.toString();
    }

    private void retrieveFromJSONUpdateUser(String response) {
        try {
            JSONObject jsonObject = new JSONObject(response);
            int code = jsonObject.getInt("code");
            String message = jsonObject.getString("message");
            if (code == 1) {
                Config.setName(profileNameEt.getText().toString());
            } else {
                ToastUtils.showShortSafe(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        _mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                profileConfirmBtn.setVisibility(View.GONE);
                profileEditBtn.setVisibility(View.VISIBLE);
                profileNameEt.setVisibility(View.GONE);
                profileNameTv.setVisibility(View.VISIBLE);
                profileNameTv.setText(Config.sName);
                profileNameEt.setText("");
            }
        });
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
        if (requestId == UPDATE_USER) {
            retrieveFromJSONUpdateUser(response);
        }
    }
}

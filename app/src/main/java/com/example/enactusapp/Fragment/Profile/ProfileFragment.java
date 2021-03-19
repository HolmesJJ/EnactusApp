package com.example.enactusapp.Fragment.Profile;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import com.shehuan.niv.NiceImageView;

import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.io.File;

import me.yokeyword.eventbusactivityscope.EventBusActivityScope;
import me.yokeyword.fragmentation.SupportFragment;
import pl.droidsonroids.gif.GifImageView;

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

    private NiceImageView mNivProfileImage;
    private ImageButton profileEditBtn;
    private ImageButton profileConfirmBtn;
    private TextView profileNameTv;
    private EditText profileNameEt;
    private Button startCalibrationBtn;
    private Button muscleSensorBtn;
    private Button logoutBtn;
    private GifImageView mGivLoading;

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
        mNivProfileImage = (NiceImageView) view.findViewById(R.id.niv_profile_image);
        profileEditBtn = (ImageButton) view.findViewById(R.id.btn_profile_edit);
        profileConfirmBtn = (ImageButton) view.findViewById(R.id.btn_profile_confirm);
        profileConfirmBtn.setVisibility(View.GONE);
        profileNameTv = (TextView) view.findViewById(R.id.profile_name_tv);
        profileNameEt = (EditText) view.findViewById(R.id.profile_name_et);
        profileNameEt.setVisibility(View.GONE);
        startCalibrationBtn = (Button) view.findViewById(R.id.btn_start_calibration);
        muscleSensorBtn = (Button) view.findViewById(R.id.btn_muscle_sensor);
        logoutBtn = (Button) view.findViewById(R.id.btn_logout);
        mGivLoading = (GifImageView) view.findViewById(R.id.giv_loading);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        String thumbnail = Constants.IP_ADDRESS + "Images" + File.separator + Config.sUserId + ".jpg";
        Glide.with(this).load(thumbnail).circleCrop().into(mNivProfileImage);
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
                    mGivLoading.setVisibility(View.VISIBLE);
                    HttpAsyncTaskPost task = new HttpAsyncTaskPost(ProfileFragment.this, UPDATE_USER);
                    String jsonData = convertToJSONUpdateUser(Config.sUserId, profileNameEt.getText().toString());
                    task.execute(Constants.IP_ADDRESS + "api/Account/EditName", jsonData, null);
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

    @Override
    public void onTaskCompleted(String response, int requestId) {
        mGivLoading.setVisibility(View.GONE);
        if (requestId == UPDATE_USER) {
            retrieveFromJSONUpdateUser(response);
        }
    }
}

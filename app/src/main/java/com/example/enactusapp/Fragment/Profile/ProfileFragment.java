package com.example.enactusapp.Fragment.Profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.enactusapp.Listener.OnTaskCompleted;
import com.example.enactusapp.R;
import com.example.enactusapp.Config.Config;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

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

    private Toolbar mToolbar;
    private ImageButton profileImageBtn;
    private ImageButton profileEditBtn;
    private ImageButton profileConfirmBtn;
    private TextView profileNameTv;
    private EditText profileNameEt;
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
        profileImageBtn = (ImageButton) view.findViewById(R.id.profile_image_btn);
        profileEditBtn = (ImageButton) view.findViewById(R.id.profile_edit_btn);
        profileConfirmBtn = (ImageButton) view.findViewById(R.id.profile_confirm_btn);
        profileConfirmBtn.setVisibility(View.GONE);
        profileNameTv = (TextView) view.findViewById(R.id.profile_name_tv);
        profileNameEt = (EditText) view.findViewById(R.id.profile_name_et);
        profileNameEt.setVisibility(View.GONE);
        logoutBtn = (Button) view.findViewById(R.id.logout_btn);
    }

    @Override
    public void onEnterAnimationEnd(Bundle savedInstanceState) {
        initDelayView();
    }

    private void initDelayView() {
        if (Config.sIsLogin && Config.sUserId.equals("A1234567B")) {
            profileImageBtn.setImageResource(_mActivity.getResources().getIdentifier("user1", "drawable", _mActivity.getPackageName()));
            profileNameTv.setText("Mr.Wong");
        } else if (Config.sIsLogin && Config.sUserId.equals("C7654321D")) {
            profileImageBtn.setImageResource(_mActivity.getResources().getIdentifier("user2", "drawable", _mActivity.getPackageName()));
            profileNameTv.setText("Mr.Chai");
        }

        profileEditBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                profileEditBtn.setVisibility(View.GONE);
                profileConfirmBtn.setVisibility(View.VISIBLE);
                profileNameTv.setVisibility(View.GONE);
                profileNameEt.setVisibility(View.VISIBLE);
                profileNameEt.setText(profileNameTv.getText());
                profileNameTv.setText("");
            }
        });

        profileConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                profileConfirmBtn.setVisibility(View.GONE);
                profileEditBtn.setVisibility(View.VISIBLE);
                profileNameEt.setVisibility(View.GONE);
                profileNameTv.setVisibility(View.VISIBLE);
                profileNameTv.setText(profileNameEt.getText());
                profileNameEt.setText("");
            }
        });

        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetConfig();
                _mActivity.finish();
            }
        });
    }

    @Override
    public void onTaskCompleted(String response) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

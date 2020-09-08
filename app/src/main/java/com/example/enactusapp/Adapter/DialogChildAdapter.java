package com.example.enactusapp.Adapter;


import com.example.enactusapp.Fragment.Dialog.Child.PossibleAnswersFragment;
import com.example.enactusapp.Fragment.Dialog.Child.T26KeyboardFragment;
import com.example.enactusapp.Fragment.Dialog.Child.T2KeyboardFragment;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * @author Administrator
 * @des ${TODO}
 * @verson $Rev$
 * @updateAuthor $Author$
 * @updateDes ${TODO}
 */
public class DialogChildAdapter extends FragmentPagerAdapter {

    private String[] mTitles;

    public DialogChildAdapter(FragmentManager fm, String... titles) {
        super(fm);
        mTitles = titles;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return PossibleAnswersFragment.newInstance();
        }
        else if (position == 1) {
            return T2KeyboardFragment.newInstance();
        }
        else {
            return T26KeyboardFragment.newInstance();
        }
    }

    @Override
    public int getCount() {
        return mTitles.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }
}

/*
 * Copyright (C) 2013 Dan Morrill
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dude.morrildl.weatherport;

import java.util.Locale;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * A basic PagerAdapter with only two panes.
 */
public class TwoFragmentPagerAdapter extends FragmentPagerAdapter {
    private final Context context;
    Fragment left, right;

    public TwoFragmentPagerAdapter(Context context, FragmentManager fm, Fragment left,
            Fragment right) {
        super(fm);
        this.context = context;
        this.right = right;
        this.left = left;
    }

    @Override
    public Fragment getItem(int position) {
        if (position == 0) {
            return left;
        } else {
            return right;
        }
    }

    @Override
    public int getCount() {
        return 2;  // we don't change at runtime
    }

    @Override
    public CharSequence getPageTitle(int position) {
        Locale l = Locale.getDefault();
        switch (position) {
        // upper case tab labels are not strictly required, but are the convention
        case 0:
            return this.context.getString(R.string.details_section).toUpperCase(l);
        case 1:
            return this.context.getString(R.string.map_section).toUpperCase(l);
        }
        return null;
    }
}
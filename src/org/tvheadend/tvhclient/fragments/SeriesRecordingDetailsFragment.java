/*
 *  Copyright (C) 2013 Robert Siebert
 *  Copyright (C) 2011 John Törnblom
 *
 * This file is part of TVHGuide.
 *
 * TVHGuide is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * TVHGuide is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with TVHGuide.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tvheadend.tvhclient.fragments;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.SeriesRecording;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SeriesRecordingDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = SeriesRecordingDetailsFragment.class.getSimpleName();

    private Activity activity;
    private boolean showControls = false;
    private SeriesRecording srec;

    private TextView isEnabled;
    private TextView minDuration;
    private TextView maxDuration;
    private TextView retention;
    private TextView daysOfWeek;
    private TextView approxTime;
    private TextView startWindow;
    private TextView priority;
    private TextView startExtra;
    private TextView stopExtra;
    private TextView title;
    private TextView name;
    private TextView directory;
    private TextView owner;
    private TextView creator;
    private TextView channelName;

    private LinearLayout playerLayout;
    private TextView recordRemove;

    public static SeriesRecordingDetailsFragment newInstance(Bundle args) {
        SeriesRecordingDetailsFragment f = new SeriesRecordingDetailsFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = (Activity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        String srecId = "";
        Bundle bundle = getArguments();
        if (bundle != null) {
            srecId = bundle.getString(Constants.BUNDLE_SERIES_RECORDING_ID);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
        }

        // Get the recording so we can show its details 
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        srec = app.getSeriesRecording(srecId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.series_recording_details_layout, container, false);
        channelName = (TextView) v.findViewById(R.id.channel);
        isEnabled = (TextView) v.findViewById(R.id.is_enabled);
        name = (TextView) v.findViewById(R.id.name);
        minDuration = (TextView) v.findViewById(R.id.minimum_duration);
        maxDuration = (TextView) v.findViewById(R.id.maximum_duration);
        retention = (TextView) v.findViewById(R.id.retention);
        daysOfWeek = (TextView) v.findViewById(R.id.days_of_week);
        approxTime = (TextView) v.findViewById(R.id.approx_time);
        startWindow = (TextView) v.findViewById(R.id.start_window);
        priority = (TextView) v.findViewById(R.id.priority);
        startExtra = (TextView) v.findViewById(R.id.start_extra);
        stopExtra = (TextView) v.findViewById(R.id.stop_extra);
        directory = (TextView) v.findViewById(R.id.directory);
        owner = (TextView) v.findViewById(R.id.owner);
        creator = (TextView) v.findViewById(R.id.creator);
        
        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        recordRemove = (TextView) v.findViewById(R.id.menu_record_remove);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // If the series recording is null exit
        if (srec == null) {
            return;
        }
        if (getDialog() != null) {
            getDialog().setTitle(srec.title);
        }
        // Show the player controls
        if (showControls) {
            addPlayerControlListeners();
        }
        showPlayerControls();

        if (channelName != null && srec.channel != null) {
            channelName.setText(srec.channel.name);
        }
        if (title != null && srec.title.length() > 0) {
            title.setText(srec.title);
        }
        if (name != null && srec.name.length() > 0) {
            name.setText(srec.name);
        }
        if (directory != null && srec.directory.length() > 0) {
            directory.setText(srec.directory);
        }
        if (owner != null && srec.owner.length() > 0) {
            owner.setText(srec.owner);
        }
        if (creator != null && srec.creator.length() > 0) {
            creator.setText(srec.creator);
        }
        Utils.setDaysOfWeek(activity, null, daysOfWeek, srec.daysOfWeek);

        if (priority != null) {
            String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
            if (srec.priority >= 0 && srec.priority < priorityItems.length) {
                priority.setText(priorityItems[(int) (srec.priority)]);
            }
        }

        if (minDuration != null && srec.minDuration > 0) {
            minDuration.setText(getString(R.string.minutes, (int) srec.minDuration));
        }
        if (maxDuration != null && srec.maxDuration > 0) {
            maxDuration.setText(getString(R.string.minutes, (int) srec.maxDuration));
        }
        if (retention != null) {
            retention.setText(getString(R.string.days, (int) srec.retention));
        }
        if (approxTime != null && srec.approxTime >= 0) {
            approxTime.setText(getString(R.string.minutes, srec.approxTime));
        }
        if (startWindow != null && srec.startWindow >= 0) {
            startWindow.setText(getString(R.string.minutes, srec.startWindow));
        }
        if (startExtra != null && srec.startExtra >= 0) {
            startExtra.setText(getString(R.string.minutes, (int) srec.startExtra));
        }
        if (stopExtra != null && srec.stopExtra >= 0) {
            stopExtra.setText(getString(R.string.minutes, (int) srec.stopExtra));
        }

        if (isEnabled != null) {
            if (srec.enabled) {
                isEnabled.setText(R.string.recording_enabled);
            } else {
                isEnabled.setText(R.string.recording_disabled);
            }
        }
    }

    /**
     * 
     */
    private void showPlayerControls() {
        playerLayout.setVisibility(showControls ? View.VISIBLE : View.GONE);
        recordRemove.setVisibility(View.VISIBLE);
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        recordRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.confirmRemoveRecording(activity, srec);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.addListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        app.removeListener(this);
    }

    /**
     * This method is part of the HTSListener interface. Whenever the HTSService
     * sends a new message the correct action will then be executed here.
     */
    @Override
    public void onMessage(String action, Object obj) {
        // NOP
    }
}

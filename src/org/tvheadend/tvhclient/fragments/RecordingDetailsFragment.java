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
import org.tvheadend.tvhclient.PlaybackSelectionActivity;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.Utils;
import org.tvheadend.tvhclient.interfaces.HTSListener;
import org.tvheadend.tvhclient.model.Recording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class RecordingDetailsFragment extends DialogFragment implements HTSListener {

    @SuppressWarnings("unused")
    private final static String TAG = RecordingDetailsFragment.class.getSimpleName();

    private Activity activity;
    private boolean showControls = false;
    private boolean isDualPane = false;
    private Recording rec;

    private TextView descLabel;
    private TextView desc;
    private TextView channelLabel;
    private TextView channelName;
    private TextView date;
    private TextView time;
    private TextView duration;
    private TextView failed_reason;
    private TextView recording_type;
    private TextView priority;
    private TextView retention;
    private TextView startExtra;
    private TextView stopExtra;
    private TextView path;
    private TextView owner;
    private TextView creator;

    private LinearLayout playerLayout;
    private TextView play;
    private TextView recordCancel;
    private TextView recordRemove;

    public static RecordingDetailsFragment newInstance(Bundle args) {
        RecordingDetailsFragment f = new RecordingDetailsFragment();
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

        long recId = 0;
        Bundle bundle = getArguments();
        if (bundle != null) {
            recId = bundle.getLong(Constants.BUNDLE_RECORDING_ID, 0);
            showControls = bundle.getBoolean(Constants.BUNDLE_SHOW_CONTROLS, false);
            isDualPane = bundle.getBoolean(Constants.BUNDLE_DUAL_PANE, false);
        }

        // Get the recording so we can show its details 
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        rec = app.getRecording(recId);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.recording_details_layout, container, false);
        descLabel = (TextView) v.findViewById(R.id.description_label);
        desc = (TextView) v.findViewById(R.id.description);
        channelLabel = (TextView) v.findViewById(R.id.channel_label);
        channelName = (TextView) v.findViewById(R.id.channel);
        date = (TextView) v.findViewById(R.id.date);
        time = (TextView) v.findViewById(R.id.time);
        duration = (TextView) v.findViewById(R.id.duration);
        failed_reason = (TextView) v.findViewById(R.id.failed_reason);
        recording_type = (TextView) v.findViewById(R.id.recording_type);
        priority = (TextView) v.findViewById(R.id.priority);
        retention = (TextView) v.findViewById(R.id.retention);
        startExtra = (TextView) v.findViewById(R.id.start_extra);
        stopExtra = (TextView) v.findViewById(R.id.stop_extra);
        owner = (TextView) v.findViewById(R.id.owner);
        creator = (TextView) v.findViewById(R.id.creator);
        path = (TextView) v.findViewById(R.id.path);
        
        // Initialize the player layout
        playerLayout = (LinearLayout) v.findViewById(R.id.player_layout);
        play = (TextView) v.findViewById(R.id.menu_play);
        recordCancel = (TextView) v.findViewById(R.id.menu_record_cancel);
        recordRemove = (TextView) v.findViewById(R.id.menu_record_remove);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        // If the recording is null exit
        if (rec == null) {
            return;
        }

        if (getDialog() != null) {
            getDialog().setTitle(rec.title);
        }

        // Show the player controls
        if (showControls) {
            addPlayerControlListeners();
        }
        showPlayerControls();

        Utils.setDate(date, rec.start);
        Utils.setTime(time, rec.start, rec.stop);
        Utils.setDuration(duration, rec.start, rec.stop);
        Utils.setProgressText(null, rec.start, rec.stop);
        Utils.setDescription(channelLabel, channelName, ((rec.channel != null) ? rec.channel.name : ""));
        Utils.setDescription(descLabel, desc, rec.description);
        Utils.setFailedReason(failed_reason, rec);

        if (owner != null && rec.owner.length() > 0) {
            owner.setText(rec.owner);
        }
        if (creator != null && rec.creator.length() > 0) {
            creator.setText(rec.creator);
        }
        if (path != null && rec.path.length() > 0) {
            path.setText(rec.path);
        }
        if (priority != null) {
            String[] priorityItems = getResources().getStringArray(R.array.dvr_priorities);
            if (rec.priority >= 0 && rec.priority < priorityItems.length) {
                priority.setText(priorityItems[(int) (rec.priority)]);
            }
        }
        if (retention != null) {
            retention.setText(getString(R.string.days, (int) rec.retention));
        }
        if (startExtra != null && rec.startExtra >= 0) {
            startExtra.setText(getString(R.string.minutes, (int) rec.startExtra));
        }
        if (stopExtra != null && rec.stopExtra >= 0) {
            stopExtra.setText(getString(R.string.minutes, (int) rec.stopExtra));
        }

        // Show the information if the recording belongs to a series recording
        // only when no dual pane is active (the controls shall be shown)
        if (recording_type != null) {
            if ((rec.autorecId.length() == 0 && rec.timerecId.length() == 0) || isDualPane) {
                recording_type.setVisibility(ImageView.GONE);
            } else if (rec.autorecId.length() > 0 && rec.timerecId.length() == 0 && !isDualPane) {
                recording_type.setText(R.string.is_series_recording);
            } else if (rec.autorecId.length() == 0 && rec.timerecId.length() > 0 && !isDualPane) {
                recording_type.setText(R.string.is_timer_recording);
            }
        }
    }

    /**
     * 
     */
    private void showPlayerControls() {
        playerLayout.setVisibility(showControls ? View.VISIBLE : View.GONE);
        play.setVisibility(View.GONE);
        recordCancel.setVisibility(View.GONE);
        recordRemove.setVisibility(View.GONE);

        // Show the play menu items
        if (rec.error == null && rec.state.equals("completed")) {
            // The recording is available, it can be played and removed
            recordRemove.setVisibility(View.VISIBLE);
            play.setVisibility(View.VISIBLE);
        } else if (rec.isRecording() || rec.isScheduled()) {
            // The recording is recording or scheduled, it can only be cancelled
            recordCancel.setVisibility(View.VISIBLE);
        } else if (rec.error != null || rec.state.equals("missed")) {
            // The recording has failed or has been missed, allow removal
            recordRemove.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 
     */
    private void addPlayerControlListeners() {
        play.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Open a new activity that starts playing the program
                if (rec != null) {
                    Intent intent = new Intent(activity, PlaybackSelectionActivity.class);
                    intent.putExtra(Constants.BUNDLE_RECORDING_ID, rec.id);
                    startActivity(intent);
                }
            }
        });
        recordCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.confirmCancelRecording(activity, rec);
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
            }
        });
        recordRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.confirmRemoveRecording(activity, rec);
                if (getDialog() != null) {
                    getDialog().dismiss();
                }
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
        // An existing recording has been updated, this is valid for all menu options
        if (action.equals(Constants.ACTION_PROGRAM_UPDATE)
                || action.equals(Constants.ACTION_DVR_ADD)
                || action.equals(Constants.ACTION_DVR_DELETE)
                || action.equals(Constants.ACTION_DVR_UPDATE)) {
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    if (showControls) {
                        showPlayerControls();
                    }
                }
            });
        }
    }
}

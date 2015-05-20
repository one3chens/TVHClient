package org.tvheadend.tvhclient.fragments;

import org.tvheadend.tvhclient.Constants;
import org.tvheadend.tvhclient.DatabaseHelper;
import org.tvheadend.tvhclient.R;
import org.tvheadend.tvhclient.TVHClientApplication;
import org.tvheadend.tvhclient.htsp.HTSService;
import org.tvheadend.tvhclient.interfaces.FragmentStatusInterface;
import org.tvheadend.tvhclient.model.Channel;
import org.tvheadend.tvhclient.model.Connection;
import org.tvheadend.tvhclient.model.Profile;
import org.tvheadend.tvhclient.model.SeriesRecording;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.Snackbar.SnackbarDuration;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.enums.SnackbarType;

public class SeriesRecordingAddFragment extends DialogFragment {

    private final static String TAG = SeriesRecordingAddFragment.class.getSimpleName();

    private Activity activity;
    private SeriesRecording rec;
    private Toolbar toolbar;

    private CheckBox isEnabled;
    private TextView priority;
    private EditText minDuration;
    private EditText maxDuration;
    private LinearLayout daysOfWeekLayout;
    private ToggleButton[] daysOfWeekButtons = new ToggleButton[7];
    private EditText startTime;
    private EditText stopTime;
    private EditText title;
    private TextView channelName;

    private long priorityValue;
    private long minDurationValue;
    private long maxDurationValue;
    private long startTimeValue;
    private long stopTimeValue;
    private long daysOfWeekValue;
    private String titleValue;
    private boolean enabledValue;
    private int channelSelectionValue;

    String[] channelList;
    String[] priorityList;

    private static final int DEFAULT_MIN_DURATION = 30;
    private static final int DEFAULT_MAX_DURATION = 60;
    private static final int DEFAULT_START_EXTRA = 2;
    private static final int DEFAULT_STOP_EXTRA = 2;

    public static SeriesRecordingAddFragment newInstance(Bundle args) {
        SeriesRecordingAddFragment f = new SeriesRecordingAddFragment();
        f.setArguments(args);
        return f;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.activity = activity;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getDialog() != null) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.dialog_animation_fade;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        getValues();
        outState.putLong("priorityValue", priorityValue);
        outState.putLong("minDurationValue", minDurationValue);
        outState.putLong("maxDurationValue", maxDurationValue);
        outState.putLong("startTimeValue", startTimeValue);
        outState.putLong("stopTimeValue", stopTimeValue);
        outState.putLong("daysOfWeekValue", daysOfWeekValue);
        outState.putString("titleValue", titleValue);
        outState.putBoolean("enabledValue", enabledValue);
        outState.putInt("channelNameValue", channelSelectionValue);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        // Initialize all the widgets from the layout
        View v = inflater.inflate(R.layout.series_recording_add_layout, container, false);
        channelName = (TextView) v.findViewById(R.id.channel);
        isEnabled = (CheckBox) v.findViewById(R.id.is_enabled);
        title = (EditText) v.findViewById(R.id.title);
        minDuration = (EditText) v.findViewById(R.id.minimum_duration);
        maxDuration = (EditText) v.findViewById(R.id.maximum_duration);

        // For the shown days in each toggle button the array with the short
        // names is used. If the screen width is not large enough then the short
        // names of all seven days would not fit. Therefore reduce the number of
        // shown letters for each day depending on the screen width.
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        final int displayWidth = displaymetrics.widthPixels;

        daysOfWeekLayout = (LinearLayout) v.findViewById(R.id.days_of_week_layout);
        String[] shortDays = getResources().getStringArray(R.array.day_short_names);
        for (int i = 0; i < 7; i++) {
            final ToggleButton dayButton = (ToggleButton) inflater.inflate(R.layout.day_toggle_button, daysOfWeekLayout, false);

            // Show only one character on width below 800, two characters below
            // 1000 and all characters on all remaining ones
            if (displayWidth < 800) {
                dayButton.setTextOn(shortDays[i].subSequence(0, 1));
                dayButton.setTextOff(shortDays[i].subSequence(0, 1));
            } else if (displayWidth < 1000) {
                dayButton.setTextOn(shortDays[i].subSequence(0, 2));
                dayButton.setTextOff(shortDays[i].subSequence(0, 2));
            } else {
                dayButton.setTextOn(shortDays[i]);
                dayButton.setTextOff(shortDays[i]);
            }

            // Add the button to the layout and store it in the list to have
            // access to it later 
            daysOfWeekLayout.addView(dayButton);
            daysOfWeekButtons[i] = dayButton;
        }

        startTime = (EditText) v.findViewById(R.id.start_extra);
        stopTime = (EditText) v.findViewById(R.id.stop_extra);
        priority = (TextView) v.findViewById(R.id.priority);
        toolbar = (Toolbar) v.findViewById(R.id.toolbar);

    	// Create the list of channels that the user can select
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        channelList = new String[app.getChannels().size()];
        for (int i = 0; i < app.getChannels().size(); i++) {
        	channelList[i] = app.getChannels().get(i).name;
        }

        priorityList = activity.getResources().getStringArray(R.array.dvr_priorities);

        // If the savedInstanceState is null then the fragment was created for
        // the first time. Either get the given id to edit the recording or
        // create new one. Otherwise an orientation change has occurred and the
        // saved values must be applied to the user input elements.
        if (savedInstanceState == null) {
            String recId = "";
            Bundle bundle = getArguments();
            if (bundle != null) {
                recId = bundle.getString(Constants.BUNDLE_SERIES_RECORDING_ID);
            }

            // Get the recording so we can show its details
            rec = app.getSeriesRecording(recId);
            if (rec != null) {
                priorityValue = rec.priority;
                minDurationValue = rec.minDuration;
                maxDurationValue = rec.maxDuration;
                startTimeValue = rec.start;
                stopTimeValue = rec.start;
                daysOfWeekValue = rec.daysOfWeek;
                titleValue = rec.title;
                enabledValue = rec.enabled;
                int pos = app.getChannels().indexOf(rec.channel);
                channelSelectionValue = (pos >= 0 ? pos : 0);
            } else {
                // No recording was given, set default values
                priorityValue = 2;
                minDurationValue = DEFAULT_MIN_DURATION;
                maxDurationValue = DEFAULT_MAX_DURATION;
                startTimeValue = DEFAULT_START_EXTRA;
                stopTimeValue = DEFAULT_STOP_EXTRA;
                daysOfWeekValue = 127;
                titleValue = "";
                enabledValue = true;
                channelSelectionValue = 0;
            }
        } else {
            // Restore the values before the orientation change
            priorityValue = savedInstanceState.getLong("priorityValue");
            minDurationValue = savedInstanceState.getLong("minDurationValue");
            maxDurationValue = savedInstanceState.getLong("maxDurationValue");
            startTimeValue = savedInstanceState.getLong("startTimeValue");
            stopTimeValue = savedInstanceState.getLong("stopTimeValue");
            daysOfWeekValue = savedInstanceState.getLong("daysOfWeekValue");
            titleValue = savedInstanceState.getString("titleValue");
            enabledValue = savedInstanceState.getBoolean("enabledValue");
            channelSelectionValue = savedInstanceState.getInt("channelNameValue");
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (isEnabled != null) {
            isEnabled.setChecked(enabledValue);
        }
        if (title != null) {
            title.setText(titleValue);
        }
        if (channelName != null) {
        	channelName.setText(channelList[channelSelectionValue]);
            channelName.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					new MaterialDialog.Builder(activity)
		            .title(R.string.select_channel)
		            .items(channelList)
		            .itemsCallbackSingleChoice(channelSelectionValue, new MaterialDialog.ListCallbackSingleChoice() {
		                @Override
		                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
		                	channelName.setText(channelList[which]);
		                	channelSelectionValue = which;
		                    return true;
		                }
		            })
		            .show();
				}
            });
        }
        if (priority != null) {
            priority.setText(priorityList[(int) priorityValue]);
            priority.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					new MaterialDialog.Builder(activity)
		            .title(R.string.select_channel)
		            .items(priorityList)
		            .itemsCallbackSingleChoice((int) priorityValue, new MaterialDialog.ListCallbackSingleChoice() {
		                @Override
		                public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
		                	priority.setText(priorityList[which]);
		                	priorityValue = which;
		                    return true;
		                }
		            })
		            .show();
				}
            });
        }
        if (minDuration != null) {
            minDuration.setText(String.valueOf(minDurationValue));
        }
        if (maxDuration != null) {
            maxDuration.setText(String.valueOf(maxDurationValue));
        }
        if (startTime != null) {
            startTime.setText(String.valueOf(startTimeValue));
        }
        if (stopTime != null) {
            stopTime.setText(String.valueOf(stopTimeValue));
        }

        // Set the correct days as checked or not depending on the given value.
        // For each day shift the daysOfWeekValue by one to the right and check
        // if the bit at this position is one. 
        for (int i = 0; i < 7; i++) {
            int checked = (((int) daysOfWeekValue >> i) & 1);
            daysOfWeekButtons[i].setChecked(checked == 1);
        }

        if (toolbar != null) {
            toolbar.inflateMenu(R.menu.save_cancel_menu);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    return onToolbarItemSelected(item);
                }
            });
        }
        if (getDialog() != null) {
            getDialog().setTitle(rec != null ? R.string.edit_series_recording : R.string.add_series_recording);
        }
    }

    /**
     * 
     * @param item
     * @return
     */
    protected boolean onToolbarItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_save:
            save();
            return true;

        case R.id.menu_cancel:
            cancel();
            return true;
        }
        return false;
    }

    /**
     * Retrieves and checks the values from the user input elements and stores
     * them in internal variables. These are used to remember the values during
     * an orientation change or when the recording shall be saved.
     */
    private void getValues() {
        try {
            minDurationValue = Long.valueOf(minDuration.getText().toString());
        } catch (NumberFormatException ex) {
            minDurationValue = DEFAULT_MIN_DURATION;
        }
        try {
            maxDurationValue = Long.valueOf(maxDuration.getText().toString());
        } catch (NumberFormatException ex) {
            maxDurationValue = DEFAULT_MAX_DURATION;
        }
        try {
            startTimeValue = Long.valueOf(startTime.getText().toString());
        } catch (NumberFormatException ex) {
            startTimeValue = DEFAULT_START_EXTRA;
        }
        try {
            stopTimeValue = Long.valueOf(stopTime.getText().toString());
        } catch (NumberFormatException ex) {
            stopTimeValue = DEFAULT_STOP_EXTRA;
        }
        titleValue = title.getText().toString();
        daysOfWeekValue = getDayOfWeekValue();
        enabledValue = isEnabled.isChecked();
    }

    /**
     * Checks certain given values for plausibility and if everything is fine
     * creates the intent that will be passed to the service to save the newly
     * created recording.
     */
    private void save() {
        getValues();

        // The title must not be empty
        if (titleValue.length() == 0) { 
            SnackbarManager.show(
                    Snackbar.with(activity.getApplicationContext())
                            .type(SnackbarType.MULTI_LINE)
                            .duration(SnackbarDuration.LENGTH_LONG)
                            .text(R.string.error_empty_title), activity);
            return;
        }
        // The maximum duration must be larger than the minimum duration
        if (minDurationValue > 0 && maxDurationValue > 0 && maxDurationValue < minDurationValue) {
            maxDurationValue = minDurationValue;
        }

        // If the series recording is being edited, remove it before adding it
        // again, because the API does not provide an edit call. 
        if (rec != null && rec.id != null && rec.id.length() > 0) {
            Intent intent = new Intent(activity, HTSService.class);
            intent.setAction(Constants.ACTION_DELETE_SERIES_DVR_ENTRY);
            intent.putExtra("id", rec.id);
            activity.startService(intent);
        }

        // Add the new or edited series recording
        Intent intent = new Intent(activity, HTSService.class);
        intent.setAction(Constants.ACTION_ADD_SERIES_DVR_ENTRY);
        intent.putExtra("title", titleValue);
        intent.putExtra("minDuration", minDurationValue);
        intent.putExtra("maxDuration", maxDurationValue);
        intent.putExtra("startExtra", startTimeValue);
        intent.putExtra("stopExtra", stopTimeValue);
        intent.putExtra("daysOfWeek", daysOfWeekValue);
        intent.putExtra("priority", priorityValue);
        intent.putExtra("enabled", (long) (enabledValue ? 1 : 0));

        // The id must be passed on to the server, not the name. So go through
        // all available channels and get the id for the selected channel name.
        TVHClientApplication app = (TVHClientApplication) activity.getApplication();
        for (Channel c : app.getChannels()) {
            if (c.name.equals(channelName.getText().toString())) {
                intent.putExtra("channelId", c.id);
                break;
            }
        }

        // Add the recording profile if available and enabled
        final Connection conn = DatabaseHelper.getInstance().getSelectedConnection();
        final Profile p = DatabaseHelper.getInstance().getProfile(conn.recording_profile_id);
        if (p != null 
                && p.enabled
                && app.getProtocolVersion() >= Constants.MIN_API_VERSION_PROFILES
                && app.isUnlocked()) {
            intent.putExtra("configName", p.name);
        }

        activity.startService(intent);

        if (getDialog() != null) {
            ((FragmentStatusInterface) activity).listDataInvalid(TAG);
            getDialog().dismiss();
        }
    }

    /**
     * Asks the user to confirm canceling the current activity. If no is
     * chosen the user can continue to add or edit the recording. Otherwise
     * the input will be discarded and the activity will be closed.
     */
    private void cancel() {
        // Show confirmation dialog to cancel
        new MaterialDialog.Builder(activity)
                .content(R.string.cancel_add_recording)
                .positiveText(getString(R.string.discard))
                .negativeText(getString(R.string.cancel))
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        if (getDialog() != null) {
                            getDialog().dismiss();
                        }
                    }
                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        dialog.cancel();
                    }
                }).show();
    }

    /**
     * Returns a number where each bit position is one day. If the bit position
     * is one then the day was selected.
     * 
     * @return Number with the selected day on each bit position
     */
    private long getDayOfWeekValue() {
        long value = 0;
        for (int i = 0; i < 7; i++) {
            if (daysOfWeekButtons[i].isChecked()) {
                value += (1 << i);
            }
        }
        return value;
    }
}

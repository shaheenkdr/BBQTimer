// The MIT License (MIT)
//
// Copyright (c) 2014 Jerry Morrison
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.onefishtwo.bbqtimer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * Uses AlarmManager to perform periodic reminder notifications.
 */
public class AlarmReceiver extends BroadcastReceiver {
    /* Allow wakeup time flexibility to save battery power on supporting OS levels. */
    private static final boolean ALLOW_WAKEUP_FLEXIBILITY = android.os.Build.VERSION.SDK_INT >= 19;
    private static final long WAKEUP_WINDOW_MS = ALLOW_WAKEUP_FLEXIBILITY ? 50L : 0L;

    /** Constructs a PendingIntent for the AlarmManager to invoke AlarmReceiver. */
    private static PendingIntent makeAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Returns the SystemClock.elapsedRealtime() for the next reminder notification. */
    //
    // TODO: If the user changes the period without resetting the timer, compute future reminders
    // relative to the previous reminder rather than 0:00? E.g. after a 7 minute reminder you change
    // it to 4 minutes, then it would next alert at 0:11:00 rather than 0:08:00.
    private static long nextReminderTime(ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        long periodMs     = state.getMillisecondsPerReminder();
        long now          = timer.elapsedRealtimeClock();
        long timed        = timer.getElapsedTime();
        long untilNextReminder = periodMs - (timed % periodMs);

        // Don't (re)schedule within the wakeup window. That'd double-trigger the notification.
        // (Maybe don't even schedule within the alarm sound's duration.)
        if (untilNextReminder <= WAKEUP_WINDOW_MS) {
            untilNextReminder += periodMs;
        }

        return now + untilNextReminder;
    }

    /**
     * (Re)schedules the next reminder Notification via an AlarmManager Intent.
     * Gives AlarmManager time flexibility to save battery power.
     */
    static void scheduleNextReminder(Context context, ApplicationState state) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);
        long nextReminder = nextReminderTime(state);

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            alarmMgr.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    nextReminder - WAKEUP_WINDOW_MS, WAKEUP_WINDOW_MS, pendingIntent);
        } else {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReminder, pendingIntent);
        }
    }

    /**
     * Returns the number of reminder alarms that happened by "now," allowing for the wakeup time
     * flexibility where alarms may arrive a little early.
     */
    static int numRemindersSoFar(ApplicationState state) {
        TimeCounter timer        = state.getTimeCounter();
        long elapsedMsWithWindow = timer.getElapsedTime() + WAKEUP_WINDOW_MS;
        long reminderMs          = state.getSecondsPerReminder() * 1000L;

        return (int)(elapsedMsWithWindow / reminderMs);
    }

    /**
     * Updates the app's Android Notifications area/drawer and scheduled periodic reminder
     * Notification alarms for the visible/invisible activity state, the running/paused timer state,
     * and the reminders-enabled state.
     */
    public static void updateNotifications(Context context) {
        ApplicationState state        = ApplicationState.sharedInstance(context);
        boolean enableReminders       = state.isEnableReminders();
        TimeCounter timer             = state.getTimeCounter();
        boolean isRunning             = timer.isRunning();
        Notifier notifier             = new Notifier(context);

        notifier.openOrCancel(state);

        if (isRunning && enableReminders) {
            scheduleNextReminder(context, state);
        } else {
            cancelReminders(context);
        }
    }

    /** Cancels any outstanding reminders by canceling the AlarmManager Intent. */
    public static void cancelReminders(Context context) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);

        alarmMgr.cancel(pendingIntent);
    }

    /**
     * Handles an AlarmManager Intent: Plays a reminder chime and/or vibration via the Notifier. The
     * Notification is also visible in the notification area and drawer iff the main activity is not
     * currently visible.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationState state = ApplicationState.sharedInstance(context);
        TimeCounter timer      = state.getTimeCounter();

        if (!timer.isRunning()) {
            return;
        }

        Notifier notifier = new Notifier(context).setPlayChime(true).setVibrate(true);

        notifier.openOrCancel(state);

        scheduleNextReminder(context, state);
    }
}

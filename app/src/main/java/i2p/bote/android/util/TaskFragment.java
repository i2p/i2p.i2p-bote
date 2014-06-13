package i2p.bote.android.util;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class TaskFragment<Params, Progress, Result> extends DialogFragment {
    RobustAsyncTask<Params, Progress, Result> mTask;

    public void setTask(RobustAsyncTask<Params, Progress, Result> task) {
        mTask = task;
        mTask.setFragment(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retain this instance so it isn't destroyed
        setRetainInstance(true);

        // Start the task
        if (mTask != null)
            mTask.execute(getParams());
    }

    // This is to work around what is apparently a bug. If you don't have it
    // here the dialog will be dismissed on rotation, so tell it not to dismiss.
    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setDismissMessage(null);
        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        // This is a little hacky, but we will see if the task has finished
        // while we weren't in this activity, and then we can dismiss ourselves.
        if (mTask == null)
            dismiss();
    }

    public Params[] getParams() {
        return null;
    }

    public void updateProgress(Progress... values) {}

    public void taskFinished(Result result) {
        finishTask();
    }

    public void taskCancelled(Result result) {
        finishTask();
    }

    private void finishTask() {
        // Make sure we check if it is resumed because we will crash if trying
        // to dismiss the dialog after the user has switched to another app.
        if (isResumed())
            dismiss();

        // If we aren't resumed, setting the task to null will allow us to
        // dismiss ourselves in onResume().
        mTask = null;
    }
}

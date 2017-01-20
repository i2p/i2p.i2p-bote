package i2p.bote.android.util;

import android.os.AsyncTask;

public abstract class RobustAsyncTask<Params, Progress, Result> extends
        AsyncTask<Params, Progress, Result> {
    TaskFragment<Params, Progress, Result> mDialog;

    void setFragment(TaskFragment<Params, Progress, Result> fragment) {
        mDialog = fragment;
    }

    @Override
    protected void onProgressUpdate(Progress... values) {
        if (mDialog != null)
            mDialog.updateProgress(values);
    }

    @Override
    protected void onPostExecute(Result result) {
        if (mDialog != null)
            mDialog.taskFinished(result);
    }

    @Override
    protected void onCancelled(Result result) {
        if (mDialog != null)
            mDialog.taskCancelled(result);
    }
}

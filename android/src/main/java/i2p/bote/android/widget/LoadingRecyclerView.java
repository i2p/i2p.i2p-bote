package i2p.bote.android.widget;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

import com.pnikosis.materialishprogress.ProgressWheel;

public class LoadingRecyclerView extends RecyclerView {
    private View mLoadingView;
    private ProgressWheel mLoadingWheel;
    private boolean mLoading;
    final private AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            mLoading = false;
            updateLoading();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mLoading = false;
            updateLoading();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mLoading = false;
            updateLoading();
        }
    };

    public LoadingRecyclerView(Context context) {
        super(context);
    }

    public LoadingRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LoadingRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void updateLoading() {
        if (mLoadingView != null) {
            mLoadingView.setVisibility(mLoading ? VISIBLE : GONE);
            setVisibility(mLoading ? GONE : VISIBLE);
            if (mLoadingWheel != null) {
                if (mLoading && !mLoadingWheel.isSpinning())
                    mLoadingWheel.spin();
                else if (!mLoading && mLoadingWheel.isSpinning())
                    mLoadingWheel.stopSpinning();
            }
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
    }

    /**
     * Set the views to use for showing state.
     * <p/>
     * This method also sets the state to "loading".
     *
     * @param loadingView   The view to show in place of the RecyclerView while loading.
     * @param progressWheel The indeterminate ProgressWheel to spin while loading, if any.
     */
    public void setLoadingView(View loadingView, ProgressWheel progressWheel) {
        mLoadingView = loadingView;
        mLoadingWheel = progressWheel;
        setLoading(true);
    }

    public void setLoading(boolean loading) {
        mLoading = loading;
        updateLoading();
    }
}

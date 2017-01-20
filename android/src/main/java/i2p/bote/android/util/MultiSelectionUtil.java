/**
 * Copyright (C) 2015 str4d
 * Copyright (C) 2013 The Android Open Source Project
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

package i2p.bote.android.util;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Utilities for handling multiple selection in list views. Contains functionality similar to {@link
 * AbsListView#CHOICE_MODE_MULTIPLE_MODAL} which works with {@link AppCompatActivity} and
 * backward-compatible action bars.
 */
public class MultiSelectionUtil {

    /**
     * Attach a Controller to the given <code>recyclerView</code>, <code>activity</code>
     * and <code>listener</code>.
     *
     * @param recyclerView RecyclerView which displays {@link android.widget.Checkable} items.
     * @param activity Activity which contains the ListView.
     * @param listener Listener that will manage the selection mode.
     * @return the attached Controller instance.
     */
    public static Controller attachMultiSelectionController(final RecyclerView recyclerView,
            final AppCompatActivity activity, final MultiChoiceModeListener listener) {
        if (!(recyclerView.getAdapter() instanceof SelectableAdapter))
            throw new IllegalArgumentException("Adapter must extend SelectableAdapter");

        return new Controller(recyclerView, activity, listener);
    }

    public interface Selector {
        public boolean inActionMode();
        public void selectItem(int position, long id);
    }

    /**
     * Class which provides functionality similar to {@link AbsListView#CHOICE_MODE_MULTIPLE_MODAL}
     * for the {@link RecyclerView} provided to it.
     */
    public static class Controller implements Selector {

        private final RecyclerView mRecyclerView;
        private final SelectableAdapter mAdapter;
        private final AppCompatActivity mActivity;
        private final MultiChoiceModeListener mListener;
        private final Callbacks mCallbacks;

        // Current Action Mode (if there is one)
        private ActionMode mActionMode;

        // Keeps record of any items that should be checked on the next action mode creation
        private HashSet<Pair<Integer, Long>> mItemsToCheck;

        private Controller(RecyclerView recyclerView, AppCompatActivity activity,
                           MultiChoiceModeListener listener) {
            mRecyclerView = recyclerView;
            mAdapter = (SelectableAdapter) recyclerView.getAdapter();
            mActivity = activity;
            mListener = listener;
            mCallbacks = new Callbacks();

            mAdapter.setSelector(this);
        }

        @Override
        public boolean inActionMode() {
            return mActionMode != null;
        }

        @Override
        public void selectItem(int position, long id) {
            if (mActionMode == null) {
                mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                mItemsToCheck.add(new Pair<Integer, Long>(position, id));
                mActionMode = mActivity.startSupportActionMode(mCallbacks);
            } else {
                mAdapter.toggleSelection(position);

                // Check to see what the new checked state is, and then notify the listener
                final boolean checked = mAdapter.isSelected(position);
                mListener.onItemCheckedStateChanged(mActionMode, position, id, checked);

                boolean hasCheckedItem = checked;

                // Check to see if we have any checked items
                if (!hasCheckedItem)
                    hasCheckedItem = mAdapter.getSelectedItemCount() > 0;

                // If we don't have any checked items, finish the action mode
                if (!hasCheckedItem)
                    mActionMode.finish();
            }
        }

        /**
         * Finish the current Action Mode (if there is one).
         */
        public void finish() {
            if (mActionMode != null) {
                mActionMode.finish();
            }
        }

        /**
         * This method should be called from your {@link AppCompatActivity} or
         * {@link android.support.v4.app.Fragment Fragment} to allow the controller to restore any
         * instance state.
         *
         * @param savedInstanceState - The state passed to your Activity or Fragment.
         */
        public void restoreInstanceState(Bundle savedInstanceState) {
            if (savedInstanceState != null) {
                long[] checkedIds = savedInstanceState.getLongArray(getStateKey());
                if (checkedIds != null && checkedIds.length > 0) {
                    HashSet<Long> idsToCheckOnRestore = new HashSet<Long>();
                    for (long id : checkedIds) {
                        idsToCheckOnRestore.add(id);
                    }
                    tryRestoreInstanceState(idsToCheckOnRestore);
                }
            }
        }

        /**
         * This method should be called from
         * {@link AppCompatActivity#onSaveInstanceState(android.os.Bundle)} or
         * {@link android.support.v4.app.Fragment#onSaveInstanceState(android.os.Bundle)
         * Fragment.onSaveInstanceState(Bundle)} to allow the controller to save its instance
         * state.
         *
         * @param outState - The state passed to your Activity or Fragment.
         */
        public void saveInstanceState(Bundle outState) {
            if (mActionMode != null && mAdapter.hasStableIds()) {
                List<Integer> selectedItems = mAdapter.getSelectedItems();
                long[] selectedItemIds = new long[selectedItems.size()];
                for (int i = 0; i < selectedItems.size(); i++) {
                    selectedItemIds[i] = mAdapter.getItemId(selectedItems.get(i));
                }
                outState.putLongArray(getStateKey(), selectedItemIds);
            }
        }

        // Internal utility methods

        private String getStateKey() {
            return MultiSelectionUtil.class.getSimpleName() + "_" + mRecyclerView.getId();
        }

        private void tryRestoreInstanceState(HashSet<Long> idsToCheckOnRestore) {
            if (idsToCheckOnRestore == null) {
                return;
            }

            boolean idsFound = false;
            for (int pos = mAdapter.getItemCount() - 1; pos >= 0; pos--) {
                if (idsToCheckOnRestore.contains(mAdapter.getItemId(pos))) {
                    idsFound = true;
                    if (mItemsToCheck == null) {
                        mItemsToCheck = new HashSet<Pair<Integer, Long>>();
                    }
                    mItemsToCheck.add(new Pair<Integer, Long>(pos, mAdapter.getItemId(pos)));
                }
            }

            if (idsFound) {
                // We found some IDs that were checked. Let's now restore the multi-selection
                // state.
                mActionMode = mActivity.startSupportActionMode(mCallbacks);
            }
        }

        /**
         * This class encapsulates all of the callbacks necessary for the controller class.
         */
        final class Callbacks implements ActionMode.Callback {
            @Override
            public final boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                if (mListener.onCreateActionMode(actionMode, menu)) {
                    mActionMode = actionMode;

                    // If there are some items to check, do it now
                    if (mItemsToCheck != null) {
                        for (Pair<Integer, Long> posAndId : mItemsToCheck) {
                            mAdapter.toggleSelection(posAndId.first);
                            // Notify the listener that the item has been checked
                            mListener.onItemCheckedStateChanged(mActionMode, posAndId.first,
                                    posAndId.second, true);
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                // Proxy listener
                return mListener.onPrepareActionMode(actionMode, menu);
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                // Proxy listener
                return mListener.onActionItemClicked(actionMode, menuItem);
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                mListener.onDestroyActionMode(actionMode);

                // Clear all the checked items
                mAdapter.clearSelections();

                // Clear the Action Mode
                mActionMode = null;
            }
        }
    }

    /**
     * @see android.widget.AbsListView.MultiChoiceModeListener
     */
    public static interface MultiChoiceModeListener extends ActionMode.Callback {

        /**
         * @see android.widget.AbsListView.MultiChoiceModeListener#onItemCheckedStateChanged(
         *android.view.ActionMode, int, long, boolean)
         */
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked);
    }

    public static abstract class SelectableAdapter<VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {
        private Selector mSelector;
        private SparseBooleanArray selectedItems;

        public SelectableAdapter() {
            selectedItems = new SparseBooleanArray();
        }

        public void setSelector(Selector selector) {
            mSelector = selector;
        }

        public Selector getSelector() {
            return mSelector;
        }

        public void toggleSelection(int position) {
            if (selectedItems.get(position, false)) {
                selectedItems.delete(position);
            } else {
                selectedItems.put(position, true);
            }
            notifyItemChanged(position);
        }

        public boolean isSelected(int position) {
            return selectedItems.get(position, false);
        }

        public void clearSelections() {
            selectedItems.clear();
            notifyDataSetChanged();
        }

        public int getSelectedItemCount() {
            return selectedItems.size();
        }

        public List<Integer> getSelectedItems() {
            List<Integer> items =
                    new ArrayList<Integer>(selectedItems.size());
            for (int i = 0; i < selectedItems.size(); i++) {
                items.add(selectedItems.keyAt(i));
            }
            return items;
        }
    }
}

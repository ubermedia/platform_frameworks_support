/*
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

package androidx.appcompat.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import androidx.mediarouter.media.MediaRouter;
import androidx.mediarouter.media.MediaRouteSelector;
import androidx.appcompat.mediarouter.R;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * This class implements the route chooser dialog for {@link MediaRouter}.
 * <p>
 * This dialog allows the user to choose a route that matches a given selector.
 * </p>
 *
 * @see MediaRouteButton
 * @see MediaRouteActionProvider
 */
public class MediaRouteChooserDialog extends Dialog {
    private final MediaRouter mRouter;
    private final MediaRouterCallback mCallback;

    private MediaRouteSelector mSelector = MediaRouteSelector.EMPTY;
    private ArrayList<MediaRouter.RouteInfo> mRoutes;
    private RouteAdapter mAdapter;
    private ListView mListView;
    private boolean mAttachedToWindow;

    public MediaRouteChooserDialog(Context context) {
        this(context, 0);
    }

    public MediaRouteChooserDialog(Context context, int theme) {
        super(MediaRouterThemeHelper.createThemedContext(context, true), theme);
        context = getContext();

        mRouter = MediaRouter.getInstance(context);
        mCallback = new MediaRouterCallback();
    }

    /**
     * Gets the media route selector for filtering the routes that the user can select.
     *
     * @return The selector, never null.
     */
    public MediaRouteSelector getRouteSelector() {
        return mSelector;
    }

    /**
     * Sets the media route selector for filtering the routes that the user can select.
     *
     * @param selector The selector, must not be null.
     */
    public void setRouteSelector(MediaRouteSelector selector) {
        if (selector == null) {
            throw new IllegalArgumentException("selector must not be null");
        }

        if (!mSelector.equals(selector)) {
            mSelector = selector;

            if (mAttachedToWindow) {
                mRouter.removeCallback(mCallback);
                mRouter.addCallback(selector, mCallback,
                        MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
            }

            refreshRoutes();
        }
    }

    /**
     * Called to filter the set of routes that should be included in the list.
     * <p>
     * The default implementation iterates over all routes in the provided list and
     * removes those for which {@link #onFilterRoute} returns false.
     * </p>
     *
     * @param routes The list of routes to filter in-place, never null.
     */
    public void onFilterRoutes(List<MediaRouter.RouteInfo> routes) {
        for (int i = routes.size(); i-- > 0; ) {
            if (!onFilterRoute(routes.get(i))) {
                routes.remove(i);
            }
        }
    }

    /**
     * Returns true if the route should be included in the list.
     * <p>
     * The default implementation returns true for enabled non-default routes that
     * match the selector.  Subclasses can override this method to filter routes
     * differently.
     * </p>
     *
     * @param route The route to consider, never null.
     * @return True if the route should be included in the chooser dialog.
     */
    public boolean onFilterRoute(MediaRouter.RouteInfo route) {
        return !route.isDefault() && route.isEnabled() && route.matchesSelector(mSelector);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_LEFT_ICON);

        setContentView(R.layout.mr_media_route_chooser_dialog);
        setTitle(R.string.mr_media_route_chooser_title);

        // Must be called after setContentView.
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                MediaRouterThemeHelper.getThemeResource(
                        getContext(), R.attr.mediaRouteOffDrawable));

        mRoutes = new ArrayList<MediaRouter.RouteInfo>();
        mAdapter = new RouteAdapter(getContext(), mRoutes);
        mListView = (ListView)findViewById(R.id.media_route_list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(mAdapter);
        mListView.setEmptyView(findViewById(android.R.id.empty));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        mAttachedToWindow = true;
        mRouter.addCallback(mSelector, mCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
        refreshRoutes();
    }

    @Override
    public void onDetachedFromWindow() {
        mAttachedToWindow = false;
        mRouter.removeCallback(mCallback);

        super.onDetachedFromWindow();
    }

    /**
     * Refreshes the list of routes that are shown in the chooser dialog.
     */
    public void refreshRoutes() {
        if (mAttachedToWindow) {
            mRoutes.clear();
            mRoutes.addAll(mRouter.getRoutes());
            onFilterRoutes(mRoutes);
            Collections.sort(mRoutes, RouteComparator.sInstance);
            mAdapter.notifyDataSetChanged();
        }
    }

    private final class RouteAdapter extends ArrayAdapter<MediaRouter.RouteInfo>
            implements ListView.OnItemClickListener {
        private final LayoutInflater mInflater;

        public RouteAdapter(Context context, List<MediaRouter.RouteInfo> routes) {
            super(context, 0, routes);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = mInflater.inflate(R.layout.mr_media_route_list_item, parent, false);
            }
            MediaRouter.RouteInfo route = getItem(position);
            TextView text1 = (TextView)view.findViewById(android.R.id.text1);
            TextView text2 = (TextView)view.findViewById(android.R.id.text2);
            text1.setText(route.getName());
            String description = route.getDescription();
            if (TextUtils.isEmpty(description)) {
                text2.setVisibility(View.GONE);
                text2.setText("");
            } else {
                text2.setVisibility(View.VISIBLE);
                text2.setText(description);
            }
            view.setEnabled(route.isEnabled());
            return view;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            MediaRouter.RouteInfo route = getItem(position);
            if (route.isEnabled()) {
                route.select();
                dismiss();
            }
        }
    }

    private final class MediaRouterCallback extends MediaRouter.Callback {
        @Override
        public void onRouteAdded(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteRemoved(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteChanged(MediaRouter router, MediaRouter.RouteInfo info) {
            refreshRoutes();
        }

        @Override
        public void onRouteSelected(MediaRouter router, MediaRouter.RouteInfo route) {
            dismiss();
        }
    }

    private static final class RouteComparator implements Comparator<MediaRouter.RouteInfo> {
        public static final RouteComparator sInstance = new RouteComparator();

        @Override
        public int compare(MediaRouter.RouteInfo lhs, MediaRouter.RouteInfo rhs) {
            return lhs.getName().compareTo(rhs.getName());
        }
    }
}

/*
 * Copyright (C) 2015 Domoticz
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package nl.hnogames.domoticz.Fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.nhaarman.listviewanimations.appearance.simple.SwingBottomInAnimationAdapter;

import java.util.ArrayList;

import nl.hnogames.domoticz.Adapters.UtilityAdapter;
import nl.hnogames.domoticz.Containers.GraphPointInfo;
import nl.hnogames.domoticz.Containers.SwitchLogInfo;
import nl.hnogames.domoticz.Containers.UtilitiesInfo;
import nl.hnogames.domoticz.Domoticz.Domoticz;
import nl.hnogames.domoticz.Interfaces.DomoticzFragmentListener;
import nl.hnogames.domoticz.Interfaces.GraphDataReceiver;
import nl.hnogames.domoticz.Interfaces.SwitchLogReceiver;
import nl.hnogames.domoticz.Interfaces.UtilitiesReceiver;
import nl.hnogames.domoticz.Interfaces.UtilityClickListener;
import nl.hnogames.domoticz.Interfaces.setCommandReceiver;
import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.UI.GraphDialog;
import nl.hnogames.domoticz.UI.SwitchLogInfoDialog;
import nl.hnogames.domoticz.UI.TemperatureDialog;
import nl.hnogames.domoticz.UI.UtilitiesInfoDialog;
import nl.hnogames.domoticz.app.DomoticzFragment;

public class Utilities extends DomoticzFragment implements DomoticzFragmentListener,
        UtilityClickListener {

    private Domoticz mDomoticz;
    private ArrayList<UtilitiesInfo> mUtilitiesInfos;
    private CoordinatorLayout coordinatorLayout;

    private double thermostatSetPointValue;
    private ListView listView;
    private UtilityAdapter adapter;
    private ProgressDialog progressDialog;
    private Context mContext;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String filter = "";


    @Override
    public void refreshFragment() {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(true);

        processUtilities();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        getActionBar().setTitle(R.string.title_utilities);
    }

    @Override
    public void Filter(String text) {
        filter=text;
        try {
            if (adapter != null)
                adapter.getFilter().filter(text);
            super.Filter(text);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onConnectionOk() {
        super.showSpinner(true);
        mSwipeRefreshLayout = (SwipeRefreshLayout) getView().findViewById(R.id.swipe_refresh_layout);
        coordinatorLayout = (CoordinatorLayout) getView().findViewById(R.id
                .coordinatorLayout);

        listView = (ListView) getView().findViewById(R.id.listView);

        mDomoticz = new Domoticz(mContext);
        processUtilities();
    }

    private void processUtilities() {
        mSwipeRefreshLayout.setRefreshing(true);

        final UtilityClickListener listener = this;
        mDomoticz.getUtilities(new UtilitiesReceiver() {

            @Override
            public void onReceiveUtilities(ArrayList<UtilitiesInfo> mUtilitiesInfos) {
                successHandling(mUtilitiesInfos.toString(), false);

                Utilities.this.mUtilitiesInfos = mUtilitiesInfos;
                adapter = new UtilityAdapter(mContext, mUtilitiesInfos, listener);

                createListView();
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    private void createListView() {

        if (getView() != null) {
            SwingBottomInAnimationAdapter animationAdapter = new SwingBottomInAnimationAdapter(adapter);
            animationAdapter.setAbsListView(listView);
            listView.setAdapter(animationAdapter);

            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapterView, View view,
                                               int index, long id) {
                    showInfoDialog(adapter.filteredData.get(index));
                    return true;
                }
            });
            mSwipeRefreshLayout.setRefreshing(false);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    processUtilities();
                }
            });
            super.showSpinner(false);
            this.Filter(filter);
        }
    }

    private void showInfoDialog(final UtilitiesInfo mUtilitiesInfo) {
        UtilitiesInfoDialog infoDialog = new UtilitiesInfoDialog(
                getActivity(),
                mUtilitiesInfo,
                R.layout.dialog_utilities_info);
        infoDialog.setIdx(String.valueOf(mUtilitiesInfo.getIdx()));
        infoDialog.setLastUpdate(mUtilitiesInfo.getLastUpdate());
        infoDialog.setIsFavorite(mUtilitiesInfo.getFavoriteBoolean());
        infoDialog.show();
        infoDialog.onDismissListener(new UtilitiesInfoDialog.DismissListener() {
            @Override
            public void onDismiss(boolean isChanged, boolean isFavorite) {
                if (isChanged) changeFavorite(mUtilitiesInfo, isFavorite);
            }
        });
    }

    private void changeFavorite(final UtilitiesInfo mUtilitiesInfo, final boolean isFavorite) {
        addDebugText("changeFavorite");
        addDebugText("Set idx " + mUtilitiesInfo.getIdx() + " favorite to " + isFavorite);

        if (isFavorite)
            Snackbar.make(coordinatorLayout, mUtilitiesInfo.getName() + " " + getActivity().getString(R.string.favorite_added), Snackbar.LENGTH_SHORT).show();
        else
            Snackbar.make(coordinatorLayout, mUtilitiesInfo.getName() + " " + getActivity().getString(R.string.favorite_removed), Snackbar.LENGTH_SHORT).show();

        int jsonAction;
        int jsonUrl = Domoticz.Json.Url.Set.FAVORITE;

        if (isFavorite) jsonAction = Domoticz.Device.Favorite.ON;
        else jsonAction = Domoticz.Device.Favorite.OFF;

        mDomoticz.setAction(mUtilitiesInfo.getIdx(), jsonUrl, jsonAction, 0, new setCommandReceiver() {
            @Override
            public void onReceiveResult(String result) {
                successHandling(result, false);
                mUtilitiesInfo.setFavoriteBoolean(isFavorite);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    /**
     * Updates the set point in the Utilities container
     *
     * @param idx         ID of the utility to be changed
     * @param newSetPoint The new set point value
     */
    private void updateThermostatSetPointValue(int idx, double newSetPoint) {
        addDebugText("updateThermostatSetPointValue");

        for (UtilitiesInfo info : mUtilitiesInfos) {
            if (info.getIdx() == idx) {
                info.setSetPoint(newSetPoint);
                break;
            }
        }

        notifyDataSetChanged();
    }

    /**
     * Notifies the list view adapter the data has changed and refreshes the list view
     */
    private void notifyDataSetChanged() {
        addDebugText("notifyDataSetChanged");

        // save index and top position
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : v.getTop();

        adapter.notifyDataSetChanged();

        listView.setAdapter(adapter);
        listView.setSelectionFromTop(index, top);
    }

    @Override
    public void errorHandling(Exception error) {
        // Let's check if were still attached to an activity
        if (isAdded()) {
            super.errorHandling(error);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private UtilitiesInfo getUtility(int idx) {
        for (UtilitiesInfo info : mUtilitiesInfos) {
            if (info.getIdx() == idx) {
                return info;
            }
        }
        return null;
    }

    @Override
    public void onClick(UtilitiesInfo utility) {
    }

    @Override
    public void onLogClick(final UtilitiesInfo utility, final String range) {
        final String graphType = utility.getSubType()
                .replace("Electric", "counter")
                .replace("kWh", "counter")
                .replace("Energy", "counter");

        mDomoticz.getGraphData(utility.getIdx(), range, graphType, new GraphDataReceiver() {
            @Override
            public void onReceive(ArrayList<GraphPointInfo> mGraphList) {
                Log.i("GRAPH", mGraphList.toString());
                GraphDialog infoDialog = new GraphDialog(
                        getActivity(),
                        mGraphList,
                        R.layout.dialog_graph);
                infoDialog.setRange(range);

                if (range.equals("day"))
                    infoDialog.setSteps(3);

                infoDialog.setTitle(graphType.toUpperCase());
                infoDialog.show();
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
                Snackbar.make(coordinatorLayout, getActivity().getString(R.string.error_log) + ": " + utility.getName() + " " + graphType, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onThermostatClick(final int idx) {
        addDebugText("onThermostatClick");
        final UtilitiesInfo tempUtil = getUtility(idx);

        TemperatureDialog tempDialog = new TemperatureDialog(
                getActivity(),
                idx,
                tempUtil.getSetPoint());

        tempDialog.onDismissListener(new TemperatureDialog.DismissListener() {
            @Override
            public void onDismiss(double newSetPoint) {
                addDebugText("Set idx " + idx + " to " + String.valueOf(newSetPoint));
                if (tempUtil != null) {
                    thermostatSetPointValue = newSetPoint;
                    int jsonUrl = Domoticz.Json.Url.Set.TEMP;

                    int action = Domoticz.Device.Thermostat.Action.PLUS;
                    if (newSetPoint < tempUtil.getSetPoint())
                        action = Domoticz.Device.Thermostat.Action.MIN;

                    mDomoticz.setAction(idx, jsonUrl, action, newSetPoint, new setCommandReceiver() {
                        @Override
                        public void onReceiveResult(String result) {
                            updateThermostatSetPointValue(idx, thermostatSetPointValue);
                            successHandling(result, false);
                        }

                        @Override
                        public void onError(Exception error) {
                            errorHandling(error);
                        }
                    });
                }
            }
        });

        tempDialog.show();
    }


    @Override
    public void onLogButtonClick(int idx) {
        mDomoticz.getTextLogs(idx, new SwitchLogReceiver() {
            @Override
            public void onReceiveSwitches(ArrayList<SwitchLogInfo> switchesLogs) {
                showLogDialog(switchesLogs);
            }

            @Override
            public void onError(Exception error) {
                Snackbar.make(coordinatorLayout, getContext().getString(R.string.error_logs), Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogDialog(ArrayList<SwitchLogInfo> switchLogs) {
        if (switchLogs.size() <= 0) {
            Toast.makeText(getContext(), "No logs found.", Toast.LENGTH_LONG).show();
        } else {
            SwitchLogInfoDialog infoDialog = new SwitchLogInfoDialog(
                    getActivity(),
                    switchLogs,
                    R.layout.dialog_switch_logs);
            infoDialog.show();
        }
    }
}
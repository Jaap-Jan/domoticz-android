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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import nl.hnogames.domoticz.Adapters.CamerasAdapter;
import nl.hnogames.domoticz.CameraActivity;
import nl.hnogames.domoticz.Containers.CameraInfo;
import nl.hnogames.domoticz.Domoticz.Domoticz;
import nl.hnogames.domoticz.Interfaces.CameraReceiver;
import nl.hnogames.domoticz.Interfaces.DomoticzFragmentListener;
import nl.hnogames.domoticz.R;
import nl.hnogames.domoticz.Utils.PermissionsUtil;
import nl.hnogames.domoticz.app.DomoticzCardFragment;

public class Cameras extends DomoticzCardFragment implements DomoticzFragmentListener {

    @SuppressWarnings("unused")
    private static final String TAG = Cameras.class.getSimpleName();

    private ProgressDialog progressDialog;
    private Activity mActivity;
    private Domoticz mDomoticz;
    private RecyclerView mRecyclerView;
    private CamerasAdapter mAdapter;
    private ArrayList<CameraInfo> mCameras;
    private CoordinatorLayout coordinatorLayout;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void refreshFragment() {
        getCameras();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void getCameras() {
        coordinatorLayout = (CoordinatorLayout) getView().findViewById(R.id
                .coordinatorLayout);
        mDomoticz = new Domoticz(mActivity);
        mDomoticz.getCameras(new CameraReceiver() {

            @Override
            public void OnReceiveCameras(ArrayList<CameraInfo> Cameras) {
                successHandling(Cameras.toString(), false);

                Cameras.this.mCameras = Cameras;
                mAdapter = new CamerasAdapter(Cameras, getActivity(), mDomoticz);
                mAdapter.setOnItemClickListener(new CamerasAdapter.onClickListener() {
                    @Override
                    public void onItemClick(int position, View v) {
                        ImageView cameraImage = (ImageView) v.findViewById(R.id.image);
                        TextView cameraTitle = (TextView) v.findViewById(R.id.name);
                        Bitmap savePic = ((BitmapDrawable) cameraImage.getDrawable()).getBitmap();

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!PermissionsUtil.canAccessStorage(getActivity())) {
                                requestPermissions(PermissionsUtil.INITIAL_STORAGE_PERMS, PermissionsUtil.INITIAL_CAMERA_REQUEST);
                            } else
                                processImage(savePic, (String) cameraTitle.getText());
                        } else {
                            processImage(savePic, (String) cameraTitle.getText());
                        }
                    }
                });
                mRecyclerView.setAdapter(mAdapter);
            }

            @Override
            public void onError(Exception error) {
                errorHandling(error);
            }
        });
    }

    private void processImage(Bitmap savePic, String title) {
        File dir = mDomoticz.saveSnapShot(savePic, title);
        if (dir != null) {
            Intent intent = new Intent(getActivity(), CameraActivity.class);
            intent.putExtra("IMAGETITLE", title);
            intent.putExtra("IMAGEURL", dir.getPath());
            startActivity(intent);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
        getActionBar().setTitle(R.string.title_cameras);
    }

    public void errorHandling(Exception error) {
        // Let's check if were still attached to an activity
        if (isAdded()) {
            super.errorHandling(error);
        }
    }

    public ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    @Override
    public void onConnectionOk() {
        mDomoticz = new Domoticz(getActivity());
        mRecyclerView = (RecyclerView) getView().findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        GridLayoutManager mLayoutManager = new GridLayoutManager(getActivity(), 2);
        mRecyclerView.setLayoutManager(mLayoutManager);
        getCameras();
    }
}
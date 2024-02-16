/*
 * Version for Cordova/PhoneGap
 * © 2017-2019 YANDEX
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://yandex.com/legal/appmetrica_sdk_agreement/
 */

package com.yandex.metrica.plugin.cordova;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.appmetrica.analytics.AppMetrica;
import io.appmetrica.analytics.AppMetricaConfig;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AppMetricaPlugin extends CordovaPlugin {

    private final Object mLock = new Object();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private boolean mActivityPaused = true;
    private boolean mAppMetricaActivated = false;

    @Override
    public boolean execute(final String action, final JSONArray args,
                           final CallbackContext callbackContext) throws JSONException {
        getAppMetricaExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if ("activate".equals(action)) {
                        activate(args, callbackContext);
                    } else if ("reportEvent".equals(action)) {
                        reportEvent(args, callbackContext);
                    } else {
                        callbackContext.error("Unknown action: " + action);
                    }
                } catch (JSONException ex) {
                    callbackContext.error(ex.getMessage());
                }
            }
        });
        return true;
    }

    private Activity getActivity() {
        return cordova.getActivity();
    }

    private ExecutorService getAppMetricaExecutor() {
        return mExecutor;
    }

    private void activate(final JSONArray args,
                          final CallbackContext callbackContext) throws JSONException {
        final JSONObject configObj = args.getJSONObject(0);
        final String apiKey = configObj.getString("apiKey");

        // Creating an extended library configuration.
        final AppMetricaConfig config = AppMetricaConfig.newConfigBuilder(apiKey).withLogs().build();

        final Context context = getActivity().getApplicationContext();
        // Initializing the AppMetrica SDK.
        AppMetrica.activate(context, config);

        synchronized (mLock) {
            if (mAppMetricaActivated == false) {
                AppMetrica.reportAppOpen(getActivity());
                if (mActivityPaused == false) {
                    AppMetrica.resumeSession(getActivity());
                }
            }
            mAppMetricaActivated = true;
        }
    }

    private void reportEvent(final JSONArray args,
                             final CallbackContext callbackContext) throws JSONException {
        final String eventName = args.getString(0);
        String eventParametersJSONString = null;
        try {
            final JSONObject eventParametersObj = args.getJSONObject(1);
            eventParametersJSONString = eventParametersObj.toString();
        } catch (JSONException ignored) {}

        if (eventParametersJSONString != null) {
            AppMetrica.reportEvent(eventName, eventParametersJSONString);
        } else {
            AppMetrica.reportEvent(eventName);
        }
    }
}

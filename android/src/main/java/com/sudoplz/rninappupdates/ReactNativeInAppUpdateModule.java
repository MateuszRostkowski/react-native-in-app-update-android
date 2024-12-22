package com.sudoplz.rninappupdates;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.util.Log;
import android.graphics.Color;

import androidx.annotation.MainThread;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.Map;
import static android.app.Activity.RESULT_OK;

@ReactModule(name = ReactNativeInAppUpdateModule.NAME)
public class ReactNativeInAppUpdateModule extends ReactContextBaseJavaModule implements InstallStateUpdatedListener, LifecycleEventListener, ActivityEventListener  {
    public static final String NAME = "InAppUpdates";

    private static final int APP_UPDATE_REQUEST_CODE = 11;
    private Promise updatePromise;

    private static ReactApplicationContext reactContext;

    public static String IN_APP_UPDATE_RESULT_KEY = "in_app_update_result";
    public static String IN_APP_UPDATE_STATUS_KEY = "in_app_update_status";

    private AppUpdateManager appUpdateManager = null;
    private boolean subscribedToUpdateStatuses = false;

    private void handlePromise(String status, boolean isError) {
        if (updatePromise != null) {
            if (isError) {
                updatePromise.reject(status);
            } else {
                updatePromise.resolve(status);
            }
            updatePromise = null;
        }
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
       if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                handlePromise("IGNORED", false);
            }
        }
    }


    public ReactNativeInAppUpdateModule(ReactApplicationContext context) {
        super(reactContext);
        appUpdateManager = AppUpdateManagerFactory.create(reactContext);
        reactContext = context;
        appUpdateManager.registerListener(this);
        reactContext.addActivityEventListener(this);
        reactContext.addLifecycleEventListener(this);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // has to be here but no need for implementation
    }



    @Override
    @NonNull
    public String getName() {
        return NAME;
    }

    @ReactMethod
    public void isUpdateAvailable(Promise promise) {
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                promise.resolve(true);
            } else {
                promise.resolve(false);
            }
        }).addOnFailureListener(e -> {
            promise.resolve(false);
        });
    }

    @ReactMethod
    public void checkUpdate(Promise promise) {
        updatePromise = promise;

        appUpdateManager.registerListener(this);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) 
                    || appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.FLEXIBLE,
                            reactContext.getCurrentActivity(),
                            APP_UPDATE_REQUEST_CODE);
                } catch (IntentSender.SendIntentException e) {
                    handlePromise("ERROR", true);
                    e.printStackTrace();
                }
            } else {
                handlePromise("NO_UPDATE", false);
            }
        }).addOnFailureListener(e -> {
            handlePromise("ERROR", true);
        });

    }

    @Override
    public void onStateUpdate(InstallState state) {
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            popupSnackbarForCompleteUpdate();
        }
    }

    private void popupSnackbarForCompleteUpdate() {
        handlePromise("SUCCESS", false);

        appUpdateManager.completeUpdate();
    }

    @Override
    public void onHostResume() {
        if (appUpdateManager != null) {
            appUpdateManager
                    .getAppUpdateInfo()
                    .addOnSuccessListener(
                            appUpdateInfo -> {
                                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                                    popupSnackbarForCompleteUpdate();
                                }
                                if (appUpdateInfo.updateAvailability()
                                        == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                                    try {
                                        appUpdateManager.startUpdateFlowForResult(
                                                appUpdateInfo,
                                                AppUpdateType.IMMEDIATE,
                                                reactContext.getCurrentActivity(),
                                                APP_UPDATE_REQUEST_CODE);
                                    } catch (IntentSender.SendIntentException e) {
                                        handlePromise("ERROR", true);
                                        e.printStackTrace();
                                    }
                                }

                            });
        }
    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        if (appUpdateManager != null) {
            appUpdateManager.unregisterListener(this);
        }
    }
}

package com.thisisnotajoke.wearatron.mobile.model;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;
import com.google.gson.Gson;
import com.thisisnotajoke.lockitron.Lock;
import com.thisisnotajoke.lockitron.model.DataManager;
import com.thisisnotajoke.lockitron.model.LockBody;
import com.thisisnotajoke.lockitron.model.LockStore;
import com.thisisnotajoke.lockitron.model.LockitronWebService;
import com.thisisnotajoke.lockitron.model.PreferenceManager;
import com.thisisnotajoke.lockitron.model.WearDataApi;
import com.thisisnotajoke.lockitron.model.event.LockUpdatedEvent;

import org.scribe.model.Token;

import java.util.List;

import de.greenrobot.event.EventBus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MobileDataManager implements DataManager {
    private static final String TAG = "DataManager";
    private final PreferenceManager mPreferenceManager;
    private final Gson mGson;
    private final GoogleApiClient mGoogleApiClient;
    private final LockStore mLockStore;
    private final LockitronWebService mWebService;
    private final FirebaseAnalytics mAnalytics;

    public MobileDataManager(Context c, PreferenceManager preferenceManager, Gson gson, LockitronWebService webService, LockStore lockStore) {
        mGson = gson;
        mPreferenceManager = preferenceManager;

        mGoogleApiClient = new GoogleApiClient.Builder(c)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        mWebService = webService;
        mLockStore = lockStore;
        mAnalytics = FirebaseAnalytics.getInstance(c);
    }

    public void setActiveLock(Lock lock) {
        mPreferenceManager.setLock(lock);
        mPreferenceManager.requestBackup();
        PutDataMapRequest dataMap = PutDataMapRequest.create(WearDataApi.LOCK_ITEM_PATH);
        dataMap.getDataMap().putString(WearDataApi.LOCK_ITEM_KEY, mGson.toJson(lock));
        PutDataRequest request = dataMap.asPutDataRequest().setUrgent();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request);
    }

    public Lock getActiveLock() {
        return mPreferenceManager.getLock();
    }

    public List<Lock> getMyLocks() {
        return mLockStore.all();
    }

    public void loadLocks() {
        mWebService.getMyLocks(new Callback<List<Lock>>() {
            @Override
            public void success(List<Lock> locks, Response response) {
                for(Lock lock : locks) {
                    mLockStore.putLock(lock);
                }
                EventBus.getDefault().post(new LockUpdatedEvent());
            }

            @Override
            public void failure(RetrofitError error) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Load locks failed");
                FirebaseCrash.report(error);
            }
        });
    }

    public Token getToken() {
        return mPreferenceManager.getToken();
    }

    public void setToken(Token token) {
        mPreferenceManager.setToken(token);
    }

    public void lockMyLock() {
        Lock lock = getActiveLock();
        if(lock == null)
            return;
        LockBody body = new LockBody();
        body.state = "lock";
        mWebService.updateLock(lock.getUUID(), body, new Callback<Lock>() {
            @Override
            public void success(Lock lock, Response response) {
                Bundle bundle = new Bundle();
                bundle.putString("TYPE", "lock");
                mAnalytics.logEvent("ACTION", bundle);
            }

            @Override
            public void failure(RetrofitError error) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Lock lock failed");
                FirebaseCrash.report(error);
            }
        });
    }

    public void unlockMyLock() {
        Lock lock = getActiveLock();
        if(lock == null)
            return;
        LockBody body = new LockBody();
        body.state = "unlock";
        mWebService.updateLock(lock.getUUID(), body, new Callback<Lock>() {
            @Override
            public void success(Lock lock, Response response) {
                Bundle bundle = new Bundle();
                bundle.putString("TYPE", "unlock");
                mAnalytics.logEvent("ACTION", bundle);
            }

            @Override
            public void failure(RetrofitError error) {
                FirebaseCrash.logcat(Log.ERROR, TAG, "Unlock lock failed");
                FirebaseCrash.report(error);
            }
        });
    }
}

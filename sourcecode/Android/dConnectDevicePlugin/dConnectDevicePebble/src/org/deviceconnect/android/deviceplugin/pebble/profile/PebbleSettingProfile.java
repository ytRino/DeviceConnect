/*
 PebbleSettingProfile.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.pebble.profile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.deviceconnect.android.deviceplugin.pebble.PebbleDeviceService;
import org.deviceconnect.android.deviceplugin.pebble.util.PebbleManager;
import org.deviceconnect.android.deviceplugin.pebble.util.PebbleManager.OnSendCommandListener;

import android.content.Intent;

import com.getpebble.android.kit.util.PebbleDictionary;
import org.deviceconnect.android.message.MessageUtils;
import org.deviceconnect.android.profile.SettingsProfile;
import org.deviceconnect.message.DConnectMessage;

/**
 * Pebble端末内時間情報取得プロファイル.
 * @author NTT DOCOMO, INC.
 */
public class PebbleSettingProfile extends SettingsProfile {
    @Override
    protected boolean onGetDate(final Intent request, final Intent response, 
            final String deviceId) {
        if (deviceId == null) {
            createEmptyDeviceId(response);
            return true;
        } else if (!checkDeviceId(deviceId)) {
            createNotFoundDevice(response);
            return true;
        } else {
            // Pebbleに送信
            PebbleManager mgr = ((PebbleDeviceService) getContext()).getPebbleManager();
            PebbleDictionary dic = new PebbleDictionary();
            dic.addInt8(PebbleManager.KEY_PROFILE, (byte) PebbleManager.PROFILE_SETTING);
            dic.addInt8(PebbleManager.KEY_ATTRIBUTE, (byte) PebbleManager.SETTING_ATTRIBUTE_DATE);
            dic.addInt8(PebbleManager.KEY_ACTION, (byte) PebbleManager.ACTION_GET);
            mgr.sendCommandToPebble(dic, new OnSendCommandListener() {
                @Override
                public void onReceivedData(final PebbleDictionary dic) {
                    if (dic == null) {
                        MessageUtils.setTimeoutError(response);
                    } else {
                        String date = dic.getString(PebbleManager.KEY_PARAM_SETTING_DATE);
                        if (date == null) {
                            MessageUtils.setTimeoutError(response);
                        } else {
                            setResult(response, DConnectMessage.RESULT_OK);
                            setDate(response, date);
                        }
                    }
                    getContext().sendBroadcast(response);
                }
            });
            return false;
        }
    }

    /**
     * デバイスIDをチェックする.
     * 
     * @param deviceId デバイスID
     * @return <code>deviceId</code>がテスト用デバイスIDに等しい場合はtrue、そうでない場合はfalse
     */
    private boolean checkDeviceId(final String deviceId) {
        String regex = PebbleNetworkServceDiscoveryProfile.DEVICE_ID;
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(deviceId);
        return m.find();
    }

    /**
     * デバイスIDが空の場合のエラーを作成する.
     * 
     * @param response レスポンスを格納するIntent
     */
    private void createEmptyDeviceId(final Intent response) {
        MessageUtils.setEmptyDeviceIdError(response);
    }

    /**
     * デバイスが発見できなかった場合のエラーを作成する.
     * 
     * @param response レスポンスを格納するIntent
     */
    private void createNotFoundDevice(final Intent response) {
        MessageUtils.setNotFoundDeviceError(response);
    }
}

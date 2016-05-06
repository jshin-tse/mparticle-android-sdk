package com.mparticle.internal;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.mparticle.MParticle;
import com.mparticle.MParticle.LogLevel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Mixin utility class responsible for generating all sorts of device information, mostly
 * used by the DeviceInfo and AppInfo dictionaries within batch messages.
 */
public class MPUtility {

    static final String NO_BLUETOOTH = "none";
    private static String sOpenUDID;
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String getCpuUsage() {
        String str1 = "unknown";
        String str2 = String.valueOf(android.os.Process.myPid());
        java.lang.Process process = null;
        BufferedReader bufferedReader = null;
        String str3 = null;
        try {
            String[] command = {"top", "-d", "1", "-n", "1"};
            process = new ProcessBuilder().command(command).redirectErrorStream(true).start();
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((str3 = bufferedReader.readLine()) != null)
                if (str3.contains(str2)) {
                    String[] arrayOfString = str3.split(" ");
                    if (arrayOfString != null) {
                        for (int i = 0; i < arrayOfString.length; i++) {
                            if ((arrayOfString[i] != null) && (arrayOfString[i].contains("%"))) {
                                str1 = arrayOfString[i];
                                str1 = str1.substring(0, str1.length() - 1);
                                return str1;
                            }
                        }
                    }
                }
        } catch (IOException localIOException2) {
            ConfigManager.log(MParticle.LogLevel.WARNING, "Error computing CPU usage");
        } finally {
            try {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
                if (process != null) {
                    try {
                        // use exitValue() to determine if process is still running.
                        process.exitValue();

                    } catch (IllegalThreadStateException e) {
                        // process is still running, kill it.
                        process.destroy();
                    }
                }
            } catch (IOException localIOException4) {
                ConfigManager.log(MParticle.LogLevel.WARNING, "Error computing CPU usage");
            }
        }
        return str1;
    }

    public static long getAvailableMemory(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.availMem;
    }

    public static boolean isSystemMemoryLow(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.lowMemory;
    }

    public static long getSystemMemoryThreshold(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.threshold;
    }

    public static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    public static void getGoogleAdIdInfo(Context context, GoogleAdIdListener adIdListener) {
        if (adIdListener != null) {
            try {
                Class AdvertisingIdClient = Class
                        .forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
                Method getAdvertisingInfo = AdvertisingIdClient.getMethod("getAdvertisingIdInfo",
                        Context.class);
                Object advertisingInfo = getAdvertisingInfo.invoke(null, context);
                Method isLimitAdTrackingEnabled = advertisingInfo.getClass().getMethod(
                        "isLimitAdTrackingEnabled");
                Boolean limitAdTrackingEnabled = (Boolean) isLimitAdTrackingEnabled
                        .invoke(advertisingInfo);
                String advertisingId = null;
                if (!limitAdTrackingEnabled) {
                    Method getId = advertisingInfo.getClass().getMethod("getId");
                    advertisingId = (String) getId.invoke(advertisingInfo);
                }
                adIdListener.onGoogleIdInfoRetrieved(advertisingId, limitAdTrackingEnabled);
            } catch (Exception cnfe) {
            }
        }
    }

    public static String getGpsEnabled(Context context) {
        if (PackageManager.PERMISSION_GRANTED == context
                .checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            final LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return Boolean.toString(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
        } else {
            return null;
        }
    }

    public static long getAvailableInternalDisk() {
        File path = Environment.getDataDirectory();
        return getDiskSpace(path);
    }

    public static long getAvailableExternalDisk() {
        File path = Environment.getExternalStorageDirectory();
        return getDiskSpace(path);
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (Exception e) {
            // ignore missing data
        }
        return "unknown";
    }

    public static String hmacSha256Encode(String key, String data) throws NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("utf-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        return asHex(sha256_HMAC.doFinal(data.getBytes("utf-8")));
    }

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    public static JSONObject getJsonResponse(HttpURLConnection connection) {
        try {
            StringBuilder responseBuilder = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                responseBuilder.append(line + '\n');
            }
            in.close();
            return new JSONObject(responseBuilder.toString());
        } catch (IOException ex) {

        } catch (JSONException jse) {

        }
        return null;
    }

    public static long getDiskSpace(File path){
        long availableSpace = -1L;
        StatFs stat = new StatFs(path.getPath());
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            availableSpace = JellybeanHelper.getAvailableMemory(stat);
        }
        if (availableSpace == 0){
            availableSpace = (long) stat.getAvailableBlocks() * (long) stat.getBlockSize();
        }
        return availableSpace;
    }

    public static long millitime(){
        return TimeUnit.MILLISECONDS.convert(System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    public static String getAndroidID(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), "android_id");
    }

    public static String getTimeZone() {
        return TimeZone.getDefault().getDisplayName(false, 0);
    }

    public static int getOrientation(Context context) {
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display getOrient = windowManager.getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if (getOrient.getWidth() == getOrient.getHeight()) {
            orientation = Configuration.ORIENTATION_SQUARE;
        } else {
            if (getOrient.getWidth() < getOrient.getHeight()) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            } else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    public static long getTotalMemory(Context context) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            return getTotalMemoryJB(context);
        } else {
            return getTotalMemoryPreJB();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static long getTotalMemoryJB(Context context) {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        return mi.totalMem;
    }

    public static long getTotalMemoryPreJB() {
        String str1 = "/proc/meminfo";
        String str2;
        String[] arrayOfString;
        long initial_memory = 0;
        try {
            FileReader localFileReader = new FileReader(str1);
            BufferedReader localBufferedReader = new BufferedReader(localFileReader, 8192);
            str2 = localBufferedReader.readLine();//meminfo
            arrayOfString = str2.split("\\s+");
            initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;
            localBufferedReader.close();
            return initial_memory;
        } catch (IOException e) {
            return -1;
        }
    }

    public static String getOpenUDID(Context context) {
        if (sOpenUDID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    Constants.PREFS_FILE, Context.MODE_PRIVATE);
            sOpenUDID = sharedPrefs.getString(Constants.PrefKeys.OPEN_UDID, null);
            if (sOpenUDID == null) {
                sOpenUDID = getAndroidID(context);
                if (sOpenUDID == null)
                    sOpenUDID = getGeneratedUdid();

                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(Constants.PrefKeys.OPEN_UDID, sOpenUDID);
                editor.apply();
            }
        }
        return sOpenUDID;
    }

    public static String getRampUdid(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                    Constants.PREFS_FILE, Context.MODE_PRIVATE);
        String rampUdid = sharedPrefs.getString(Constants.PrefKeys.DEVICE_RAMP_UDID, null);
        if (rampUdid == null) {
            rampUdid = getGeneratedUdid();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(Constants.PrefKeys.DEVICE_RAMP_UDID, rampUdid);
            editor.apply();
        }
        return rampUdid;
    }

    static String getGeneratedUdid() {
        SecureRandom localSecureRandom = new SecureRandom();
        return new BigInteger(64, localSecureRandom).toString(16);
    }

    static String getBuildUUID(String versionCode) {
        if (versionCode == null) {
            versionCode = DeviceAttributes.UNKNOWN;
        }
        return UUID.nameUUIDFromBytes(versionCode.getBytes()).toString();
    }

    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    public static boolean hasNfc(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    public static String getBluetoothVersion(Context context) {
        String bluetoothVersion = NO_BLUETOOTH;
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) && (context.getPackageManager().hasSystemFeature("android.hardware.bluetooth_le"))) {
            bluetoothVersion = "ble";
        } else if (context.getPackageManager().hasSystemFeature("android.hardware.bluetooth")) {
            bluetoothVersion = "classic";
        }
        return bluetoothVersion;
    }

    public static boolean isPhoneRooted() {

        // get from build info
        String buildTags = android.os.Build.TAGS;
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }

        boolean bool = false;
        String[] arrayOfString1 = {"/sbin/", "/system/bin/", "/system/xbin/", "/data/local/xbin/", "/data/local/bin/", "/system/sd/xbin/", "/system/bin/failsafe/", "/data/local/"};
        for (String str : arrayOfString1) {
            File localFile = new File(str + "su");
            if (localFile.exists()) {
                bool = true;
                break;
            }
        }
        return bool;
    }

    public static int mpHash(String input) {
        int hash = 0;

        if (input == null || input.length() == 0)
            return hash;

        char[] chars = input.toLowerCase().toCharArray();

        for (char c : chars) {
            hash = ((hash << 5) - hash) + c;
        }

        return hash;
    }

    public static boolean hasTelephony(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    public static boolean isBluetoothEnabled(Context context) {
        if (checkPermission(context, Manifest.permission.BLUETOOTH)) {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter != null) {
                return mBluetoothAdapter.isEnabled();
            }
        }
        return false;
    }

    public static boolean checkPermission(Context context, String permission) {
        int res = context.checkCallingOrSelfPermission(permission);
        return (res == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean isGmsAdIdAvailable() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
            return false;
        }
        try {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient");
            return true;
        } catch (ClassNotFoundException cnfe) {

        }
        return false;
    }

    public static boolean isSupportLibAvailable(){

        try {
            Class.forName("android.support.v4.app.FragmentActivity");
            return true;
        } catch (Exception cnfe) {

        }
        return false;
    }

    public static boolean isGcmServicesAvailable() {
        try {
            Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
            return true;
        } catch (Exception cnfe) {

        }
        return false;
    }

    public static BigInteger hashFnv1A(byte[] data) {
        final BigInteger INIT64 = new BigInteger("cbf29ce484222325", 16);
        final BigInteger PRIME64 = new BigInteger("100000001b3", 16);
        final BigInteger MOD64 = new BigInteger("2").pow(64);

        BigInteger hash = INIT64;

        for (byte b : data) {
            hash = hash.xor(BigInteger.valueOf((int) b & 0xff));
            hash = hash.multiply(PRIME64).mod(MOD64);
        }

        return hash;
    }

    public static boolean isServiceAvailable(Context context, Class<?> service) {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(context, service);
        List resolveInfo =
                packageManager.queryIntentServices(intent,
                        PackageManager.MATCH_DEFAULT_ONLY);
        if (resolveInfo.size() > 0) {
            return true;
        }
        return false;
    }

    public static JSONObject wrapExtras(Bundle extras) {
        if (extras != null && !extras.isEmpty()) {
            JSONObject parameters = new JSONObject();
            for (String key : extras.keySet()) {
                Object value;
                if ((value = extras.getBundle(key)) != null) {
                    try {
                        parameters.put(key, wrapExtras((Bundle) value));
                    } catch (JSONException e) {

                    }
                } else if ((value = extras.get(key)) != null) {
                    String stringVal = value.toString();
                    if ((stringVal.length() < 500)) {
                        try {
                            parameters.put(key, stringVal);
                        } catch (JSONException e) {

                        }
                    }
                }
            }
            return parameters;
        }else{
            return null;
        }
    }

    public static boolean isAppDebuggable(Context context){
        return ( 0 != ( context.getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE ) );
    }

    /**
     * This method makes sure the constraints on event attributes are enforced. A JSONObject version
     * of the attributes is return with data that exceeds the limits removed. NOTE: Non-string
     * attributes are not converted to strings, currently.
     *
     * @param attributes the user-provided JSONObject
     * @return a cleansed copy of the JSONObject
     */
    public static JSONObject enforceAttributeConstraints(Map<String, String> attributes) {
        if (null == attributes) {
            return null;
        }
        JSONObject checkedAttributes = new JSONObject();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            setCheckedAttribute(checkedAttributes, key, value, false, false);
        }
        return checkedAttributes;
    }
    public static Boolean setCheckedAttribute(JSONObject attributes, String key, Object value, boolean increment, boolean userAttribute) {
        return setCheckedAttribute(attributes, key, value, false, increment, userAttribute);
    }

    public static Boolean setCheckedAttribute(JSONObject attributes, String key, Object value, Boolean caseInsensitive, boolean increment, boolean userAttribute) {
        if (null == attributes || null == key) {
            return false;
        }
        try {
            if (caseInsensitive) {
                key = findCaseInsensitiveKey(attributes, key);
            }

            if (Constants.LIMIT_ATTR_COUNT == attributes.length() && !attributes.has(key)) {
                ConfigManager.log(LogLevel.ERROR, "Attribute count exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (value != null) {
                String stringValue = value.toString();
                if ((userAttribute && stringValue.length() > Constants.LIMIT_USER_ATTR_VALUE) || (!userAttribute && stringValue.length() > Constants.LIMIT_ATTR_VALUE) ){
                    ConfigManager.log(LogLevel.ERROR, "Attribute value length exceeds limit. Discarding attribute: " + key);
                    return false;
                }
            }
            if (key.length() > Constants.LIMIT_ATTR_NAME) {
                ConfigManager.log(LogLevel.ERROR, "Attribute name length exceeds limit. Discarding attribute: " + key);
                return false;
            }
            if (value == null) {
                value = JSONObject.NULL;
            }
            if (increment){
                String oldValue = attributes.optString(key, "0");
                int oldInt = Integer.parseInt(oldValue);
                value = Integer.toString((Integer)value + oldInt);
            }
            attributes.put(key, value);
        } catch (JSONException e) {
            ConfigManager.log(LogLevel.ERROR, "JSON error processing attributes. Discarding attribute: " + key);
            return false;
        } catch (NumberFormatException nfe){
            ConfigManager.log(LogLevel.ERROR, "Attempted to increment a key that could not be parsed as an integer: " + key);
            return false;
        } catch (Exception e){
            ConfigManager.log(LogLevel.ERROR, "Failed to add attribute: " + e.getMessage());
            return false;
        }
        return true;
    }

    public static String findCaseInsensitiveKey(JSONObject jsonObject, String key) {
        Iterator<String> keys = jsonObject.keys();
        while (keys.hasNext()) {
            String currentKey = keys.next();
            if (currentKey.equalsIgnoreCase(key)) {
                return currentKey;
            }
        }
        return key;
    }

    public static long generateMpid() {
        while (true)
        {
            long id = hashFnv1A(UUID.randomUUID().toString().getBytes()).longValue();
            if (id != 0) {
                return id;
            }
        }
    }

    public interface GoogleAdIdListener {
        void onGoogleIdInfoRetrieved(String googleAdId, Boolean limitAdTrackingEnabled);
    }
}

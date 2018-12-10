package com.trytunnels.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import org.strongswan.android.R;
import org.strongswan.android.logic.StrongSwanApplication;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import devliving.online.securedpreferencestore.DefaultRecoveryHandler;
import devliving.online.securedpreferencestore.SecuredPreferenceStore;

public class PreferenceStoreUtil {
    private static PreferenceStoreUtil INSTANCE = null;

    private boolean useSecureStore = true;
    private boolean initialized = false;

    private SecuredPreferenceStore mSecureStore;
    private SharedPreferences mSharedPref = null;

    private SecretKey secretKey = null;
    private boolean encrypting = true;

    private static final String TAG = "PreferenceStoreUtil";

    private PreferenceStoreUtil() {

    };

    public static PreferenceStoreUtil getInstance()
    {
        if(INSTANCE == null)
        {
            INSTANCE = new PreferenceStoreUtil();
        }
        return INSTANCE;
    }

    public void initializeStore(Context context)
    {
        if(!initialized) {
            try {
                SecuredPreferenceStore.init(context, new DefaultRecoveryHandler());
                mSecureStore = SecuredPreferenceStore.getSharedInstance();
            } catch (Exception e)
            {
                useSecureStore = false;

                mSharedPref = context.getSharedPreferences(context.getString(R.string.tt_shared_pref_file), Context.MODE_PRIVATE);

                try {
                    generateKey(context);
                }
                catch(Exception e2)
                {
                    encrypting = false;
                }
            }

            initialized = true;
        }
    }

    static byte[] salt = {(byte)0x7a, (byte)0xc3, (byte)0x60, (byte)0x6d,
            (byte)0x38, (byte)0xb1, (byte)0x1c, (byte)0xaf};

    private void generateKey(Context context)
            throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID).toCharArray(), salt, 65536, 256);
        secretKey = factory.generateSecret(spec);
    }

    private byte[] encryptMsg(String message)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException
    {
   /* Encrypt the message. */
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] cipherText = cipher.doFinal(message.getBytes("UTF-8"));
        return cipherText;
    }

    private String decryptMsg(byte[] cipherText)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidParameterSpecException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException, UnsupportedEncodingException
    {
    /* Decrypt the message, given derived encContentValues and initialization vector. */
        Cipher cipher = null;
        cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        String decryptString = new String(cipher.doFinal(cipherText), "UTF-8");
        return decryptString;
    }

    public String getString(String key, String defaultValue)
    {
        if(!initialized)
        {
            initializeStore(StrongSwanApplication.getContext());
        }

        if(useSecureStore)
        {
            return mSecureStore.getString(key, defaultValue);
        }
        else
        {
            if(encrypting) {
                try
                {
                    String result = mSharedPref.getString(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), defaultValue);
                    if(!result.equals(defaultValue)) {
                        result = decryptMsg(Base64.decode(result, Base64.DEFAULT));
                    }
                    return result;
                }
                catch (Exception e)
                {
                    encrypting = false;
                }
            }

            return mSharedPref.getString(key, defaultValue);
        }
    }

    public boolean getBoolean(String key, Boolean defaultValue)
    {
        if(useSecureStore)
        {
            return mSecureStore.getBoolean(key, defaultValue);
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    Boolean result = mSharedPref.getBoolean(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), defaultValue);
                    return  result;
                }
                catch (Exception e)
                {
                    encrypting = false;
                }
            }
            return mSharedPref.getBoolean(key, defaultValue);
        }
    }

    public int getInt(String key, int defaultValue)
    {
        if(useSecureStore)
        {
            return mSecureStore.getInt(key, defaultValue);
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    int result = mSharedPref.getInt(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), defaultValue);
                    return  result;
                }
                catch (Exception e)
                {
                    encrypting = false;
                }
            }

            return mSharedPref.getInt(key, defaultValue);
        }
    }

    public Set<String> getStringSet(String key, Set<String> defaultValue)
    {
        if(useSecureStore)
        {
            return mSecureStore.getStringSet(key, defaultValue);
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    Set<String> result = mSharedPref.getStringSet(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), defaultValue);
                    return  result;
                }
                catch (Exception e)
                {
                    encrypting = false;
                }
            }

            return mSharedPref.getStringSet(key, defaultValue);
        }
    }

    public long getLong(String key, long defaultValue)
    {
        if(useSecureStore)
        {
            return mSecureStore.getLong(key, defaultValue);
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    long result = mSharedPref.getLong(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), defaultValue);
                    return  result;
                }
                catch (Exception e)
                {
                    encrypting = false;
                }
            }

            return mSharedPref.getLong(key, defaultValue);
        }
    }

    public void putString(String key, String value)
    {
        if(useSecureStore)
        {
            mSecureStore.edit().putString(key, value).apply();
        }
        else
        {
            if(encrypting)
            {
                try {
                    mSharedPref.edit().putString(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT),
                            Base64.encodeToString(encryptMsg(value), Base64.DEFAULT)).apply();
                }
                catch(Exception e)
                {

                }
            }
            else {
                mSharedPref.edit().putString(key, value).apply();
            }
        }
    }

    public void putBoolean(String key, Boolean value)
    {
        if(!initialized)
        {
            initializeStore(StrongSwanApplication.getContext());
        }

        if(useSecureStore)
        {
            mSecureStore.edit().putBoolean(key, value).apply();
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    mSharedPref.edit().putBoolean(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), value).apply();
                }
                catch(Exception e)
                {

                }
            }
            else {
                mSharedPref.edit().putBoolean(key, value).apply();
            }
        }
    }

    public void putInt(String key, int value)
    {
        if(useSecureStore)
        {
            mSecureStore.edit().putInt(key, value).apply();
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    mSharedPref.edit().putInt(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), value).apply();
                }
                catch(Exception e)
                {

                }
            }
            else {
                mSharedPref.edit().putInt(key, value).apply();
            }
        }
    }

    public void putStringSet(String key, Set<String> value)
    {
        if(useSecureStore)
        {
            mSecureStore.edit().putStringSet(key, value).apply();
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    mSharedPref.edit().putStringSet(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), value).apply();
                }
                catch(Exception e)
                {

                }
            }
            else {
                mSharedPref.edit().putStringSet(key, value).apply();
            }
        }
    }

    public void putLong(String key, long value)
    {
        if(useSecureStore)
        {
            mSecureStore.edit().putLong(key, value).apply();
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    mSharedPref.edit().putLong(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT), value).apply();
                }
                catch(Exception e)
                {

                }
            }
            else {
                mSharedPref.edit().putLong(key, value).apply();
            }
        }
    }

    public void remove(String key)
    {
        if(useSecureStore)
        {
            mSecureStore.edit().remove(key).apply();
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    mSharedPref.edit().remove(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT)).apply();
                }
                catch(Exception e)
                {

                }
            }
            else {
                mSharedPref.edit().remove(key).apply();
            }
        }
    }

    public Boolean contains(String key)
    {
        if(!initialized)
        {
            initializeStore(StrongSwanApplication.getContext());
        }

        if(useSecureStore)
        {
            return mSecureStore.contains(key);
        }
        else
        {
            if(encrypting)
            {
                try
                {
                    return mSharedPref.contains(Base64.encodeToString(encryptMsg(key), Base64.DEFAULT));
                }
                catch (Exception e)
                {
                    encrypting = false;
                }
            }
            return mSharedPref.contains(key);
        }
    }
}
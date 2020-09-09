package com.example.enactusapp.Utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class SecuritySharedPreference implements SharedPreferences {
    private static final String TAG = SecuritySharedPreference.class.getName();
    private SharedPreferences mSharedPreferences;
    private Context mContext;

    public SecuritySharedPreference(Context context, String name, int mode) {
        this.mContext = context;
        if (TextUtils.isEmpty(name)) {
            this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        } else {
            this.mSharedPreferences = context.getSharedPreferences(name, mode);
        }

    }

    public Map<String, String> getAll() {
        Map<String, ?> encryptMap = this.mSharedPreferences.getAll();
        Map<String, String> decryptMap = new HashMap(10);
        Iterator var3 = encryptMap.entrySet().iterator();

        while (var3.hasNext()) {
            Map.Entry<String, ?> entry = (Map.Entry) var3.next();
            Object cipherText = entry.getValue();
            if (cipherText != null) {
                decryptMap.put(entry.getKey(), entry.getValue().toString());
            }
        }

        return decryptMap;
    }

    @Nullable
    public String getString(String key, String defValue) {
        String encryptValue = this.mSharedPreferences.getString(this.encryptPreference(key), (String) null);
        if (encryptValue == null) {
            encryptValue = this.mSharedPreferences.getString(this.encryptPreferenceOld(key), (String) null);
            return encryptValue != null ? this.decryptPreferenceOld(encryptValue) : defValue;
        } else {
            return this.decryptPreference(encryptValue);
        }
    }

    @Nullable
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Set<String> encryptSet = this.mSharedPreferences.getStringSet(this.encryptPreference(key), (Set) null);
        HashSet decryptSet;
        Iterator var5;
        String encryptValue;
        if (encryptSet == null) {
            encryptSet = this.mSharedPreferences.getStringSet(this.encryptPreferenceOld(key), (Set) null);
            if (encryptSet == null) {
                return defValues;
            } else {
                decryptSet = new HashSet();
                var5 = encryptSet.iterator();

                while (var5.hasNext()) {
                    encryptValue = (String) var5.next();
                    decryptSet.add(this.decryptPreferenceOld(encryptValue));
                }

                return decryptSet;
            }
        } else {
            decryptSet = new HashSet();
            var5 = encryptSet.iterator();

            while (var5.hasNext()) {
                encryptValue = (String) var5.next();
                decryptSet.add(this.decryptPreference(encryptValue));
            }

            return decryptSet;
        }
    }

    public int getInt(String key, int defValue) {
        String encryptValue = this.mSharedPreferences.getString(this.encryptPreference(key), (String) null);
        if (encryptValue == null) {
            encryptValue = this.mSharedPreferences.getString(this.encryptPreferenceOld(key), (String) null);
            return encryptValue != null ? Integer.parseInt(this.decryptPreferenceOld(encryptValue)) : defValue;
        } else {
            return Integer.parseInt(this.decryptPreference(encryptValue));
        }
    }

    public long getLong(String key, long defValue) {
        String encryptValue = this.mSharedPreferences.getString(this.encryptPreference(key), (String) null);
        if (encryptValue == null) {
            encryptValue = this.mSharedPreferences.getString(this.encryptPreferenceOld(key), (String) null);
            return encryptValue != null ? Long.parseLong(this.decryptPreferenceOld(encryptValue)) : defValue;
        } else {
            return Long.parseLong(this.decryptPreference(encryptValue));
        }
    }

    public float getFloat(String key, float defValue) {
        String encryptValue = this.mSharedPreferences.getString(this.encryptPreference(key), (String) null);
        if (encryptValue == null) {
            encryptValue = this.mSharedPreferences.getString(this.encryptPreferenceOld(key), (String) null);
            return encryptValue != null ? Float.parseFloat(this.decryptPreferenceOld(encryptValue)) : defValue;
        } else {
            return Float.parseFloat(this.decryptPreference(encryptValue));
        }
    }

    public boolean getBoolean(String key, boolean defValue) {
        String encryptValue = this.mSharedPreferences.getString(this.encryptPreference(key), (String) null);
        if (encryptValue == null) {
            encryptValue = this.mSharedPreferences.getString(this.encryptPreferenceOld(key), (String) null);
            return encryptValue != null ? Boolean.parseBoolean(this.decryptPreferenceOld(encryptValue)) : defValue;
        } else {
            return Boolean.parseBoolean(this.decryptPreference(encryptValue));
        }
    }

    public boolean contains(String key) {
        return this.mSharedPreferences.contains(this.encryptPreference(key)) || this.mSharedPreferences.contains(this.encryptPreferenceOld(key));
    }

    public SecuritySharedPreference.SecurityEditor edit() {
        return new SecuritySharedPreference.SecurityEditor();
    }

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        this.mSharedPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        this.mSharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    private String encryptPreference(String plainText) {
        return EncryptUtils.getInstance(this.mContext).encryptByAes128(plainText);
    }

    private String decryptPreference(String cipherText) {
        return EncryptUtils.getInstance(this.mContext).decryptByAes128(cipherText);
    }

    /**
     * @deprecated
     */
    @Deprecated
    private String encryptPreferenceOld(String plainText) {
        return EncryptUtils.getInstance(this.mContext).encryptByAes128Old(plainText);
    }

    /**
     * @deprecated
     */
    @Deprecated
    private String decryptPreferenceOld(String cipherText) {
        return EncryptUtils.getInstance(this.mContext).decryptByAes128Old(cipherText);
    }

    public void handleTransition() {
        Map<String, ?> oldMap = this.mSharedPreferences.getAll();
        Map<String, String> newMap = new HashMap(10);
        Iterator var3 = oldMap.entrySet().iterator();

        while (var3.hasNext()) {
            Map.Entry<String, ?> entry = (Map.Entry) var3.next();
            newMap.put(this.encryptPreference((String) entry.getKey()), this.encryptPreference(entry.getValue().toString()));
        }

        Editor editor = this.mSharedPreferences.edit();
        editor.clear().commit();
        Iterator var7 = newMap.entrySet().iterator();

        while (var7.hasNext()) {
            Map.Entry<String, String> entry = (Map.Entry) var7.next();
            editor.putString((String) entry.getKey(), (String) entry.getValue());
        }

        editor.commit();
    }

    public final class SecurityEditor implements Editor {
        private Editor mEditor;

        private SecurityEditor() {
            this.mEditor = SecuritySharedPreference.this.mSharedPreferences.edit();
        }

        public Editor putString(String key, String value) {
            this.mEditor.putString(SecuritySharedPreference.this.encryptPreference(key), SecuritySharedPreference.this.encryptPreference(value));
            return this;
        }

        public Editor putStringSet(String key, Set<String> values) {
            Set<String> encryptSet = new HashSet();
            Iterator var4 = values.iterator();

            while (var4.hasNext()) {
                String value = (String) var4.next();
                encryptSet.add(SecuritySharedPreference.this.encryptPreference(value));
            }

            this.mEditor.putStringSet(SecuritySharedPreference.this.encryptPreference(key), encryptSet);
            return this;
        }

        public Editor putInt(String key, int value) {
            this.mEditor.putString(SecuritySharedPreference.this.encryptPreference(key), SecuritySharedPreference.this.encryptPreference(Integer.toString(value)));
            return this;
        }

        public Editor putLong(String key, long value) {
            this.mEditor.putString(SecuritySharedPreference.this.encryptPreference(key), SecuritySharedPreference.this.encryptPreference(Long.toString(value)));
            return this;
        }

        public Editor putFloat(String key, float value) {
            this.mEditor.putString(SecuritySharedPreference.this.encryptPreference(key), SecuritySharedPreference.this.encryptPreference(Float.toString(value)));
            return this;
        }

        public Editor putBoolean(String key, boolean value) {
            this.mEditor.putString(SecuritySharedPreference.this.encryptPreference(key), SecuritySharedPreference.this.encryptPreference(Boolean.toString(value)));
            return this;
        }

        public Editor remove(String key) {
            this.mEditor.remove(SecuritySharedPreference.this.encryptPreference(key));
            return this;
        }

        public Editor clear() {
            this.mEditor.clear();
            return this;
        }

        public boolean commit() {
            return this.mEditor.commit();
        }

        @TargetApi(9)
        public void apply() {
            if (Build.VERSION.SDK_INT >= 9) {
                this.mEditor.apply();
            } else {
                this.commit();
            }

        }
    }
}

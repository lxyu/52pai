package me.zhaoren;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

public class Rms {
    private _52pai parent;
    private Hashtable mPreferences = new Hashtable();
    // the md5 value for "me.zhaoren._52pai + salt";
    static final private String mRecordStoreName = "ce416c05cd1cf7a10478c67b2e96a7cd";

    public Rms(_52pai p){
        parent = p;
    }

    final public boolean loadPreference() {
        RecordStore rs = null;
        RecordEnumeration re = null;
        try {
            try {
                rs = RecordStore.openRecordStore(mRecordStoreName, true);
                re = rs.enumerateRecords(null, null, false);
                while (re.hasNextElement()) {
                    byte[] raw = re.nextRecord();
                    String pref = new String(raw);
                    // Parse out the name.
                    int index = pref.indexOf('|');
                    String name = pref.substring(0, index);
                    String value = pref.substring(index + 1);
                    mPreferences.put(name, value);
                }

                //get UID
                parent.uid = (String) (mPreferences.get("id"));
            } catch ( Exception e ) {
                //parent.printit("Exception: ", e.toString());
                parent.printit("读取记录失败", e.toString());
            } finally {
                if (re != null) re.destroy();
                if (rs != null) rs.closeRecordStore();
            }
        } catch( RecordStoreException e) {
            //parent.printit("RecordStoreException", e.toString());
            parent.printit("读取记录失败", e.toString());
            return false;
        }
        return true;
    }

    final public boolean putUserPassword(String target, String username, String password, boolean saveit) {
        // Save the user name and password.
        mPreferences.put(target + ":username", username);
        mPreferences.put(target + ":password", password);
        if (saveit)
            return savePreferences();
        else
            return true;
    }
    
    final public boolean putUID(String d, boolean saveit){
        mPreferences.put("id", d);
        if (saveit)
            return savePreferences();
        return true;
    }
    
    final public boolean deleteUserPassword(String target, boolean saveit) {
        if (mPreferences.containsKey(target + ":username"))
            mPreferences.remove(target + ":username");
        if (mPreferences.containsKey(target + ":password"))
            mPreferences.remove(target + ":password");
        if (saveit)
            return savePreferences();
        return true;
    }
    
    final public String getUsername(String target) {
        String s = (String) (mPreferences.get(target + ":username"));
        return (s != null) ? s: "";
    }

    final public String getPassword(String target) {
        String s = (String) (mPreferences.get(target + ":password"));
        return (s != null) ? s: "";
    }

    final public String getUID(){
        String s = (String) (mPreferences.get("id"));
        return (s != null) ? s: "";
    }

    final public boolean savePreferences() {
        RecordStore rs = null;
        RecordEnumeration re = null;
        try {
            try {
                rs = RecordStore.openRecordStore(mRecordStoreName, true);
                re = rs.enumerateRecords(null, null, false);

                // First remove all records, a little clumsy.
                while (re.hasNextElement()) {
                    int id = re.nextRecordId();
                    rs.deleteRecord(id);
                }

                // Now save the preferences records.
                Enumeration keys = mPreferences.keys();
                while (keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    String value = (String)mPreferences.get(key);
                    String pref = key + "|" + value;
                    byte[] raw = pref.getBytes();
                    rs.addRecord(raw, 0, raw.length);
                }
            } catch ( Exception e ) {
                //parent.printit("Exception: ", e.toString());
                parent.printit("写入数据失败", e.toString());
                return false;
            } finally {
                if (re != null) re.destroy();
                if (rs != null) rs.closeRecordStore();
            }
        } catch( RecordStoreException e) {
            //parent.printit("RecordStoreException", e.toString());
            parent.printit("写入数据失败", e.toString());
            return false;
        }
        return true;
    }
}

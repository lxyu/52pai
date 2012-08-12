package www.pai.mobi;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class MultiSites extends ListActivity {
    public static final String mStore = "ce416c05cd1cf7a10478c67b2e96a7cd";

    // options menu
    private static final int MENU_UPLOAD       = Menu.FIRST;

    private ArrayList<String> addedSites = null;
    private ArrayList<String> addedSites_str = null;
    private ArrayList<String> addedSites_titles = null;
    private ArrayList<String> checked = null;

    private Toast mToast = null;
    private IconAdapter list = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiSites.this.setTitle("我爱拍 www.52pai.mobi");
        MultiSites.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mToast = Toast.makeText(MultiSites.this, "请按下轨迹球上传图片", Toast.LENGTH_LONG);
        mToast.show();

        // get extras passed with intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            addedSites = extras.getStringArrayList("addedSites");
            addedSites_str = extras.getStringArrayList("addedSites_str");
            addedSites_titles = extras.getStringArrayList("addedSites_titles");
        }
        int size = addedSites.size();
        addedSites.remove(size - 1);
        addedSites.remove(size - 2);
        addedSites.remove(size - 3);
        addedSites_str.remove(size - 1);
        addedSites_str.remove(size - 2);
        addedSites_str.remove(size - 3);

        list = new IconAdapter(this, addedSites, addedSites_str, true);
        setListAdapter(list);
    }

    // whenever this activity is destroyed, return a RESULT_OK
    // in order to refresh the defaultUI.
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );
        menu.add(0, MENU_UPLOAD, 0, "上传图片至多站点");
        return true;
    }

    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_UPLOAD:
            uploadToMultiSites();
            break;
        default:
            break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected void onListItemClick( ListView l, View v, int position, long id ) {
        super.onListItemClick( l, v, position, id );
        if (position != addedSites.size() - 1) {
            // TODO make the box checked
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_CANCELED);
            MultiSites.this.finish();
            return true;
        } else if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER) {
            uploadToMultiSites();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    private void uploadToMultiSites() {
        // write targetSite to SharedPreferences
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        Editor prefEditor = sharedPref.edit();
        String targetsitename = "";
        String targetsite_str = "";
        int targettitle_int = 0;
        checked = list.getChecked();
        if (checked.size() > 0) {
            ArrayList<String> bbs = new ArrayList<String>();
            for (int i = 0; i < list.getCheckedSize(); i++) {
                int p = Integer.parseInt(checked.get(i));
                targetsitename += addedSites.get(p) + "\t\t";
                targetsite_str += addedSites_str.get(p) + "\t\t";
                if ("yssy".equals(addedSites_str.get(p)) 
                        || "rygh".equals(addedSites_str.get(p)) 
                        || "mop".equals(addedSites_str.get(p)) 
                        || "tianya".equals(addedSites_str.get(p))
                        || "baidu".equals(addedSites_str.get(p))
                        || "discuz".equals(addedSites_str.get(p))) {
                    bbs.add(addedSites.get(p).substring(0, addedSites.get(p).indexOf("\n")));
                }
                // get the max allowed title permission
                int targettitle_tmp = Integer.parseInt(addedSites_titles.get(p));
                if (targettitle_tmp > targettitle_int) {
                    targettitle_int = targettitle_tmp;
                }
            }
            String targettitle = "" + targettitle_int;
            if (bbs.size() >= 2) {
                String message = "";
                for (int i = 0; i < bbs.size(); i++) {
                    message += " " + bbs.get(i);
                }
                new AlertDialog.Builder(MultiSites.this)
                .setTitle("提醒")
                .setIcon(R.drawable.boto)
                .setMessage("您选择了" + bbs.size() + "个bbs:\n" + message +"\n一次只能上传到一个bbs，请重新选择")
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
            } else {
                prefEditor.putString("targetsitename", targetsitename);
                prefEditor.putString("targetsite", targetsite_str);
                prefEditor.putString("targettitle", targettitle);
                prefEditor.commit();
                
                Intent i = new Intent(this, CameraUI.class);
                startActivity(i);
                MultiSites.this.finish();
            }
        } else {
            new AlertDialog.Builder(MultiSites.this)
            .setTitle("提示")
            .setIcon(R.drawable.boto)
            .setMessage("您尚未选择任何站点")
            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        }
        return;
    }
}
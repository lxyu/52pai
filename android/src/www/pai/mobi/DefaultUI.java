package www.pai.mobi;

import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.telephony.gsm.SmsManager;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class DefaultUI extends ListActivity {
    public static final String mStore = "ce416c05cd1cf7a10478c67b2e96a7cd";

    // options menu
    private static final int MENU_ABOUT        = Menu.FIRST + 1;
    private static final int MENU_PAYMENT      = Menu.FIRST + 2;
    private static final int MENU_BUY          = Menu.FIRST + 3;
    // context menu
    private static final int DELETE_SITE       = Menu.FIRST + 10;
    private static final int MODIFY_SITE       = Menu.FIRST + 11;
    // verify states
    private static final int VERIFY_OK         = 0;
    private static final int VERIFY_FAIL       = 1;
    //choose the method to send a pic
    //private static final int CHOOSE_CAMERA     = 0;
    //private static final int CHOOSE_FILE       = 1;
    // result
    private static final int RESULT_ADDSITE      = 0;
    private static final int RESULT_FILE         = 1;
    private static final int RESULT_CAMERA       = 2;
    private static final int SELECT_PICTURE      = 3;
    private static final int RESULT_IMGPREVIEW   = 4;
    private static final int RESULT_SAVE_TO_DISK = 5;

    private ArrayList<String> availableSites        = null;
    private ArrayList<String> availableSites_str    = null;
    private ArrayList<String> availableSites_logins = null;
    private ArrayList<String> availableSites_titles = null;
    private ArrayList<String> addedSites            = null;
    private ArrayList<String> addedSites_str        = null;
    private ArrayList<String> addedSites_logins     = null;
    private ArrayList<String> addedSites_titles     = null;

    // use a boolean to mark which list is showing. true for the addedSites and false for the availableSites.
    private boolean     sList    = true;
    private IconAdapter siteList = null; 

    private ProgressDialog loadingDialog = null;

    private String   username    = null;
    private String   password    = null;
    
    private EditText usernameEdit   = null;
    private EditText passwordEdit   = null;
    
    private int selected = -1;
    
    private boolean multisites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //requestWindowFeature(Window.FEATURE_LEFT_ICON);
        //setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.logo);
        DefaultUI.this.setTitle( "我爱拍 www.52pai.mobi" );
        DefaultUI.this.setRequestedOrientation( ActivityInfo.SCREEN_ORIENTATION_PORTRAIT );

        setContentView(R.layout.mainlist);

        // TODO wifimanager
        // scan wifi in the background, if wifi is available, ask user whether to use it.

        // initialize data
        availableSites        = new ArrayList<String>();
        availableSites_str    = new ArrayList<String>();
        availableSites_logins = new ArrayList<String>();
        availableSites_titles = new ArrayList<String>();
        addedSites            = new ArrayList<String>();
        addedSites_str        = new ArrayList<String>();
        addedSites_logins     = new ArrayList<String>();
        addedSites_titles     = new ArrayList<String>();

        // get extras passed with intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            String response = extras.getString("response");
            parseIt( response );
        }

        // load all added sites and set list view
        loadAddedSites();


        // for context menu
        DefaultUI.this.registerForContextMenu(getListView());
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );
        menu.add(0, MENU_ABOUT, 1, "关于");//.setIcon(android.R.drawable.ic_dialog_info);
        menu.add(0, MENU_PAYMENT, 2, "资费");//.setIcon(android.R.drawable.ic_menu_manage);
        menu.add(0, MENU_BUY, 2, "购买");
        return true;
    }

    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        switch( item.getItemId() ) {
        case MENU_ABOUT:
            String sAbout = sharedPref.getString("about", "我爱拍 \nwww.52pai.mobi");
            new AlertDialog.Builder(DefaultUI.this)
            .setTitle("关于")
            .setIcon(R.drawable.boto)
            .setMessage(sAbout)
            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
            break;
        case MENU_PAYMENT:
            String sPaymentInfo = sharedPref.getString("paymentinfo", "我爱拍 每月2元");
            new AlertDialog.Builder(DefaultUI.this)
            .setTitle("资费")
            .setIcon(R.drawable.boto)
            .setMessage(sPaymentInfo)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // TODO send SMS
                }
            })
            .show();
            //return true;
            break;
        case MENU_BUY:
            new AlertDialog.Builder(DefaultUI.this)
            .setTitle("付费")
            .setIcon(R.drawable.boto)
            .setMessage("点击按钮付费, 2元/次")
            .setPositiveButton("付费", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    sendSMS("+8613601676914", "Hello");
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            })
            .show();
            break;
        default:
            break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderIcon( R.drawable.boto );
        menu.setHeaderTitle( "52pai" );
        menu.add(0, DELETE_SITE, 0, "删除相册配置");
        menu.add(0, MODIFY_SITE, 0, "修改相册配置");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = info.position;
        if ( ( !multisites && position != addedSites.size() - 1 ) || ( multisites && position < addedSites.size() - 2 ) ) { 
            switch(item.getItemId()) {
            case DELETE_SITE:
                deleteSite( position );
                // reload list
                loadAddedSites();
                siteList.notifyDataSetChanged();
                break;
            case MODIFY_SITE:
                promptDialog( position, addedSites, addedSites_str, addedSites_logins );
                break;
            default:
                return super.onContextItemSelected(item);
            }
        } else {
            showDialog("错误", "选择错误，请重新选择。");
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        if ( sList == true ) {
            if ( ( multisites && position == addedSites.size() - 3 ) || ( !multisites && position == addedSites.size() - 2 )) {
                switchListView();
            } else if ( multisites && position == addedSites.size() - 2) {
                // start multisites activity
                Intent multisite = new Intent(DefaultUI.this, MultiSites.class);
                multisite.putExtra("addedSites", addedSites);
                multisite.putExtra("addedSites_str", addedSites_str);
                multisite.putExtra("addedSites_titles", addedSites_titles);
                startActivity(multisite);
            } else if (position == addedSites.size() - 1) {
                // write picture to disk
                Intent i = new Intent(this, CameraUI.class);
                i.putExtra("disk", true);
                startActivityForResult(i, RESULT_SAVE_TO_DISK);
            } else {
                // write targetSite to SharedPreferences
                SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
                Editor prefEditor = sharedPref.edit();
                prefEditor.putString("targetsitename", addedSites.get(position));
                prefEditor.putString("targetsite", addedSites_str.get(position));
                prefEditor.putString("targettitle", addedSites_titles.get(position));
                prefEditor.commit();

                Intent i = new Intent(this, CameraUI.class);
                startActivityForResult(i, RESULT_CAMERA);
            }
        } else if ( sList == false ) {
            promptDialog( position, availableSites, availableSites_str, availableSites_logins );
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == RESULT_ADDSITE ) { 
            // refresh list
            loadAddedSites();
            siteList.notifyDataSetChanged();
        } else if ( requestCode == RESULT_CAMERA ) {
            // do nothing
        } else if ( requestCode == RESULT_FILE ) {
            // do nothing
        } else if ( requestCode == SELECT_PICTURE ) {
            if ( resultCode == RESULT_OK ) {
                // Successful activity call, picture selected from gallery
                Uri selected = data.getData();
                String imgFilePath = getPath( selected );
                Intent upload = new Intent( DefaultUI.this, ImagePreview.class );
                upload.putExtra("imgFilePath", imgFilePath);
                startActivityForResult(upload, RESULT_IMGPREVIEW);
            }
        } else if ( requestCode == RESULT_IMGPREVIEW ) {
            imgBrowser();
        }
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event){
        if ( keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 ) {
            if ( sList == true ) {
                DefaultUI.this.finish();
                return true;
            } else {
                switchListView();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void switchListView() {
        if ( sList == true ) {
            DefaultUI.this.setListAdapter(new IconAdapter(this, availableSites, availableSites_str, false));
            DefaultUI.this.unregisterForContextMenu(getListView());
            sList = false;
        } else {
            DefaultUI.this.setListAdapter(new IconAdapter(this, addedSites, addedSites_str, true));
            sList = true;
            // then refresh list
            loadAddedSites();
            siteList.notifyDataSetChanged();
            DefaultUI.this.registerForContextMenu(getListView());
        }
    }

    private void loadAddedSites () {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        // clear all info
        addedSites       .clear();
        addedSites_str   .clear();
        addedSites_logins.clear();
        addedSites_titles.clear();

        // if one site has username in sharedPreferences, add it to addedSites
        for ( int i = 0; i < availableSites.size(); i++) {
            if ( sharedPref.contains( availableSites_str.get(i) + ":username") ) {
                addedSites       .add(availableSites       .get(i));
                addedSites_str   .add(availableSites_str   .get(i));
                addedSites_logins.add(availableSites_logins.get(i));
                addedSites_titles.add(availableSites_titles.get(i));
            }
        }

        // add the addSite button
        addedSites.add("添加站点");
        addedSites_str.add("addsite");
        // add the multiSite button
        if ( addedSites.size() > 2 ) {
            addedSites.add("上传至多站点");
            addedSites_str.add("multisite");
            multisites = true;
        } else {
            multisites = false;
        }
        addedSites.add("拍照并保存至本地");
        addedSites_str.add("savetodisk");

        // Show siteList
        siteList = new IconAdapter(this, addedSites, addedSites_str, false);
        DefaultUI.this.setListAdapter(siteList);
        sList = true;

        // for ContextMenu
        DefaultUI.this.registerForContextMenu(DefaultUI.this.getListView());
    }

    // show a dialog prompt for userinfo
    public void promptDialog ( int position, ArrayList<String> Sites, ArrayList<String> Sites_str, ArrayList<String> Sites_logins ) {
        selected = position;
        LayoutInflater factory = LayoutInflater.from( DefaultUI.this );
        final View textEntryView = factory.inflate( R.layout.userinfo, (ViewGroup) findViewById(R.id.textentry) );
        final String site = Sites_str.get( position );

        usernameEdit = (EditText) textEntryView.findViewById( R.id.username_edit );
        passwordEdit = (EditText) textEntryView.findViewById( R.id.password_edit );
        usernameEdit.setText( getUserinfo( site + ":username" ) );
        passwordEdit.setText( getUserinfo( site + ":password" ) );

        final TextView loginTextView = (TextView) textEntryView.findViewById( R.id.username_view );
        loginTextView.setText( Sites_logins.get(position) );

        // prompt for username and password
        new AlertDialog.Builder(DefaultUI.this)
        .setTitle(Sites.get(position))
        .setView(textEntryView)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // verify and save changes
                username = ((EditText) textEntryView.findViewById(R.id.username_edit)).getText().toString();
                password = ((EditText) textEntryView.findViewById(R.id.password_edit)).getText().toString();

                if ( "".equals(username) || "".equals(password) ) {

                    new AlertDialog.Builder(DefaultUI.this)
                    .setMessage("用户名或密码为空，请重新输入。")
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            promptDialog( selected, availableSites, availableSites_str, availableSites_logins );
                        }
                    }).show();

                } else {
                    loadingDialog = ProgressDialog.show(DefaultUI.this, "", "验证中，请稍后...", true, true);
                    new Thread( new Runnable() {
                        public void run () {
                            try {
                                if ( verifyUserPassword(site, username, password) ) {
                                    updateUserInfo (site, username, password);
                                }
                            } catch (Exception e) {
                                showDialog( "Exception", e.toString() );
                            }
                        }
                    }).start();
                }
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        })
        .show();
    }

    //update username and password
    private boolean updateUserInfo (String site, String username, String password) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        Editor prefEditor = sharedPref.edit();
        prefEditor.putString(site + ":username", username);
        prefEditor.putString(site + ":password", password);
        prefEditor.commit();
        return true;
    }

    private String getUserinfo ( String key ) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        String r = sharedPref.getString(key, "");
        return r;
    }

    private boolean verifyUserPassword(String s, String u, String p) {
        final String verifyURL = "http://www.52pai.mobi/verify/";
        //final String verifyURL = "http://test.52pai.mobi/verify/";

        HttpPost httpRequest = new HttpPost(verifyURL);
        String strResult = null;
        //see ref = http://www.cnblogs.com/webabcd/archive/2010/01/29/1658928.html
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(); 
        params.add( new BasicNameValuePair("target", s) );
        params.add( new BasicNameValuePair("username", u) );
        params.add( new BasicNameValuePair("password", p) );

        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            if ( httpResponse.getStatusLine().getStatusCode() == 200 ) {
                strResult = EntityUtils.toString( httpResponse.getEntity() );
            } else {
                //for debug
                strResult = "Error Response:" + httpResponse.getStatusLine().toString() + "\n" 
                + EntityUtils.toString( httpResponse.getEntity() );
            }
        } catch ( Exception e ) {
            showDialog( "Exception", e.toString() );
        }

        if ( "OK".equals(strResult) ) {
            // send a successful message to handler
            Message m = new Message();
            m.what = VERIFY_OK;
            DefaultUI.this.handler.sendMessage(m);
            return true;
        }

        Message m = new Message();
        m.what = VERIFY_FAIL;
        DefaultUI.this.handler.sendMessage(m);
        // return false for default
        return false;
    }

    // handle the message send from sendImgFile()
    private final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch ( msg.what ) {
            case DefaultUI.VERIFY_OK:
                loadingDialog.dismiss();
                // show result
                new AlertDialog.Builder(DefaultUI.this)
                .setTitle("验证成功")
                .setMessage("验证通过。按确定返回主菜单。")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if ( sList == false) {
                            switchListView();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                }).show();
                break;
            case DefaultUI.VERIFY_FAIL:
                loadingDialog.dismiss();
                // show result
                new AlertDialog.Builder(DefaultUI.this)
                .setTitle("验证失败")
                .setMessage("请检查用户名和密码，并重新输入。")
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        promptDialog( selected, availableSites, availableSites_str, availableSites_logins );
                    }
                }).show();
                break;
            }
            super.handleMessage( msg );
        }
    };

    // parse the response from handshake
    private boolean parseIt (String str) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        Editor prefEditor = sharedPref.edit();

        boolean sUpdate;
        // TODO change the word used to specify siteList state. ( FROM 'NEW' to another word )
        // check whether we need to update the siteList
        if ( str.substring(0, 3).equals("NEW") ) {
            sUpdate = false;
            str = str.substring(3);
            str = str + sharedPref.getString("sitelist", "");
        } else {
            sUpdate = true;
        }

        //parse the response from handshake
        String[] parse = str.split("\\|");

        // TODO refine id logic
        // update id
        if ( !sharedPref.getString("uid", "").equals(parse[0])) {
            prefEditor.putString("uid", parse[0]);
        }
        // update version
        if ( !sharedPref.getString("version", "").equals(parse[1])) {
            prefEditor.putString("version", parse[1]);
        }
        // update payment
        if ( !sharedPref.getString("payment", "").equals(parse[2])) {
            prefEditor.putString("peyment", parse[2]);
        }
        // update traffic
        if ( !sharedPref.getString("version", "").equals(parse[3])) {
            prefEditor.putString("version", parse[3]);
        }
        // update uploaded picNum
        if ( !sharedPref.getString("picnum", "").equals(parse[4])) {
            prefEditor.putString("picnum", parse[4]);
        }
        // update paymentinfo
        if ( !sharedPref.getString("paymentinfo", "").equals(parse[5])) {
            prefEditor.putString("paymentinfo", parse[5]);
        }
        // update about
        if ( !sharedPref.getString("about", "").equals(parse[6])) {
            prefEditor.putString("about", parse[6]);
        }
        // update default boards
        if ( !sharedPref.getString("yssyboards", "").equals(parse[7])) {
            prefEditor.putString("yssyboards", parse[7]);
        }
        // update supported siteNum
        if ( !sharedPref.getString("sitenum", "").equals(parse[8])) {
            prefEditor.putString("sitenum", parse[8]);
        }
        // update sitelist
        if ( sUpdate ) {
            String sitelist = "";
            for ( int j = 9; j < parse.length; j++ ) {
                sitelist += parse[j] + "|";
            }
            prefEditor.putString("sitelist", sitelist);
        }
        prefEditor.commit();

        // update sites info
        for ( int j = 9; j < parse.length; j++ ) {
            String[] site = parse[j].split(",");
            availableSites       .add( site[0] );
            availableSites_str   .add( site[1] );
            availableSites_logins.add( site[2] );
            availableSites_titles.add( site[3] );
        }
        return true;
    }

    //update username and password
    /*
    private boolean updateUserInfo (String site, String username, String password) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        Editor prefEditor = sharedPref.edit();
        prefEditor.putString(site + ":username", username);
        prefEditor.putString(site + ":password", password);
        prefEditor.commit();
        return true;
    }*/

    // browse images using gallery
    private void imgBrowser() {
        Intent fileint = new Intent();
        fileint.setType("image/*");
        fileint.setAction(Intent.ACTION_GET_CONTENT);
        //Gallery.this.startActivityForResult(Intent.createChooser(fileint, Gallery.this.getString(R.string.chooseimage)), SELECT_PICTURE);
        startActivityForResult( fileint, SELECT_PICTURE );
    }

    // And to convert the image URI to the direct file system path of the image file  
    private String getPath(Uri contentUri) {
        // can post image  
        String [] proj={MediaStore.Images.Media.DATA};  
        Cursor cursor = managedQuery( contentUri,
                proj,         // Which columns to return  
                null,       // WHERE clause; which rows to return (all rows)  
                null,       // WHERE clause selection arguments (none)  
                null);        // Order-by clause (ascending by name)  
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);  
        cursor.moveToFirst();  
        return cursor.getString(column_index);
    }  

    private void deleteSite ( int position ) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        Editor prefEditor = sharedPref.edit();
        prefEditor.remove( addedSites_str.get(position) + ":username" );
        prefEditor.remove( addedSites_str.get(position) + ":password" );
        prefEditor.commit();
        return;
    }

    private void sendSMS(String phoneNumber, String message)
    {
        final SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(phoneNumber, null, message, null, null);
        return;
    }

    private void showDialog(String title, String msg)
    {
        new AlertDialog.Builder(DefaultUI.this)
        .setIcon( R.drawable.boto )
        .setTitle( title )
        .setMessage( msg )
        .setNegativeButton( "确定", new DialogInterface.OnClickListener()
        {
            public void onClick( DialogInterface dialog, int which )
            {
            }
        })
        .show();
    }
}

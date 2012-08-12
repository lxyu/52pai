package www.pai.mobi;

import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Window;

public class LoadingUI extends Activity {
    public static final String mStore = "ce416c05cd1cf7a10478c67b2e96a7cd";
    
    private static final String HOST = "_52pai";
    private static final String MODEL = "android";

    private final String serverBaseURL = "http://www.52pai.mobi/";
    //private final String serverBaseURL = "http://192.168.1.100/pu/";
    private final String handshakeURL  = serverBaseURL + "login/";

    private ProgressDialog loadingDialog = null;
    
    private boolean canceled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set to full screen
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        //this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LoadingUI.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // check if the network is available
        final ConnectivityManager conn_manager = (ConnectivityManager) LoadingUI.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo network_info = conn_manager.getActiveNetworkInfo();
        if (network_info != null && network_info.isConnectedOrConnecting()) {
            // show a loading dialog
            loadingDialog = new ProgressDialog(LoadingUI.this);
            loadingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            loadingDialog.setIcon(R.drawable.boto);
            loadingDialog.setCancelable(true);
            loadingDialog.setTitle("我爱拍 www.52pai.mobi");
            loadingDialog.setMessage("程序载入中，请稍后...");
            loadingDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface d) {
                    canceled = true;
                    LoadingUI.this.finish();
                }
            });
            loadingDialog.show();

            try {
                new Thread(new Runnable() {
                    public void run () {
                        handshake();
                    }
                }).start();
            } catch (Exception e) {
                showDialog("出错了", "网络错误，请检查您的网络连接。");
            } catch (Error er) {
                showDialog("出错了", "网络错误，请检查您的网络连接。");
            }
        } else {
            // show an alertDialog when network is unconnected
            new AlertDialog.Builder(LoadingUI.this)
            .setIcon(R.drawable.boto)
            .setTitle("错误")
            .setMessage("网络连接不可用，请检查网络连接并重试")
            .setNegativeButton("关闭", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    LoadingUI.this.finish();
                }
            })
            .show();
        }
    }

    private void handshake () {
        SharedPreferences sharedPref = this.getSharedPreferences(mStore, MODE_WORLD_WRITEABLE);
        String uid = sharedPref.getString("uid", "");
        // TODO get version from SharedPreferences
        String version = "0";

        HttpPost httpRequest = new HttpPost(handshakeURL);
        String strResult = null;
        //see ref = http://www.cnblogs.com/webabcd/archive/2010/01/29/1658928.html
        ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>(); 
        params.add(new BasicNameValuePair("uid", uid));
        params.add(new BasicNameValuePair("versionnum", version));
        params.add(new BasicNameValuePair("model", MODEL));
        params.add(new BasicNameValuePair("host", HOST));

        try {
            httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
            HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                strResult = EntityUtils.toString(httpResponse.getEntity());
            } else {
                //for debug
                //strResult = "Error Response:" + httpResponse.getStatusLine().toString() + "\n" 
                //+ EntityUtils.toString(httpResponse.getEntity());
                strResult = "fail";
                showDialog("出错了", strResult);
                return;
            }
        } catch (Exception e) {
            strResult = "fail";
            showDialog("出错了", "网络错误，请检查您的网络连接。");
            return;
        } catch (Error er) {
            strResult = "fail";
            showDialog("出错了", "网络错误，请检查您的网络连接。");
            return;
        }

        if (!"fail".equals(strResult) && !canceled) {
            // if handshake success and not canceled, go on.
            loadingDialog.dismiss();
            Intent mainIntent = new Intent(LoadingUI.this, DefaultUI.class); 
            mainIntent.putExtra("response", strResult);
            LoadingUI.this.startActivity(mainIntent); 
            LoadingUI.this.finish(); 
        } else if (!canceled) {// if not canceled, pop out an alert.
            showDialog("错误", "网络连接错误，请稍候再试。");
        }
        // user canceled
        return;
    }

    private void showDialog(String title, String msg)
    {
        new AlertDialog.Builder(LoadingUI.this)
        .setIcon(R.drawable.boto)
        .setTitle(title)
        .setMessage(msg)
        .setNegativeButton("确定",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
            }
        })
        .show();
    }
}
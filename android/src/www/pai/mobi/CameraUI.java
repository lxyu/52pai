package www.pai.mobi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

/* 使Activity实现SurfaceHolder.Callback */
public class CameraUI extends Activity implements SurfaceHolder.Callback
{
    // result
    private static final int RESULT_IMGPREVIEW_FROM_CAMERA = 0;
    private static final int SELECT_PICTURE                = 1;
    private static final int RESULT_IMGPREVIEW             = 2;
    
    private static final int MENU_FILESYS        = Menu.FIRST;

    private static String captureFilePath = "/sdcard/52pai.mobi.cameraSnapTmp.jpg";

    private Toast         mToast;
    private Camera        mCamera;
    private SurfaceView   mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Button        fsButton;


    private boolean disk = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // set full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera);

        CameraUI.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        mSurfaceView   = (SurfaceView) findViewById(R.id.mSurfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(CameraUI.this);
        mSurfaceHolder.setType (SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        fsButton = (Button) findViewById(R.id.camera_filesystem);
        fsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg) {
                imgBrowser();
            }
        });
        // get extras passed with intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            disk = extras.getBoolean("disk");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ( requestCode == RESULT_IMGPREVIEW_FROM_CAMERA )  {
            if ( resultCode == RESULT_CANCELED ) {
                // do nothing
            }
        } else if ( requestCode == SELECT_PICTURE ) {
            if ( resultCode == RESULT_OK ) {
                // Successful activity call, picture selected from gallery
                String imgFilePath = getPath(data.getData());
                Intent upload = new Intent( CameraUI.this, ImagePreview.class );
                upload.putExtra("imgFilePath", imgFilePath);
                startActivityForResult(upload, RESULT_IMGPREVIEW);
                CameraUI.this.finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event){
        if ( keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            takePicture();
            return true;
        } else if ( keyCode == KeyEvent.KEYCODE_BACK ) {
            setResult(RESULT_CANCELED);
            CameraUI.this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void surfaceChanged (SurfaceHolder surfaceholder, int format, int w, int h)
    {
        initCamera();
        // show a toast to tell the user how to capture a picture
        mToast = Toast.makeText(CameraUI.this, "请按下轨迹球拍照", Toast.LENGTH_LONG);
        mToast.show();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceholder)
    {
        resetCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceholder)
    {
        try
        {
            mToast.cancel();
            resetCamera();
        }
        catch(Exception e)
        {
            showDialog( "出错了", e.toString() );
        }
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu( menu );
        menu.add(0, MENU_FILESYS, 1, "上传本地图片");//.setIcon(android.R.drawable.ic_dialog_info);
        return true;
    }

    @Override
    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MENU_FILESYS:
            imgBrowser();
            break;
        default:
            break;
        }
        return super.onMenuItemSelected(featureId, item);
    }
    
    private void initCamera()
    {
        try {
            mCamera = Camera.open();

            if (mCamera != null)
            {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setPictureFormat(PixelFormat.JPEG);
                //parameters.setPreviewSize(320, 240);
                if ( !disk ) {
                    parameters.setPictureSize(640, 480);
                } else {
                    parameters.setPictureSize(1280, 960);
                }
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
            }
        } catch ( Exception e) {
            showDialog( "出错了", e.toString() );
        }
    }

    private void takePicture()
    {
        if (mCamera != null) 
        {
            mCamera.takePicture (shutterCallback, rawCallback, jpegCallback);
        }
    }

    private void resetCamera()
    {
        if (mCamera != null)
        {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private ShutterCallback shutterCallback = new ShutterCallback()
    { 
        public void onShutter() 
        {
        }
    }; 

    private PictureCallback rawCallback = new PictureCallback() 
    { 
        public void onPictureTaken(byte[] _data, Camera _camera) 
        { 
        } 
    }; 

    private PictureCallback jpegCallback = new PictureCallback() 
    {
        public void onPictureTaken(byte[] _data, Camera _camera)
        {
            Bitmap bm = BitmapFactory.decodeByteArray (_data, 0, _data.length);

            // TODO try automatically decide whether to rotate the picture
            // rotate the picture by 90 rate
            int w = bm.getWidth();
            int h = bm.getHeight();
            // Setting post rotate to 90
            Matrix mtx = new Matrix();
            mtx.postRotate(90);
            // Rotating Bitmap
            bm = Bitmap.createBitmap(bm, 0, 0, w, h, mtx, true);


            try
            {
                // write data to file
                if ( !disk ) {
                    // write tmp file
                    File myCaptureFile = new File(captureFilePath);
                    BufferedOutputStream bos = new BufferedOutputStream (new FileOutputStream(myCaptureFile));
                    bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    bos.flush();
                    bos.close();
                    // recycle bitmap
                    bm.recycle();
                    resetCamera();
                    showPicture();
                } else {
                    // write to disk
                    String name = getPictureName();
                    File myCaptureFile = new File(name);
                    BufferedOutputStream bos = new BufferedOutputStream (new FileOutputStream(myCaptureFile));
                    bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                    bos.flush();
                    bos.close();
                    // recycle bitmap
                    bm.recycle();
                    resetCamera();

                    new AlertDialog.Builder(CameraUI.this)
                    .setIcon( R.drawable.boto )
                    .setTitle( "照片已保存至文件系统" )
                    .setMessage( "你的照片已保存至 " + name )
                    .setNegativeButton("确定",new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int which)
                        {
                            CameraUI.this.setResult(RESULT_OK);
                            CameraUI.this.finish();
                        }
                    })
                    .show();
                }
            }
            catch (Exception e)
            {
                showDialog( "出错了", e.toString() );
            }
        }
    };

    // go to ImagePreview
    private void showPicture() {
        Intent imgPre = new Intent (CameraUI.this, ImagePreview.class);
        imgPre.putExtra("imgFilePath", captureFilePath);
        CameraUI.this.startActivityForResult(imgPre, RESULT_IMGPREVIEW_FROM_CAMERA);
        CameraUI.this.finish();
    }

    private void showDialog(String title, String msg)
    {
        new AlertDialog.Builder(CameraUI.this)
        .setIcon( R.drawable.boto )
        .setTitle( title )
        .setMessage( msg )
        .setNegativeButton("确定",new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
            }
        }).show();
    }

    private String getPictureName() {        
        final Calendar c = Calendar.getInstance();
        final String name_str = "/sdcard/www.52pai.mobi/" 
            + c.get(Calendar.YEAR) 
            + "-" + c.get(Calendar.MONTH) 
            + "-" + c.get(Calendar.DAY_OF_MONTH) 
            + "-" + c.get(Calendar.HOUR)
            + "-" + c.get(Calendar.MINUTE)
            + "-" + c.get(Calendar.SECOND) + ".jpg";

        try
        {
            File sddir = new File("/sdcard/www.52pai.mobi");
            if (!sddir.exists()) {
                sddir.mkdirs();
            }
            sddir = null;
        }
        catch (Exception e)
        {
            showDialog("出错了", e.toString());
        }

        return name_str;
    }


    // browse images using gallery
    private void imgBrowser() {
        Intent fileint = new Intent();
        fileint.setType("image/*");
        fileint.setAction(Intent.ACTION_GET_CONTENT);
        //Gallery.this.startActivityForResult(Intent.createChooser(fileint, Gallery.this.getString(R.string.chooseimage)), SELECT_PICTURE);
        startActivityForResult(fileint, SELECT_PICTURE);
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
}

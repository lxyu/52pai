package www.pai.mobi;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImagePreview extends Activity {
    public static final String mStore = "ce416c05cd1cf7a10478c67b2e96a7cd";

    // handler flag
    private static final int UPLOAD_IMAGE_PACKAGE_DONE = 0;
    private static final int UPLOAD_IMAGE_DONE         = 1;
    private static final int UPLOAD_IMAGE_TIME_OUT     = 2;
    private static final int UPLOAD_IMAGE_CANCELED     = 3;
    private static final int UPLOAD_IMAGE_EXCEPTION    = 4;
    private static final int UPLOAD_IMAGE_ERROR        = 5;
    private static final int IMAGE_LOADING_DONE        = 6;

    private URL uploadURL = null;
    static final private String serverBaseURL = "http://www.52pai.mobi/receive/";
    //static final private String serverBaseURL = "http://192.168.1.100/pu/receive/";
    static final private String cameraTmpFile = "/sdcard/52pai.mobi.cameraSnapTmp.jpg";
    static final private String rotateTmpFile = "/sdcard/52pai.mobi.rotateTmpFile.jpg";

    private Toast     mToast;
    private ImageView thumbView;
    private TextView  picTitle;
    private Button    uploadButton;
    private Button    commentButton;
    private Button    rotateButton;
    private Button    boardButton;
    private EditText  mTitle;
    private EditText  mDescription;
    private TextView  mDescriptionView;

    private ProgressDialog uploadProgress;

    private String title       = "";
    private String description = "";

    // multiSites infomation
    private String [] sitename = null;
    private String [] site_str = null;
    private String [] album    = null;
    private String albums = "";
    private String tl = "";

    //private String imgName = null;
    private String uploadFile = null;
    private Bitmap bm         = null;

    private int chunckSize   = 10240 * 2;
    private int pictureSize  = 0;
    private int packageSent  = 0;
    private int total_blocks = 0;

    private boolean stopSender  = true;
    private boolean imageStatus = false;
    private int cancelFlag;

    private ArrayList<String> boards = null;
    private String selected_board = "";
    private boolean isBBS = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //ImagePreview.this.setTitle("我爱拍 www.52pai.mobi");
        ImagePreview.this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SharedPreferences sharedPref = this.getSharedPreferences(mStore, MODE_WORLD_WRITEABLE);
        // determine whether the commentButton is clickable
        tl = sharedPref.getString("targettitle", "0");
        // if in multisites mode, we suppose at least one site has description and leave the problem to server
        if ("0".equals(tl)) {
            // if the title and description are unavailable, display the text in a different color
            commentButton.setTextColor(R.drawable.darkgray);
            commentButton.setClickable(false);
        }

        // get sitename and site_str
        sitename = sharedPref.getString("targetsitename", "52pai").split("\t\t");
        site_str = sharedPref.getString("targetsite", "52pai").split("\t\t");
        String sitenames = "";
        for ( int i = 0; i < sitename.length; i++) {
            // remove Chinese part of siteNames
            sitename[i] = sitename[i].substring(sitename[i].indexOf("\n"));
            sitenames += sitename[i] + " ";
            if (!isBBS)
                isBBS = testBBS(site_str[i]);
        }
        ImagePreview.this.setTitle("照片将上传到: " + sitenames);

        // show a loading toast
        int duration = 100000;
        mToast = Toast.makeText(ImagePreview.this, "图片载入中，请稍候...", duration);
        mToast.show();

        // initialize views
        mTitle        = (EditText)  findViewById(R.id.title_edit);
        mDescription  = (EditText)  findViewById(R.id.description_edit);

        if (!isBBS) {
            setContentView(R.layout.imagepreview);
            thumbView     = (ImageView) findViewById(R.id.mImageView);
            picTitle      = (TextView)  findViewById(R.id.mTextView);
            uploadButton  = (Button) findViewById(R.id.uploadImg);
            rotateButton  = (Button) findViewById(R.id.mImageRotate);
            commentButton = (Button) findViewById(R.id.imageSummary);
        } else {
            setContentView(R.layout.imagepreview_bbs);
            thumbView     = (ImageView) findViewById(R.id.mImageView_bbs);
            picTitle      = (TextView)  findViewById(R.id.mTextView_bbs);
            uploadButton  = (Button) findViewById(R.id.uploadImg_bbs);
            rotateButton  = (Button) findViewById(R.id.mImageRotate_bbs);
            commentButton = (Button) findViewById(R.id.imageSummary_bbs);

            boardButton   = (Button) findViewById(R.id.mBoard_bbs);
            // initialize boards
            boards = new ArrayList<String>();
            // TODO return by server
            String defaultBoards = sharedPref.getString("yssyboards", "");//"sjtunews&&ppperson&&picture&&lovebridge&&water";
            loadBoardsPref(defaultBoards);
            boardButton.setText(boards.get(0) + "版");
            boardButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View arg) {
                    new AlertDialog.Builder(ImagePreview.this)
                    .setTitle("请选择要上传的版面")
                    .setItems(boards.toArray(new CharSequence[boards.size()]), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == boards.size() - 1) {
                                promptForBoard();
                            } else {
                                selected_board = boards.get(which);
                                boards.remove(selected_board);
                                boards.add(0, selected_board);
                                saveBoardPref();
                                boardButton.setText(selected_board + "版");
                            }
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                        }
                    }).show();

                }
            });



            /*loadBoardsPref();
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, boards);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mBoards.setAdapter(adapter);
            mBoards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (position == boards.size() - 1 || boards.size() == 1) {
                        promptForBoard();
                    } else {
                        selected_board = boards.get(position);
                        saveBoardPref(selected_board);
                        loadBoardsPref();
                    }
                }
                public void onNothingSelected(AdapterView parent) {
                    // Do nothing.
                }
            });*/
        }

        uploadButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg) {
                uploadDialog();
            }
        });

        rotateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg) {
                try {
                    rotateDialog();
                } catch (Exception e) {
                    showDialog("Exception", e.toString());
                } catch (Error oom) {
                    showDialog("Error", oom.toString());
                }
            }
        });

        commentButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg) {
                commentDialog();
            }
        });

        // get extras passed with intent
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            uploadFile = extras.getString("imgFilePath");
        }

        showImg();
    }

    // whenever the activity is destroyed, delete the tmp files.
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (bm != null) {
            bm.recycle();
            bm = null;
        }
        delFile(cameraTmpFile);
        delFile(rotateTmpFile);
    }
    
    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            uploadProgress = null;
            setResult(RESULT_CANCELED);
            ImagePreview.this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    
    private void uploadDialog() {
        // if targetSite is BBS, then must input a title
        if (isBBS && "".equals(title)) {
            new AlertDialog.Builder(ImagePreview.this)
            .setTitle("提醒")
            .setMessage("您选择的目标站点包含bbs，请输入标题。")
            .setIcon(R.drawable.boto)
            .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    commentDialog();
                }
            })
            .show();
            return;
        }
        
        // if the picSize > 200kb, recommend the user to resize the picture
        if (pictureSize <= 200) {
            uploadImg();
        } else {
            new AlertDialog.Builder(ImagePreview.this)
            .setTitle("提醒")
            .setIcon(R.drawable.boto)
            .setMessage("这张图片大小为 " + pictureSize + "KB，为节约流量，建议缩小图片上传，这不会明显影响照片质量和观感。")
            .setPositiveButton("缩小上传", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // resize the picture
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inSampleSize = pictureSize / 200;
                    bm = BitmapFactory.decodeFile(uploadFile, opts);
                    // though not rotated, we use the rotateTmpfilename for convenience.
                    uploadFile = rotateTmpFile;
                    File resizeImgFile = new File(uploadFile);
                    try
                    {
                        // write change to file
                        BufferedOutputStream bos = new BufferedOutputStream (new FileOutputStream(resizeImgFile));
                        bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
                        bos.flush();
                        bos.close();
                    } catch (Exception e) {
                        showDialog ("出错了", e.toString());
                    }

                    // update the pictureSize info
                    pictureSize = (int) resizeImgFile.length() / 1024;
                    picTitle.setText("图片大小：" + pictureSize + "KB");
                    resizeImgFile = null;
                    uploadImg();
                }
            }).setNegativeButton("上传原图", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    uploadImg();
                }
            }).show();
        }
    }
    
    private void commentDialog() {
        if (!"0".equals(tl)) {
            LayoutInflater factory = LayoutInflater.from(ImagePreview.this);
            final View summary = factory.inflate(R.layout.summary, (ViewGroup) findViewById(R.id.summary));

            if ("1".equals(tl)) {
                mTitle           = (EditText) summary.findViewById(R.id.title_edit);
                mDescription     = (EditText) summary.findViewById(R.id.description_edit);
                mDescriptionView = (TextView) summary.findViewById(R.id.description_view);
                mDescription.setVisibility(View.INVISIBLE);
                mDescriptionView.setVisibility(View.INVISIBLE);
            } else if ("2".equals(tl)) {
                mTitle       = (EditText) summary.findViewById(R.id.title_edit);
                mDescription = (EditText) summary.findViewById(R.id.description_edit);
            }

            // prompt for username and password
            new AlertDialog.Builder(ImagePreview.this)
            .setTitle("图片信息")
            .setIcon(R.drawable.boto)
            .setView(summary)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    title       = mTitle.getText().toString();
                    description = mDescription.getText().toString();
                    if (title.length() <= 128) {
                        if (!"".equals(title)) {
                            picTitle.setText(title + " (" + pictureSize + "KB)");
                        }
                    } else {
                        new AlertDialog.Builder(ImagePreview.this)
                        .setTitle("提醒")
                        .setIcon(R.drawable.boto)
                        .setMessage("文字长度超出许可，请重新输入。")
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .show();
                    }
                }
            })
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            })
            .show();
        } else {
            new AlertDialog.Builder(ImagePreview.this)
            .setTitle("提醒")
            .setMessage("此相册不支持自定义标题")
            .setIcon(R.drawable.boto)
            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // do nothing
                }
            })
            .show();
        }
    }

    private void rotateDialog() {
        // TODO consider the best size of picture. (200 or larger?)
        if (pictureSize <= 300) {
            // if the picture was captured by camera, we change the original file
            // else if the picture is from the sdcard, we create a tmp file
            if (uploadFile != cameraTmpFile) {
                uploadFile = rotateTmpFile;
            }
            rotateImg();
        } else {
            new AlertDialog.Builder(ImagePreview.this)
            .setTitle("提醒")
            .setIcon(R.drawable.boto)
            .setMessage("图片过大，无法旋转原图。选择确定将缩小图片后旋转，这不会明显影响照片质量和观感。")
            .setPositiveButton("缩小旋转", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // if pictureSize > 200 kb, it must be a file from disk. 
                    // so we create a tmp file instead.
                    uploadFile = rotateTmpFile;
                    rotateImg();
                }
            }).setNegativeButton("放弃旋转", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                }
            }).show();
        }
    }
    

    private void showImg() {
        new Thread(new Runnable() {
            public void run () {
                try {
                    // check if the file exists
                    File f = new File (uploadFile);
                    if (f.exists()) {
                        try {
                            pictureSize = (int) (f.length()/1024);
                            // picTitle.setText("图片大小为：" + pictureSize + "Kb");
                            // set the BitmapFactory options to avoid out of memory error
                            // see ref http://developer.android.com/reference/android/graphics/BitmapFactory.Options.html
                            BitmapFactory.Options opts = new BitmapFactory.Options();
                            opts.inSampleSize = pictureSize / 300;
                            bm = BitmapFactory.decodeFile(uploadFile, opts);

                            // send a message to refresh layout
                            Message m = new Message();
                            m.what = IMAGE_LOADING_DONE;
                            ImagePreview.this.handler.sendMessage(m);
                        } catch (Exception e) {
                            showDialog("Exception", e.toString());
                        } catch (Error oom) {
                            showDialog("Error", oom.toString());
                        }
                    } else {
                        // give out an alert and stop the activity
                        showDialog("出错了",  "文件不存在");
                        ImagePreview.this.setResult(RESULT_CANCELED);
                        ImagePreview.this.finish();
                    }
                } catch (Exception e) {
                    showDialog("Exception", e.toString());
                }
            }
        }).start();


    }

    private void uploadImg() {
        // display the upload progress
        uploadProgress = new ProgressDialog(ImagePreview.this);
        uploadProgress.setCancelable(true);
        uploadProgress.setMessage("图片上传中，请稍候。     0%");
        // set the progress to be horizontal
        // uploadProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        uploadProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        uploadProgress.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface d) {
                if (!imageStatus) {
                    stopSender = true;
                    Message m = new Message();
                    m.what = cancelFlag;
                    ImagePreview.this.handler.sendMessage(m);
                }
            }
        });
        uploadProgress.show();

        try {
            new Thread(new Runnable() {
                public void run () {
                    try {
                        uploadURL = new URL(serverBaseURL);
                    } catch (MalformedURLException e) {
                        // do nothing
                    }
                    SharedPreferences sharedPref = getSharedPreferences(mStore, MODE_WORLD_WRITEABLE);
                    // for multisites mode
                    //String targetSite = sharedPref.getString("targetsite", "");
                    //String username = sharedPref.getString(targetSite + ":username", "");
                    //String password = sharedPref.getString(targetSite + ":password", "");
                    String uid = sharedPref.getString("uid", "");
                    String targetSite = "";
                    String username = "";
                    String password = "";
                    for ( int i = 0; i < sitename.length; i++) {
                        targetSite += site_str[i] + "\t\t";
                        username += sharedPref.getString(site_str[i] + ":username", "") + "\t\t";
                        password += sharedPref.getString(site_str[i] + ":password", "") + "\t\t";
                    }
                    targetSite = targetSite.substring(0, targetSite.length() - 2);
                    username = username.substring(0, username.length() - 2);
                    password = password.substring(0, password.length() - 2);
                    try {
                        sendImgFile(uid, targetSite, username, password);
                    } catch (Exception e) {
                        showDialog("Exception", e.toString());
                    } catch (Error er) {
                        showDialog("Error", er.toString());
                    }
                }
            }).start();
        } catch (Exception e) {
            showDialog("Exception", e.toString());
        } catch (Error er) {
            showDialog("Error", er.toString());
        }
    }
    
    private void loadBoardsPref(String defaultBoards) {
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        String boards_str = sharedPref.getString("boards", "");
        String [] boards_arr = boards_str.split("&&");
        if (!"".equals(boards_str)) {
            for (int i = 0; i < boards_arr.length; i++) {
                if (i < boards_arr.length) {
                    boards.add(i, boards_arr[i]);
                }
            }
        }
        // add the boards returned by server
        String [] boards_default = defaultBoards.split("&&");
        for (int i = 0; i < boards_default.length; i++) {
            boards.add(boards_default[i]);
        }

        // remove duplicate elements
        HashSet<String> hs = new LinkedHashSet<String>();
        hs.addAll(boards);
        boards.clear();
        boards.addAll(hs);
        boards.add("输入版名");
        // select the first board by default
        selected_board = boards.get(0);
    }

    private void saveBoardPref() {
        String str = boards.get(0);
        for (int i = 1; i < 5; i++) {
            str +=  "&&" + boards.get(i);
        }
        // write to sharedPreference
        SharedPreferences sharedPref = getSharedPreferences( mStore, MODE_WORLD_WRITEABLE);
        Editor prefEditor = sharedPref.edit();
        prefEditor.putString("boards", str);
        prefEditor.commit();
    }

    private void promptForBoard() {
        LayoutInflater factory = LayoutInflater.from(ImagePreview.this);
        final View textEntryView = factory.inflate(R.layout.boards, (ViewGroup) findViewById(R.id.boardentry));
        new AlertDialog.Builder(ImagePreview.this)
        .setTitle("请输入bbs板块名称")
        .setView(textEntryView)
        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // verify and save changes
                String board_str = ((EditText) textEntryView.findViewById(R.id.board_edit)).getText().toString();
                if (!"".equals(board_str)) {
                    selected_board = board_str;
                    if (boards.contains(board_str)) {
                        boards.remove(board_str);
                    }
                    boards.add(0, board_str);
                    boardButton.setText(selected_board + "版");
                    saveBoardPref();
                } else {
                    new AlertDialog.Builder(ImagePreview.this)
                    .setMessage("用户名或密码为空，请重新输入。")
                    .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            promptForBoard();
                        }
                    }).show();
                }
            }
        })
        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        })
        .show();
    }

    private void rotateImg() {
        // rotate the picture by 90 rate
        int w = bm.getWidth();
        int h = bm.getHeight();

        // Setting post rotate to 90 and rotate bitmap
        Matrix mtx = new Matrix();
        mtx.postRotate(90);
        bm = Bitmap.createBitmap(bm, 0, 0, w, h, mtx, true);
        thumbView.setImageBitmap(bm);

        File myCaptureFile = new File(uploadFile);
        try
        {
            // write change to file
            BufferedOutputStream bos = new BufferedOutputStream (new FileOutputStream(myCaptureFile));
            bm.compress(Bitmap.CompressFormat.JPEG, 80, bos);
            bos.flush();
            bos.close();
        } catch (Exception e) {
            showDialog ("出错了", e.toString());
        }

        // update the pictureSize info
        pictureSize = (int) myCaptureFile.length() / 1024;
        if ("".equals(title)) {
            picTitle.setText("图片大小：" + pictureSize + "KB");
        } else {
            picTitle.setText(title + " (" + pictureSize + "KB)");
        }
        myCaptureFile = null;
    }

    // handle the message send from sendImgFile()
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
            // loading image
            case ImagePreview.IMAGE_LOADING_DONE:
                picTitle.setText("图片大小：" + pictureSize + "Kb");
                thumbView.setImageBitmap(bm);
                // when the picture was shown, cancel toast
                mToast.cancel();
                break;
                // uploadProgress
            case ImagePreview.UPLOAD_IMAGE_PACKAGE_DONE:
                if (uploadProgress != null) {
                    // for every package sent, increase the progress by 1
                    //uploadProgress.incrementProgressBy(1);
                    packageSent++;
                    uploadProgress.setMessage("图片上传中，请稍候。    " + (int) 100 * packageSent / total_blocks + "%");

                    // if the upload was canceled left one package, a bug happen, so:
                    if (packageSent >= total_blocks - 1) {
                        uploadProgress.setCancelable(false);
                    }
                }
                break;
            case ImagePreview.UPLOAD_IMAGE_DONE:
                if (uploadProgress != null) {
                    uploadProgress.dismiss();
                }

                // display an alert message to user.

                String message = "已上传至：";
                album = albums.split("\t\t");
                for ( int i = 0; i < sitename.length; i++) {
                    if (testBBS(sitename[i])) {
                        message += sitename[i] + " 的 " + album[i] + "版";
                    } else {
                        message += sitename[i] + " 的 " + album[i] + "相册";
                    }
                }
                new AlertDialog.Builder(ImagePreview.this)
                .setTitle("上传成功")
                .setMessage(message)
                .setIcon(R.drawable.boto)
                .setNegativeButton("返回", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();

                uploadProgress = null;
                break;
            case ImagePreview.UPLOAD_IMAGE_CANCELED:
                if (uploadProgress != null) {
                    uploadProgress.dismiss();
                    uploadProgress = null;
                }

                new AlertDialog.Builder(ImagePreview.this)
                .setTitle("上传取消")
                .setIcon(R.drawable.boto)
                .setMessage("上传已被取消。")
                .show();
                break;
            case ImagePreview.UPLOAD_IMAGE_TIME_OUT:
                if (uploadProgress != null) {
                    uploadProgress.dismiss();
                    uploadProgress = null;
                }

                new AlertDialog.Builder(ImagePreview.this)
                .setTitle("上传超时")
                .setIcon(R.drawable.boto)
                .setMessage("网络状态不稳定，上传超时，请更换地点重试。")
                .show();
                break;
                // TODO TODO TODO TODO later
            case ImagePreview.UPLOAD_IMAGE_EXCEPTION:
                if (uploadProgress != null) {
                    uploadProgress.dismiss();
                    uploadProgress = null;
                }
                showDialog("Exception", "");
                break;
            case ImagePreview.UPLOAD_IMAGE_ERROR:
                if (uploadProgress != null) {
                    uploadProgress.dismiss();
                    uploadProgress = null;
                }

                showDialog("Error", "");
                break;
            }
            super.handleMessage(msg);
        }
    };

    public void sendImgFile(String uid, String target, String username, String password) {
        packageSent = 0;
        stopSender = false;
        cancelFlag = UPLOAD_IMAGE_CANCELED;

        long overallSize = 0;
        int packagecount = 0;
        String filename = uploadFile;
        String filetype = "image";

        try {
            int sessionID = random();

            File f = new File (uploadFile);
            overallSize = f.length();

            // initialize bitmap string
            String retTargetStr = "";
            total_blocks = (int)((overallSize / chunckSize) + (overallSize % chunckSize > 0 ? 1 : 0));
            for (int ix = 0; ix < total_blocks; ++ix) {
                retTargetStr += "0";
            }

            f = null;

            int whileCount = 0;

            // limit the while loop to 3 times
            while (overallSize > 0 && whileCount < 3 && !stopSender) {
                FileInputStream fis = new FileInputStream(uploadFile);

                /*if (uploadProgress != null) {
                    // set the parameters of uploadProgressBar
                    uploadProgress.setMax(total_blocks);
                    uploadProgress.setProgress(0);
                }*/

                byte[] buffer = new byte[chunckSize];
                int bytesRead = 0;
                int bytesSent = 0;
                int seq = 0;

                while (bytesRead < overallSize && !stopSender) {
                    int chunck = chunckSize;
                    if ((overallSize - bytesRead) < chunckSize) {
                        chunck = (int)(overallSize - bytesRead);
                        buffer = new byte[chunck];
                    }

                    int count = fis.read(buffer, 0, chunck);

                    if (count > 0 && (retTargetStr.length() <= seq || retTargetStr.charAt(seq) == '0') && !stopSender) {


                        Hashtable<String, String> params = new Hashtable<String, String>();
                        params.put("target", target);
                        params.put("uid", uid);
                        params.put("username", username);
                        params.put("password", password);
                        params.put("sessionid", "" + sessionID);
                        params.put("offset", "" + bytesRead);
                        params.put("size", "" + overallSize);
                        params.put("bs", "" + chunckSize);
                        params.put("seq", "" + seq);
                        // add title and description
                        if (!"".equals(title) && title.length() <= 60) { 
                            params.put("title", title);
                        }
                        if (!"".equals(description)) {
                            params.put("description", description);
                        }
                        if (!"".equals(selected_board)) {
                            params.put("board", selected_board);
                        }

                        String rrr = "fail";
                        for (int trycount = 0; trycount < 2; trycount ++) {
                            // send data
                            if (!stopSender) {
                                try {
                                    rrr = sendMultipart(serverBaseURL, params, "upload_filed", filename, filetype, buffer);
                                } catch (Exception e) {
                                    showDialog("出错了", e.toString());
                                } catch (Error e) {
                                    showDialog("出错了", e.toString());
                                }
                            }
                            if (!"fail".equals(rrr))
                                break;
                        }
                        if ("fail".equals(rrr)) {
                            //sent error
                            /*Message m = new Message();
                            m.what = UPLOAD_IMAGE_PACKAGE_FAIL;
                            ImagePreview.this.handler.sendMessage(m);*/
                            packagecount++;
                            if (packagecount == 2) {
                                stopSender = true;
                                cancelFlag = UPLOAD_IMAGE_TIME_OUT;
                                break;
                            }
                        } else if (!"fail".equals(rrr)) { // success sent
                            // get album name
                            if (seq == total_blocks - 1) {
                                int pos = rrr.lastIndexOf('\n');
                                albums = rrr.substring(pos + 1);
                                rrr = rrr.substring(0, pos);
                            }

                            Message m = new Message();
                            m.what = UPLOAD_IMAGE_PACKAGE_DONE;
                            //Bundle b = new Bundle();
                            //b.putInt("percent", (int) 100 * seq / total_blocks);
                            //m.setData(b);
                            ImagePreview.this.handler.sendMessage(m);

                            packagecount = 0;
                            int pos = rrr.lastIndexOf('\n');
                            retTargetStr = rrr.substring(pos + 1);
                            bytesSent += count;
                        }
                    }
                    if (stopSender) {
                        break;
                    }
                    bytesRead += count;
                    seq ++;
                } // end of inner while
                fis.close();
                if (retTargetStr.indexOf("0") < 0 || stopSender) {
                    break;
                }
                showDialog("whileCount", "" + whileCount);
                whileCount++;
            } // end of overall>0 while
            if (!stopSender) {
                imageStatus = true;
                Message m = new Message();
                m.what = UPLOAD_IMAGE_DONE;
                ImagePreview.this.handler.sendMessage(m);
            }
        } catch (Exception e) {
            showDialog("出错了", e.toString());
        } catch (Error er) {
            showDialog("出错了", er.toString());
        }
    }


    final public String getBoundaryMessage(String boundary, Hashtable<String, String> params, String fileField, String fileName, String fileType)
    {
        StringBuffer res = new StringBuffer("--").append(boundary).append("\r\n");

        Enumeration<String> keys = params.keys();

        while(keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = (String)params.get(key);

            res.append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n")    
            .append("\r\n").append(value).append("\r\n")
            .append("--").append(boundary).append("\r\n");
        }
        res.append("Content-Disposition: form-data; name=\"").append(fileField).append("\"; filename=\"").append(fileName).append("\"\r\n") 
        .append("Content-Type: ").append(fileType).append("\r\n\r\n");
        return res.toString();
    }

    final public String sendMultipart(String url, Hashtable<String, String> params, String fileField, String fileName, String fileType, byte[] fileBytes) throws IOException {
        final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
        String boundaryMessage = getBoundaryMessage(BOUNDARY, params, fileField, fileName, fileType);
        String endBoundary = "\r\n--" + BOUNDARY + "--\r\n";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] postBytes = null;
        HttpURLConnection hc = null;
        InputStream is = null;
        String res = "";

        try {
            bos.write(boundaryMessage.getBytes());
            bos.write(fileBytes);
            bos.write(endBoundary.getBytes());
            postBytes = bos.toByteArray();
            bos.close();

            bos = new ByteArrayOutputStream();

            hc = (HttpURLConnection) uploadURL.openConnection();
            hc.setDoInput(true);
            hc.setDoOutput(true);
            hc.setUseCaches(false);
            hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            hc.setRequestMethod("POST");
            OutputStream dout = hc.getOutputStream();
            dout.write(postBytes);
            dout.close();
            is = hc.getInputStream();
            res = getServerResponse(hc, is);
        } catch (Exception e) {
            showDialog("出错了", e.toString());
        } catch (Error er) {
            showDialog("出错了", er.toString());
        } finally {
            try {
                if(bos != null)
                    bos.close();
                if(is != null)
                    is.close();
                if(hc != null)
                    hc.disconnect();
            } catch(Exception e2) {
                showDialog("出错了", e2.toString());
            } catch (Error er) {
                showDialog("出错了", er.toString());
            }
        }
        return res;
    }

    final public String getServerResponse(HttpURLConnection http, InputStream iStrm) throws IOException
    {
        //Reset error message
        String str = null;
        if (http.getResponseCode() == HttpURLConnection.HTTP_OK) {
            ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
            int ch;
            while ((ch = iStrm.read()) != -1)
                bStrm.write(ch);
            str = new String(bStrm.toByteArray(), "utf-8");
            bStrm.close();
            return str;
        } else {
            return "fail";
        }
    }

    final public String POST(String url, Hashtable<String, String> params) throws IOException{
        HttpURLConnection http = null;
        OutputStream oStrm = null;
        InputStream iStrm = null;    
        String ret = null;
        try {
            http = (HttpURLConnection) uploadURL.openConnection();
            //currentConnection = http; // expose http connection to other threads
            //----------------
            // Client Request
            //----------------
            // 1) Send request type
            http.setRequestMethod("POST");

            // 2) Send header information. Required for POST to work!
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            oStrm = http.getOutputStream();
            // If you experience connection/IO problems, try 
            // removing the comment from the following line
            //   http.setRequestProperty("Connection", "close");      

            // 3) Send data/body
            // Write account number
            Enumeration<String> keys = params.keys();
            boolean firstparam = true;

            while(keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                String value = (String)params.get(key);
                if (! firstparam)
                    key = "&" + key;
                byte data[]    = (key + "=" + value).getBytes();
                oStrm.write(data);
                firstparam = false;
            }

            /*
            The call to flush() uses a feature of HTTP 1.1 that allows data to be
            sent in smaller loads. When calling flush() or sending a large
            amount of data (in version 1.0.3) chunked encoding is used. 

            If you are using an HTTP 1.0 server or proxy server the chunked 
            transfer may cause problems.

            You can avoid the chunking behavior with small transactions by just
            calling close() where you were using flush(). For larger transactions
            you need to buffer your output so a single write() and close() are
            issued to the output stream.
             */
            // For 1.0.3 remove flush command
            //          oStrm.flush();

            //----------------
            // Server Response
            //----------------
            iStrm = http.getInputStream();
            // Three steps are processed in this method call
            ret = getServerResponse(http, iStrm);
        } catch (Exception e) {
            showDialog("出错了", e.toString());
        } catch (Error er) {
            showDialog("出错了", er.toString());
        } finally {
            // Clean up
            if (iStrm != null)
                iStrm.close();
            if (oStrm != null)
                oStrm.close();        
            if (http != null)
                http.disconnect();
        }
        return ret;
    }

    final public int random()  {
        Random number = new Random();
        number.setSeed(System.currentTimeMillis());
        return number.nextInt();
    }

    private void delFile(String strFileName)
    {
        try
        {
            File myFile = new File(strFileName);
            if(myFile.exists())
            {
                myFile.delete();
            }
        }
        catch (Exception e)
        {
            showDialog("出错了", e.toString());
        }
    }
    
    private boolean testBBS(String str) {
        if ("yssy".equals(str) || "rygh".equals(str) || "mop".equals(str) || "tianya".equals(str)) {
            return true;
        }
        return false;
    }

    private void showDialog(String title, String msg)
    {
        new AlertDialog.Builder(ImagePreview.this)
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
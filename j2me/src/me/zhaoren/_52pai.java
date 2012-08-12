package me.zhaoren;

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;  
import javax.microedition.io.Connector;
import javax.microedition.media.*;
import javax.microedition.media.control.*;

//import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
//import java.util.*;
import java.util.*;

public class _52pai extends MIDlet {
	private Network cNetwork = null;
	private Rms 	cRms 	 = null;
	private Util 	cUtil 	 = null;

	//0->Nokia S60, 1->nexian, 2->Simulator
	public int sku = 2;

    public boolean verifyret  = false;
    public boolean nUpdate    = false;
    public boolean payment    = false;
    public boolean changeID   = false;
    public String  uid        = "";
    public String  version    = "1.0";
    public int     sitenum    = 0;
    public long    traffic    = 0;
    public long    picnum     = 0;
    public boolean stopSender = false;

    // 1. define GUI components
    private Display mDisplay   = null;
    private Gauge   gaProgress = null;
    private Form    gForm      = null;

    private final Command send_4   = new Command("上传", Command.OK, 0);
    private final Command back_4   = new Command("返回", Command.EXIT, 1);
    private final Command cancel_4 = new Command("取消", Command.EXIT, 0);
    private final Command exit_4   = new Command("退出", Command.EXIT, 0);
    //private final Command suspend_4 = new Command("暂停", Command.OK, 0);
    //final Command back_4_2 = new Command("返回", Command.OK, 0);

    // for file system
    public String currDirName = null;
    // TODO: tune this parameter for specific machine/network
    //set file size and chunk size
    //private int chunckSize = 10240;
    private int chunckSize = 20480;
    //private String prevDirName = null;
    public Hashtable file_extensions = new Hashtable();

    private boolean switching = true;
    private Object UILock = new Object();
    private byte[] gCapturedImageRaw = null;
    //private Player mPlayer = null;
    //private VideoControl mVideoControl = null;

    // available site list
    public String[] targets_str    = null;
    public String[] targets        = null;
    public String[] targets_logins = null;
    public Image[]  targets_images = null;
    public int[]    targets_titles = null;
    // added site list
    private String[] gTargets      = null;
    private String[] gTargets_str  = null;
    private Image[]  gImages       = null;
    private String[] gLogins       = null;
    public  int[]    gTitles       = null;

    // 5. internal logics
    private int selected_target = -1;
    private String username = null;
    private String password = null;
    private String filename = null;
    private String filetype = null;

    // picture title and description
    private String title       = "";
    private String description = "";

    private class SiteItem {
        public String target;
        public String target_str;
        public Image  image;
        public String login;
        public int    title;
        //public int importance = 0;
        public SiteItem next = null;
        //public SiteItem prev = null;
        //public SiteItem() {}
        public SiteItem(String t, String t_str, Image i, String l, int tl) {
            target     = t;
            target_str = t_str;
            image      = i;
            login      = l;
            title      = tl;
            //importance = 0;
        }
    }

    public _52pai() {
        //initialize class
        cNetwork = new me.zhaoren.Network();
        cRms     = new me.zhaoren.Rms(this);
        cUtil    = new me.zhaoren.Util();

        String MEGA_ROOT = "/";
        currDirName = MEGA_ROOT;
        //prevDirName = MEGA_ROOT;

        // initialize utility data
        file_extensions.put("jpg", "image/jpeg");
        file_extensions.put("jpe", "image/jpeg");
        file_extensions.put("jpeg", "image/jpeg");
        file_extensions.put("bmp", "image/x-ms-bmp");
        file_extensions.put("gif", "image/gif");
        file_extensions.put("png", "image/png");
        file_extensions.put("ico", "image/vnd.microsoft.icon");
        file_extensions.put("tiff", "image/tiff");
        file_extensions.put("svf", "image/vnd.svf");

        // initializing GUI display
        mDisplay = Display.getDisplay(this);
    }

    public void startApp() {
        loadingUI();
        cRms.loadPreference();

        if (cUtil.testSystem()) {
            //testPictureDir();
            //TODO: make it hard coded for DEMO version
            /*if ( sku == 0 ) {
                currDirName = "e:/test/";
                //chunckSize = 10240;
                chunckSize = 20480;
            }
            else if ( sku == 1 ) {
                currDirName = "c:/";
                //chunckSize = 10240;
                chuckSize = 20480;
            }
            else */
            if ( sku == 2 ) {
                //currDirName = "root1/";
                chunckSize = 900;
            }
        } else {
            printit( "Exception in startApp", "this phone support neigther MMAPI nor JSR75." );
        }

        boolean handshakeok = false;
        handshakeok = handshake( uid, version );

        if ( handshakeok ) {
            if( changeID )
                cRms.putUID(uid, true);
            if ( !payment )
                paymentUI();
            else if ( nUpdate )
                updateUI();
            else if ( payment && !nUpdate )
                defaultUI();
        } else {
            gForm = new Form ( "出错了" );
            gForm.append(new StringItem ( "登入失败，网络状态不稳定，请更换地点重试。", "" ));

            final Command exit_hs = new Command("退出", Command.EXIT, 0);
            gForm.addCommand(exit_hs);
            gForm.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    synchronized(UILock) {
                        if (!switching) {
                            if ( c.equals(exit_hs) ) {
                                destroyApp(false);
                            }
                        }
                    }
                }
            });

            synchronized(UILock) {
                switching = false;
            }
            mDisplay.setCurrent(gForm);
        }
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean unconditional) {
        notifyDestroyed();
    }

    /*final public String testPictureDir() {
        //TODO: figure out whether nokia differentiate capital or not.
        //TODO: figure out S60 English/Chinese version dir position for two saving preferences: 2phone 2disk
        //TODO: other machine need other care
        String []guesses = new String[4];
        //String []guesses = new String[2];
        guesses[0] = "C:/DATA/Images/";
        guesses[1] = "C:/Data/Images/";
        guesses[2] = "E:/Images/";
        guesses[3] = "root1/photos/";

        final String MEGA_ROOT = "/";
        final char SEP = '/';
        String maxg = MEGA_ROOT;
        int maxn = 0;
        Enumeration e;  
        FileConnection currDir = null;
        for(int i=0; i < guesses.length; i ++) {
            final int MAX_PIC_NUM = 30;
            String g = guesses[i];
            int n = 0;
            try {
                if (MEGA_ROOT.equals(g)) {  
                    e = FileSystemRegistry.listRoots();  
                } else {
                    currDir = (FileConnection)Connector.open("file://localhost/" + g);
                    if (currDir == null)
                        continue;
                    e = currDir.list();
                }
                while (e != null && e.hasMoreElements()) {
                    String fileName = (String)e.nextElement();  
                    if (fileName.charAt(fileName.length()-1) != SEP) {
                        String fe = cUtil.getFileExt(fileName);
                        if (fe != null && file_extensions.get(fe) != null)
                            n ++;
                    }
                    if (n > MAX_PIC_NUM)
                        break;
                }
            } catch (IOException ioe) {
                continue;
            } catch (Exception ee) {
                if ("root1/photos/".equals(g))
                    currDirName = "root1/photos/";
                //printit("Exception in testPictureDir()", ee.toString());
                continue;
            }
            if (n > maxn) {
                maxn = n;
                maxg = g;
            }
        }
        return maxg;
        //prevDirName = maxg;
    }*/

    public void printit(String title, String content) {
        Alert timeAlert;
        timeAlert = new Alert(title);
        timeAlert.setString(content);
        mDisplay.setCurrent(timeAlert);
    }

    private void loadingUI(){
        ImageItem logo = null;
        try{
            logo = new ImageItem( "", Image.createImage ( "/boto.png" ),
                    ImageItem.LAYOUT_CENTER    | ImageItem.LAYOUT_NEWLINE_BEFORE, "Powered by 52pai" );
        } catch ( IOException e ){
            printit("出错了", e.toString());
            return;
        }
        gForm = new Form("我爱拍 (52pai.mobi) 传照片");
        gForm.append("\n\n");
        gForm.append(logo);
        gForm.append(new StringItem("\n", "载入中，请稍候..."));

        mDisplay.setCurrent(gForm);
    }

    private boolean loadSites() {
        // remove all data
        SiteItem siteRoot = null;
        SiteItem siteitem = null;

        int i = 0;
        while( i < targets.length ) {
            if( cRms.getUsername( targets_str[i] ) != "" ) {
                siteitem = new SiteItem( targets[i], targets_str[i], targets_images[i], targets_logins[i], targets_titles[i] );
                //siteitem.importance = 0;
                if ( siteRoot == null ) {
                    siteRoot = siteitem;
                } else {
                    siteitem.next = siteRoot;
                    siteRoot       = siteitem;
                }
            }
            i++;
        }

        Image newicon = null;
        try{
            newicon = Image.createImage( "/new.png" );
        } catch ( IOException e ) {
            printit( "出错了", e.toString() );
            return false;
        }

        siteitem = new SiteItem( "添加站点\n", "_addsite", newicon, "", 0 );
        SiteItem si = siteRoot;

        int count = 0;
        if ( si == null ) {
            siteRoot = siteitem;
            count++;
        } else {
            count = 1;
            while ( si.next != null ) {
                count++;
                si = si.next;
            }
            si.next = siteitem;
            count ++;
        }

        //生成数组
        int j = 0;
        gTargets     = new String [ count ];
        gTargets_str = new String [ count ];
        gImages      = new Image  [ count ];
        gLogins      = new String [ count ];
        gTitles      = new int    [ count ];

        for ( j=0, si = siteRoot; j < count; j++, si = si.next ) {  
            gTargets_str[j] = (String) si.target_str;  
            gTargets[j]     = (String) si.target;
            gImages[j]      = (Image)  si.image;
            gLogins[j]      = (String) si.login;
            gTitles[j]      = (int)    si.title;
        }
        /*gTargets_str = new String[count];
        for (j=0, si = siteRoot; j<count; j++, si = si.next) {  
            gTargets_str[j] = (String)si.target_str;  
        }

        gTargets = new String[count];
        for (j=0, si = siteRoot; j<count; j++, si = si.next) {  
            gTargets[j] = (String)si.target;
        }

        gImages = new Image[count];
        for (j=0, si = siteRoot; j<count; j++, si = si.next) {  
            gImages[j] = (Image)si.image;
        }

        gLogins = new String[count];
        for (j=0, si = siteRoot; j<count; j++, si = si.next) {  
            gLogins[j] = (String)si.login;
        }

        gTitles = new int [count];
        for (j=0, si = siteRoot; j<count; j++, si = si.next) {  
            gLogins[j] = (String)si.login;
        }*/
        return true;
    }

    private boolean verifyUserPassword(String site, String un, String pw) {
        final String s = site;
        final String u = un;
        final String p = pw;
        final String verifyURL = cNetwork.serverBaseURL + "pu/verify/";
        verifyret = false;

        Hashtable h = new Hashtable();
        h.put( "target"  , s );
        h.put( "username", u );
        h.put( "password", p );
        for (int trycount = 0; trycount < 2; trycount ++) {
            try {
                String ret = cNetwork.timeOutPOST( verifyURL, h );
                if ( "OK".equals( ret ) ) {
                    verifyret = true;
                } else {
                    verifyret = false;
                }
            } catch ( Exception e ) {
                //printit("出错了！", e.toString());
                //verifyret = false;
            } catch (Error e) {
                //printit("网络端了！", e.toString());
                //verifyret = false;
            }
            if (verifyret)
                break;
        }

        return verifyret;
    }

    final public boolean handshake (String id, String s) {
        final String handshakeURL = cNetwork.serverBaseURL + "pu/login/";

        if ( id == null ) {
            id = "";
        }
        String v = s;
        Hashtable shake = new Hashtable();

        Hashtable h = new Hashtable();
        h.put( "uid", id );
        h.put( "versionnum", v );
        try {
            //去掉下一行
            String ret = //"ede17d0173|1.0|true|0|0|11|Qzone相册\nqzone.qq.com,qzone,QQ帐号,qzone|人人网相册\nrenren.com,renren,用户邮箱/手机号/用户名,renren|开心网相册\nkaixin001.com,kaixin,邮箱/手机号/其他用户名,kaixin|MSN像册\nphotos.live.com,msn,Windows Live ID,msn|豆瓣像册\ndouban.com,douban,Email,douban|360圈像册\n360quan.com,_360quan,邮箱,_360quan|51.com像册\n51.com,_51,用户名/彩虹号,_51|新浪微博\nt.sina.com.cn,sina,登陆名(邮箱),sina|139说客\n139.com,_139,手机号,_139|flickr像册\nflickr.com,flickr,Yahoo! ID,flickr|又拍像册\nyupoo.com,yupoo,用户名/Email地址,yupoo|";
                cNetwork.timeOutPOST( handshakeURL, h );
            if ( "fail".equals(ret) || "".equals(ret) )
                return false;
            for (int i=0; i<20; i++) {
                if (ret != null && ret.length() != 0){
                    try{
                        int index = ret.indexOf("|");
                        shake.put("" + i, ret.substring(0, index));
                        ret = ret.substring(index+1);
                    } catch( Exception e ) {
                        printit( "出错了", e.toString() );
                    }
                }
            }
        } catch ( Exception e ) {
            //parent.printit("网络连接错误", e.toString());
            return false;
        }

        //TODO refine the payment & changeID/getID & update logic
        //get id
        if( shake.get( "0" ) != "" && !shake.get( "0" ).equals( uid ) ) {
            uid = (String)shake.get("0");
            changeID = true;
        }
        //get version
        if( !version.equals( shake.get( "1" ) ) ) {
            version = (String)shake.get("1");
            nUpdate = true;
        }
        //get paymentinfo
        if( "true".equals( shake.get( "2" ) ) )
            payment = true;
        //get traffic info
        if( !"".equals( shake.get( "3" ) ) )
            traffic = Long.parseLong(shake.get("3") + "");
        //get already uploaded pic num
        if( !"".equals( shake.get("4") ) )
            picnum = Long.parseLong(shake.get("4") + "");
        //get sitenum
        if(!"".equals(shake.get("5")))
            sitenum = Integer.parseInt(shake.get("5") + "");
        //initialize targets...
        targets        = new String [ sitenum ];
        targets_str    = new String [ sitenum ];
        targets_logins = new String [ sitenum ];
        targets_images = new Image  [ sitenum ];
        targets_titles = new int    [ sitenum ];

        int index =0;
        String im = null;
        String str = null;
        Image defaulticon = null;
        for ( int i = 6; i < ( sitenum + 6 ); i++ ) {
            str = "" + shake.get( "" + i );
            //解析每个站点
            if ( str != null && !"".equals( str ) ) {
                // get targets
                index            = str.indexOf( "," );
                targets[ i - 6 ] = str.substring( 0, index );
                str              = str.substring( index + 1 );
                // get targets_str
                index                = str.indexOf( "," );
                targets_str[ i - 6 ] = str.substring( 0, index );
                str                  = str.substring( index + 1 );
                // get login_prompts
                index                   = str.indexOf( "," );
                targets_logins[ i - 6 ] = str.substring( 0, index );
                str                     = str.substring( index + 1 );
                // get title condition
                index                   = str.indexOf( "," );
                targets_titles[ i - 6 ] = Integer.parseInt( str.substring( 0, index ) );
                str                     = str.substring( index + 1 );
                // create icons
                im = "/" + str + ".png";
                try{
                    targets_images[ i - 6 ] = Image.createImage(im);
                } catch ( Exception e ) {
                    //parent.printit("出错了", str);
                    try{
                        if (defaulticon == null){
                            defaulticon = Image.createImage("/boto.png"); 
                        }
                        targets_images[i-6] = defaulticon; 
                    } catch (Exception ee) {
                        printit("出错了", ee.toString());
                        return false;
                    }
                }
            } 
        }
        return true;
    }

    // default added sites list
    private void defaultUI() {
        try {
            loadSites();
        } catch ( Exception e ) {
            printit("Exception", e.toString());
            return;
        }
        final List browser = new List(
                "我爱拍(52pai.mobi)帮您传照片",
                Choice.IMPLICIT,
                gTargets,
                gImages );
        final Command confirm_0 = new Command("确认", Command.ITEM, 0);
        final Command delete_0  = new Command("删除", Command.ITEM, 1);
        final Command exit_0    = new Command("退出", Command.EXIT, 0);
        final Command modify_0  = new Command("修改", Command.EXIT, 3);
        final Command about_0   = new Command("关于", Command.EXIT, 4);
        final Command help_0    = new Command("帮助", Command.EXIT, 7);
        final Command traffic_0 = new Command("流量", Command.EXIT, 9);
        //final Command buy_0 = new Command("购买", Command.EXIT, 3);
        //final Command recommend_0 = new Command("推荐", Command.EXIT, 6);
        browser.addCommand( confirm_0 );
        browser.addCommand( delete_0 );
        browser.addCommand( exit_0 );
        browser.addCommand( about_0 );
        browser.addCommand( help_0 );
        browser.addCommand( modify_0 );
        //browser.addCommand(buy_0);
        //browser.addCommand(recommend_0);
        browser.addCommand(traffic_0);

        browser.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                int selected = -1;
                synchronized(UILock) {
                    if (!switching) {
                        if (c == List.SELECT_COMMAND || c.equals(confirm_0)) {
                            Choice choice = (Choice) d;
                            selected = choice.getSelectedIndex();
                            if (selected >= 0 && selected < (gTargets.length-1)) {
                                //选择需要上传的站点
                                selected_target = selected;
                                username = cRms.getUsername(gTargets_str[selected_target]);
                                password = cRms.getPassword(gTargets_str[selected_target]);
                                //TODO: fine tune return path for jsr75 only or mmapi only situation.
                                /*if (cUtil.jsr75support && cUtil.mmapisupport) {
                                    targetWin = 5; switching = true;
                                } else if (cUtil.jsr75support) {
                                    targetWin = 3; switching = true;
                                } else if (cUtil.mmapisupport) {
                                    targetWin = 6; switching = true;
                                }*/
                                targetWin = 5; switching = true;
                            } else if (selected == (gTargets.length-1)) {
                                //选择添加站点
                                targetWin = 1; switching = true;
                            }
                        } else if (c.equals(exit_0)) {
                            targetWin = -1; switching = true;
                        } else if (c.equals(modify_0)) {
                            Choice choice = (Choice) d;
                            selected = choice.getSelectedIndex();
                            targetWin = 2; 
                            switching = true;
                        } else if (c.equals(delete_0)){
                            Choice choice = (Choice) d;
                            final int selected_del = choice.getSelectedIndex();
                            // not the last.
                            if (selected_del >= 0 && selected_del < (gImages.length - 1)) {
                                //TODO: speed it up
                                new Thread(new Runnable() {
                                    public void run()
                                    {
                                        cRms.deleteUserPassword(gTargets_str[selected_del], true);
                                    }
                                }).start();
                                targetWin = 0; switching = true;
                            }
                        } else if (c.equals(traffic_0)) {
                            targetWin = 7; switching = true;
                        } else if (c.equals(about_0)) {
                            targetWin = 8; switching = true;
                        } else if (c.equals(help_0)) {
                            targetWin = 9; switching = true;
                        }/* else if (c.equals(buy_0)){
                            targetWin = 15; switching = true;
                        }*/
                    }
                }
                switch(targetWin) {
                case -1: destroyApp(false); break;
                case 0: defaultUI(); break;
                case 1: firstUI(); break;
                case 2: secondUI(selected, false); break;
                //case 3: thirdUI(); break;
                case 5: 
                    if (!cUtil.jsr75support){
                        sixthUI(); break;
                    } else if (!cUtil.mmapisupport){
                        thirdUI(); break;
                    } else if (cUtil.jsr75support && cUtil.mmapisupport) {
                        fifthUI(); break;
                    }
                    //case 6: sixthUI(); break;
                    //TODO: enable traffic information from server
                    //TODO: enable reporting remaining days
                    //TODO: server logs the payment activity
                    //TODO: auto-upgrade
                case 7: blankUI("流量"); break;
                case 8: blankUI("关于"); break;
                case 9: blankUI("帮助"); break;
                case 10: blankUI("检查更新"); break;
                //case 15: paymentUI(); break;
                default: break;                    
                }
            }
        });
        mDisplay.setCurrent(browser);
        synchronized(UILock) {
            switching = false;
        }
    }

    // display all available sites
    private void firstUI() {
        final List browser = new List(
                "选择相册站点",
                Choice.IMPLICIT,
                targets,
                targets_images);
        final Command confirm_1 = new Command("确认", Command.OK, 0);
        final Command back_1    = new Command("返回", Command.EXIT, 0);
        browser.addCommand(confirm_1);
        browser.addCommand(back_1);

        browser.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                int selected = -1;
                synchronized(UILock) {
                    if (!switching) {
                        if (c == List.SELECT_COMMAND || c.equals(confirm_1)) {
                            Choice choice = (Choice) d;
                            selected = choice.getSelectedIndex();
                            if (selected >= 0 && selected < targets.length) {
                                targetWin = 2; switching = true;
                            }
                        } else if (c.equals(back_1)) {
                            targetWin = 0;  switching = true;
                        }
                    }
                }
                switch(targetWin) {
                case 0: defaultUI(); break;
                case 2: secondUI(selected, true); break;
                default: break;                    
                }
            }
        });
        mDisplay.setCurrent(browser);
        synchronized(UILock) {
            switching = false;
        }
    }

    // prompt username and password
    private void secondUI(int selected, boolean it) {
        final int fselected = selected;
        final boolean IT = it;
        String t;
        verifyret = false;

        if (IT) {
            t = targets[selected].substring(0, targets[selected].indexOf('\n'));
        } else {
            t = gTargets[selected].substring(0, gTargets[selected].indexOf('\n'));
        }

        final Form mForm = new Form("为" + t + "设置帐号");

        if (IT) {
            mForm.append(
                    new TextField(targets_logins[selected], cRms.getUsername(targets_str[selected]),
                            64, TextField.ANY));
            mForm.append(
                    new TextField("密码", cRms.getPassword(targets_str[selected]), 
                            32, TextField.PASSWORD));
        } else {
            mForm.append(
                    new TextField(gLogins[selected], cRms.getUsername(gTargets_str[selected]),
                            64, TextField.ANY));
            mForm.append(
                    new TextField("密码", cRms.getPassword(gTargets_str[selected]), 
                            32, TextField.PASSWORD));
        }

        //TODO handle RMS related stuff earlier.
        final Command ok_2     = new Command("确认", Command.OK, 0);
        final Command verify_2 = new Command("验证", Command.OK, 0);
        final Command back_2   = new Command("返回", Command.EXIT, 0);
        mForm.addCommand( back_2 );
        mForm.addCommand( verify_2 );
        mForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if (IT) {
                            if (c.equals(back_2)) {
                                targetWin = 1; switching = true;
                            } else if (verifyret) {
                                if (c == List.SELECT_COMMAND || c.equals(ok_2)) {
                                    username = ((TextField)mForm.get(0)).getString();
                                    password = ((TextField)mForm.get(1)).getString();
                                    if ( username == null || "".equals(username) || password == null || "".equals(password) )
                                    {
                                        targetWin = -3;
                                    } else {
                                        //TODO: speed it up
                                        targetWin = 0; switching = true;
                                    }
                                } 
                            } else if (!verifyret) {
                                if (c == List.SELECT_COMMAND || c.equals(verify_2)) {
                                    username = ((TextField)mForm.get(0)).getString();
                                    password = ((TextField)mForm.get(1)).getString();
                                    if ( username == null || "".equals(username) || password == null || "".equals(password) ) {
                                        targetWin = -3;
                                    } else { 
                                        targetWin = -4; switching = true;
                                    }
                                }
                            }
                        } else {
                            if (c.equals(back_2)) {
                                targetWin = 0; switching = true;
                            } else if (verifyret) {
                                if (c == List.SELECT_COMMAND || c.equals(ok_2)) {
                                    username = ((TextField)mForm.get(0)).getString();
                                    password = ((TextField)mForm.get(1)).getString();
                                    if ( username == null || "".equals(username) || password == null || "".equals(password) )
                                    {
                                        targetWin = -3;
                                    } else {
                                        //TODO: speed it up
                                        targetWin = 0; switching = true;
                                    }
                                } 
                            } else if (!verifyret) {
                                if (c == List.SELECT_COMMAND || c.equals(verify_2)) {
                                    username = ((TextField)mForm.get(0)).getString();
                                    password = ((TextField)mForm.get(1)).getString();
                                    if ( username == null || "".equals(username) || password == null || "".equals(password) ) {
                                        targetWin = -3;
                                    } else { 
                                        targetWin = -4; switching = true;
                                    }
                                }
                            }
                        }
                    }
                }
                switch(targetWin) {
                case -3: printit("提醒", "您必须输入用户名和密码才能添加相册!"); break;
                case -4:
                    if(mForm.size() >= 3)
                        mForm.delete(2);
                    // TODO: 检查不显示"正在验证..."的bug
                    mForm.insert(2, new StringItem("", "正在验证..."));
                    mForm.removeCommand(verify_2);
                    mForm.removeCommand(back_2);
                    mDisplay.setCurrent(mForm);
                    if (IT) {
                        Thread t = new Thread(new Runnable() {
                            public void run()
                            {
                                if (verifyUserPassword(targets_str[fselected], username, password)) {
                                    cRms.putUserPassword(targets_str[fselected], username, password, true);
                                }
                            }
                        });
                        t.start();
                        try {
                            t.join();
                        } catch ( Exception e ) {
                            printit("出错了", e.toString());
                        }
                    } else {
                        Thread t = new Thread(new Runnable() {
                            public void run()
                            {
                                if (verifyUserPassword(gTargets_str[fselected], username, password)){
                                    cRms.putUserPassword(gTargets_str[fselected], username, password, true);
                                }
                            }
                        });
                        t.start();
                        try {
                            t.join();
                        } catch ( Exception e ) {
                            printit("出错了", e.toString());
                        }
                    }

                    if (verifyret) {
                        mForm.delete(2);
                        mForm.insert(2, new StringItem("", "验证通过!"));
                        mForm.addCommand(ok_2);
                    } else {
                        //TODO: 调整验证失败的处理逻辑
                        mForm.addCommand(verify_2);
                        mForm.addCommand(back_2);
                        mForm.delete(2);
                        mForm.insert(2, new StringItem("", "验证失败，请检查用户名和密码!"));
                    }
                    synchronized (UILock) {
                        switching = false;
                    }
                    break;
                case 0: defaultUI(); break;
                case 1: firstUI(); break;
                default: break;
                }
            }
        });

        mDisplay.setCurrent(mForm);
        synchronized(UILock) {
            switching = false;
        }
    }

    void showCurrDir()
    {
        Enumeration e;  
        FileConnection currDir = null;
        final String SEP_STR = "/";
        final char SEP = '/';
        final String MEGA_ROOT = "/";
        final String UP_DIRECTORY = "上层目录";
        try {
            Image foldericon = null;
            Image pictureicon = null; 
            if ( sku == 0 ){
                foldericon  = Image.createImage("/folder_big.png");
                pictureicon = Image.createImage("/picture_big.png");
            } else {
                foldericon  = Image.createImage("/folder_small.png");
                pictureicon = Image.createImage("/picture_small.png");
            }
            //System.out.println("In showCurrDir");  
            //System.out.println("mega_root:"+MEGA_ROOT+"cur_dir:"+currDirName);
            Hashtable pool = new Hashtable();
            Hashtable poold = new Hashtable();
            int putupfolder = 0;
            try {
                if (MEGA_ROOT.equals(currDirName)) {  
                    e = FileSystemRegistry.listRoots();
                    //browser = new List(currDirName, List.IMPLICIT);  
                    //System.out.println("here");  
                } else {  
                    //System.out.println("connector");
                    currDir = (FileConnection)Connector.open("file://localhost/" + currDirName);  
                    //System.out.println("curr_dir:"+currDir);  
                    //currDir = (FileConnection)Connector.open("http://localhost:8080/" + currDirName);  
                    e = currDir.list();
                    putupfolder = 1;
                    //poold.put(UP_DIRECTORY, foldericon);
                    //browser = new List( currDirName, List.IMPLICIT);  
                    //browser.append(UP_DIRECTORY, null);
                }  

                while (e.hasMoreElements()) {  
                    //System.out.println("h2");  
                    String fileName = (String)e.nextElement();  
                    //System.out.println("fileName:"+fileName+" char_at:"+fileName.charAt(fileName.length()-1));  

                    if (fileName.charAt(fileName.length()-1) == SEP) {
                        //browser.append(fileName,null);
                        poold.put(fileName, foldericon);
                    } else {
                        String fe = cUtil.getFileExt(fileName);
                        if (fe != null && file_extensions.get(fe) != null) {
                            // TODO: open the thumbnail feature
                            //FileConnection imgConn =(FileConnection) Connector.open("file://localhost/" + currDirName + fileName, Connector.READ);
                            //InputStream imgFile = imgConn.openInputStream();
                            //Image currImage = Image.createImage(imgFile);
                            //currImage = null;//rescaleImage(currImage, iconWidth, iconHeight);
                            pool.put(fileName, pictureicon);
                        }
                    }  
                }
                if (currDir != null) {  
                    currDir.close();  
                }
            } catch (Exception ex) {
                currDirName = MEGA_ROOT;
                //thirdUI();
                // TODO test
                printit("error", "showdir");
                return;
            }


            String []ss = new String[pool.size() + poold.size() + putupfolder];
            //TODO: open the thumbnail feature
            Image []is = new Image[pool.size() + poold.size() + putupfolder];
            int counter = 0;

            Enumeration keyenum = pool.keys();
            while ( keyenum.hasMoreElements() ) {
                String k    = (String) keyenum.nextElement();
                Image i     = (Image) pool.get(k);
                ss[counter] = k;
                is[counter] = i;
                counter ++;
            }
            Enumeration keyenum2 = poold.keys();
            while( keyenum2.hasMoreElements() ) {
                String k2   = (String) keyenum2.nextElement();
                Image i     = (Image) poold.get(k2);
                ss[counter] = k2;
                is[counter] = i;
                counter ++;
            }
            if (putupfolder == 1) {
                ss[counter] = UP_DIRECTORY;
                is[counter] = foldericon;
                counter ++;
            }
            final List browser = new List(
                    "选照片(当前目录 " + currDirName + " )",
                    Choice.IMPLICIT,
                    ss,
                    is);
            final Command ok_3   = new Command("确定", Command.OK, 0);  
            final Command back_3 = new Command("返回", Command.EXIT, 0);

            browser.setSelectCommand(ok_3);  
            browser.addCommand(back_3);  
            browser.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    int targetWin = -2;
                    String currFile = "";
                    synchronized(UILock) {
                        if (!switching) {
                            if (c == List.SELECT_COMMAND || c.equals(ok_3)) {
                                List curr = (List)d;
                                currFile = curr.getString(curr.getSelectedIndex());
                                //System.out.println(currFile);
                                if (currFile.endsWith(SEP_STR) || currFile.equals(UP_DIRECTORY))   
                                {
                                    targetWin = -1; switching = true;
                                } else {
                                    String fe = cUtil.getFileExt(currFile);
                                    if (fe != null) {
                                        filetype = (String)file_extensions.get(fe);
                                        if (filetype != null) {
                                            filename = "file://localhost/" + currDirName + currFile;
                                            targetWin = 4; switching = true;
                                        }
                                    }
                                }
                            } else if (c.equals(back_3)){
                                targetWin = 5; switching = true;
                            }
                        }
                    }
                    switch(targetWin) {
                    case -1: traverseDirectory(currFile); break;
                    case 4: fourthUI(); break;
                    case 5:
                        if (!cUtil.mmapisupport){
                            defaultUI(); break;
                        } else if (cUtil.jsr75support && cUtil.mmapisupport) {
                            fifthUI(); break;
                        }
                    default: break;
                    }
                }
            });

            mDisplay.setCurrent(browser);
            synchronized(UILock) {
                switching = false;
            }
        } catch (IOException ioe)   
        {
            printit("读文件系统出错了！", ioe.toString());
        } catch (Error er) {
            cUtil.jsr75support = false;
            printit("出错了！", er.toString());
            //return to defaultUI
            defaultUI();

        }
    }

    private void traverseDirectory( String fileName )
    {  
        final char   SEP          = '/';
        final String MEGA_ROOT    = "/";
        final String UP_DIRECTORY = "上层目录";
        //System.out.println("fileName:"+fileName+"cur_dir:"+currDirName+"mega_root:"+MEGA_ROOT);  
        if ( currDirName.equals(MEGA_ROOT) ) {  
            if ( fileName.equals(UP_DIRECTORY) ) {
                // can not go up from MEGA_ROOT  
                return;  
            }
            //prevDirName = currDirName;
            currDirName = fileName;
        } else if ( fileName.equals(UP_DIRECTORY) ) {
            // Go up one directory
            // use setFileConnection when implemented
            int i = currDirName.lastIndexOf( SEP, currDirName.length() - 2 );  
            if ( i != -1 ) {
                //prevDirName = currDirName;
                currDirName = currDirName.substring( 0, i + 1 );  
            } else {
                //prevDirName = currDirName;
                currDirName = MEGA_ROOT;  
            }
        } else {
            //prevDirName = currDirName;
            currDirName = currDirName + fileName;
        }
        thirdUI();
    }

    // file browser
    private void thirdUI()
    {
        try {  
            new Thread( new Runnable() {
                public void run()   
                {  
                    showCurrDir();
                }
            }).start();
        } catch (SecurityException e) {
            printit ( "读文件系统出错了！", e.toString() );
        } catch ( Exception e ) {
            printit ( "出错了！", e.toString() );
        }

    }

    // display image and upload it
    private void fourthUI()
    {
        try {
            Image currImage     = null;
            InputStream imgFile = null;
            String title_str     = null;
            boolean Rescale     = true;
            String t = gTargets[selected_target].substring(0, gTargets[selected_target].indexOf('\n'));
            gForm = new Form("传照片到" + t);

            // toggle garbage collection
            System.gc();
            try {
                if (gCapturedImageRaw == null) {
                    FileConnection imgConn =(FileConnection) Connector.open(filename, Connector.READ);
                    title_str = filename.substring(filename.lastIndexOf('/') + 1) + " 图片大小: " + (long)(imgConn.fileSize()/1024) + "KB";
                    imgFile = imgConn.openInputStream();
                    currImage = Image.createImage(imgFile);
                } else {
                    title_str = "";//"图片大小" + imgfSize + "KB"; 
                    currImage = Image.createImage(gCapturedImageRaw, 0, gCapturedImageRaw.length);
                }
            } catch ( Exception e ) {
                Rescale = false;
            } catch (/*OutOfMemory*/Error oom) {
                /*try {
                    Rescale = false;
                    JPGResizer resize = new JPGResizer();
                    if (gCapturedImageRaw == null) {
                        currImage = resize.resizeItByStream(imgFile, 120, 120, 16);
                    } else {
                        currImage = resize.resizeItByByteArray(gCapturedImageRaw, 120, 120, 16);
                    }
                    gForm.append(new ImageItem(title_str, currImage, ImageItem.LAYOUT_CENTER, null));
                } catch ( Exception e ) {
                    gForm.append(new StringItem(title_str, ""));
                } catch (Error om) {
                    gForm.append(new StringItem(title_str, ""));
                }*/
                Rescale = false;
                gForm.append(new StringItem(title_str, ""));
            }

            if (Rescale){
                if ( sku == 0 || sku == 2 ){
                    currImage = cUtil.rescaleImage(currImage, 160, 220);
                } else if ( sku == 1 ) {
                    currImage = cUtil.rescaleImage(currImage, 220, 160);
                }
                gForm.append(new ImageItem(title_str, currImage, ImageItem.LAYOUT_CENTER, null));
            }

            final Command title_4 = new Command( "标题", Command.EXIT, 0 );
            gForm.addCommand(send_4);
            gForm.addCommand(back_4);
            // TODO 
            int i = gTitles[selected_target];
            if ( gTitles[selected_target] != 0 ) {
                gForm.addCommand( title_4 );
            }
            gForm.setCommandListener( new CommandListener() {
                public void commandAction( Command c, Displayable d ) {
                    int targetWin = -2;
                    synchronized( UILock ) {
                        if ( !switching ) {
                            if ( c == List.SELECT_COMMAND || c.equals( send_4 ) ) {
                                targetWin = -1; switching = true;
                            } else if ( c.equals( back_4 ) ){
                                if (gCapturedImageRaw == null){
                                    targetWin = 3; switching = true;
                                } else {
                                    targetWin = 6; switching = true;
                                }
                                gCapturedImageRaw = null;
                            } else if ( c.equals( exit_4 ) ) {
                                targetWin = -2; switching = true;
                                gCapturedImageRaw = null;
                            } else if ( c.equals( cancel_4 ) ) {
                                targetWin = -100; stopSender = true;
                            } else if ( c.equals( title_4 ) ) {
                                targetWin = 7; switching = true;
                            }
                        }
                    }
                    switch(targetWin) {
                    case -2:
                        destroyApp(false);
                        break;
                    case -1:
                        //progress Bar
                        gForm.removeCommand(send_4);
                        gForm.removeCommand(back_4);
                        //TODO
                        //cancel button
                        gForm.addCommand(cancel_4);
                        //gForm.addCommand(suspend_4);

                        gaProgress = new Gauge("上传中...", false, 100, 1);
                        gForm.append(gaProgress);
                        synchronized (UILock) {
                            switching = false;
                        }
                        sendImgFile();
                        break;
                    case 3: 
                        thirdUI();
                        break;
                    case 6: 
                        sixthUI(); 
                        break;
                    case 7:
                        titleUI();
                        break;
                    default: break;
                    }
                }
            });

            mDisplay.setCurrent(gForm);

            synchronized(UILock) {
                switching = false;
            }
        } catch ( Exception e ) {
            //printit("Exception in fourthUI()", e.toString());
            printit("出错了！", e.toString());
        }
    }

    // choose the method to get a picture
    void fifthUI() {
        Image camicon      = null;
        Image fromfileicon = null;
        try{
            camicon = Image.createImage("/cam.png");
            fromfileicon = Image.createImage("/fromfile.png");
        } catch ( IOException e ) {
            printit("出错了", e.toString());
        }
        String t = gTargets[selected_target].substring(0, gTargets[selected_target].indexOf('\n'));
        t += ("(" + username + ")");
        String[] strings = new String[2];
        Image[]  images  = new Image[2];
        strings[0] = "即拍即传\n";
        strings[1] = "在已经拍好的照片里面选\n";
        images[0]  = camicon;
        images[1]  = fromfileicon;
        final List browser = new List(
                "怎样上传照片到" + t,
                Choice.IMPLICIT,
                strings,
                images);
        final Command confirm_5 = new Command("确定", Command.OK, 0);
        final Command back_5    = new Command("返回", Command.EXIT, 0);
        browser.addCommand(confirm_5);
        browser.addCommand(back_5);
        browser.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if (c == List.SELECT_COMMAND || c.equals(confirm_5)) {
                            Choice choice = (Choice) d;
                            int selected = choice.getSelectedIndex();
                            if (selected == 0) { // take and transfer
                                targetWin = 6; switching = true;
                            } else if (selected == 1) { // in the existing file. 
                                targetWin = 3; switching = true;
                            }
                        } else if (c.equals(back_5)){
                            targetWin = 0; switching = true;
                        }
                    }
                }
                switch(targetWin) {
                case 0: defaultUI(); break;
                case 3: thirdUI(); break;
                case 6: sixthUI(); break;
                default: break;
                }
            }
        });
        mDisplay.setCurrent(browser);
        synchronized(UILock) {
            switching = false;
        }
    }

    // camera preview
    private void sixthUI(){
        try{
            // Initiliaze Camera
            final Player mPlayer = Manager.createPlayer("capture://video");
            mPlayer.realize();

            // Showing the Camera Video
            final VideoControl mVideoControl = (VideoControl)mPlayer.getControl("VideoControl");
            final Form mForm = new Form("即拍即传");

            final Command back_6 = new Command("返回", Command.EXIT, 0);
            final Command take_6 = new Command("拍摄", Command.OK, 0);

            if (mVideoControl != null) {
                mForm.append((Item)(mVideoControl.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, null)));
                mForm.addCommand( back_6 );
                mForm.addCommand( take_6 );
            }

            //Start Camera
            try {
                if (mPlayer != null) {
                    mPlayer.start();
                }
                if (mVideoControl != null) {
                    mVideoControl.setVisible(true);
                }
            } catch (MediaException me) {
                //printit("出错了", me.toString());
                printit("出错了", "");
            } catch (SecurityException se) {
                //printit("出错了", se.toString());
                printit("出错了", "");
            }

            mForm.setCommandListener(new CommandListener() {
                public void commandAction(Command c, Displayable d) {
                    int targetWin = -2;
                    synchronized(UILock) {
                        if (!switching) {
                            if (c == List.SELECT_COMMAND || c.equals(take_6)) {
                                targetWin = -1;
                            } else if (c.equals(back_6)) {
                                targetWin = 5; switching = true;
                            }
                        }
                    }
                    switch(targetWin) {
                    case -1:
                        mForm.removeCommand(take_6);
                        mForm.removeCommand(back_6);
                        new Thread(new Runnable()   
                        {  
                            public void run()
                            {
                                System.gc();
                                //Get SnapShot
                                try {
                                    if ( sku == 0 ) {
                                        gCapturedImageRaw = mVideoControl.getSnapshot("encoding=jpeg&quality=100&width=640&height=480");
                                    } else if ( sku == 1 || sku == 2 ) {
                                        gCapturedImageRaw = mVideoControl.getSnapshot(null);
                                    }
                                } catch (MediaException me) {
                                    printit("出错了", me.toString());
                                }

                                filename = "picture.jpg";
                                filetype = (String)file_extensions.get("jpg");
                                //stop camera
                                try {
                                    if (mVideoControl != null) {
                                        mVideoControl.setVisible(false);
                                    }
                                    if (mPlayer != null) {
                                        mPlayer.stop();
                                        mPlayer.close();
                                    }
                                } catch (MediaException me) {
                                    printit("出错了", me.toString());
                                }
                                fourthUI();
                            }
                        }).start();
                        break;
                    case 5: 
                        try {
                            if (mVideoControl != null) {
                                mVideoControl.setVisible(false);
                            }
                            if (mPlayer != null) {
                                mPlayer.stop();
                                mPlayer.close();
                            }
                        } catch (MediaException me) {
                            printit("出错了", me.toString());
                        }
                        if (!cUtil.jsr75support){
                            defaultUI(); break;
                        } else if (cUtil.jsr75support && cUtil.mmapisupport) {
                            fifthUI(); break;
                        }
                    default: 
                        try {
                            if (mVideoControl != null) {
                                mVideoControl.setVisible(false);
                            }
                            if (mPlayer != null) {
                                mPlayer.stop();
                                mPlayer.close();
                            }
                        } catch (MediaException me) {
                            printit("出错了", me.toString());
                        }
                        break;
                    }
                }
            });
            mDisplay.setCurrent(null);
            mDisplay.setCurrent(mForm);
            synchronized(UILock) {
                switching = false;
            }
        } catch (IOException ioe) {
            //printit("Exception in sixthUI", ioe.toString());
            printit("出错了！", ioe.toString());
        } catch (MediaException me) {
            //printit("Exception in sixthUI", me.toString());
            printit("出错了！", me.toString());
        } catch (SecurityException se) {
            printit("出错了！", se.toString());
        }

    }

    // picture title and description
    private void titleUI () {
        gForm = new Form("输入图片标题和简介");
        gForm.append( new TextField( "标题", title, 64, TextField.ANY ) );
        gForm.append( new TextField( "简介", description, 128, TextField.ANY ) );        

        final Command ok_7   = new Command("确认", Command.OK, 0);
        final Command back_7 = new Command("返回", Command.EXIT, 0);
        gForm.addCommand( ok_7 );
        gForm.addCommand( back_7 );
        gForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if ( c.equals( ok_7 ) ) {
                            title = ((TextField)gForm.get(0)).getString();
                            description = ((TextField)gForm.get(1)).getString();
                            targetWin = 4; switching = true;
                        } else if ( c.equals( back_7 ) ) {
                            targetWin = 4; switching = true;
                        }
                    }
                }
                switch(targetWin) {
                case 4: 
                    fourthUI(); 
                    break;
                default: break;
                }
            }
        });

        mDisplay.setCurrent(gForm);
        synchronized(UILock) {
            switching = false;
        }
    }

    // static text UI
    private void blankUI(String s){
        gForm = new Form(s);

        if (s.equals("关于")) { gForm.append("我爱拍"); }
        else if (s.equals("帮助")) { gForm.append("help"); }
        else if (s.equals("流量统计")) { gForm.append("已上传图片" + picnum +"张"); gForm.append("流量" + traffic + "K");}
        else if (s.equals("检查更新")) { gForm.append("当前版本已是最新版：\n 版本号：" + version); }
        //else if (s.equals("已付费")) { gForm.append("付费成功，感谢您的使用。");} 

        final Command back_blank = new Command("返回", Command.EXIT,0);
        gForm.addCommand(back_blank);

        gForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if (c.equals(back_blank)) {
                            targetWin = 0; switching = true;
                        }
                    }
                }

                switch(targetWin) {
                case 0:
                    gForm = null;
                    defaultUI();
                    break;
                }
            }
        });

        mDisplay.setCurrent(gForm);
        synchronized(UILock) {
            switching = false;
        }
    }

    // prompt update
    private void updateUI(){
        gForm = new Form("检查更新");
        gForm.append("有新版本!新版本号为：" + version + "\n是否立即更新？");
        final Command back_update    = new Command("返回", Command.EXIT, 0);
        final Command confirm_update = new Command("更新", Command.OK, 0);
        gForm.addCommand(back_update);
        gForm.addCommand(confirm_update);

        gForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if (c.equals(back_update)) {
                            targetWin = 0; switching = true;
                        } else if (c.equals(confirm_update)){
                            targetWin = -1; switching = true;
                        }
                    }
                }

                switch(targetWin) {
                case 0:
                    gForm = null;
                    defaultUI(); 
                    break;
                case -1:
                    update();
                    break;
                default: break;
                }
            }
        });

        mDisplay.setCurrent(gForm);
        synchronized(UILock) {
            switching = false;
        }
    }

    // prompt payment
    private void paymentUI(){
        gForm = new Form("付费");
        gForm.append("你的ID：\n" + uid);
        gForm.append("\n点击“付费”购买我爱拍，月租5元");

        final Command ok_pay   = new Command("付费", Command.OK, 0);
        final Command exit_pay = new Command("退出", Command.EXIT, 0);
        gForm.addCommand(ok_pay);
        gForm.addCommand(exit_pay);

        gForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if (c == List.SELECT_COMMAND || c.equals(ok_pay)) {
                            targetWin = -1; switching = true;
                        } else if (c.equals(exit_pay)){
                            targetWin = 0; switching = true;
                        }
                        switch(targetWin) {
                        case 0: destroyApp(false); break;
                        case -1: 
                            gForm = null;
                            paidUI();
                            break;
                        default: break;
                        }
                    }
                }

            }
        });

        mDisplay.setCurrent(gForm);
        synchronized(UILock) {
            switching = false;
        }
    }

    // send SMS
    private void paidUI(){
        gForm = new Form("付费中");
        gForm.append(new StringItem("", "短信发送中，请稍候。。。"));
        final Command confirm_payment = new Command("确定", Command.OK,0);

        //TODO: find the right pay number and send the right text
        final String smsAddress = "13601676914";
        Thread t = new Thread(new Runnable()   
        {  
            public void run()
            {
                cNetwork.SendSMS(smsAddress, "注册");
            }
        });
        t.start();

        mDisplay.setCurrent(gForm);

        try {
            t.join();
        } catch ( Exception e ) {
            printit("出错了", e.toString());
        }

        if (handshake(uid, version)){
            gForm.delete(0);
            if (payment) {
                gForm.append(new StringItem("", "付费成功！"));
            } else if (!payment) {
                gForm.append(new StringItem("", "付费失败！"));
            }
            gForm.addCommand(confirm_payment);
        } else {
            printit("出错了", "");
            destroyApp(false);
        }

        gForm.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                int targetWin = -2;
                synchronized(UILock) {
                    if (!switching) {
                        if (c.equals(confirm_payment)) {
                            targetWin = 0; switching = true;
                        }
                    }
                }

                switch(targetWin) {
                case 0:
                    gForm = null;
                    if ( !payment )
                        paymentUI(); 
                    if ( nUpdate )
                        updateUI();
                    else if ( payment && !nUpdate )
                        defaultUI();
                    break;
                }
            }
        });

        mDisplay.setCurrent( gForm );
        synchronized( UILock ) {
            switching = false;
        }
    }

    private void sendImgFile() {
        uid = cRms.getUID(); // get latest uid before send

        final String uploadURL = Network.serverBaseURL + "pu/receive/";
        stopSender = false;
        Thread t = new Thread( new Runnable()   
        {
            public void run()
            {
                long overallSize;
                int packagecount = 0;
                String cancelReason = "上传被您取消";
                try {
                    if ( filename == null || filetype == null || selected_target == -1 
                            || username == null || password == null ) {
                        printit( "failed sendImgFile()", "filename: " + filename + " filetype: " + filetype 
                                + " selected_target: " + selected_target
                                + " username: " + username + " password: " + password );
                        return;
                    }
                    String target = gTargets_str[selected_target];
                    try {
                        int sessionID = cUtil.random();//1215156557;
                        FileConnection fileConn = null;
                        if ( gCapturedImageRaw == null ) {
                            fileConn     = (FileConnection) Connector.open(filename, Connector.READ);
                            overallSize = fileConn.fileSize();
                        } else {
                            overallSize = gCapturedImageRaw.length;
                        }

                        int percent;
                        long timeStart = System.currentTimeMillis();
                        long timeNow;
                        long timeRemain;

                        // init bitmap string
                        String retTargetStr = "";
                        int total_blocks = (int)((overallSize / chunckSize) + (overallSize % chunckSize > 0 ? 1 : 0));
                        for (int ix = 0; ix < total_blocks; ++ix) {
                            retTargetStr += "0";
                        }

                        while ( overallSize > 0 ) {
                            byte[] buffer = new byte[chunckSize];
                            int bytesRead = 0;
                            int bytesSent = 0;
                            int seq       = 0;

                            InputStream fis = null;
                            if (gCapturedImageRaw == null)
                                fis = fileConn.openInputStream();// open input stream
                            else
                                fis = new ByteArrayInputStream(gCapturedImageRaw);
                            while (bytesRead < overallSize && !stopSender) {
                                // 1 read
                                int chunck = chunckSize;
                                if ( (overallSize - bytesRead) < chunckSize ) {
                                    chunck = (int) (overallSize - bytesRead);
                                    buffer = new byte[chunck];
                                }
                                //fis.skip(bytesRead);

                                //int count = fis.read(buffer, bytesRead, chunck);
                                int count = fis.read(buffer, 0, chunck);

                                if ( count > 0 && (retTargetStr.length() <= seq || retTargetStr.charAt(seq) == '0')) {
                                    // 2 send
                                    Hashtable params = new Hashtable();
                                    params.put("target", target);
                                    params.put("uid", uid);
                                    params.put("username", username);
                                    params.put("password", password);
                                    params.put("sessionid", "" + sessionID);
                                    params.put("offset", "" + bytesRead);
                                    params.put("size", "" + overallSize);
                                    params.put("bs", "" + chunckSize);
                                    params.put("seq", "" + seq);
                                    String rrr = "fail";
                                    if ( !"".equals( title ) ) {
                                        params.put( "title", title );
                                    }
                                    if ( !"".equals( description ) ) {
                                        params.put( "description", description );
                                    }
                                    for (int trycount = 0; trycount < 2; trycount ++) {
                                        try {
                                            rrr = cNetwork.timeOutsendMultipart(uploadURL, params, "upload_filed", filename, filetype, buffer);
                                        } catch ( Exception e ) {
                                            //printit("Network Error", e.toString());
                                        } catch (Error e) {
                                            //printit("Network Error", e.toString());
                                        }
                                        if (! "fail".equals(rrr))
                                            break;
                                    }
                                    //rrr = rrr.substring(0, 2);
                                    if ("fail".equals(rrr)) {//sent error
                                        packagecount++;
                                        if ( packagecount == 5 ) {
                                            cancelReason = "网络异常，请您到网络更好的地方重试。";
                                            stopSender = true;
                                            break;
                                        }
                                    } else if (!"fail".equals(rrr)) { // success sent
                                        packagecount = 0;
                                        int pos = rrr.lastIndexOf('\n');
                                        retTargetStr = rrr.substring(pos + 1);
                                        //System.out.println(gaProgress.getValue());
                                        //time remain
                                        bytesSent += count;
                                        percent = (int)(95 * (1.0 * bytesSent / overallSize)) + 1;
                                        gaProgress.setValue(percent);
                                        timeNow = System.currentTimeMillis();
                                        timeRemain = (int) (((timeNow - timeStart) * ( 100 - percent) / percent)/1000);
                                        gaProgress.setLabel("上传中...  还有" + timeRemain + "秒");
                                        //mDisplay.setCurrent(gForm);
                                    }
                                }
                                bytesRead += count;
                                seq ++;
                            } // end of inner while
                            fis.close();
                            if (retTargetStr.indexOf("0") < 0 || stopSender)
                                break;
                        } // end of overall>0 while
                        if (!stopSender) {
                            gaProgress.setValue(100);
                            gaProgress.setLabel("上传完毕");
                            picnum +=1;
                        } else {
                            gaProgress.setLabel( cancelReason );
                        }
                        if ( gCapturedImageRaw == null )
                            fileConn.close();
                    } catch ( IOException e ) {
                        //printit("exception", "catch IOException: " + e.toString());
                        printit("上传出错，请重传", e.toString());
                        return;
                    }
                    gForm.removeCommand(cancel_4);
                    //gForm.removeCommand(suspend_4);
                    gForm.addCommand(back_4);
                    gForm.addCommand(exit_4);

                    synchronized(UILock) {
                        switching = false;
                    }
                } catch ( Exception e ) {
                    //printit("Exception in send",  e.toString());
                    printit("上传出错，请重传", e.toString());
                } finally {
                    //gCapturedImageRaw = null;
                }
            }
        });
        t.start();
    }

    //TODO 重新下载软件 更新。
    final public void update(){
        try{
            if( platformRequest( Network.serverBaseURL + "_52pai.jad" ) ){
                destroyApp( true ); 
            }
        } catch ( Exception e ) {
            printit ( "exception: ", "get Exception" + e.toString() );
            return;
        }
    }
}


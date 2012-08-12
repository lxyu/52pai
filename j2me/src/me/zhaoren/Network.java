package me.zhaoren;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;


public class Network {
    private static long timeOutMillis      = 15000;
    private static long multiTimeOutMillis = 30000;
    private static long pollingStartIntrv  = 50;
    private static long sleepInterval      = 600;
    private static long retryCounts        = 2;
    
    // global current http connenction
    private static HttpConnection currentConnection = null;
    private static OutputStream   currentOut        = null;
    private static InputStream    currentIn         = null;
    //private int i =1;

    //final public String uploadURL = "http://192.168.1.100:80/pu/receive/";
    //final public String verifyURL = "http://192.168.1.100:80/pu/verify/";
    //final public String handshakeURL = "http://192.168.1.100:80/pu/login/";
    //public final String uploadURL = "http://52pai.mobi:80/pu/receive/";
    //public final String verifyURL = "http://52pai.mobi:80/pu/verify/";
    //public final String handshakeURL = "http://52pai.mobi:80/pu/login/";
    //final private String serverBaseURL = "http://192.168.1.100/pu/";

    //static final public String serverBaseURL = "http://www.52pai.mobi/";
    //static final public String serverHost = "www.52pai.mobi";
    static final public String serverBaseURL = "http://192.168.1.100/";
    static final public String serverHost    = "192.168.1.100";

    final private boolean debugging = false;

    final public String getBoundaryMessage(String boundary, Hashtable params, String fileField, String fileName, String fileType)
    {
        StringBuffer res = new StringBuffer("--").append(boundary).append("\r\n");

        Enumeration keys = params.keys();

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
    
    final private class SendMultiPartThread extends Thread {
        private Hashtable params;
        private String url;
        private String fileField;
        private String fileType;
        private String fileName;
        private byte[] fileBytes;
        
        public  String result = null;
        public  boolean success = false;
        
        SendMultiPartThread (String _url, Hashtable _params, String _fileField, String _fileName, String _fileType, byte[] _fileBytes) {
            url       = _url;
            params    = _params;
            fileField = _fileField;
            fileName  = _fileName;
            fileType  = _fileType;
            fileBytes = _fileBytes;
        }
        
        public void run() {
            currentConnection = null;
            currentIn         = null;
            currentOut        = null;
            for (int ix = 0; ix < retryCounts; ++ix) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        try {
                            result = sendMultipart(url, params, fileField, fileName, fileType, fileBytes);
                        } catch (IOException e) {
                            e.printStackTrace();
                            result = "fail";
                            return;
                        }
                    }
                });
                t.start();
                try {
                    // check if thread is done repeatedly
                    long total = multiTimeOutMillis;
                    long temp  = pollingStartIntrv;
                    while (t.isAlive() && total > 0) {
                        total -= temp;
                        Thread.sleep(temp);
                        temp = (temp >= sleepInterval) ? sleepInterval : temp + pollingStartIntrv;
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    result = null;
                }
                // timeout
                if (result == null) {
                    t.interrupt();
                    if (currentConnection != null) {
                        try {
                            currentConnection.close();
                        } catch (IOException e) {
                            // connection successful closed already.
                        }
                        currentConnection = null; // clear connection
                    }
                    if (currentOut != null) {
                        try {
                            currentOut.close();
                        } catch ( Exception e ) {
                        }
                        currentOut = null;
                    }
                    if (currentIn != null) {
                        try {
                            currentIn.close();
                        } catch ( Exception e ) {
                        }
                        currentIn = null;
                    }
                    continue;
                } else {
                    // result got, POST finished in time.
                    success = true;
                    break;
                }
            }
        }        
    }
    
    final public String timeOutsendMultipart(String url, Hashtable params, String fileField, String fileName, String fileType, byte[] fileBytes) {
        SendMultiPartThread pt = new SendMultiPartThread(url, params, fileField, fileName, fileType, fileBytes);
        pt.start();
        try {
            pt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "fail";
        }
        if (pt.success)
            return pt.result;
        return "fail";
    }
        

    final public String sendMultipart(String url, Hashtable params, String fileField, String fileName, String fileType, byte[] fileBytes) throws IOException {
        final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
        String boundaryMessage = getBoundaryMessage(BOUNDARY, params, fileField, fileName, fileType);
        String endBoundary = "\r\n--" + BOUNDARY + "--\r\n";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] postBytes = null;
        HttpConnection hc = null;
        InputStream is = null;
        //byte[] res = null;
        String res = "";

        try {
            bos.write(boundaryMessage.getBytes());
            bos.write(fileBytes);
            bos.write(endBoundary.getBytes());
            postBytes = bos.toByteArray();
            
            /*
            FileConnection fc = (FileConnection)Connector.open("file://localhost/root1/a" + i + ".txt");
            i++;
            if(!fc.exists())
                fc.create();
            DataOutputStream os = fc.openDataOutputStream();
            os.write(postBytes);
            os.flush();
            fc.close();
            */
            bos.close();

            bos = new ByteArrayOutputStream();

            hc = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true);
            currentConnection = hc; // expose connection
            
            hc.setRequestProperty("X-Online-Host", serverHost);
            hc.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
            
            hc.setRequestMethod(HttpConnection.POST);
            //String t = hc.getHeaderField("boundary");
            OutputStream dout = hc.openOutputStream();
            currentOut = dout;
            dout.write(postBytes);
            dout.close();
            is = hc.openInputStream();
            currentIn = is;
            res = getServerResponse(hc, is);
        } /*catch ( Exception e ) {
            parent.printit("出错了", e.toString());            
        }*/finally {
            try {
                if(bos != null)
                    bos.close();
                if(is != null)
                    is.close();
                if(hc != null)
                    hc.close();
            } catch(Exception e2) {
                e2.printStackTrace();
            }
            currentConnection = null;
            currentOut = null;
            currentIn = null;
        }
        return res;
    }

    final public String getServerResponse(HttpConnection http, InputStream iStrm) throws IOException
    {
        //Reset error message
        String str = null;
        
        // 1) Get status Line
        if (debugging || http.getResponseCode() == HttpConnection.HTTP_OK) {
            // 2) Get header information - none

            // 3) Get body (data)
            int length = (int) http.getLength();
            if (length != -1) {
                byte servletData[] = new byte[length];
                iStrm.read(servletData);
                str = new String(servletData, "utf-8");
            } else { // Length not available...
                ByteArrayOutputStream bStrm = new ByteArrayOutputStream();
                int ch;
                while ((ch = iStrm.read()) != -1)
                    bStrm.write(ch);
                str = new String(bStrm.toByteArray(), "utf-8");
                bStrm.close();
            }
            // Update the string item on the display
            return str;
        } else {
            return "fail";
        }
    }
    
    final private class PostThread extends Thread {
        private String url;
        private Hashtable params;
        public  String result = null;
        public  boolean success = false;
        PostThread (String _url, Hashtable _params) {
            url    = _url;
            params = _params;
        }
        public void run() {
            currentConnection = null;
            currentIn         = null;
            currentOut        = null;
            for (int ix = 0; ix < retryCounts; ++ix) {
                Thread t = new Thread(new Runnable() {
                    public void run() {
                        // do post
                        try {
                            result = POST(url, params);
                        } catch (IOException e) {
                            //e.printStackTrace();
                            return;
                        }
                    }
                });
                t.start();
                try {
                    // check if thread is done repeatedly
                    long total = timeOutMillis;
                    long temp = pollingStartIntrv;
                    while (t.isAlive() && total > 0) {
                        total -= temp;
                        Thread.sleep(temp);
                        temp = (temp >= sleepInterval) ? sleepInterval : temp + pollingStartIntrv;
                    }
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                    result = null;
                }
                // timeout
                if (result == null) {
                    t.interrupt();
                    if (currentConnection != null) {
                        try {
                            currentConnection.close();
                        } catch (IOException e) {
                            // connection successful closed already.
                        }
                        currentConnection = null; // clear connection
                    }
                    if (currentOut != null) {
                        try {
                            currentOut.close();
                        } catch ( Exception e ) {
                        }
                        currentOut = null;
                    }
                    if (currentIn != null) {
                        try {
                            currentIn.close();
                        } catch ( Exception e ) {
                        }
                        currentIn = null;
                    }
                    //t.interrupt();
                    continue;
                } else {
                    // result got, POST finished in time.
                    success = true;
                    break;
                }
            }
        }
    
        
    }
    
    final public String timeOutPOST(String url, Hashtable params) {
        PostThread pt = new PostThread(url, params);
        pt.start();
        try {
            pt.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "fail";
        }
        if (pt.success)
            return pt.result;
        return "fail";
    }
    
    final public String POST(String url, Hashtable params) throws IOException{
        HttpConnection http  = null;
        OutputStream   oStrm = null;
        InputStream    iStrm = null;
        String ret = null;
        try {
            http = (HttpConnection) Connector.open(url, Connector.READ_WRITE, true); // true to enable timeout
            currentConnection = http; // expose http connection to other threads
            //----------------
            // Client Request
            //----------------
            // 1) Send request type
            http.setRequestMethod(HttpConnection.POST);

            // 2) Send header information. Required for POST to work!
            http.setRequestProperty("X-Online-Host", serverHost);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            oStrm = http.openOutputStream();
            currentOut = oStrm;
            // If you experience connection/IO problems, try 
            // removing the comment from the following line
            //   http.setRequestProperty("Connection", "close");      

            // 3) Send data/body
            // Write account number
            Enumeration keys = params.keys();
            boolean firstparam = true;
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] postBytes = null;
            
            //TODO URL value encoding...
            try {
                while(keys.hasMoreElements()) {
                    String key = (String)keys.nextElement();
                    String value = (String)params.get(key);
                    if (! firstparam)
                        key = "&" + key;
                    byte data[]    = (key + "=" + value).getBytes();
                    bos.write(data);
                    firstparam = false;
                }
                postBytes = bos.toByteArray();
                bos.close();
            }/*catch ( Exception e ) {
            parent.printit("出错了", e.toString());            
            }*/finally {
                try {
                    if(bos != null)
                        bos.close();
                } catch(Exception e2) {
                    e2.printStackTrace();
                }
            }
            oStrm.write(postBytes);
            oStrm.close();

            // For 1.0.3 remove flush command
            //          oStrm.flush();

            //----------------
            // Server Response
            //----------------
            iStrm = http.openInputStream();
            currentIn = iStrm;
            // Three steps are processed in this method call
            ret = getServerResponse(http, iStrm);
        } /*catch ( Exception e ) {
            parent.printit("出错了", e.toString());            
        }*/ finally {
            // Clean up
            if (iStrm != null)
                iStrm.close();
            if (oStrm != null)
                oStrm.close();        
            if (http != null)
                http.close();
            currentConnection = null;
            currentIn = null;
            currentOut = null;
        }
        // Process request failed, show alert    
        return ret;
    }

    //TODO catch exception in _52pai
    public boolean SendSMS(String number, String Message)//Number就是手机号码，Message就是短信的内容
    {
        boolean result = true;
        try{
            MessageConnection conn = (MessageConnection)Connector.open("sms://+86" + number);
            TextMessage msg = (TextMessage)conn.newMessage(MessageConnection.TEXT_MESSAGE);
            msg.setAddress("sms://+86" + number);
            msg.setPayloadText(Message);
            conn.send(msg);
            result = true;
        } catch( Exception e ) {
            result = false;
        } catch(Error er) {
            result = false;
        }
        return result;
    }
    /* 
    final public String encode(String s)
      {
        StringBuffer sbuf = new StringBuffer();
        int len = s.length();
        for (int i = 0; i < len; i++) {
          int ch = s.charAt(i);
          if ('A' <= ch && ch <= 'Z') {        // 'A'..'Z'
            sbuf.append((char)ch);
          } else if ('a' <= ch && ch <= 'z') {    // 'a'..'z'
               sbuf.append((char)ch);
          } else if ('0' <= ch && ch <= '9') {    // '0'..'9'
               sbuf.append((char)ch);
          } else if (ch == ' ') {            // space
               sbuf.append('+');
          } else if (ch == '-' || ch == '_'        // unreserved
              || ch == '.' || ch == '!'
              || ch == '~' || ch == '*'
              || ch == '\'' || ch == '('
              || ch == ')') {
            sbuf.append((char)ch);
          } else if (ch <= 0x007f) {        // other ASCII
               sbuf.append(hex[ch]);
          } else if (ch <= 0x07FF) {        // non-ASCII <= 0x7FF
               sbuf.append(hex[0xc0 | (ch >> 6)]);
               sbuf.append(hex[0x80 | (ch & 0x3F)]);
          } else {                    // 0x7FF < ch <= 0xFFFF
               sbuf.append(hex[0xe0 | (ch >> 12)]);
               sbuf.append(hex[0x80 | ((ch >> 6) & 0x3F)]);
               sbuf.append(hex[0x80 | (ch & 0x3F)]);
          }
        }
        return sbuf.toString();
      }
    }*/
}


/*final public void operatePreference(String select, String operation) {
        final String s = select;
        final String op = operation;
        Thread t = new Thread(new Runnable() {
            public void run()   
            {
                Hashtable h = new Hashtable();
                h.put("operation:", op);
                h.put("selected:", s);
                try {
                    String ret = POST(operateURL, h);
                } catch(IOException e) {
                    //parent.printit("Exception in verifyUserPassword", e.toString());
                    parent.printit("出错了！", e.toString());
                }
            }
        });
        t.start();
        try {
            t.join();
        }  catch ( Exception e ) {
            //parent.printit("Exception in verifyUserPassword", e.toString());
            parent.printit("网络连接错误", e.toString());
        }
    }
}*/


/*final public boolean checkUpdate(){
    try{
        HttpConnection conn = (HttpConnection)Connector.open(updateURL);
        InputStream re = conn.openInputStream();
        if( getServerResponse(conn, re) == "true") {
            return true;
        } else {
            return false;
        }
    }catch( Exception e ){
        parent.printit("网络连接失败", e.toString());
        return false;
    }
}

final public void requestUID(){
    try{
        HttpConnection conn = (HttpConnection)Connector.open(requestURL);
        InputStream re = conn.openInputStream();
        parent.UID = getServerResponse(conn, re);
    }catch( Exception e ){
        parent.printit("网络连接失败", e.toString());
    }
}*/
package me.zhaoren;

import java.util.Random;
//import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class Util {
    public boolean jsr75support = true;
    public boolean mmapisupport = true;
    
    /*class ScreenCanvas extends Canvas {
        protected void paint(Graphics g) {
        }
    }*/
    
    final public Image rescaleImage(Image image, int width, int height) {
        int sourceWidth  = image.getWidth();
        int sourceHeight = image.getHeight();

        if (sourceWidth > sourceHeight) {
            height = sourceHeight*width/sourceWidth;
        } else if (sourceWidth < sourceHeight){
            width = sourceWidth*height/sourceHeight;
        }

        Image newImage = Image.createImage(width, height);
        Graphics g = newImage.getGraphics();

        for(int y=0; y<height; y++)
        {
            for(int x=0; x<width; x++)
            {
                g.setClip(x, y, 1, 1);
                int dx = x * sourceWidth / width;
                int dy = y * sourceHeight / height;
                g.drawImage(image, x-dx, y-dy, Graphics.LEFT | Graphics.TOP);
            }
        }
        return Image.createImage(newImage);
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
                        String fe = getFileExt(fileName);
                        if (fe != null && parent.file_extensions.get(fe) != null)
                            n ++;
                    }
                    if (n > MAX_PIC_NUM)
                        break;
                }
            } /*catch (IOException ioe) {
                continue;
            } *//*catch (Exception ee) {
                if ("root1/photos/".equals(g))
                    sku = 3;
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
    
    final public boolean testSystem() {
        //jsr75 fileconnection
        if (System.getProperty("microedition.io.file.FileConnection.version") == null) {  
            jsr75support = false;
        }
        //jsr135 MMAPI
        if (System.getProperty("microedition.media.version") == null) {
            mmapisupport = false;
        }
        return (jsr75support || mmapisupport);
    }
    
    final public int random()  {
        Random number = new Random();
        number.setSeed(System.currentTimeMillis());
        return number.nextInt();
    }
    
    final public String getFileExt(String filename) { 
        int dot_pos = filename.lastIndexOf('.');
        if (dot_pos >= 0)
            return filename.toLowerCase().substring(dot_pos + 1);
        return null;
    }
}
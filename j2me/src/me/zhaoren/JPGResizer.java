package me.zhaoren;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import javax.microedition.lcdui.*;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;


public class JPGResizer implements JPEGDecoder.PixelArray{
	private int MAX_W = 120;
	private int MAX_H = 120;
	
	private int des_w ;
	private int des_h ;
	
	private int width;
	private int height;
	private int buf_h;
	private int[] pix_buf;
	private int[] target_pix ;
	JPEGDecoder decoder = null;
	
	public void setPixel(int x, int y, int argb) {
		pix_buf[x+y*width] = argb;
	}

	public void setSize(int width, int height, int buf_h) {
		this.width = width;
		this.height = height;
		this.buf_h = buf_h;
		pix_buf = new int[width*buf_h];		//leave 2 DU height(16 pixels)
		if(this.width<=this.MAX_W && this.height<=this.MAX_H){
			this.des_w = this.width;
			this.des_h = this.height;
		}else if(this.width<=this.MAX_W && this.height>this.MAX_H){
			this.des_h = this.MAX_H;
			this.des_w = (int)((this.width*this.MAX_H)/this.height);
		}else if(this.width>this.MAX_W && this.height<=this.MAX_H){
			this.des_w = this.MAX_W;
			this.des_h = (int)((this.height*this.MAX_W)/this.width);
		}else if(this.width*this.MAX_H >= this.height*this.MAX_W){
			this.des_w = this.MAX_W;
			this.des_h = (int)((this.height*this.MAX_W)/this.width);
		}else if(this.width*this.MAX_H < this.height*this.MAX_W){
			this.des_h = this.MAX_H;
			this.des_w = (int)((this.width*this.MAX_H)/this.height);
		}else{
			System.out.println("What's wrong?!\n Can't be here");
		}
		this.target_pix = new int[this.des_w*this.des_h];
		
	}
	
	//buf_h should be n times of 8 (n=1,2,3,4...)
	public Image resizeItByStream(InputStream in, int max_w, int max_h, int buf_h){
		decoder = new JPEGDecoder();
		Image img  = null;
		this.MAX_H = max_h;
		this.MAX_W = max_w;
		decoder.setBufH(buf_h);
		try{
			int current = decoder.decode_init(in, this);
			algo1(in, current);
			img = Image.createRGBImage(this.target_pix, this.des_w, this.des_h, false);
		}catch(Exception e){
			e.printStackTrace();
		}
		return img;
	}
	
	//buf_h should be n times of 16 (n=1,2,3,4...)
	public Image resizeItByByteArray(byte[] bin, int max_w, int max_h, int buf_h){
//		System.out.println("dec:"+Runtime.getRuntime().totalMemory()+" "+Runtime.getRuntime().freeMemory());
		decoder = new JPEGDecoder();
//		System.out.println("dec1:"+Runtime.getRuntime().totalMemory()+" "+Runtime.getRuntime().freeMemory());
		Image img  = null;
		this.MAX_H = max_h;
		this.MAX_W = max_w;
		decoder.setBufH(buf_h);
		try{
			ByteArrayInputStream in = new ByteArrayInputStream(bin);
			int current = decoder.decode_init(in, this);
//			System.out.println("algo:"+Runtime.getRuntime().totalMemory()+" "+Runtime.getRuntime().freeMemory());
			algo1(in, current);
//			System.out.println("algo1:"+Runtime.getRuntime().totalMemory()+" "+Runtime.getRuntime().freeMemory());
			img = Image.createRGBImage(this.target_pix, this.des_w, this.des_h, false);
		}catch(Exception e){
			e.printStackTrace();
		}
		return img;
	}
	
	//nearest interpolation
	public void algo1(InputStream in, int c)throws Exception{
		int srcX = 0;
		int srcY = 0;
		int lastY=0, current=c, i=0,j=0;
		int desY=0, desX=0;
		int[] temp = new int[1];	// to store remainded bits
		int[] index = new int[1];
		int[] PRED = new int[10];
		temp[0] = 0;
		index[0] = 0;
		for(i=0; i<PRED.length; ++i) PRED[i] = 0;

		int count = 0;
		current = decoder.decodeOneBuf(in, this, current, PRED, temp, index);
		++count;
		for(i=0; i<this.des_h; ++i){
            srcY = (int)(i*this.height/this.des_h);
            while(srcY >= count*this.buf_h){
            	current = decoder.decodeOneBuf(in, this, current, PRED, temp, index);
            	if(current == 0xFFD9)
            		return;
    			++count;
            }
            for(j=0;j<this.des_w; ++j){
                    srcX = (int)(j*this.width/this.des_w);
                    target_pix[i*des_w+j] = pix_buf[srcX + (srcY-(count-1)*this.buf_h)*width];
            }
		}
		
	}
	
}

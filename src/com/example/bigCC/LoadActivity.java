package com.example.bigCC;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.example.bigCC.R;

public class LoadActivity extends Activity{
	  
	      private static final int LOAD_DISPLAY_TIME = 500;
	     
	    /** Called when the activity is first created. */
	     @Override
	     public void onCreate(Bundle savedInstanceState) {
	         super.onCreate(savedInstanceState);
	         
	       //getWindow().setFormat(PixelFormat.RGBA_8888);
	       //getWindow().addFlags(WindowManager.LayoutParams.FLAG_DITHER);
	
	       setContentView(R.layout.loading);
	
	       new Handler().postDelayed(new Runnable() {
	    	   public void run() {
	                 /* Create an Intent that will start the Main WordPress Activity. */
	                 Intent mainIntent = new Intent(LoadActivity.this, MainActivity.class);
	                 LoadActivity.this.startActivity(mainIntent);
	                 LoadActivity.this.finish();
	            }
	        }, LOAD_DISPLAY_TIME); //1500 for release
	
	     }
	
}
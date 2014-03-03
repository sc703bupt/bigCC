package com.example.util;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.KeyEvent;

public class BaseActivity extends Activity {
	
	    private static String Finish_App = "finish";
	    private static String Back_Main = "back";
	    private final int NotMainLay = 1;
	    //默认界面不是主界面
	    private int flag = NotMainLay;
	    private int MainLay = 2;
	    
	    public void onCreate(Bundle savedInstanceState) {
	           super.onCreate(savedInstanceState); 	           
	           // 在当前的activity中注册广播
		       IntentFilter filter = new IntentFilter();
			   filter.addAction(Finish_App);
			   if(flag == NotMainLay){
				   filter.addAction(Back_Main);
			   }			   
			   registerReceiver(this.broadcastReceiver, filter); // 注册			  
	    }	 	    
   
	    @Override
	    protected void onDestroy(){
	    	super.onDestroy();
	    	unregisterReceiver(this.broadcastReceiver);
	    }
	    
	    
	    protected void isMain(){
	    	flag = MainLay;
	    }
	    
        // 广播的内部类，当收到动作时，结束activity
		private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context arg0, Intent intent) {
					if(Finish_App.equals(intent.getAction())){
						finish();						
					}	
					if(Back_Main.equals(intent.getAction())){
						finish();
					}
				}
		};		

		
		public boolean onKeyDown(int keyCode, KeyEvent event) {
		     if (keyCode == KeyEvent.KEYCODE_BACK    //监听返回键是否被按下
		               && event.getRepeatCount() == 0) {
		           event.startTracking();
		           return true;
		     }
		     return false;
		}
		   

		public boolean onKeyLongPress (int keyCode, KeyEvent event) {
		   	if(keyCode == KeyEvent.KEYCODE_BACK   //监听返回键被长时间按下
		   			&& event.isLongPress()){
		   		DialogUtil.finishCPDP(this);      //弹出询问是否退出的对话框
		   		return true;	
		   	}
		   	return false;
		}
}

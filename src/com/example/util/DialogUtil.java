package com.example.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;

public class DialogUtil {
	
	
	private static String Finish_App = "finish";	
	/**
	 * 定义一个显示消息的对话框
	 * @param ctx
	 * @param msg 需要显示的信息
	 * @param closeSelf 该Activity是否允许随之关闭
	 */
	public static void showDialog(final Context ctx,
			String msg, boolean closeSelf){		
		//创建一个AlertDialog.Builder对象
	    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
	    builder.setMessage(msg).setCancelable(false); //将对话框设为不可被back键取消
	    if(closeSelf){
	    	builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//结束当前Activity
	    	    	((Activity)ctx).finish();
				}
	    	});	    		
	    }
	    else{
	    	builder.setPositiveButton("确定", null);
	    }
	    builder.create().show();
	}	

		
	//定义一个显示指定组件的对话框
	public static void showDialog(Context ctx, View view){
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setView(view).setCancelable(false).setPositiveButton("确定", null);
		builder.create().show();		
	}
	
	//定义询问是否退出程序的对话框
	public static void finishCPDP(final Context ctx){          //弹出对话框判断是否退出程序	   
		AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
		builder.setMessage("确定要退出程序吗?")
		       .setCancelable(false)   //将对话框设为不可被back键取消
		       .setPositiveButton("是", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   Intent intent = new Intent();
					   intent.setAction(Finish_App); //说明动作
					   ctx.sendBroadcast(intent);    //发送广播						    
		           }
		       })
		       .setNegativeButton("否", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                dialog.cancel();
		           }
		       }).show();
   } 
}

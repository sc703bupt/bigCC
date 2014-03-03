package com.example.bigCC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.bigCC.R;
import com.example.util.BaseActivity;

public class UserCenter extends BaseActivity implements OnClickListener{
	
	private TextView settingButton,ridingRecordButton,logoutButton;
	View views;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		
        views = LayoutInflater.from(getParent()).inflate(R.layout.user, null); 
        setContentView(views);
        
		settingButton = (TextView)views.findViewById(R.id.settingbtn);
		ridingRecordButton = (TextView)views.findViewById(R.id.ridingrecordbtn);
		logoutButton = (TextView)views.findViewById(R.id.logoutbtn);
		 
		settingButton.setOnClickListener(this);
		ridingRecordButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
    	{
		   case R.id.settingbtn:
   				Intent intent1 = new Intent(UserCenter.this,Setting.class);
   				startActivity(intent1);
			   break;
    	   case R.id.ridingrecordbtn:
    			Intent intent2 = new Intent(UserCenter.this,ridingRecord.class);
    			startActivity(intent2);
      	       break;		
           case R.id.logoutbtn:
        	   break;
    	}	
	}
}
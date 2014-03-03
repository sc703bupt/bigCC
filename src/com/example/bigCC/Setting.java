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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.bigCC.R;
import com.example.util.BaseActivity;

public class Setting extends BaseActivity implements OnClickListener,OnCheckedChangeListener{
	
	TextView settingBackButton;
	View views;
	public static boolean isGPSRecordChecked = false;
	public static boolean isSceneryChecked = true;
	public static boolean isFastestChecked = false;
	
	public static String startText = "315 Boulevard Rene-Levesque Est,Montreal";
	public static String endText = "28 st Paul E,Montreal";
	public static String latlngForSearchStart = "45.511641973167514,-73.55912055820227";
	public static String latlngForSearchEnd = "45.506424911617785,-73.55248209089041";
//	public static String latlngForSearchStart = "45.51197, -73.54933";
//	public static String latlngForSearchEnd = "45.50544, -73.55662";

	public static int startFlag = 2;
	public static int endFlag = 2;
	
	CheckBox GPSRecrodCheckBox,sceneryCheckBox,fastestCheckBox;
	
	public static boolean isSettingRunning = false;
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.setting);
		
		settingBackButton = (TextView)findViewById(R.id.settingBackButton);
		settingBackButton.setOnClickListener(this);
		
		GPSRecrodCheckBox = (CheckBox)findViewById(R.id.enableGPSRecordcheckBox);
		GPSRecrodCheckBox.setOnCheckedChangeListener(this);
		GPSRecrodCheckBox.setChecked(isGPSRecordChecked);
		
		sceneryCheckBox = (CheckBox)findViewById(R.id.sceneryRouteCheckBox);
		sceneryCheckBox.setOnCheckedChangeListener(this);
		sceneryCheckBox.setChecked(isSceneryChecked);
		
		fastestCheckBox = (CheckBox)findViewById(R.id.fastestRouteCheckBox);
		fastestCheckBox.setOnCheckedChangeListener(this);
		fastestCheckBox.setChecked(isFastestChecked);
		
		isSettingRunning = true;
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
    	{
		   case R.id.settingBackButton:
			   finish();
			   break;
    	}	
	}

	@Override
	public void onCheckedChanged(CompoundButton cb, boolean ischecked) {
		// TODO Auto-generated method stub
		switch(cb.getId()){
			case R.id.enableGPSRecordcheckBox:
				isGPSRecordChecked = ischecked;
				break;
			case R.id.sceneryRouteCheckBox:
				isSceneryChecked = ischecked;
				if(ischecked == true&&FindRoute1.isFindRoute1Running == true){
					FindRoute1.start.setText(startText);
					FindRoute1.end.setText(endText);
					FindRoute1.latlngForSearchStart = latlngForSearchStart;
					FindRoute1.latlngForSearchEnd = latlngForSearchEnd;
					FindRoute1.startFlag = 2;
					FindRoute1.endFlag = 2;
				}
				if(ischecked == false&&FindRoute1.isFindRoute1Running == true){
					FindRoute1.start.setText("");
					FindRoute1.end.setText("");
					FindRoute1.latlngForSearchStart = "";
					FindRoute1.latlngForSearchEnd = "";
					FindRoute1.startFlag = 3;
					FindRoute1.endFlag = 3;
				}
				break;
			case R.id.fastestRouteCheckBox:
				isFastestChecked = ischecked;
				break;
			default:
				break;
		}
	}
}
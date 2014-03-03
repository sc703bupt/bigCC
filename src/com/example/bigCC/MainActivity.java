package com.example.bigCC;

import com.example.bigCC.R;

import android.os.Bundle;
import android.app.TabActivity;
import android.content.Intent;

import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TabHost;

public class MainActivity extends TabActivity implements OnCheckedChangeListener{
	    private RadioGroup mainTab;
	    private TabHost tabhost;
	    private Intent iSearch;
	    private Intent iNearby;
	    private Intent iRoute;
	    private Intent iUser;

	    
	    @Override
	    public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.main);
	        //在有输入法出现时，底部tab栏不会随之弹起
	        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
	        mainTab=(RadioGroup)findViewById(R.id.main_tab);
	        mainTab.setOnCheckedChangeListener(this);
	        tabhost = getTabHost();

	        iNearby = new Intent(this, Nearby.class);
	        tabhost.addTab(tabhost.newTabSpec("iNearby")
	                .setIndicator(getResources().getString(R.string.nearby), getResources().getDrawable(R.drawable.locate_blue))
	                .setContent(iNearby));
	        
	        iSearch = new Intent(this, Search1.class);
	        tabhost.addTab(tabhost.newTabSpec("iSearch")
	                .setIndicator(getResources().getString(R.string.search), getResources().getDrawable(R.drawable.search_gray))
	                .setContent(iSearch));
	        
	        iRoute = new Intent(this, FindRoute1.class);
	        tabhost.addTab(tabhost.newTabSpec("iRoute")
	                .setIndicator(getResources().getString(R.string.route), getResources().getDrawable(R.drawable.route_gray))
	                .setContent(iRoute));	        
	       
	        iUser = new Intent(this,UserCenter.class);
	        tabhost.addTab(tabhost.newTabSpec("iUser")
	                .setIndicator(getResources().getString(R.string.user), getResources().getDrawable(R.drawable.more_gray))
	                .setContent(iUser));
	    }
	   

	    @Override
	    public void onCheckedChanged(RadioGroup group, int checkedId) {
	        switch(checkedId){
	        case R.id.radio_button0:
	            this.tabhost.setCurrentTabByTag("iNearby");
	            break;
	        case R.id.radio_button1:
	            this.tabhost.setCurrentTabByTag("iSearch");
	            break;
	        case R.id.radio_button2:
	            this.tabhost.setCurrentTabByTag("iRoute");
	            break;
	        case R.id.radio_button3:
	            this.tabhost.setCurrentTabByTag("iUser");
	            break;
	        }
	    }  
 }
	


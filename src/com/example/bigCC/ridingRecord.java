package com.example.bigCC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.bigCC.R;
import com.example.util.BaseActivity;

public class ridingRecord extends BaseActivity implements OnClickListener{
	
	private ListView ridingRecordList;
	private TextView ridingRecordBackButton;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.ridingrecord);
		
		ridingRecordBackButton = (TextView)findViewById(R.id.ridingRecordBackButton);
		ridingRecordBackButton.setOnClickListener(this);
		
		ridingRecordList = (ListView)findViewById(R.id.ridingRecordList);
		SimpleAdapter adapter = new SimpleAdapter(this,getData(),R.layout.ridingrecorditem, 
	            new String[]{"date","startStation","endStation","totalMiles","averageSpeed","timeCost"}, 
	            new int[]{R.id.ridingRecordDate,R.id.startStationName,R.id.endStationName,R.id.totalMiles,R.id.averageSpeed,R.id.timeCost}); 
	    ridingRecordList.setAdapter(adapter);
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
    	{
		   case R.id.ridingRecordBackButton:
			   finish();
			   break;
    	}	
	} 
	
	private List<Map<String, Object>> getData() { 
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
  
        Map<String, Object> map = new HashMap<String, Object>(); 
        map.put("date", "2013年6月28日"); 
        map.put("startStation", "Notre-Dame / Saint-Gabrie"); 
        map.put("endStation", "Saint-Urbain / René-Lévesque");
        map.put("totalMiles", "12公里");
        map.put("averageSpeed", "24公里/小时");
        map.put("timeCost", "30分钟");
        list.add(map); 
  
        map = new HashMap<String, Object>(); 
        map.put("date", "2013年6月28日"); 
        map.put("startStation", "Terasse Mercure / Fulum"); 
        map.put("endStation", "Notre-Dame / Saint-Gabrie");
        map.put("totalMiles", "30公里");
        map.put("averageSpeed", "30公里/小时");
        map.put("timeCost", "1小时"); 
        list.add(map); 
         
        map = new HashMap<String, Object>(); 
        map.put("date", "2013年6月29日"); 
        map.put("startStation", "Saint-Urbain / René-Lévesque"); 
        map.put("endStation", "Terasse Mercure / Fulum");
        map.put("totalMiles", "10公里");
        map.put("averageSpeed", "30公里/小时");
        map.put("timeCost", "20分钟"); 
        list.add(map); 
        
          
        map = new HashMap<String, Object>(); 
        map.put("date", "2013年6月30日"); 
        map.put("startStation", "Peel / ave des Canadiens de Montréal"); 
        map.put("endStation", "Poupart / Sainte-Catherine");
        map.put("totalMiles", "60公里");
        map.put("averageSpeed", "30公里/小时");
        map.put("timeCost", "2小时");
        list.add(map); 
        
        
       map = new HashMap<String, Object>(); 
       map.put("date", "2013年7月1日"); 
       map.put("startStation", "Poupart / Sainte-Catherine"); 
       map.put("endStation", "Métro Atwater");
       map.put("totalMiles", "5公里");
       map.put("averageSpeed", "20公里/小时");
       map.put("timeCost", "15分钟"); 
       list.add(map); 
       
         
       map = new HashMap<String, Object>(); 
       map.put("date", "2013年7月3日"); 
       map.put("startStation", "Métro Atwater"); 
       map.put("endStation", "Peel / ave des Canadiens de Montréal");
       map.put("totalMiles", "50公里");
       map.put("averageSpeed", "25公里/小时");
       map.put("timeCost", "2小时"); 
       list.add(map); 
       
       map = new HashMap<String, Object>(); 
       map.put("date", "2013年7月5日"); 
       map.put("startStation", "des Érables / Mont-Royal"); 
       map.put("endStation", "des Écores / Jean-Talon");
       map.put("totalMiles", "25公里");
       map.put("averageSpeed", "20公里/小时");
       map.put("timeCost", "1小时15分钟");
       list.add(map); 
              
       return list; 
    }
}
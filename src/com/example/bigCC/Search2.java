package com.example.bigCC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bigCC.R;
import com.example.util.BaseActivity;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class Search2 extends BaseActivity implements OnClickListener,OnItemSelectedListener{

	private ListView lvSearch;
	private String place,time;
	private Spinner sprank;
	private TextView search2BackButton,tvstation,tvrent,tvleft,tvdistance;

	private static List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(); 
	private static Map<String, Object> map;
	
	//进度条
	private ProgressDialog progressDialog = null;
	
	private static final String Bike_Most = "排序方式：车辆最多";
	private static final String Lock_Most = "排序方式：空位最多";
	private static final String Nearest = "排序方式：距离最近";
	
	private static final int Distance = 0;
	private static final int Msg_No_Bike = 4;
	private static final int Msg_Not_Get = 5;
	private static final int Msg_Has_Got = 6;
	
	public static final double SEARCH_RADIUS = 0.3;//单位为km
	
	public static double placeLat = 0;
	public static double placeLng = 0;
	public static ArrayList<DisplayUnit> displayUnitList = new ArrayList<DisplayUnit>();
	
	public void onCreate(Bundle savedInstanceState) {
		
		 //防止ksoap2调用网络时异常
		 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads().detectDiskWrites().detectNetwork()
        .penaltyLog().build());
		 StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
        .build());
		
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search2 );        
        Intent intent = getIntent();
        place = intent.getStringExtra("place");
		time = intent.getStringExtra("spTime");
		
		search2BackButton = (TextView)findViewById(R.id.search2BackButton);
		search2BackButton.setOnClickListener(this);
		lvSearch = (ListView)findViewById(R.id.lv_search);
        tvstation = (TextView)findViewById(R.id.tv_station);
        tvdistance = (TextView)findViewById(R.id.tv_distance);     
        
        sprank = (Spinner)findViewById(R.id.Splook);//spinner
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
             R.array.look, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        sprank.setAdapter(adapter);
        sprank.setOnItemSelectedListener(this);
		
		//与网络连接及处理数据过程中显示进度条
	    progressDialog = ProgressDialog.show(Search2.this,
							"请稍等", "获取数据中...", true);
	    //按下返回键可以关闭进度条
		progressDialog.setCancelable(true);
		//refreshview();
		init();
	}
	
	public void init(){
		try{
				list.clear();
			   //发送请求,并判定返回结果
				String request = Search2.generateRequset(place,time);
				//Log.d("request",request);
				String response = getRecommendStationViaWebservice(request);
				//Log.d("response",response);
				displayUnitList = parseResponse(response);
				//Log.d("displayUnitList",displayUnitList.toString());
			    if(displayUnitList == null){				    	
					Search2.this.hd.sendEmptyMessage(Msg_Not_Get);
				}
			    //无车辆的返回处理
			    else if(displayUnitList.size()==0){
			    	Search2.this.hd.sendEmptyMessage(Msg_No_Bike);
			    }
			    //正常的处理
				else{
					Search2.this.hd.sendEmptyMessage(Msg_Has_Got);		      				        	   
				}
			}
		    catch(Exception e){	
		       Search2.this.hd.sendEmptyMessage(Msg_Not_Get);			       
			  //e.printStackTrace();
		    }
	}
	
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1,
			int arg2, long arg3) {
		// TODO Auto-generated method stub
		switch(arg2){
		case 0:
			refresh2(Nearest);
			break;
		case 1:
			refresh2(Bike_Most);
			break;
		case 2:
			refresh2(Lock_Most);
			break;
			default:break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId())
    	{
		   case R.id.search2BackButton:
			   finish();
			   break;
    	}	
	}
	
	//选择不同排序方式后触发的响应函数
	private void refresh2(String mode) {
		
		list.clear();
		int[] sortArray = new int[displayUnitList.size()];
		for(int i = 0;i<=sortArray.length-1;i++){
			sortArray[i] = i;
		}
		if(mode.equals(Bike_Most)){
			int temp;
			for(int i = 0;i<=sortArray.length-1;i++){
				for(int j = 0;j<=sortArray.length-1-i-1;j++){
					if(displayUnitList.get(sortArray[j]).availableBikesFlag<displayUnitList.get(sortArray[j+1]).availableBikesFlag){
						temp = sortArray[j+1];
						sortArray[j+1] = sortArray[j];
						sortArray[j] = temp;
					}
				}
			}
			for(int i:sortArray){
				Search2.addmap(displayUnitList.get(i).stationName, displayUnitList.get(i).availableBikesFlag, displayUnitList.get(i).emptyDocksFlag, displayUnitList.get(i).distance+"米");
			}
		}
		else if(mode.equals(Lock_Most)){
			int temp;
			for(int i = 0;i<=sortArray.length-1;i++){
				for(int j = 0;j<=sortArray.length-1-i-1;j++){
					if(displayUnitList.get(sortArray[j]).emptyDocksFlag<displayUnitList.get(sortArray[j+1]).emptyDocksFlag){
						temp = sortArray[j+1];
						sortArray[j+1] = sortArray[j];
						sortArray[j] = temp;
					}
				}
			}
			for(int i:sortArray){
				Search2.addmap(displayUnitList.get(i).stationName, displayUnitList.get(i).availableBikesFlag, displayUnitList.get(i).emptyDocksFlag, displayUnitList.get(i).distance+"米");
			}
		}
		else if(mode.equals(Nearest)){
			int temp;
			for(int i = 0;i<=sortArray.length-1;i++){
				for(int j = 0;j<=sortArray.length-1-i-1;j++){
					if(displayUnitList.get(sortArray[j]).distance>displayUnitList.get(sortArray[j+1]).distance){
						temp = sortArray[j+1];
						sortArray[j+1] = sortArray[j];
						sortArray[j] = temp;
					}
				}
			}
			StringBuilder sb = new StringBuilder();
			for(int i:sortArray){
				Search2.addmap(displayUnitList.get(i).stationName, displayUnitList.get(i).availableBikesFlag, displayUnitList.get(i).emptyDocksFlag, displayUnitList.get(i).distance+"米");
				sb.append(displayUnitList.get(i).distance+"米");
			}
		}
		SimpleAdapter sadapter = new SimpleAdapter(Search2.this,getData(),R.layout.search_result, 
                new String[]{"stationName","availableBikesFlag","emptyDocksFlag","distance"}, 
                new int[]{R.id.tv_station,R.id.iv_bike,R.id.iv_lock,R.id.tv_distance});
		lvSearch.setAdapter(sadapter);		
	}

	private static void addmap(String stationName,int availableBikesFlag,
			int emptyDocksFlag,String distance){
		map = new HashMap<String, Object>(); 
        map.put("stationName", stationName); 
        
        if(availableBikesFlag == 2){
        	map.put("availableBikesFlag", R.drawable.more); 
        }
        else if(availableBikesFlag == 1){
        	map.put("availableBikesFlag", R.drawable.few); 
        }
        else map.put("availableBikesFlag", R.drawable.none);
        
        if(emptyDocksFlag == 2){
        	map.put("emptyDocksFlag", R.drawable.more); 
        }
        else if(emptyDocksFlag == 1){
        	map.put("emptyDocksFlag", R.drawable.few); 
        }
        else map.put("emptyDocksFlag", R.drawable.none);        
       
        map.put("distance", distance); 
        list.add(map); 
	}
	
	static class DisplayUnit{
		int stationId;
		String stationName;
		int availableBikesFlag;
		int emptyDocksFlag;
		int distance;//单位为m
		public DisplayUnit(int stationId,String stationName,int availableBikesFlag,int emptyDocksFlag,int distance){
			this.stationId = stationId;
			this.stationName = stationName;
			this.availableBikesFlag = availableBikesFlag;
			this.emptyDocksFlag = emptyDocksFlag;
			this.distance = distance;
		}
		
		public String toString(){
			return "stationId:"+stationId+
					" stationName:"+stationName+
					" availableBikesFlag:"+availableBikesFlag+
					" emptyDocksFlaag:"+emptyDocksFlag+
					" distance:"+distance;
		}
	}
	
	private List<Map<String, Object>> getData(){
		return list;
	}
	
	/**
	 * Handle消息传递
	 * 获取与服务器交互的子线程中发送的消息
	 */
	Handler hd = new Handler(){//生命消息处理器
    	@Override
    	public void handleMessage(Message msg){
    		//获取数据后，关闭进度条
    		progressDialog.dismiss();
    		if(msg.what == Msg_Not_Get){
    			//创建一个Toast提示信息,并设置其持续时间
                Toast toast = Toast.makeText(Search2.this, "无法获取数据 ",Toast.LENGTH_SHORT);                   
                toast.show();
                //finish();
    		}
    		if(msg.what == Msg_No_Bike){
    			//创建一个Toast提示信息,并设置其持续时间
                Toast toast = Toast.makeText(Search2.this, "该地点周围"+SEARCH_RADIUS*1000+"米内无自行车租赁点",Toast.LENGTH_SHORT);                   
                toast.show();               
    		}
    		if(msg.what == Msg_Has_Got){
//				for(DisplayUnit dunit:displayUnitList){
//					addmap(dunit.stationName, dunit.availableBikesFlag, dunit.emptyDocksFlag, (int)(dunit.distance)+"米");
//				}
//    			SimpleAdapter sadapter = new SimpleAdapter(Search2.this,getData(),R.layout.search_result, 
//    	                new String[]{"stationName","availableBikesFlag","emptyDocksFlag","distance"}, 
//    	                new int[]{R.id.tv_station,R.id.iv_bike,R.id.iv_lock,R.id.tv_distance});
//    			lvSearch.setAdapter(sadapter);
    			refresh2(Nearest);
    		}    		
    	}
	};	

//	private void refreshview() {
//		Toast toast = Toast.makeText(Search2.this, "111 ",Toast.LENGTH_SHORT);                   
//        toast.show();
//		new Thread(){
//			 public void run(){			    
//				try{					
//				   //发送请求,并判定返回结果
//					String request = Search2.generateRequset(place, month, day, hour, minute);
//					Log.d("request",request);
//					String response = getRecommendStationViaWebservice(request);
//					Log.d("response",response);
//					displayUnitList = parseResponse(response);
//					Log.d("displayUnitList",displayUnitList.toString());
//				    if(displayUnitList == null){				    	
//						Search2.this.hd.sendEmptyMessage(Msg_Not_Get);
//					}
//				    //无车辆的返回处理
//				    else if(displayUnitList == null){
//				    	
//				    	
//				    	Search2.this.hd.sendEmptyMessage(Msg_No_Bike);
//				    }
//				    //正常的处理
//					else{
//						Search2.this.hd.sendEmptyMessage(Msg_Has_Got);		      				        	   
//					}
//				}
//			    catch(Exception e){	
//			       Search2.this.hd.sendEmptyMessage(Msg_Not_Get);			       
//				   e.printStackTrace();
//			    }	
//			 }
//		}.start();				
//	}


	
	public static String generateRequset(String place,String time){
		StringBuilder request = new StringBuilder();
		if(place == null||time == null){
			return null;
		}
		String[] split = Pattern.compile(",").split(place);
		if(split.length==2){
			placeLat = Double.parseDouble(split[0]);
			placeLng = Double.parseDouble(split[1]);
			ArrayList<Integer> stationIdList = getNearestMarkers(new LatLng(placeLat,placeLng),0.3);//1km
			if(stationIdList.size()==0){
				return "";
			}
			for(Integer id:stationIdList){
				request.append(id+":");
				request.append(time+" ");
			}
			return request.toString().trim();
		}
		return null;
	}
	
	public static ArrayList<DisplayUnit> parseResponse(String reponse){
		ArrayList<DisplayUnit> list = new ArrayList<DisplayUnit>();
		
		if(reponse == null){
			return null;
		}
		if(reponse.length() == 0){
			return list;
		}
		String[] StationSplit = Pattern.compile(" ").split(reponse);
		for(String singleStationSplit:StationSplit){
			String[] IdAndState = Pattern.compile(":").split(singleStationSplit);
			if(IdAndState.length!=2){
				continue;
			}else{
				if(IdAndState[1].length()!=2){
					continue;
				}
				int stationId = Integer.parseInt(IdAndState[0]);
				String stationName = Nearby.markerGroup[stationId].getTitle();
				int availableBikesFlag = IdAndState[1].charAt(0)-'0';
				int emptyDocksFlag = IdAndState[1].charAt(1)-'0';
				int distance = (int) (1000*calculateStraightDistance(new LatLng(placeLat,placeLng),Nearby.markerGroup[stationId].getPosition()));
				DisplayUnit du = new DisplayUnit(stationId,stationName,availableBikesFlag,emptyDocksFlag,distance);
				list.add(du);
			}
		}
		return list;
	}	
	
	//返回最近的marker,radius是范围
			public ArrayList<Integer> getNearestMarkers(Marker marker,double radius){
				ArrayList<Integer> NearestMarkerIdList = new ArrayList<Integer>();
				for(Marker m:Nearby.markerGroup){
					if(m!=null){//markerGroup是稀疏数组
						if(calculateStraightDistance(marker,m)<=radius){
							NearestMarkerIdList.add(Integer.parseInt(m.getSnippet()));
						}
					}
				}
				return  NearestMarkerIdList;
			}
			
			public static ArrayList<Integer> getNearestMarkers(LatLng latlng,double radius){
				ArrayList<Integer> NearestMarkerIdList = new ArrayList<Integer>();
				for(Marker m:Nearby.markerGroup){
					if(m!=null){//markerGroup是稀疏数组
						if(calculateStraightDistance(latlng,m.getPosition())<=radius){
							NearestMarkerIdList.add(Integer.parseInt(m.getSnippet()));
						}
					}
				}
				return  NearestMarkerIdList;
			}
			
	//根据经纬度计算物理距离方法参数
			static final double EARTH_DIAMETER = 2*6378.2;
			static final double PI = 3.1415926;
			static final double RAD_CONVERT = PI/180;
			
			public double calculateStraightDistance(Marker marker1,Marker marker2){
		        double delta_lat, delta_lon;
		        double temp;

		        double lat1 = marker1.getPosition().latitude;
		        double lng1 = marker1.getPosition().longitude;
		        double lat2 = marker2.getPosition().latitude;
		        double lng2 = marker2.getPosition().longitude;

		        // convert degrees to radians
		        lat1 *= RAD_CONVERT;
		        lat2 *= RAD_CONVERT;

		        // find the deltas
		        delta_lat = lat2 - lat1;
		        delta_lon = (lng2 - lng1) * RAD_CONVERT;

		        // Find the great circle distance
		        temp = Math.pow(Math.sin(delta_lat/2),2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(delta_lon/2),2);
		        return EARTH_DIAMETER * Math.atan2(Math.sqrt(temp),Math.sqrt(1-temp));
			}
			
			public static double calculateStraightDistance(LatLng latlng1,LatLng latlng2){
		        double delta_lat, delta_lon;
		        double temp;

		        double lat1 = latlng1.latitude;
		        double lng1 = latlng1.longitude;
		        double lat2 = latlng2.latitude;
		        double lng2 = latlng2.longitude;

		        // convert degrees to radians
		        lat1 *= RAD_CONVERT;
		        lat2 *= RAD_CONVERT;

		        // find the deltas
		        delta_lat = lat2 - lat1;
		        delta_lon = (lng2 - lng1) * RAD_CONVERT;

		        // Find the great circle distance
		        temp = Math.pow(Math.sin(delta_lat/2),2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(delta_lon/2),2);
		        return EARTH_DIAMETER * Math.atan2(Math.sqrt(temp),Math.sqrt(1-temp));
			}
			
			//参数格式:id1:hh_mm id2:hh_mm...idn:hh_mm
			//返回值格式:id1:ae id2:ae ... idn:ae 其中a为0-2的数值，e为0-2的数值
			public static String getRecommendStationViaWebservice(String stationIdAndDate){
				HttpTransportSE transport = new HttpTransportSE(Nearby.url);
				transport.debug = true;
				SoapObject soapObject = new SoapObject(Nearby.namespace, Nearby.methodNameGetRecommendStation);
				soapObject.addProperty("in0",stationIdAndDate);
				SoapSerializationEnvelope envelope=new SoapSerializationEnvelope(SoapEnvelope.VER11);
				envelope.bodyOut =soapObject;
				envelope.dotNet = true;
				envelope.setOutputSoapObject(soapObject);
				try {
					transport.call("", envelope);
				} catch (IOException e) {
					return null;
				} catch (XmlPullParserException e) {
					return null;
				}
				SoapObject response = null;
				try {
					response = (SoapObject)envelope.bodyIn;
				} catch (Exception e) {
					return null;
				}
				if(response == null){
					return null;
				}
				return response.getProperty(0).toString();
			}
			
			public static String convertLatLngToPlaceNameViaWebservice(double lat,double lng){
				HttpTransportSE transport = new HttpTransportSE(Nearby.url);
				transport.debug = true;
				SoapObject soapObject = new SoapObject(Nearby.namespace, Nearby.methodNameConvertLatLngToPlaceName);
				soapObject.addProperty("in0",new Double(lat).toString());
				soapObject.addProperty("in1",new Double(lng).toString());
				SoapSerializationEnvelope envelope=new SoapSerializationEnvelope(SoapEnvelope.VER11);
				envelope.bodyOut =soapObject;
				envelope.dotNet = true;
				envelope.setOutputSoapObject(soapObject);
				try {
					transport.call("", envelope);
				} catch (IOException e) {
					return null;
				} catch (XmlPullParserException e) {
					return null;
				}
				SoapObject response = null;
				
				try {
					response = (SoapObject)envelope.bodyIn;
				} catch (Exception e) {
					return null;
				}
				if(response == null){
					return null;
				}
				return response.getProperty(0).toString();
			}


}

package com.example.bigCC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;


import com.example.bigCC.R;
import com.example.bigCC.chooseSite.MyInfoWindowAdapter;
import com.example.util.BaseActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class FindRoute2 extends BaseActivity implements OnClickListener,OnInfoWindowClickListener{
	
	private GoogleMap mMap = null;
	Marker myLocationMarker = null;
	Marker startPlaceMarker = null;
	Marker endPlaceMarker = null;
	Marker startStationMarker = null;
	Marker endStationMarker = null;
	
	Integer startStationId = null;
	Integer endStationId = null;
	
	public static final double SHOW_RADIUS = 0.3;//单位为km
	public static final double SEARCH_DELTA = 0.1;//单位为km
	public static final double RADIUS_CEIL = 0.6;//单位为km
	public static final double RADIUS_FLOOR = 0.3;//单位为km
	
	private String startPlace,endPlace;
	
	private TextView findrouteBackButton;
	
	public void onCreate(Bundle savedInstanceState){
		
		//防止ksoap2调用网络时异常
		 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
       .detectDiskReads().detectDiskWrites().detectNetwork()
       .penaltyLog().build());
		 StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
       .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
       .build());
		
		super.onCreate(savedInstanceState);		   
		setContentView(R.layout.findroute2);
		
		Intent intent = getIntent();

		startPlace = intent.getStringExtra("startPlace");//源程序此处有错
		endPlace = intent.getStringExtra("endPlace");
		
		Log.d("startPlace",startPlace);
		Log.d("endPlace",endPlace);
		
	    findrouteBackButton = (TextView)findViewById(R.id.findrouteBackButton);
	    findrouteBackButton.setOnClickListener(this);
	    
		if(mMap == null){
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapdisplayroute)).getMap();
			if(mMap != null){
				mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
				mMap.setOnInfoWindowClickListener(this);
				Log.d("1","1");
				initMarkers();
				Log.d("2","2");
				displayAll(startPlace,endPlace);
				myLocationMarker.setVisible(false);
				Log.d("3","3");
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
    	{
		   case R.id.findrouteBackButton:
			   finish();
			   break;
    	}
	}
	
	class MyInfoWindowAdapter implements InfoWindowAdapter{
		@Override
		public View getInfoWindow(Marker marker) {//无论用户点击或是通过showInfoWindow()触发，该方法会首先被调用
			View view = null;
			
			if(marker.equals(myLocationMarker)){
				view =  getLayoutInflater().inflate(R.layout.twotips_info_window, null);
				TextView tip1UI = ((TextView) view.findViewById(R.id.tip1));
				TextView tip2UI = ((TextView) view.findViewById(R.id.tip2));
				tip1UI.setText("我的位置");
				tip2UI.setText(convertLatLngToPlaceNameViaWebservice(marker.getPosition().latitude,marker.getPosition().longitude));
			}else if(marker.equals(startPlaceMarker)||marker.equals(endPlaceMarker)){
				view =  getLayoutInflater().inflate(R.layout.twotips_info_window, null);
				TextView tip1UI = ((TextView) view.findViewById(R.id.tip1));
				TextView tip2UI = ((TextView) view.findViewById(R.id.tip2));
				if(marker.equals(startPlaceMarker)){
					tip1UI.setText("起点");
				}else{
					tip1UI.setText("终点");
				}
				tip2UI.setText(convertLatLngToPlaceNameViaWebservice(marker.getPosition().latitude,marker.getPosition().longitude));
			}else if(marker.equals(startStationMarker)||marker.equals(endStationMarker)){
				view =  getLayoutInflater().inflate(R.layout.custom_info_window, null);
				int stationId;
				if(marker.equals(startStationMarker)){
					stationId = startStationId;
				}else{
					stationId = endStationId;
				}
	            String stationName = Nearby.markerGroup[stationId].getTitle();
	            TextView stationNameUI = ((TextView) view.findViewById(R.id.stationName));
	            if (stationName != null) {
	            	stationNameUI.setText(stationName);
	            } else {
	            	stationNameUI.setText("Unknown");
	            }
	            
	            String availableBikesAndEmtpyDocks = getStationInfoViaWebservice(Integer.parseInt(Nearby.markerGroup[stationId].getSnippet()));
	            TextView availableBikesUI = ((TextView) view.findViewById(R.id.availableBikes));
	            TextView emptyDocksUI = ((TextView) view.findViewById(R.id.emptyDocks));
	            if (availableBikesAndEmtpyDocks != null) {
	            	String[] split = Pattern.compile(" ").split(availableBikesAndEmtpyDocks);
	            	//Toast.makeText(getBaseContext(),availableBikesAndEmtpyDocks, Toast.LENGTH_SHORT).show();
	            	if(split.length==2){
	            		availableBikesUI.setText("-可用车辆数 : "+split[0]);
	            		emptyDocksUI.setText("-可用车位数 : "+split[1]);
	            	}else{
	            		availableBikesUI.setText("-可用车辆数 : 未知");
	                    emptyDocksUI.setText("-可用车位数 : 未知");
	            	}
	            } else {
	                availableBikesUI.setText("-可用车辆数 : 未知");
	                emptyDocksUI.setText("-可用车辆数 : 未知");
	            }
			}else{//sceneryMarker
				view =  getLayoutInflater().inflate(R.layout.twotips_info_window, null);
				TextView tip1UI = ((TextView) view.findViewById(R.id.tip1));
				TextView tip2UI = ((TextView) view.findViewById(R.id.tip2));
				tip1UI.setText(marker.getTitle());
				tip2UI.setText("点击获得该地点详细信息");
			}
			return view;
		}
		@Override
		public View getInfoContents(Marker marker) {//getInfoWindow()返回null后该方法被调用，如果该方法也返回null，则调用默认的窗体
			return null;
		}
	}
	
	@Override
	public void onInfoWindowClick(Marker marker) {
		// TODO Auto-generated method stub
		if(!marker.equals(myLocationMarker)&&!marker.equals(startPlaceMarker)&&!marker.equals(endPlaceMarker)&&!marker.equals(startStationMarker)&&!marker.equals(endStationMarker)){
			Intent intent = new Intent(FindRoute2.this,ShowScenery.class);
			ShowScenery.showNo = Integer.parseInt(marker.getSnippet().trim());
		    startActivity(intent);  
	    }
	}
	
	//显示起点，终点，及其中线路 
	void displayAll(String startPlace,String endPlace){
		boolean isStartPlaceLegal = true;
		boolean isEndPlaceLegal = true;
		if(startPlace == null||endPlace == null){
			Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
			return;
		}
		Log.d("4","4");
		String[] splitStart = Pattern.compile(",").split(startPlace);
		String[] splitEnd = Pattern.compile(",").split(endPlace);
		if(splitStart.length != 2){
			isStartPlaceLegal = false;
		}
		if(splitEnd.length != 2){
			isEndPlaceLegal = false;
		}
		//Toast.makeText(getBaseContext(),isStartPlaceLegal+"|||"+isEndPlaceLegal, Toast.LENGTH_SHORT).show();
		Log.d("5","5");
		if(isStartPlaceLegal == false||isEndPlaceLegal == false){
			Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
			return;
		}else{//合法			
			LatLng startPlaceLatLng = new LatLng(new Double(splitStart[0]),new Double(splitStart[1]));
			LatLng endPlaceLatLng = new LatLng(new Double(splitEnd[0]),new Double(splitEnd[1]));
			startPlaceMarker = mMap.addMarker(new MarkerOptions().position(startPlaceLatLng)
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker)));
			endPlaceMarker = mMap.addMarker(new MarkerOptions().position(endPlaceLatLng)
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.end_marker)));
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(startPlaceLatLng,15),2000,null);
			Log.d("6","6");
			if(calculateStraightDistance(startPlaceLatLng, endPlaceLatLng)<RADIUS_FLOOR){//如果起点和终点的距离过近，则推荐步行前往
				//mode == 0 步行，mode == 1 自行车,mode == 2 自驾 ，mode==3 公交
				String route = getRouteViaWebservice(startPlaceLatLng, endPlaceLatLng,"0");
				if(route == null){
					Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
				}else{
					drawPolyline(route);
					Toast.makeText(getBaseContext(),"两地距离小于"+RADIUS_FLOOR*1000+"米，建议步行前往", Toast.LENGTH_SHORT).show();
				}
			}else{
				Log.d("7","7");
				boolean isFindStart = true;
				boolean isFindEnd = true;
				double tempRadius = RADIUS_FLOOR;
				ArrayList<Integer> startStationList = getNearestMarkers(startPlaceMarker, tempRadius);
				while(startStationList.isEmpty()){
					tempRadius += SEARCH_DELTA;
					if(tempRadius>RADIUS_CEIL){
						isFindStart = false;
					}
					startStationList = getNearestMarkers(startPlaceMarker, tempRadius);
				}
				
				tempRadius = RADIUS_FLOOR;
				ArrayList<Integer> endStationList = getNearestMarkers(endPlaceMarker, tempRadius);
				Log.d("8","8");
				if(isFindStart == true){
					while(startStationList.isEmpty()){
						tempRadius += SEARCH_DELTA;
						if(tempRadius>RADIUS_CEIL){
							isFindEnd = false;
						}
						startStationList = getNearestMarkers(endPlaceMarker, tempRadius);
					}
				}
				Log.d("9","9");
				if(isFindStart!=true){//起点附近RADIUS_CEIL内无车站，建议公交
					String route = getRouteViaWebservice(startPlaceLatLng, endPlaceLatLng,"3");
					if(route == null){
						Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
					}else{
						drawPolyline(route);
						Toast.makeText(getBaseContext(),"起点"+RADIUS_CEIL*1000+"米无自行车站，建议搭乘公交前往", Toast.LENGTH_SHORT).show();
					}
				}
				Log.d("10","10");
				if(isFindEnd!=true){//终点附近RADIUS_CEIL内无车站，建议公交
					String route = getRouteViaWebservice(startPlaceLatLng, endPlaceLatLng,"3");
					if(route == null){
						Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
					}else{
						drawPolyline(route);
						Toast.makeText(getBaseContext(),"终点"+RADIUS_CEIL*1000+"米无自行车站，建议搭乘公交前往", Toast.LENGTH_SHORT).show();
					}
				}
				Log.d("11","11");
				if(isFindStart&&isFindEnd){//起点和终点附近均存在车站
					startStationId = startStationList.get(0);
					//webservice存在问题，暂时禁用
//					for(Integer id:startStationList){//找出可用车辆最多的站
//						startStationId = getAvailablebikesViaWebservice(id)>getAvailablebikesViaWebservice(startStationId)?id:startStationId;
//					}
					Log.d("12","12");
					endStationId = endStationList.get(0);
//					for(Integer id:endStationList){//找出可用车位最多的站
//						endStationId = getEmptyDocksViaWebservice(id)>getEmptyDocksViaWebservice(endStationId)?id:endStationId;
//					}
//					Log.d("13","13");
					if(startStationId.equals(endStationId)){//起点和终点车站相同，推荐步行路线
						String route = getRouteViaWebservice(startPlaceLatLng, endPlaceLatLng,"0");
						if(route == null){
							Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
						}else{
							drawPolyline(route);
							Toast.makeText(getBaseContext(),"两地距离较近，建议步行前往", Toast.LENGTH_SHORT).show();
						}
					}
					Log.d("14","14");
					startStationMarker = mMap.addMarker(new MarkerOptions().position(Nearby.markerGroup[startStationId].getPosition())
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					endStationMarker = mMap.addMarker(new MarkerOptions().position(Nearby.markerGroup[endStationId].getPosition())
							.icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					Log.d("15","15");
					String startPlaceToStartStationPoints = getRouteViaWebservice(startPlaceLatLng, startStationMarker.getPosition(), "0");
					String startStationToEndStationPoints = getRouteViaWebservice(startStationMarker.getPosition(),endStationMarker.getPosition(), "1");
					String endStationToEndPlacePoints = getRouteViaWebservice(endStationMarker.getPosition(), endPlaceLatLng, "0");
					Log.d("16","16");
					if(startPlaceToStartStationPoints == null||startStationToEndStationPoints == null||endStationToEndPlacePoints == null){
						Toast.makeText(getBaseContext(),"获取路线失败，请检查网络后重试", Toast.LENGTH_SHORT).show();
					}else{
						String totalRoutes = startPlaceToStartStationPoints+" "+startStationToEndStationPoints+" "+endStationToEndPlacePoints;
						drawPolyline(totalRoutes);
					}
					
					//Setting.isSettingRunning == true && 
					if(Setting.isSceneryChecked == true){//观光路线sample
						Marker sceneryMarker1 = mMap.addMarker(new MarkerOptions().position(ShowScenery.place1)
								.snippet("1").title("Parc Ville-de-la-Fleche").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
						Marker sceneryMarker2 = mMap.addMarker(new MarkerOptions().position(ShowScenery.place2)
								.snippet("2").title("Sir George-Etienne Cartier National Historic Site").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
						Marker sceneryMarker3 = mMap.addMarker(new MarkerOptions().position(ShowScenery.place3)
								.snippet("3").title("Marguerite Bourgeoys Museum").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
						Marker sceneryMarker4 = mMap.addMarker(new MarkerOptions().position(ShowScenery.place4)
								.snippet("4").title("Auberge du Vieux-Port").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
						Marker sceneryMarker5 = mMap.addMarker(new MarkerOptions().position(ShowScenery.place5)
								.snippet("5").title("Durga Sovenirs").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
//						Marker sceneryMarker6 = mMap.addMarker(new MarkerOptions().position(ShowScenery.place6)
//								.snippet("6").title("Universite du Quebec").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));	
//						Marker sceneryMarker1 = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50988, -73.55173))
//						.snippet("1").title("Parc Ville-de-la-Fleche").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
//				Marker sceneryMarker2 = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50793, -73.55308))
//						.snippet("2").title("Sir George-Etienne Cartier National Historic Site").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
//				Marker sceneryMarker3 = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50740, -73.54888))
//						.snippet("3").title("Marguerite Bourgeoys Museum").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
//				Marker sceneryMarker4 = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50641, -73.55280))
//						.snippet("4").title("Auberge du Vieux-Port").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
//				Marker sceneryMarker5 = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50740, -73.54888))
//						.snippet("5").title("Durga Sovenirs").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));
//				Marker sceneryMarker6 = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50502, -73.55746))
//						.snippet("6").title("Universite du Quebec").icon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot_36x43)));	
						return;
					}
				}
			}
		}
	}
	
	public Integer getAvailablebikesViaWebservice(int stationId){
		String returnValue = getStationInfoViaWebservice(stationId);
		if(returnValue == null){
			return -1;
		}
		String[] split = Pattern.compile(" ").split(returnValue);
		if(split.length != 2){
			return 0;
		}else{
			return Integer.parseInt(split[0]);
		}
	}
	
	public Integer getEmptyDocksViaWebservice(int stationId){
		String returnValue = getStationInfoViaWebservice(stationId);
		if(returnValue == null){
			return -1;
		}
		String[] split = Pattern.compile(" ").split(returnValue);
		if(split.length != 2){
			return 0;
		}else{
			return Integer.parseInt(split[1]);
		}
	}
	
	public void drawPolyline(String route){
		ArrayList<LatLng> pointsList = new ArrayList<LatLng>();
		String[] split = Pattern.compile(" ").split(route);;
		String[] latlng = null;
		for(int i = 0;i<=split.length-1;i++){
			latlng = Pattern.compile(",").split(split[i]);
			if(latlng.length==2){
				pointsList.add(new LatLng(Double.parseDouble(latlng[0]),Double.parseDouble(latlng[1])));
			}
			else{}
		}
		mMap.addPolyline(new PolylineOptions().addAll(pointsList).width(5).color(Color.RED));
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
			
			//overload
			public double calculateStraightDistance(LatLng latlng1,LatLng latlng2){
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

	//预加载所有marker
	public boolean initMarkers(){
		myLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(45.511641973167514,-73.55912055820227))
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.mylocationmarkericon)));
		return true;
	}
			
			public String getStationInfoViaWebservice(int markerId){
				HttpTransportSE transport = new HttpTransportSE(Nearby.url);
				transport.debug = true;
				SoapObject soapObject = new SoapObject(Nearby.namespace, Nearby.methodNameGetStationInfo);
				soapObject.addProperty("in0",markerId); 
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
			
			public String convertLatLngToPlaceNameViaWebservice(double lat,double lng){
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
			

			public String convertPlaceNameToLatLngViaWebservice(String placeName){
				HttpTransportSE transport = new HttpTransportSE(Nearby.url);
				transport.debug = true;
				SoapObject soapObject = new SoapObject(Nearby.namespace, Nearby.methodNameConvertPlaceNameToLatLng);
				soapObject.addProperty("in0",placeName);
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
			
			//mode == 0 步行，mode == 1 自行车,mode == 2 自驾 ，mode==3 公交
			public String getRouteViaWebservice(LatLng startPoint,LatLng endPoint,String mode){
				HttpTransportSE transport = new HttpTransportSE(Nearby.url);
				transport.debug = true;
				SoapObject soapObject = new SoapObject(Nearby.namespace, Nearby.methodNameGetRoute);
				soapObject.addProperty("in0",new Double(startPoint.latitude).toString());
				soapObject.addProperty("in1",new Double(startPoint.longitude).toString());
				soapObject.addProperty("in2",new Double(endPoint.latitude).toString());
				soapObject.addProperty("in3",new Double(endPoint.longitude).toString());
				soapObject.addProperty("in4",mode);
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

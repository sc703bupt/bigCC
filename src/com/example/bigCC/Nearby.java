package com.example.bigCC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bigCC.R;
import com.example.util.BaseActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class Nearby extends BaseActivity implements OnMarkerClickListener,OnMarkerDragListener{
	    
		private GoogleMap mMap = null;
		public static Marker[] markerGroup = new Marker[453];//车站位置marker,只用1-452的稀疏数组,其中某些元素为空，遍历该数组时注意判空
		public static Marker myLocationMarker = null;//当前位置marker
		ArrayList<Integer> lastShowNearestMarkerIdList = new ArrayList<Integer>();
		static final double SHOW_RADIUS = 0.3;//单位为km
		Circle circle = null;
		Polyline routeUI = null;
		Polyline routeUIlast = null;

		@Override
	    public void onCreate(Bundle savedInstanceState) {
	    	
			 //防止ksoap2调用网络时异常
			 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
	         .detectDiskReads().detectDiskWrites().detectNetwork()
	         .penaltyLog().build());
			 StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
	         .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
	         .build());
	    	
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.nearby);
	        
			if(mMap == null){
				mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapnearby)).getMap();
				if(mMap != null){
					mMap.setOnMarkerClickListener(this);
					mMap.setOnMarkerDragListener(this);
					mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
					initMarkers();
					initCircle(myLocationMarker.getPosition(),SHOW_RADIUS);
				}
			}
	    }
	    
		class MyInfoWindowAdapter implements InfoWindowAdapter{
			@Override
			public View getInfoWindow(Marker marker) {//无论用户点击或是通过showInfoWindow()触发，该方法会首先被调用
				//如果点击的用户位置的marker则直接返回
				if(marker.equals(myLocationMarker)){
					String placeName = convertLatLngToPlaceNameViaWebservice(marker.getPosition().latitude,marker.getPosition().longitude);
					if(placeName == null){
						marker.setTitle("未知");
					}else{
						marker.setTitle(placeName);
					}
					return null;
				}
				
				//绘制所在位置到所选车站的路线
				//drawPolyline(getRouteViaWebservice(myLocationMarker.getPosition(), marker.getPosition(),"0"));//mode==0为步行
				
				View v =  getLayoutInflater().inflate(R.layout.custom_info_window, null);
	            String stationName = marker.getTitle();
	            TextView stationNameUI = ((TextView) v.findViewById(R.id.stationName));
	            if (stationName != null) {
	            	stationNameUI.setText(stationName);
	            } else {
	            	stationNameUI.setText("未知");
	            }
	            
	            //调用webservice获得availableBikesAndEmtpyDocks，初始化时snippet存储stationId
	            String availableBikesAndEmtpyDocks = getStationInfoViaWebservice(Integer.parseInt(marker.getSnippet()));
	            TextView availableBikesUI = ((TextView) v.findViewById(R.id.availableBikes));
	            TextView emptyDocksUI = ((TextView) v.findViewById(R.id.emptyDocks));
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
	                emptyDocksUI.setText("-可用车位数 : 未知");
	            }
				return v;
			}
			
			@Override
			public View getInfoContents(Marker marker) {//getInfoWindow()返回null后该方法被调用，如果该方法也返回null，则调用默认的窗体
				return null;
			}
		}
		
		//显示指定的marker
		public void showMarkers(ArrayList<Integer> markerList){
			for(Integer i:markerList){
				markerGroup[i].setVisible(true);
			}
		}
		
		//隐藏指定的marker
		public void hideMarkers(ArrayList<Integer> markerList){
			for(Integer i:markerList){
				markerGroup[i].setVisible(false);
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
			if(routeUIlast!=null){
				routeUIlast.remove();
			}
			routeUI = mMap.addPolyline(new PolylineOptions().addAll(pointsList).width(5).color(Color.RED));
			routeUIlast = routeUI;
		}
		
		//初始化范围示意圆
		public boolean initCircle(LatLng latlng,double r){//传入半径单位为km	
			circle = mMap.addCircle(new CircleOptions().center(latlng).radius(r*1000)
					.strokeWidth(2).strokeColor(0xB4EE695B).fillColor(0x64F6C3C1));
			return true;
		}
		
		//刷新范围示意圆
		public void refreshCircle(LatLng latlng,double r){//传入半径单位为km
			circle.setCenter(latlng);
			circle.setRadius(r*1000);
		}
		
		//返回最近的marker,radius是范围
		public ArrayList<Integer> getNearestMarkers(Marker marker,double radius){
			ArrayList<Integer> NearestMarkerIdList = new ArrayList<Integer>();
			for(Marker m:markerGroup){
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
		
		//WebService参数设置
		public static final String namespace = "http://bixiapi.bixigo";
		//public static final String url = "http://10.108.122.242:8081/axis/services/IBixi?wsdl";
		public static final String url = "http://192.168.231.1:8081/axis/services/IBixi?wsdl";
		public static final String methodNameGetStationInfo = "getStationInfo";
		public static final String methodNameConvertLatLngToPlaceName = "convertLatLngToPlaceName";
		public static final String methodNameConvertPlaceNameToLatLng = "converPlaceNameToLatLng";
		public static final String methodNameGetRoute = "getRoute";
		public static final String methodNameGetRecommendStation = "getRecommendStation";
		public static final String SOAP_ACTION ="";
		
		public String getStationInfoViaWebservice(int markerId){
			HttpTransportSE transport = new HttpTransportSE(url);
			transport.debug = true;
			SoapObject soapObject = new SoapObject(namespace, methodNameGetStationInfo);
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
			HttpTransportSE transport = new HttpTransportSE(url);
			transport.debug = true;
			SoapObject soapObject = new SoapObject(namespace, methodNameConvertLatLngToPlaceName);
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
			HttpTransportSE transport = new HttpTransportSE(url);
			transport.debug = true;
			SoapObject soapObject = new SoapObject(namespace, methodNameConvertPlaceNameToLatLng);
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
			HttpTransportSE transport = new HttpTransportSE(url);
			transport.debug = true;
			SoapObject soapObject = new SoapObject(namespace, methodNameGetRoute);
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
		
		@Override
		public void onMarkerDrag(Marker arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onMarkerDragEnd(Marker marker) {
			// TODO Auto-generated method stub
			hideMarkers(lastShowNearestMarkerIdList);//隐藏之前的最近的站点
			lastShowNearestMarkerIdList = getNearestMarkers(marker,SHOW_RADIUS);//得到新位置最近的站点
			if(lastShowNearestMarkerIdList.isEmpty()){
				Toast.makeText(getBaseContext(),"半径"+SHOW_RADIUS*1000+"米之内不存在自行车站点", Toast.LENGTH_SHORT).show();
			}else{
				Toast.makeText(getBaseContext(),"半径"+SHOW_RADIUS*1000+"米之内有"+lastShowNearestMarkerIdList.size()+"个自行车站点", Toast.LENGTH_SHORT).show();
			}
			refreshCircle(marker.getPosition(), SHOW_RADIUS);
			showMarkers(lastShowNearestMarkerIdList);//显示当前最近的站点
		}

		@Override
		public void onMarkerDragStart(Marker arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean onMarkerClick(Marker arg0) {
			// TODO Auto-generated method stub
			return false;
		}
		
		//预加载所有marker
		public boolean initMarkers(){
			
			myLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(45.511641973167514,-73.55912055820227)).title("You are here")
					.draggable(true).icon(BitmapDescriptorFactory.fromResource(R.drawable.mylocationmarkericon)));
			
			markerGroup[1] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.508183,-73.554094)).title("Notre Dame / Place Jacques Cartier")
					.snippet("1").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[2] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5392,-73.5414)).title("Dézery/Ste-Catherine")
					.snippet("2").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[4] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5120536032709,-73.5539308190346)).title("Berri / Saint-Antoine")
					.snippet("4").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[6] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50875,-73.55613)).title("Saint-Antoine / de l'Hotel-de-Ville")
					.snippet("6").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[8] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.512738,-73.561438)).title("Sanguinet /Ste-Catherine")
					.snippet("8").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[9] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5146248135863,-73.559775352478)).title("Berri / Sainte-Catherine")
					.snippet("9").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[10] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5137113707436,-73.5605478286743)).title("Sainte-Catherine / Saint-Denis")
					.snippet("10").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[11] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.514076,-73.552439)).title("Saint-André / Saint-Antoine")
					.snippet("11").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[12] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51066,-73.56497)).title("Métro Saint-Laurent")
					.snippet("12").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[13] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51339,-73.56255)).title("Sanguinet / de Maisonneuve")
					.snippet("13").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[14] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.514649,-73.562152)).title("St-Denis / Maisonneuve")
					.snippet("14").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[15] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.515299,-73.561273)).title("Berri / de Maisonneuve")
					.snippet("15").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[16] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51774,-73.55913)).title("Saint-Timothée / de Maisonneuve")
					.snippet("16").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[17] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51905,-73.56091)).title("Robin / Amherst")
					.snippet("17").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[18] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51889,-73.56353)).title("Saint-André / Ontario")
					.snippet("18").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[19] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.517836,-73.567205)).title("Berri / Sherbrooke")
					.snippet("19").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[20] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.513481,-73.566262)).title("Sanguinet / Ontario")
					.snippet("20").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[21] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.521165,-73.562441)).title("Montcalm/Ontario")
					.snippet("21").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[22] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51086,-73.54983)).title("de la Commune / Berri")
					.snippet("22").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[23] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5169929296866,-73.5574418306351)).title("Saint-Timothée / Sainte-Catherine")
					.snippet("23").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[24] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5071439192841,-73.5551190376282)).title("Notre-Dame / Saint-Gabriel")
					.snippet("24").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[25] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5071927926745,-73.5520076751709)).title("de la Commune / Place Jacques-Cartier")
					.snippet("25").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[26] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50589,-73.55431)).title("Le Royer / Saint-Laurent")
					.snippet("26").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[28] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.482843,-73.584352)).title("Belair/St-Antoine")
					.snippet("28").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[30] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50449,-73.5592)).title("Saint-Antoine / Saint-Francois-Xavier")
					.snippet("30").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[31] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50623,-73.55976)).title("Métro Place-d'Armes")
					.snippet("31").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[32] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.560814,-73.580611)).title("20e av/beaubien")
					.snippet("32").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[33] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5079,-73.563112)).title("Saint-Urbain / René-Lévesque")
					.snippet("33").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[34] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50439,-73.56257)).title("de la Gauchetière / Bleury")
					.snippet("34").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[35] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5040685698428,-73.5536062717438)).title("de la Commune / Saint-Sulpice")
					.snippet("35").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[36] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.501974378779,-73.5551565885544)).title("Saint-Nicolas / Place d'Youville")
					.snippet("36").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[37] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49975,-73.55566)).title("McGill / d'Youville")
					.snippet("37").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[38] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.500779,-73.558826)).title("McGill / des Récollets")
					.snippet("38").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[39] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5028,-73.55917)).title("Saint-Jacques / Saint-Pierre")
					.snippet("39").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[40] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.500741,-73.560956)).title("Saint-Jacques / Gauvin")
					.snippet("40").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[41] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50112,-73.56493)).title("de la Gauchetière / University")
					.snippet("41").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[42] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50206,-73.56295)).title("Square Victoria")
					.snippet("42").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[45] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49871,-73.56671)).title("de la Gauchetière / Mansfield")
					.snippet("45").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[46] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.508231,-73.574707)).title("Milton / Durocher")
					.snippet("46").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[47] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.497089,-73.555852)).title("Prince/Wellington")
					.snippet("47").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[48] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4982,-73.55488)).title("King / Wellington")
					.snippet("48").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[49] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49886,-73.55232)).title("de la Commune / McGill")
					.snippet("49").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[50] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.495705,-73.553896)).title("Duke/Brennan")
					.snippet("50").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[51] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.497438,-73.552802)).title("De la Commune / King")
					.snippet("51").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[52] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.502326,-73.565138)).title("Belmont / Beaver Hall")
					.snippet("52").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[55] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.497697,-73.568646)).title("Peel / ave des Canadiens de Montréal")
					.snippet("55").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[56] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.497196,-73.571384)).title("Drummond / Rene Levesques")
					.snippet("56").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[58] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50015,-73.56928)).title("Mansfield / René-Lévesque")
					.snippet("58").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[59] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50176,-73.57128)).title("Sainte-Catherine / McGill College")
					.snippet("59").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[60] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4986393033043,-73.5742270946503)).title("Drummond / Sainte-Catherine")
					.snippet("60").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[61] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49947,-73.57591)).title("Drummond / de Maisonneuve")
					.snippet("61").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[62] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50038,-73.57507)).title("de Maisonneuve / Stanley")
					.snippet("62").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[63] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.499532,-73.57869)).title("de la montagne / Sherbrooke")
					.snippet("63").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[64] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.503475,-73.572103)).title("de Maisonneuve / University")
					.snippet("64").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[65] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50297,-73.57505)).title("Mansfield / Sherbrooke")
					.snippet("65").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[67] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50624,-73.57622)).title("University / Milton")
					.snippet("67").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[68] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50501,-73.57069)).title("de Maisonneuve / Aylmer")
					.snippet("68").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[69] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.556491,-73.530003)).title("St-Clément/Ste-Catherine")
					.snippet("69").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[71] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50708,-73.56917)).title("de Maisonneuve / de Bleury")
					.snippet("71").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[72] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4944993237821,-73.57417345047)).title("Mackay / René-Lévesque")
					.snippet("72").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[73] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.505,-73.566379)).title("Dépot Saint-Alexandre / Sainte-Catherine (Stationnement du Gesù)")
					.snippet("73").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[74] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.550613,-73.582883)).title("6e avenue/Rosemont")
					.snippet("74").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[75] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50401,-73.56865)).title("Square Phillips")
					.snippet("75").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[76] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.489609,-73.582445)).title("Lambert-Closse / Tupper")
					.snippet("76").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[77] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4952,-73.56328)).title("Notre-Dame / Peel")
					.snippet("77").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[78] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49138,-73.57015)).title("Saint-Jacques / Guy")
					.snippet("78").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[79] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49486,-73.57108)).title("Métro Lucien-L'Allier")
					.snippet("79").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[81] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49606,-73.57348)).title("Crescent / René-Lévesque")
					.snippet("81").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[82] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.535431,-73.603522)).title("Chateaubriand / Beaubien")
					.snippet("82").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[83] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.489904,-73.585962)).title("Métro Atwater")
					.snippet("83").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[84] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49156,-73.5838)).title("Chomedey / de Maisonneuve")
					.snippet("84").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[85] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49321,-73.56795)).title("Lucien-L'Allier / Saint-Jacques")
					.snippet("85").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[86] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49275,-73.58061)).title("Sainte-Catherine / Saint-Marc")
					.snippet("86").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[87] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49642,-73.57616)).title("Bishop / Sainte-Catherine")
					.snippet("87").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[88] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.498104,-73.577729)).title("Crescent / de Maisonneuve")
					.snippet("88").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[89] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49659,-73.57851)).title("de Maisonneuve / Mackay")
					.snippet("89").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[90] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.48,-73.6189)).title("Metro Villa Maria")
					.snippet("90").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[91] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.493034,-73.583836)).title("Lincoln / Dufort")
					.snippet("91").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[93] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5168726468268,-73.5541749000549)).title("Wolfe / René-Lévesque")
					.snippet("93").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[94] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51954,-73.552)).title("Plessis / René-Lévesque")
					.snippet("94").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[95] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52114,-73.54926)).title("René-Lévesque / Papineau")
					.snippet("95").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[96] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4950069670713,-73.5780358314514)).title("Sainte-Catherine / Guy")
					.snippet("96").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[97] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5284486935141,-73.5510849952698)).title("Logan / Fullum")
					.snippet("97").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[98] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52615,-73.54612)).title("Fullum / Jean-Langlois")
					.snippet("98").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[99] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.529033,-73.54641)).title("Poupart / Sainte-Catherine")
					.snippet("99").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[100] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52622,-73.54933)).title("Parthenais / Sainte-Catherine")
					.snippet("100").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[101] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.519157,-73.55786)).title("Montcalm/Maisonneuve")
					.snippet("101").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[102] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5219,-73.55534)).title("Alexandre-DeSève / Maisonneuve")
					.snippet("102").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[103] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52353,-73.55199)).title("Métro Papineau")
					.snippet("103").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[104] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4666,-73.6311)).title("Benny/ Monkland")
					.snippet("104").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[105] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.523584,-73.560941)).title("Plessis / Ontario")
					.snippet("105").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[106] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52087,-73.55922)).title("Robin / de la Visitation")
					.snippet("106").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[107] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5250475372999,-73.5600355267525)).title("Champlain / Ontario")
					.snippet("107").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[108] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52695,-73.55821)).title("Dorion / Ontario")
					.snippet("108").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[109] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52954,-73.55622)).title("Parthenais / Ontario")
					.snippet("109").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[110] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.534818,-73.555495)).title("Du havre/Rouen")
					.snippet("110").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[111] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53057,-73.54913)).title("Logan / d'Iberville")
					.snippet("111").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[112] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.523217,-73.558187)).title("Alexandre de Sève / La Fontaine")
					.snippet("112").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[113] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.531949,-73.553263)).title("Poupart/Ontario")
					.snippet("113").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[114] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.499757,-73.629298)).title("Ellendale/Cote-Des-Neiges")
					.snippet("114").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[115] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53217,-73.55859)).title("Rouen / Fullum")
					.snippet("115").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[116] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.535915483037,-73.5582196712494)).title("du Havre / Hochelaga")
					.snippet("116").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[117] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537134,-73.550173)).title("Florian / Ontario ")
					.snippet("117").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[118] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5367,-73.56081)).title("Sherbrooke / Frontenac")
					.snippet("118").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[119] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53375,-73.56196)).title("Sherbrooke / Fullum")
					.snippet("119").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[120] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.535614,-73.566014)).title("Terasse Mercure / Fulum")
					.snippet("120").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[121] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5300270607881,-73.5631763935089)).title("de Bordeaux / Sherbrooke")
					.snippet("121").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[122] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.510016,-73.570654)).title("Sainte-Famille / Sherbrooke")
					.snippet("122").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[123] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50733,-73.57845)).title("University / Prince-Arthur")
					.snippet("123").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[124] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5081646124336,-73.5801789164543)).title("University / des Pins")
					.snippet("124").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[125] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.541532,-73.565258)).title("Molson / William - Tremblay")
					.snippet("125").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[126] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4913217488607,-73.5878098011017)).title("Atwater / Sherbrooke")
					.snippet("126").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[127] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.533348,-73.605834)).title("Drolet / Beaubien")
					.snippet("127").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[128] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.531425,-73.607591)).title("Alma / Beaubien")
					.snippet("128").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[129] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54483,-73.572811)).title("Parc du Pélican")
					.snippet("129").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[130] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.550769,-73.57711)).title("9e Avenue / Dandurand")
					.snippet("130").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[131] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5473561231523,-73.6076098680496)).title("Marquette / Jean-Talon")
					.snippet("131").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[132] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5524426091348,-73.603001832962)).title("des Ecores / Jean-Talon")
					.snippet("132").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[133] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5046325218064,-73.572553396225)).title("Président-Kennedy / University")
					.snippet("133").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[134] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5019405402228,-73.5812652111053)).title("Drummond / du Docteur-Penfield")
					.snippet("134").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[135] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53797,-73.5627)).title("Gascon / Rachel")
					.snippet("135").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[136] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5291552062458,-73.559410572052)).title("Larivière / de Lorimier")
					.snippet("136").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[138] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.530202,-73.56663)).title("Gauthier / Papineau")
					.snippet("138").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[139] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5313,-73.566)).title("de Bordeaux / Gauthier")
					.snippet("139").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[140] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53237,-73.56437)).title("Gauthier / des Erables")
					.snippet("140").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[141] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5298015824607,-73.5702896118164)).title("Marquette / Rachel")
					.snippet("141").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[142] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53227,-73.56828)).title("de Bordeaux / Rachel")
					.snippet("142").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[143] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.527791,-73.572056)).title("Calixa Lavalée / Rachel")
					.snippet("143").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[144] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52689,-73.57264)).title("Rachel / Brébeuf")
					.snippet("144").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[145] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52469,-73.57221)).title("du Parc-La Fontaine / Duluth")
					.snippet("145").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[146] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.523084,-73.568731)).title("du Parc-La Fontaine / Roy")
					.snippet("146").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[147] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52479,-73.56545)).title("Calixa-Lavallée / Sherbrooke")
					.snippet("147").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[148] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52729,-73.56463)).title("Emile-Duployé / Sherbrooke")
					.snippet("148").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[149] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53867,-73.56936)).title("Chapleau / Mont-Royal")
					.snippet("149").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[150] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537553,-73.570182)).title("Fullum / Mont-Royal")
					.snippet("150").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[151] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53565,-73.57172)).title("des Erables / Mont-Royal")
					.snippet("151").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[152] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5341343661084,-73.5734921693802)).title("Chabot / Mont-Royal")
					.snippet("152").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[153] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5318947381802,-73.5723227262497)).title("Marie-Anne / Papineau")
					.snippet("153").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[154] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53229,-73.57544)).title("Marquette / Mont-Royal")
					.snippet("154").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[155] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53092,-73.57674)).title("Garnier / Mont-Royal")
					.snippet("155").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[156] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5277384137908,-73.5762494802475)).title("Marie-Anne / de la Roche")
					.snippet("156").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[157] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52856,-73.57828)).title("de la Roche / Mont-Royal")
					.snippet("157").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[158] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53073,-73.58153)).title("Gilford / Brébeuf")
					.snippet("158").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[159] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.527496,-73.588942)).title("Rivard/Laurier")
					.snippet("159").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[160] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53297,-73.58118)).title("Garnier / Saint-Joseph")
					.snippet("160").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[161] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53576,-73.57867)).title("Cartier / Saint-Joseph")
					.snippet("161").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[162] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54003,-73.575158)).title("Fullum / Saint-Joseph")
					.snippet("162").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[163] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53543,-73.5822)).title("Marquette / Laurier")
					.snippet("163").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[164] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53388,-73.58315)).title("Garnier / Laurier")
					.snippet("164").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[165] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53248,-73.58507)).title("Brébeuf / Laurier")
					.snippet("165").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[166] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5341,-73.58852)).title("Brébeuf / Saint-Grégoire")
					.snippet("166").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[167] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53703,-73.58584)).title("Marquette / Saint-Grégoire")
					.snippet("167").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[168] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.509474,-73.557278)).title("Hotel de ville/viger")
					.snippet("168").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[169] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53187,-73.59064)).title("de Mentana / Saint-Grégoire")
					.snippet("169").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[170] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53004,-73.5867)).title("de Mentana / Laurier")
					.snippet("170").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[171] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5195,-73.560269)).title("Wolfe/Robin")
					.snippet("171").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[173] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5191458,-73.5692414)).title("Cherrier / Berri")
					.snippet("173").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[174] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51908,-73.5727)).title("Roy / Saint-Denis")
					.snippet("174").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[175] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.520578,-73.567733)).title("St-André/Cherrier")
					.snippet("175").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[176] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.523171,-73.573431)).title("St-André / Duluth")
					.snippet("176").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[177] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.524222,-73.575771)).title("Saint-André / Rachel")
					.snippet("177").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[178] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.522448,-73.577821)).title("Rivard/Rachel")
					.snippet("178").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[179] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5207741909979,-73.5757774114609)).title("Duluth / Saint-Denis")
					.snippet("179").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[180] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50612,-73.556605)).title("St-Jacques/St-Laurent")
					.snippet("180").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[181] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.516853,-73.582648)).title("Clark/Rachel")
					.snippet("181").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[182] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5215,-73.5851)).title("Buillon / Mont-Royal")
					.snippet("182").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[183] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5224129092166,-73.5841298103333)).title("Laval / Mont-Royal")
					.snippet("183").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[184] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52468,-73.58199)).title("Métro Mont-Royal")
					.snippet("184").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[185] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.549286,-73.601006)).title("Des érables/Bélanger")
					.snippet("185").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[186] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52743,-73.58002)).title("Boyer / Mont-Royal")
					.snippet("186").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[187] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5272,-73.58226)).title("Saint-André / de Bienville")
					.snippet("187").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[188] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52473,-73.57878)).title("Marie-Anne / Saint-Hubert")
					.snippet("188").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[189] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5507,-73.6096)).title("Chabot/Everret")
					.snippet("189").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[190] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.527008,-73.585852)).title("Pontiac/Gilford")
					.snippet("190").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[191] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.531401,-73.612674)).title("St-Zotique/Clark")
					.snippet("191").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[192] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.527733,-73.58958)).title("Métro Laurier")
					.snippet("192").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[193] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.521495,-73.596758)).title("Esplanade/Fairmount")
					.snippet("193").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[194] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.527265,-73.593402)).title("Henri-Julien / Maguire")
					.snippet("194").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[195] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52377,-73.58978)).title("de Bullion / Boul. Saint-Joseph")
					.snippet("195").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[196] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52157,-73.58918)).title("Villeneuve/St-Laurent")
					.snippet("196").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[198] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52974,-73.59527)).title("Hélène-Baillargeon / Saint-Denis")
					.snippet("198").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[199] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5251715609733,-73.5993754863739)).title("Clark / Saint-Viateur")
					.snippet("199").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[200] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52457,-73.59588)).title("Maguire / Saint-Laurent")
					.snippet("200").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[201] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5235254063103,-73.587509393692)).title("Villeneuve / de l'Hotel-de-Ville")
					.snippet("201").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[202] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50781,-73.57208)).title("Hutchison / Sherbrooke")
					.snippet("202").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[203] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50971,-73.57354)).title("Milton / du Parc")
					.snippet("203").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[204] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51059,-73.57547)).title("Prince-Arthur / du Parc")
					.snippet("204").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[205] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5128129495349,-73.5770165920258)).title("Sainte-Famille / des Pins")
					.snippet("205").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[206] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51069,-73.57805)).title("Hutchison / des Pins")
					.snippet("206").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[207] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51252,-73.57062)).title("Milton / Clark")
					.snippet("207").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[208] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5509,-73.656)).title("Metro  Sauvé")
					.snippet("208").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[209] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51561,-73.57569)).title("Roy / Saint-Laurent")
					.snippet("209").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[210] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.515201,-73.581394)).title("Duluth/Esplanade")
					.snippet("210").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[211] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5171,-73.57921)).title("Duluth / Saint-Laurent")
					.snippet("211").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[212] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51735,-73.56906)).title("Square Saint-Louis")
					.snippet("212").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[213] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51494,-73.57821)).title("Saint-Cuthbert / Saint-Urbain")
					.snippet("213").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[214] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51496,-73.58503)).title("Parc du Mont-Royal")
					.snippet("214").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[215] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51778,-73.58531)).title("Marie-Anne / Saint-Urbain")
					.snippet("215").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[216] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51737,-73.57452)).title("de l'Hotel-de-Ville / Roy")
					.snippet("216").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[217] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5192369096477,-73.5772150754929)).title("Laval / Duluth")
					.snippet("217").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[218] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51941,-73.58685)).title("Mont-Royal / Clark")
					.snippet("218").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[219] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51833,-73.58825)).title("de l'Esplanade / Mont-Royal")
					.snippet("219").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[220] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.517,-73.589)).title("Mont-Royal / du Parc")
					.snippet("220").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[221] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51829,-73.59235)).title("Villeneuve/parc")
					.snippet("221").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[222] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52039,-73.59026)).title("Villeneuve / Saint-Urbain")
					.snippet("222").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[223] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52266,-73.5936)).title("Clark / Laurier")
					.snippet("223").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[224] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52024,-73.59494)).title("Jeanne-Mance / Laurier")
					.snippet("224").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[225] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52102,-73.585514)).title("Coloniale/ Mont-Royal")
					.snippet("225").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[226] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5240252711522,-73.6004215478897)).title("Waverly / Saint-Viateur")
					.snippet("226").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[227] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52246,-73.6019)).title("Saint-Viateur / du Parc")
					.snippet("227").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[228] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52697,-73.60261)).title("Bernard / Clark")
					.snippet("228").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[229] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.52654331973,-73.5982328653336)).title("Saint-Dominique / Saint-Viateur")
					.snippet("229").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[230] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537305,-73.578921)).title("bordeaux/Laurier")
					.snippet("230").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[232] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5379745943235,-73.5747796297073)).title("Parthenais / Gilford")
					.snippet("232").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[233] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5375124280974,-73.5669583082199)).title("Chapleau / Terrasse guindon")
					.snippet("233").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[234] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5497040782386,-73.6055123806)).title("de Bordeaux / Jean-Talon")
					.snippet("234").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[236] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.519486,-73.598419)).title("Fairmount / Hutchison")
					.snippet("236").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[237] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.517345,-73.597724)).title("Querbes / Laurier")
					.snippet("237").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[239] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.520532,-73.608144)).title("Bloomfield / Bernard")
					.snippet("239").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[241] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.522608,-73.613178)).title("Bloomfield / Van Horne")
					.snippet("241").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[242] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.520265,-73.614887)).title("Métro Outremont")
					.snippet("242").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[243] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53304,-73.61108)).title("Casgrain / Saint-Zotique")
					.snippet("243").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[244] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.518593,-73.581566)).title("St-Dminique/Rachel")
					.snippet("244").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[245] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53696,-73.61199)).title("Bélanger / Saint-Denis")
					.snippet("245").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[246] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53707,-73.61529)).title("Henri-Julien / Jean-Talon")
					.snippet("246").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[247] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.534788,-73.614726)).title("Casgrain / Mozart")
					.snippet("247").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[248] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53318,-73.61544)).title("Mozart / Saint-Laurent")
					.snippet("248").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[249] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5397368030449,-73.6142563819885)).title("Lajeunesse / Jean-Talon")
					.snippet("249").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[250] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54006,-73.60897)).title("Boyer / Bélanger")
					.snippet("250").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[251] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53848,-73.60556)).title("Boyer / Saint-Zotique")
					.snippet("251").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[253] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.536299,-73.607773)).title("Saint-Vallier / Saint-Zotique")
					.snippet("253").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[254] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537035,-73.593126)).title("Parc Père-Marquette")
					.snippet("254").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[255] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54184,-73.58644)).title("Holt / de Lorimier")
					.snippet("255").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[256] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53989,-73.58807)).title("Cartier / des Carrières")
					.snippet("256").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[257] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54458,-73.588069)).title("Louis Hémon / Rosemont")
					.snippet("257").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[258] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537202,-73.597884)).title("de la Roche /  Bellechasse ")
					.snippet("258").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[259] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4714,-73.6266)).title("Hampton/Monkland")
					.snippet("259").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[260] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5427,-73.59212)).title("Chabot / Bellechasse")
					.snippet("260").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[261] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.536967,-73.579629)).title("Chabot/Laurier")
					.snippet("261").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[262] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54857,-73.59268)).title("Louis-Hébert / Beaubien")
					.snippet("262").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[263] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54475,-73.5949)).title("de Bordeaux / Beaubien")
					.snippet("263").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[264] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.539964,-73.599716)).title("Chambord / Beaubien")
					.snippet("264").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[265] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54083,-73.60384)).title("de Normanville / Saint-Zotique")
					.snippet("265").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[266] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.543541,-73.600982)).title("Fabre / Saint-Zotique")
					.snippet("266").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[267] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.529993,-73.604214)).title("Casgrain / de Bellechasse")
					.snippet("267").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[268] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.546526,-73.598442)).title("Bordeaux / Saint-Zotique")
					.snippet("268").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[269] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.544471,-73.605145)).title("Garnier / Bélanger")
					.snippet("269").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[270] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54691,-73.60281)).title("Cartier / Bélanger")
					.snippet("270").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[271] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.544828,-73.610458)).title("Chambord / Jean-Talon")
					.snippet("271").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[272] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5416,-73.608)).title("De la Roche / Bélanger")
					.snippet("272").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[273] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.55001,-73.59576)).title("Louis-Hébert / Saint-Zotique")
					.snippet("273").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[274] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.551457,-73.598852)).title("Louis-Hébert / Bélanger")
					.snippet("274").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[275] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.542055,-73.597793)).title("Fabre / Beaubien")
					.snippet("275").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[276] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.544377,-73.581018)).title("Parc Rosemont")
					.snippet("276").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[277] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5325,-73.5973)).title("Chateaubriand/Rosemont ")
					.snippet("277").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[278] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.534136,-73.595478)).title("Boyer / Rosemont")
					.snippet("278").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[279] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.535304,-73.599387)).title("St-André / Bellechasse")
					.snippet("279").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[280] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.483977,-73.563576)).title("Augustin-Cantin / Shearer")
					.snippet("280").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[281] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.499916,-73.618089)).title("Louis-Colin / McKenna")
					.snippet("281").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[282] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.50723,-73.615085)).title("Edouard-Montpetit / Stirling")
					.snippet("282").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[283] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.503023,-73.616531)).title("Chemin de la Tour")
					.snippet("283").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[284] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.527363,-73.607723)).title("Waverly / Van Horne")
					.snippet("284").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[285] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5250212292128,-73.610737323761)).title("Hutchison / Van Horne")
					.snippet("285").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[286] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5242958728226,-73.6048471927643)).title("Bernard / Jeanne-Mance")
					.snippet("286").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[287] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.549949,-73.647982)).title("Basile-Routhier / Chabanel")
					.snippet("287").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[288] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.552649,-73.581185)).title("10e Avenue / Rosemont")
					.snippet("288").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[289] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537041,-73.602026)).title("Boyer / Beaubien")
					.snippet("289").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[290] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.49427,-73.55979)).title("Peel / Ottawa")
					.snippet("290").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[291] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4930290125837,-73.564817905426)).title("Notre-Dame / de la Montagne ")
					.snippet("291").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[292] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.489951,-73.567091)).title("Guy / Notre-Dame")
					.snippet("292").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[293] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54705,-73.59353)).title("Louis-Hémon / Beaubien")
					.snippet("293").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[294] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.524611,-73.594059)).title("Fairmount/St-Dominique")
					.snippet("294").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[297] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4782278730914,-73.5696512460709)).title("Métro Charlevoix")
					.snippet("297").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[301] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.48767,-73.56895)).title("des Seigneurs / Notre-Dame")
					.snippet("301").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[302] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.474098,-73.587206)).title("Sir Georges Etienne Cartier/Notre-Dame")
					.snippet("302").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[303] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.48501,-73.57178)).title("Georges-Vanier / Notre-Dame")
					.snippet("303").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[304] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.48205,-73.57491)).title("Duvernay / Charlevoix")
					.snippet("304").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[305] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.485889,-73.577514)).title("Quesnel / Vinet")
					.snippet("305").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[306] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.480359,-73.577102)).title("Marché Atwater")
					.snippet("306").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[307] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.484075,-73.577355)).title("Métro Lionel-Groulx")
					.snippet("307").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[308] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4891,-73.57656)).title("Saint-Antoine / Canning (Metro George-Vanier)")
					.snippet("308").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[309] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.508553,-73.618172)).title("Stirling/Cote-Sainte-Catherine")
					.snippet("309").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[311] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.456049,-73.571877)).title("5e ave/Verdun")
					.snippet("311").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[312] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.505514,-73.621215)).title("Darlington / Cote-Sainte-Catherine")
					.snippet("312").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[313] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.470303,-73.589848)).title("Palm/St-Remi")
					.snippet("313").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[314] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.501302,-73.633161)).title("Kent / Cote-Des-Neiges")
					.snippet("314").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[315] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.499365,-73.626611)).title("Gatineau / Cote-Sainte-Catherine")
					.snippet("315").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[316] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.529219,-73.574777)).title("De Lanaudiere/Marie-Anne")
					.snippet("316").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[317] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.496988,-73.623354)).title("Lacombe / Cote-des-Neiges")
					.snippet("317").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[318] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.497011,-73.618575)).title("Parc Jean-Brillant")
					.snippet("318").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[319] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.555029,-73.554902)).title("Jardin Botanique")
					.snippet("319").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[320] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.536178,-73.57641)).title("Bordeaux/Gilford")
					.snippet("320").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[321] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.541814,-73.554635)).title("Métro Préfontaine")
					.snippet("321").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[322] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547774,-73.551257)).title("Métro Joliette (Chambly/Hochelaga)")
					.snippet("322").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[323] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.550668,-73.549502)).title("D'Orléans / Hochelaga")
					.snippet("323").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[324] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.541484,-73.612441)).title("Boyer / Jean-Talon")
					.snippet("324").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[325] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.544136,-73.545609)).title("Aylwin / Ontario")
					.snippet("325").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[327] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.541044,-73.547407)).title("Ontario / Dézéry")
					.snippet("327").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[328] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547258,-73.543728)).title("Valois / Ontario")
					.snippet("328").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[329] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.55896,-73.548687)).title("Pierre-de-Coubertin / Aird")
					.snippet("329").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[330] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.553491,-73.53931)).title("Marché Maisonneuve")
					.snippet("330").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[331] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.551119,-73.540764)).title("Desjardins / Ontario")
					.snippet("331").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[332] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.539964,-73.634319)).title("Guizot / Saint-Laurent")
					.snippet("332").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[333] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.540098,-73.619986)).title("Faillon / Saint-Denis")
					.snippet("333").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[334] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.541791,-73.625894)).title("Gounod / Saint-Denis")
					.snippet("334").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[335] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.540627,-73.630089)).title("De Gaspé / Jarry")
					.snippet("335").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[336] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.543376,-73.632267)).title("Guizot / Saint-Denis")
					.snippet("336").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[337] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.542579,-73.636557)).title("De Gaspé / De Liège")
					.snippet("337").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[338] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.545987,-73.631562)).title("Foucher / Leman")
					.snippet("338").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[339] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.543583,-73.6284)).title("Lajeunesse / Jarry")
					.snippet("339").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[340] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.54467,-73.621581)).title("Du Rosaire / Saint-Hubert")
					.snippet("340").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[341] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.542892,-73.617822)).title("Faillon / Saint-Hubert")
					.snippet("341").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[342] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547283,-73.625377)).title("Boyer / Jarry")
					.snippet("342").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[343] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4743,-73.6237)).title("Harvard/Monkland")
					.snippet("343").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[344] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.544867,-73.614841)).title("De La Roche / Everett")
					.snippet("344").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[345] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.467369,-73.570769)).title("Regina / Verdun")
					.snippet("345").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[346] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.550551,-73.615021)).title("Marquette / Villeray")
					.snippet("346").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[347] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.554931,-73.610711)).title("Louis-Hémon / Villeray")
					.snippet("347").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[348] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.545409,-73.537669)).title("Valois/Ste-Catherine")
					.snippet("348").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[349] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.559199,-73.599658)).title("Metro St-Michel")
					.snippet("349").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[350] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4758,-73.5644)).title("Ryde/Charlevoix")
					.snippet("350").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[351] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4815,-73.5644)).title("Island/Centre")
					.snippet("351").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[355] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.537813,-73.624872)).title("St-Dominique / Villeray")
					.snippet("355").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[356] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.545541556277,-73.6347484588623)).title("de Liège / Lajeunesse")
					.snippet("356").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[357] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5401,-73.6234)).title("Drolet / Villeray")
					.snippet("357").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[358] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.471613,-73.613667)).title("Marcil/Sherbrooke")
					.snippet("358").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[359] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5566,-73.5373)).title("Sicard/Ontario")
					.snippet("359").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[360] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4771,-73.6212)).title("Monkland/Girouard")
					.snippet("360").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[361] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.556905,-73.58925)).title("12e Avenue / Saint-Zotique")
					.snippet("361").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[362] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.553243,-73.592336)).title("3e avenue /St-Zotique")
					.snippet("362").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[363] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.558496,-73.583489)).title("16e Avenue / Beaubien")
					.snippet("363").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[364] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.553215,-73.58752)).title("8e Avenue / Beaubien")
					.snippet("364").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[365] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.550476,-73.590446)).title("1ere Avenue / Beaubien")
					.snippet("365").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[366] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547997,-73.58523)).title("1ere Avenue / Rosemont")
					.snippet("366").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[367] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4688,-73.6198)).title("Cote St-Antoine/Royal")
					.snippet("367").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[368] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.549639,-73.558292)).title("Parc Campbell")
					.snippet("368").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[369] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.556723,-73.577636)).title("16e Avenue / Rosemont")
					.snippet("369").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[370] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.510042,-73.624835)).title("Wilderton  / Van Horne")
					.snippet("370").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[371] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547362,-73.579221)).title("3e Avenue / Dandurand")
					.snippet("371").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[372] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.550321,-73.573755)).title("10e Avenue / Masson")
					.snippet("372").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[373] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.459422,-73.637474)).title("Belmore/Sherbrooke")
					.snippet("373").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[374] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.557192,-73.569847)).title("D'Orléans / Masson")
					.snippet("374").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[375] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.546978,-73.575515)).title("4e Avenue / Masson")
					.snippet("375").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[376] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.544123,-73.577049)).title("Molson / Masson")
					.snippet("376").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[377] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547155,-73.569901)).title("7e Avenue / Saint-Joseph")
					.snippet("377").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[378] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.553408,-73.569531)).title("15e Avenue / Laurier")
					.snippet("378").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[379] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.477287,-73.585419)).title("Metro Place St-Henri")
					.snippet("379").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[380] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.554165,-73.564742)).title("18e Avenue / Saint-Joseph")
					.snippet("380").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[381] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.479245,-73.563488)).title("Mullins/Hibernia")
					.snippet("381").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[383] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.463257,-73.575621)).title("Bannantyne /de l'Eglise")
					.snippet("383").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[384] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.551584,-73.561916)).title("Parc J.-Arthur-Champagne")
					.snippet("384").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[385] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.545474,-73.561937)).title("Omer-Lavallée / Midway")
					.snippet("385").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[387] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.545425,-73.556527)).title("Darling / Sherbrooke")
					.snippet("387").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[388] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.553879,-73.551761)).title("Métro Pie-IX")
					.snippet("388").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[389] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.518215,-73.603251)).title("Parc Outremont")
					.snippet("389").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[390] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.51529,-73.60628)).title("Stuart / Cote-Ste-Catherine")
					.snippet("390").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[391] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.514905,-73.619682)).title("Dunlop / Van Horne")
					.snippet("391").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[392] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.515617,-73.614991)).title("Rockland / Lajoie")
					.snippet("392").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[393] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.514379,-73.610871)).title("Davaar / Cote-Sainte-Catherine")
					.snippet("393").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[394] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.510086,-73.611429)).title("Mont-Royal / Vincent-D'Indy ")
					.snippet("394").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[396] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.536152,-73.622351)).title("Faillon / St-Laurent")
					.snippet("396").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[397] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.535189,-73.617459)).title("Saint-Dominique / Jean talon")
					.snippet("397").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[399] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.486457,-73.573286)).title("Lionel-Groulx / George-Vanier")
					.snippet("399").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[401] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5145308386694,-73.5684710741043)).title("De l'Hotel-de-Ville / Sherbrooke")
					.snippet("401").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[403] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5014018,-73.5718136)).title("Mansfield / Ste-Catherine ")
					.snippet("403").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[404] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.512762,-73.573489)).title("Prince-Arthur / Saint-Urbain")
					.snippet("404").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[406] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.512097,-73.53382)).title("Métro Jean-Drapeau")
					.snippet("406").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[407] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.511095,-73.537674)).title("Quai de la navette fluviale")
					.snippet("407").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[408] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.530261,-73.624273)).title("Métro Parc")
					.snippet("408").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[409] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.521769,-73.534859)).title("La Ronde")
					.snippet("409").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[413] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4703,-73.6162)).title("Wilson/Sherbrooke")
					.snippet("413").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[414] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.554109,-73.547417)).title("La Salle/Hochelaga")
					.snippet("414").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[415] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.473775,-73.604223)).title("Metro Vendome")
					.snippet("415").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[416] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.502536,-73.572664)).title("McGill College /Maisonneuve")
					.snippet("416").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[417] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.541198,-73.590943)).title("Cartier/Rosemont")
					.snippet("417").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[418] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.475191,-73.607539)).title("Marlowe / Sherbrooke")
					.snippet("418").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[419] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.491513,-73.633649)).title("Beaucourt /Cote Ste-Catherine")
					.snippet("419").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[421] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.496302,-73.629822)).title("Hopital général juif")
					.snippet("421").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[422] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.485256,-73.627753)).title("Metro snowdon")
					.snippet("422").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[423] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5536,-73.6616)).title("Fleurry/Lajeunesse")
					.snippet("423").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[424] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.547009,-73.637951)).title("Basile-Routhier / Crémazie")
					.snippet("424").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[425] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.554492,-73.635151)).title("Complexe sportif Claude-Robillard")
					.snippet("425").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[426] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.503873,-73.568679)).title("Square Phillips 2")
					.snippet("426").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[427] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.480065,-73.587005)).title("Agnes/St-Antoine")
					.snippet("427").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[428] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.462742,-73.565845)).title("Ross / Ave de Léglise")
					.snippet("428").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[429] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.459707,-73.571438)).title("Willibord / Verdun")
					.snippet("429").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[430] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.470605,-73.565459)).title("Rushbrooke / Ave Caisse")
					.snippet("430").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[431] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.46714,-73.54259)).title("Place du Commerce")
					.snippet("431").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[432] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.513338,-73.57295)).title("Clarck/Prince Arthur")
					.snippet("432").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[433] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.47256,-73.53954)).title("Ch. Pointe Nord")
					.snippet("433").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[437] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.528064,-73.623837)).title("De l'épée/Jean Talon")
					.snippet("437").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[438] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.487331,-73.58895)).title("Boul Maisonneuve/Greene")
					.snippet("438").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[439] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4829,-73.591)).title("Hillside / Ste-Catherine")
					.snippet("439").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[440] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.477581,-73.600353)).title("Victoria/Maisonneuve")
					.snippet("440").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[441] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.4816,-73.6)).title("Victorial Hall")
					.snippet("441").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[442] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.486,-73.5959)).title("Argyle / Sherbrooke")
					.snippet("442").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[443] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5332,-73.5156)).title("St-Charles / Chateauguay")
					.snippet("443").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[444] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5294,-73.5178)).title("Place Longueuil")
					.snippet("444").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[445] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5382,-73.5058)).title("Grant / St-Laurent")
					.snippet("445").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[446] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5375,-73.5109)).title("St-Charles / St-Jean")
					.snippet("446").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[447] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.5368,-73.4954)).title("Collège Edouard-Montpetit")
					.snippet("447").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[448] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.523167,-73.519888)).title("Métro Longueuil - Université de Sherbrooke")
					.snippet("448").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
					markerGroup[452] = mMap.addMarker(new MarkerOptions().position(new LatLng(45.53133,-73.59155)).title("St-André/St-Grégoire")
					.snippet("452").visible(false).icon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot_36x43)));
			
					//初始化完毕，将myLocationMarker附近的点显示
					lastShowNearestMarkerIdList = getNearestMarkers(myLocationMarker,SHOW_RADIUS);
					showMarkers(lastShowNearestMarkerIdList);
					
					return true;
		}

}

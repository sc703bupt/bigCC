package com.example.bigCC;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.example.bigCC.R;
import com.example.bigCC.chooseSite_search.MyInfoWindowAdapter;
import com.example.util.BaseActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class chooseSite extends BaseActivity implements OnClickListener,OnMapClickListener,OnInfoWindowClickListener{
	
	private GoogleMap mMap = null;
	Marker myLocationMarker = null;
	Marker lastClickedMarker  = null;
	
	//分别存储要回传的地点信息
	private String startPlace,endPlace;
	private static final int startNo = 1;
	private static final int endNo = 2;
	private static String Add_startsite = "addstart";
	private static String Add_endsite = "addend";
	//标识被调用的源头种类
	private int flag = 0;//1代表选择起点，2代表选择终点

	private TextView chooseSiteBackButton,chooseSiteTitle;
	
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);			
		setContentView(R.layout.choosesite);
        Intent intent1 = getIntent();
		flag = intent1.getIntExtra("flag", startNo);
		
		chooseSiteBackButton = (TextView)findViewById(R.id.choosesiteBackButton);
		chooseSiteBackButton.setOnClickListener(this);
		chooseSiteTitle = (TextView)findViewById(R.id.choosesiteTitle);
		if(flag == 1){
			chooseSiteTitle.setText("选择起点");
		}else{
			chooseSiteTitle.setText("选择终点");
		}
		
		if(mMap == null){
			mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.mapchoosesite)).getMap();
			if(mMap != null){
				mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(45.511641973167514,-73.55912055820227),16));
				
				mMap.setOnMapClickListener(this);
				mMap.setOnInfoWindowClickListener(this);
				mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
				initMarkers();
			}
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
    	{
		   case R.id.choosesiteBackButton:
			   finish();
			   break;
    	}
	}
	
	class MyInfoWindowAdapter implements InfoWindowAdapter{
		@Override
		public View getInfoWindow(Marker marker) {//无论用户点击或是通过showInfoWindow()触发，该方法会首先被调用
			if(marker.equals(myLocationMarker)){
				if(lastClickedMarker!=null){
					lastClickedMarker.remove();
				}
			}
			
			View v =  getLayoutInflater().inflate(R.layout.place_selection_window, null);
            TextView placeSelectionTipUI = ((TextView) v.findViewById(R.id.placeSelectionTip));
            if(flag == startNo){
            	placeSelectionTipUI.setText("选择此处为起点");
            }
            if(flag == endNo){
            	placeSelectionTipUI.setText("选择此处为终点");
            }
			return v;
		}
		@Override
		public View getInfoContents(Marker marker) {//getInfoWindow()返回null后该方法被调用，如果该方法也返回null，则调用默认的窗体
			return null;
		}
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		// TODO Auto-generated method stub
		Intent intent = new Intent();
		if(flag == startNo){
			intent.setAction(Add_startsite); //说明动作
			startPlace = marker.getPosition().latitude+","+marker.getPosition().longitude;
			FindRoute1.save_startsite = startPlace;
		}
		else if(flag == endNo){
			intent.setAction(Add_endsite); //说明动作
			endPlace = marker.getPosition().latitude+","+marker.getPosition().longitude;
			FindRoute1.save_endsite = endPlace;
		}		
		sendBroadcast(intent);    //发送广播		
	    finish(); 
	}


	@Override
	public void onMapClick(LatLng latlng) {
		// TODO Auto-generated method stub
		if(lastClickedMarker != null){
			lastClickedMarker.remove();
		}
		lastClickedMarker = mMap.addMarker(new MarkerOptions().position(latlng)
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot_36x43)));
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng,16),300,null);
		lastClickedMarker.showInfoWindow();
	}
	
	//预加载所有marker
	public boolean initMarkers(){
		myLocationMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(45.511641973167514,-73.55912055820227))
				.icon(BitmapDescriptorFactory.fromResource(R.drawable.mylocationmarkericon)));
		return true;
	}
}

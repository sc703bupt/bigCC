package com.example.bigCC;

import java.util.regex.Pattern;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.bigCC.R;
import com.example.bigCC.Nearby.MyInfoWindowAdapter;
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

public class chooseSite_search extends BaseActivity implements OnClickListener,OnMapClickListener,OnInfoWindowClickListener{
	
	private GoogleMap mMap = null;
	Marker myLocationMarker = null;
	Marker lastClickedMarker  = null;
	
	private TextView chooseSiteSearchBackButton,chooseSiteSearchTitle;
	
	private static String Add_site = "add";
	
	//地点信息
	private String oneSite = "";
	
	public void onCreate(Bundle savedInstanceState){
		
		super.onCreate(savedInstanceState);	
		setContentView(R.layout.choosesite);
		
		chooseSiteSearchBackButton = (TextView)findViewById(R.id.choosesiteBackButton);
		chooseSiteSearchBackButton.setOnClickListener(this);
		chooseSiteSearchTitle = (TextView)findViewById(R.id.choosesiteTitle);
		chooseSiteSearchTitle.setText("选择地点");
		
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
            placeSelectionTipUI.setText("选择为目标地点");
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
		intent.setAction(Add_site); //说明动作	
		oneSite = marker.getPosition().latitude+","+marker.getPosition().longitude;
		Search1.save_site = oneSite;
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

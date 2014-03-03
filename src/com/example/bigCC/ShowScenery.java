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
import com.google.android.gms.maps.model.LatLng;

public class ShowScenery extends BaseActivity implements OnClickListener{
	
	TextView showSceneryBackButton;
	TextView picture,description;
	View views;
	public static int showNo = 0;
	//showNo
	//1:Parc Ville-de-la-Fleche [Rue du Champ de Mars,Montreal] [45.51148972565424,-73.5527328774333] Ville-de-la-Fleche公园
	//2:Sir George-Etienne Cartier National Historic Site [458 Rue Notre-Dame Est,Montreal] [45.5111988,-73.5517867] George-Etienne爵士国家历史遗址
	//3:Marguerite Bourgeoys Museum [400 Rue Saint Paul Est,Montreal] [45.5097570,-73.5514119] Marguerite Bourgeoys博物馆
	//4:Auberge du Vieux-Port [97 Rue de la Commune Est,Montreal] [45.5060263,-73.5528877] 酒店
	//5:Durga Sovenirs [383 Rue Saint Paul Est,Montreal] [45.5096247,-73.5515123] 杜尔加纪念品商店
	//[x]6:圣母世界之后主教座堂 [20 rue Sainte-Catherine Est Montréal] [45.51261606556533,-73.56057029217482]‎
	
	public static LatLng place1 = new LatLng(45.51148972565424,-73.5527328774333);
	public static LatLng place2 = new LatLng(45.5111988,-73.5517867);
	public static LatLng place3 = new LatLng(45.5097570,-73.5514119);
	public static LatLng place4 = new LatLng(45.5060263,-73.5528877);
	public static LatLng place5 = new LatLng(45.5096247,-73.5515123);
	//public static LatLng place6 = new LatLng(45.51261606556533,-73.56057029217482);
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		
        views = LayoutInflater.from(this).inflate(R.layout.showscenery, null); 
        setContentView(views);
		
		Intent intent = getIntent();
		
		showSceneryBackButton = (TextView)findViewById(R.id.showSceneryBackButton);
		showSceneryBackButton.setOnClickListener(this);
		
		picture = (TextView)views.findViewById(R.id.picture);
		description = (TextView)views.findViewById(R.id.description);
		
		switch(showNo){
		case 1:
			picture.setBackgroundResource(R.drawable.parcvilledelafleche);
			description.setText(R.string.description1);
			break;
		case 2:
			picture.setBackgroundResource(R.drawable.sirgeorgeetiennecartiernationalhistoricsite);
			description.setText(R.string.description2);
			break;
		case 3:
			picture.setBackgroundResource(R.drawable.margueritebourgeoysmuseum);
			description.setText(R.string.description3);
			break;
		case 4:
			picture.setBackgroundResource(R.drawable.aubergeduvieuxport);
			description.setText(R.string.description4);
			break;
		case 5:
			picture.setBackgroundResource(R.drawable.durgasovenirs);
			description.setText(R.string.description5);
			break;
//		case 6:
//			break;
		default:
			break;
		}
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
    	{
		   case R.id.showSceneryBackButton:
			   finish();
			   break;
    	}	
	}
}
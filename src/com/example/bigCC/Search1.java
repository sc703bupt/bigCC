package com.example.bigCC;

import java.io.IOException;
import java.util.regex.Pattern;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bigCC.R;
import com.example.util.BaseActivity;
import com.example.util.MyDatabaseHelper;

public class Search1 extends BaseActivity implements TextWatcher,OnClickListener,OnFocusChangeListener{

	private Cursor cursor;
	private MyDatabaseHelper dbHelper;
	
	private Spinner spTime;
	private TextView bnSearch,btStart,bnClear;
	private ListView lvHistory;
	private EditText place;
	private View views;

	private static final String No_Mes = "无最近浏览记录"; 
	private static String Add_site = "add";
	public static String save_site;
	
	public String latlngForSearch = null;
	public int flag = 3;//1代表使用地图返回，2代表使用历史记录，3代表手动输入
	
	public void onCreate(Bundle savedInstanceState) {
		 
		//防止ksoap2调用网络时异常
		 StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads().detectDiskWrites().detectNetwork()
        .penaltyLog().build());
		 StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectLeakedSqlLiteObjects().penaltyLog().penaltyDeath()
        .build());
		
        super.onCreate(savedInstanceState);
        views = LayoutInflater.from(getParent()).inflate(R.layout.search1, null); 
        setContentView(views);
        
        spTime = (Spinner)views.findViewById(R.id.SpTime);
        bnSearch = (TextView)views.findViewById(R.id.search1StartSearch);
        bnSearch.setOnClickListener(this);
        bnSearch.setEnabled(false);
        bnClear = (TextView)views.findViewById(R.id.clear_place);
    	bnClear.setOnClickListener(this);
        place = (EditText)views.findViewById(R.id.placeEditText);
        place.addTextChangedListener(this);
        place.setOnFocusChangeListener(this);
        btStart = (TextView)views.findViewById(R.id.targetPlaceSelect);
        btStart.setOnClickListener(this);      
        lvHistory = (ListView)views.findViewById(R.id.search1HistoryList);
        //在当前的activity中注册广播
	    IntentFilter filter = new IntentFilter();
		filter.addAction(Add_site);		   
		registerReceiver(this.broadcastReceiver1, filter); // 注册		
	}	

	    // 广播的内部类，当收到动作时，修改EditText的内容
	    private BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent intent) {					

						if(Add_site.equals(intent.getAction())){
							String[] split = Pattern.compile(",").split(save_site);
							if(split.length!=2){
								place.setText("");
							}else{//合法
								latlngForSearch = save_site;
								place.setText(convertLatLngToPlaceNameViaWebservice(new Double(split[0]),new Double(split[1])));
							}
							save_site = "";
							flag = 1;
						}
					}
		};		
		
		public void onStart(){
			super.onStart();
			freshView2();
		}
		
		/**
		 * 从数据库中获取最近浏览记录
		 */
		public void freshView2(){
			   //创建MyDataBaseHelper对象，指定数据库版本为1，使用相对路径
			   //数据库文件会自动保存在程序数据文件夹下的databases目录下
			   dbHelper = new MyDatabaseHelper(this, "site", 1);		
			   cursor = dbHelper.getReadableDatabase().rawQuery(
					   "select * from site", null);
			   if (cursor.moveToFirst() == false)
			   {
				  //为空的Cursor
//				  tvRecent.setVisibility(View.VISIBLE);
//			      tvRecent.setText(No_Mes);
			   }
			   else{
				   //获取历史记录 			  
				   SimpleCursorAdapter adapter =  new SimpleCursorAdapter(this,
						R.layout.eachsite, cursor, new String[]{"name"},new int[]{R.id.tvSite});	   
				   lvHistory.setAdapter(adapter);	
				   lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@SuppressWarnings("unchecked")
					@Override
					  public void onItemClick(AdapterView<?> arg0, View arg1, int position,
							long arg3) {
		                //获取该地点的名字
						Cursor cursor = (Cursor) lvHistory.getItemAtPosition(position);
						int nameCol = cursor.getColumnIndex("name");
	                    String name = cursor.getString(nameCol);    
	                    cursor.close();
	                    flag = 2;
	                    latlngForSearch = convertPlaceNameToLatLngViaWebservice(name);
	                    if(place.isFocused()){
	                    	place.setText(name);
	         			    freshView2();
	                    }	    		
					  }
				   });

			   }	 
		}

	@Override
	public void onFocusChange(View arg0, boolean arg1) {
		
	}

	@Override
	public void onClick(View v) {
		switch(v.getId())
    	{
		   case R.id.clear_place:
			   place.setText("");
			   break;
    	   case R.id.targetPlaceSelect:
      	       chooseSite();
      	       break;		
           case R.id.search1StartSearch:
        	   startSearch();
        	   break;
    	}			
	}
	
	
	private void chooseSite() {
		Intent intent = new Intent(Search1.this,chooseSite_search.class);
		startActivity(intent);
	}
 
	
	private void startSearch() {
	   addSQLite(place.getText().toString());
	   Intent intent = new Intent(Search1.this, Search2.class);
	   if(flag == 3){
		   latlngForSearch = convertPlaceNameToLatLngViaWebservice(place.getText().toString());
		   flag = 3;
	   }
	   intent.putExtra("place", latlngForSearch);
	   String time = null;
	   if(spTime.getSelectedItem().toString().equals("5分钟后")){
		   time = "00_05";
	   }
	   if(spTime.getSelectedItem().toString().equals("10分钟后")){
		   time = "00_10";
	   }
	   if(spTime.getSelectedItem().toString().equals("30分钟后")){
		   time = "00_30";
	   }
	   if(spTime.getSelectedItem().toString().equals("1小时后")){
		   time = "01_00";
	   }
	   if(spTime.getSelectedItem().toString().equals("2小时后")){
		   time = "02_00";
	   }
	   intent.putExtra("spTime", time);
	   startActivity(intent);
	}
	
	/**
	 * 向SQLite中添加新的地点 
	 */
	private void addSQLite(String name){
		dbHelper = new MyDatabaseHelper(this, "site", 1);			  
		Cursor cursor1 = dbHelper.getReadableDatabase().rawQuery(
						  "select * from site where name = ?", new String[]{name});
		boolean flag = cursor1.moveToFirst();
		cursor1.close();
		if (flag == false)
		{			     
		  //空Cursor，即数据库无该地点时则插入地点
	      Cursor cursor2 = dbHelper.getReadableDatabase().rawQuery(
							  "select count(*) from site", null);
		  cursor2.moveToFirst();				
		  long count = cursor2.getLong(0);
		  cursor2.close();
		  if(count < 20){
			//插入新地点
			dbHelper.getReadableDatabase().execSQL("insert into site(name)" +
					   		"values(?)",new String[]{name});
		  }
        }	  
	}	
	

	@Override
	public void afterTextChanged(Editable s) {
		if(s == place.getEditableText())
		{
			String oneplace = s != null ? s.toString() : "";
			//bnClear.setVisibility(place.isFocused() && oneplace.trim().length() > 0 ? View.VISIBLE:View.GONE);
			if(oneplace.equalsIgnoreCase(""))
				bnSearch.setEnabled(false);
			else
				bnSearch.setEnabled(true);
		}		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {			
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {		
	}
	
	@Override
	public void onStop(){
		super.onStop();
		//退出时关闭数据库和游标
		if(dbHelper != null){
			dbHelper.close();
		}
		if(cursor != null){
			cursor.close();
		}
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
}

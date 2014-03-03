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
import android.widget.TextView;
import android.widget.Toast;

import com.example.bigCC.R;
import com.example.util.MyDatabaseHelper;
import com.example.util.BaseActivity;

public class FindRoute1 extends BaseActivity implements TextWatcher,OnClickListener,OnFocusChangeListener {

	private TextView bnSearch,chooseStart,chooseEnd,clearStartButton,clearEndButton;
	private Cursor cursor;
	public static  EditText start,end;
	private ListView lvHistory;
	private String currentPlace;
	
	private MyDatabaseHelper dbHelper;
	private static final String No_Mes = "无最近浏览记录"; 
	private static final int startNo = 1;
	private static final int endNo = 2;
	public static String save_startsite,save_endsite;
	private View views;
	
	private static String Add_startsite = "addstart";
	private static String Add_endsite = "addend";
	
	public static String latlngForSearchStart = null;
	public static String latlngForSearchEnd = null;
	public static int startFlag = 3;//1代表使用地图返回，2代表使用历史记录，3代表手动输入
	public static int endFlag = 3;
	
	public static boolean isFindRoute1Running = false;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);		   
		views = LayoutInflater.from(getParent()).inflate(R.layout.findroute1, null); 
        setContentView(views);
		setupViews();
		//在当前的activity中注册广播
	    IntentFilter filter = new IntentFilter();
		filter.addAction(Add_startsite);	
		filter.addAction(Add_endsite);
		registerReceiver(this.broadcastReceiver1, filter); // 注册
		
		isFindRoute1Running = true;
		if(Setting.isSceneryChecked == true){
			start.setText(Setting.startText);
			end.setText(Setting.endText);
			latlngForSearchStart = Setting.latlngForSearchStart;
			latlngForSearchEnd = Setting.latlngForSearchEnd;
			startFlag = Setting.startFlag;
			endFlag = Setting.endFlag;
		}
	}	

	    // 广播的内部类，当收到动作时，修改EditText的内容
	    private BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
					@Override
					public void onReceive(Context arg0, Intent intent) {					

						if(Add_startsite.equals(intent.getAction())){
							String[] split = Pattern.compile(",").split(save_startsite);
							if(split.length!=2){
								start.setText("");
							}else{//合法
								latlngForSearchStart = save_startsite;
								start.setText(convertLatLngToPlaceNameViaWebservice(new Double(split[0]),new Double(split[1])));
							}
							startFlag = 1;						
							save_startsite = "";
						}
						if(Add_endsite.equals(intent.getAction())){	
							String[] split = Pattern.compile(",").split(save_endsite);
							if(split.length!=2){
								end.setText("");
							}else{//合法
								latlngForSearchEnd = save_endsite;
								end.setText(convertLatLngToPlaceNameViaWebservice(new Double(split[0]),new Double(split[1])));
							}
							endFlag = 1;
							save_endsite = "";
						}
					}
		};		
	
	public void onStart(){
		super.onStart();
		freshView();
		//如果此Activity是由nearby触发的，则起点自动设为传过来的参数，否则不变
		Intent intent = getIntent();
		if(intent.getStringExtra("currentPlace") != null){
			currentPlace = intent.getStringExtra("currentPlace");	
	    	start.setText(currentPlace);
		}	
		else if(intent.getStringExtra("startPlace") != null){
			start.setText(intent.getStringExtra("startPlace"));
		}
		else if(intent.getStringExtra("endPlace") != null){
			end.setText(intent.getStringExtra("endPlace"));
		}
	}

	protected void setupViews()
    {
		lvHistory = (ListView)views.findViewById(R.id.findroute1HistoryList);
		clearStartButton = (TextView)views.findViewById(R.id.clear_startplace);
		clearStartButton.setOnClickListener(this);
		clearEndButton = (TextView)views.findViewById(R.id.clear_endplace);
        clearEndButton.setOnClickListener(this);
        chooseStart = (TextView)views.findViewById(R.id.startPlaceSelect);
        chooseStart.setOnClickListener(this);
        chooseEnd = (TextView)views.findViewById(R.id.endPlaceSelect);
        chooseEnd.setOnClickListener(this);
        start = (EditText)views.findViewById(R.id.startPlaceEditText);
        start.addTextChangedListener(this);
        start.setOnFocusChangeListener(this);
        end = (EditText)views.findViewById(R.id.endPlaceEditText);
        end.addTextChangedListener(this);
        end.setOnFocusChangeListener(this);
        bnSearch = (TextView)views.findViewById(R.id.findroute1StartSearch);
        bnSearch.setOnClickListener(this);
        bnSearch.setEnabled(false);
    }

	@Override
	public void onClick(View v) {
		switch(v.getId())
    	{
		   case R.id.clear_startplace:
			   start.setText("");
			   break;
           case R.id.clear_endplace:
			   end.setText("");
			   break;
           case R.id.findroute1StartSearch:
        	   //访问webservice
        	   startSearch();
        	   break;
           case R.id.startPlaceSelect:
        	   chooseStartSite();
        	   break;
           case R.id.endPlaceSelect:
        	   chooseEndSite();
        	   break;
    	}		
	}

	private void chooseStartSite() {
		Intent intent = new Intent(FindRoute1.this,chooseSite.class);
		intent.putExtra("flag", startNo);				
        startActivity(intent);
	}
	
	private void chooseEndSite() {
		Intent intent = new Intent(FindRoute1.this,chooseSite.class);
		intent.putExtra("flag", endNo);				
		startActivity(intent);
	}

	@Override
	public void afterTextChanged(Editable s) {
		if(s == start.getEditableText())
		{
			String startplace = s != null ? s.toString() : "";
			//改变清除地址按钮状态在OnFocusChange方法里也存在
			//clearStartButton.setVisibility(start.isFocused() && startplace.trim().length() > 0 ? View.VISIBLE : View.GONE);
			checkBtnEnable();
		}
		else if(s == end.getEditableText()){
			String endplace = s != null ? s.toString() : "";
			//clearEndButton.setVisibility(end.isFocused() && endplace.trim().length() > 0 ? View.VISIBLE : View.GONE);
			checkBtnEnable();
		}		
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {	
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
	
	private void checkBtnEnable()
	{
		final String startplace = start.getEditableText().toString();
		final String endplace = end.getEditableText().toString();
		if(isValid(startplace) && isValid(endplace)){
			bnSearch.setEnabled(true);
		}
		else{
			bnSearch.setEnabled(false);
		}
	}
	
    //判断字符串是否合法
	public static boolean isValid(String str)
	{
		return (str != null && str.length() > 0);
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		switch(v.getId())
		{
			case R.id.startPlaceEditText:
				if(hasFocus)
				{
					Editable s = start.getEditableText();
					String startplace = s != null ? s.toString() : "";
					//clearStartButton.setVisibility(startplace.length() > 0 ? View.VISIBLE : View.GONE);
				}
				else
				{
					//clearStartButton.setVisibility(View.GONE);
				}
				break;
				
			case R.id.endPlaceEditText:
				if(hasFocus)
				{
					Editable s = end.getEditableText();
					String endplace = s != null ? s.toString() : "";
					//clearEndButton.setVisibility(endplace.length() > 0 ? View.VISIBLE : View.GONE);
				}
				else
				{
					//clearEndButton.setVisibility(View.GONE);
				}
				break;			
		}
	}
	
	/**
	 * 开始查询，将地点信息存储到SQLite中
	 */
	private void startSearch() {
		addSQLite(start.getText().toString());
		addSQLite(end.getText().toString());	
		Intent intent = new Intent(FindRoute1.this,FindRoute2.class);
		//将起始点信息传递到FindRoute2的Activity中
		if(startFlag == 3){
			latlngForSearchStart = convertPlaceNameToLatLngViaWebservice(start.getText().toString());
			if(latlngForSearchStart == null){
				intent.putExtra("startPlace", "");
				intent.putExtra("endPlace", "");
				startActivity(intent);  
				return;
			}
			startFlag = 3;
		}
		if(endFlag == 3){
			latlngForSearchEnd = convertPlaceNameToLatLngViaWebservice(end.getText().toString());
			if(latlngForSearchEnd == null){
				intent.putExtra("startPlace", "");
				intent.putExtra("endPlace", "");
				startActivity(intent);  
				return;
			}
			endFlag = 3;
		}
		//Toast.makeText(getBaseContext(),latlngForSearchStart+"|"+latlngForSearchEnd, Toast.LENGTH_SHORT).show();
		intent.putExtra("startPlace", latlngForSearchStart);
		intent.putExtra("endPlace", latlngForSearchEnd);		
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
	
	/**
	 * 从数据库中获取最近浏览记录
	 */
	public void freshView(){
		   //创建MyDataBaseHelper对象，指定数据库版本为1，使用相对路径
		   //数据库文件会自动保存在程序数据文件夹下的databases目录下
		   dbHelper = new MyDatabaseHelper(this, "site", 1);		
		   cursor = dbHelper.getReadableDatabase().rawQuery(
				   "select * from site", null);
		   if (cursor.moveToFirst() == false)
		   {
			  //为空的Cursor
//			  tvRecent.setVisibility(View.VISIBLE);
//		      tvRecent.setText(No_Mes);
		   }
		   else{
//			   tvRecent.setVisibility(View.INVISIBLE); 
			   //获取历史记录 			  
			   SimpleCursorAdapter adapter =  new SimpleCursorAdapter(this,
					R.layout.eachsite, cursor, new String[]{"name"},new int[]{R.id.tvSite});	   
			   lvHistory.setAdapter(adapter);			   
			   lvHistory.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@SuppressWarnings("unchecked")
				@Override
				  public void onItemClick(AdapterView<?> arg0, View arg1, int position,
						long arg3) {
	                //获取该产品的名字
					Cursor cursor = (Cursor) lvHistory.getItemAtPosition(position);
					int nameCol = cursor.getColumnIndex("name");
                    String name = cursor.getString(nameCol);    
                    cursor.close();
                    if(start.isFocused()){
                    	if(name == null){
                    		latlngForSearchStart = null;
                    		startFlag = 2;
                        	start.setText("");
                    	}else{
                        	latlngForSearchStart = convertPlaceNameToLatLngViaWebservice(name);
                        	if(latlngForSearchStart == null){
                        		latlngForSearchStart = "";
                        	}
                        	startFlag = 2;
                        	start.setText(name);
                    	}
         			    freshView();
                    }
                    else if(end.isFocused()){
                    	if(name == null){
                    		latlngForSearchEnd = null;
                    		endFlag = 2;
                        	end.setText("");
                    	}else{
                    		latlngForSearchEnd = convertPlaceNameToLatLngViaWebservice(name);
                    		if(latlngForSearchEnd == null){
                    			latlngForSearchEnd = "";
                    		}
                    		endFlag = 2;
                    		end.setText(name);
                    	}
         			    freshView();
                    }		    		
				  }
			   });

		   }	 
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

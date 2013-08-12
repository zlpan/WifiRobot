package org.hustcse.wifirobot;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class loginActivity extends Activity{
	//Debugging
	private static final String TAG ="LOGIN";
	private static final boolean D = true;
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(D)Log.e(TAG, "++onCreate++");
		setContentView(R.layout.login);
		Button zynqButton = (Button)findViewById(R.id.zynq_demo);
		Button n3Button = (Button)findViewById(R.id.n3_n4_demo);
		
		zynqButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Log.e(TAG, "++start zynq demo++");
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), WifiRobotActivity.class);
				startActivity(intent);
			}
		});
		
		n3Button.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.e(TAG, "++start n3/n4 demo++");
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(), bluetooth.class);
				startActivity(intent);
			}
		});
	}
}

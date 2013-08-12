package org.hustcse.wifirobot;

import android.R.bool;
import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import android.bluetooth.*;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

public class bluetooth extends Activity implements OnClickListener{
	//Debugging
	private static final String TAG = "BT";
	private static final boolean D = true;
	//Global flag
	private boolean flag = false;
	//The current Context
	private final Context context = this;
	//Create BlueTooth
    private BluetoothAdapter mBTAdapter = null;
    private BluetoothSocket mBTSocket = null;
    private BluetoothDevice mBTDevice = null;
    //OutPutStream
    private OutputStream mOutStream = null;
    //Connect Thread,Handle conncet
    private ConnectThread connect = null;
    private ConnectedThread connectedThread = null;
    //Connect ProgressDialog
    private ProgressDialog dialog = null;
    // Message types sent from the connectThead
    private static final int MESSAGE_CONNECT_SUCCSESS = 1;
    private static final int MESSAGE_CONNECT_FAILED = 2;
    private static final int MESSAGE_STATE_CHANGE = 3;
    private static final int MESSAGE_CONNECT_LOST = 4;
    private static final int MESSAGE_SET_PIN_ERROR = 5;
    // Constants that indicate the current connection state
    private static final int STATE_NONE = 0;       // we're doing nothing   
    private static final int STATE_LISTEN = 1;     // now listening for incoming connections
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection                                                             
    private static final int STATE_CONNECTED = 3;  // now connected to a remote device
    //control command
    private static final byte[] STOP = {0x00};
    private static final byte[] GO_FRONT = {0x02};
    private static final byte[] GO_BACK ={0x01};
    private static final byte[] TURN_LEFT ={0x04};
    private static final byte[] TURN_RIGHT ={0x08};
   
    //Layout views
    private ImageButton mFrontButton;
    private ImageButton mStopButton;
    private ImageButton mLeftButton;
    private ImageButton mRightButton;
    private ImageButton mBackButton;
    //Intent request code
    private static final int REQUEST_BT_SETTING = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    //SPP UUID
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //Target Device INFO
    private static final String PIN_CODE = "1234";
    private static final String ADDR = "20:13:06:28:43:38";
    private static final String NAME ="HC-06";
    //Toast msg
  	private void toast(String msg){
  		Toast.makeText(getApplicationContext(), msg, 
  				Toast.LENGTH_LONG).show();
  	}
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(D)Log.e(TAG, "++Create bluetooth++");
		setContentView(R.layout.bluetooth);
		//Get Local BTAdapter
		mBTAdapter = BluetoothAdapter.getDefaultAdapter();
		//if the adapter is null, then the Bluetooth is not supported
		if(mBTAdapter == null){
			toast("Bluetooth can not supported!");
			finish();
			return;
		}
		//If BT is not no, request that it be enabled.
	}
	@Override
	public void onStart(){
		super.onStart();
		if(D)Log.e(TAG, "++onStart()++");
		if(!mBTAdapter.isEnabled()){
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivityForResult(intent, REQUEST_ENABLE_BT);
		}
		//otherwise, init the session
		//init() will then be called during onActivityResult
		else if(connect == null)init();
	}
	@Override
	public void onResume(){
		super.onResume();
		if(D)Log.e(TAG, "++onResume()++");
		if(connect != null){
			if(connect.GetState() == STATE_NONE){
				Log.e(TAG, "++start connect a device++");
				while(!flag){
					mBTAdapter.cancelDiscovery();
					if(!BluetoothAdapter.checkBluetoothAddress(ADDR)){
						Log.e(TAG, "++addr is not effient++");
						finish();
					}
					try {
						BluetoothDevice device = mBTAdapter.getRemoteDevice(ADDR);
						if(device.getBondState() != BluetoothDevice.BOND_BONDED){
							ClsUtils.setPin(device.getClass(), device, PIN_CODE);
							ClsUtils.createBond(device.getClass(), device);
							mBTDevice = device;	
							flag = true;
							return;
						}
						else {
							ClsUtils.createBond(device.getClass(), device);
							ClsUtils.setPin(device.getClass(), device,PIN_CODE);
							ClsUtils.createBond(device.getClass(), device);
							mBTDevice = device;
							flag = true;
						}
					}catch (Exception e) {
							// TODO: handle exception
						Log.e(TAG, "++can not bond to the device, please check!");
						toast("++can not bond to the device,please check!");
						flag = false;
						finish();
					}
				}
				dialog = ProgressDialog.show(this, "BTConnect", 
						"Connecting, please wait!",true);
				connect.start();
			}
		}
	}
	@Override
	public void onPause()
	{
		super.onPause();
		if(D)Log.e(TAG, "++onPause++");
	}
	@Override
	public void onStop()
	{
		super.onStop();
		if(D)Log.e(TAG, "++onStop++");
		//close the bluetooth socket connect
		try {
			if(mBTSocket != null){
				mBTSocket.close();
				toast("++close bluetooth++");
			}
			//connect.SetState(STATE_NONE);
		} catch (IOException e) {
			// TODO: handle exception
			Log.e(TAG, "++ can not close socket!");
		}
		toast("++Thank you for use!bye!++");
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		if(D)Log.e(TAG, "++onDestroy++");
	}
	//init layout views
	private void init() {
		// TODO Auto-generated method stub
		if(D)Log.e(TAG, "++init++");
		mFrontButton = (ImageButton)findViewById(R.id.front);
		mBackButton  = (ImageButton)findViewById(R.id.back);
		mLeftButton  = (ImageButton)findViewById(R.id.left);
		mRightButton = (ImageButton)findViewById(R.id.right);
		mStopButton  = (ImageButton)findViewById(R.id.stop);
		//action
		mFrontButton.setOnClickListener(this);
		mBackButton.setOnClickListener(this);
		mLeftButton.setOnClickListener(this);
		mRightButton.setOnClickListener(this);		
		mStopButton.setOnClickListener(this);
		//get a thread to connect socket
		connect = new ConnectThread();
	}
	
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch(v.getId())
		{
		case R.id.front:
			write(GO_FRONT);
			break;
		case R.id.back:
			write(GO_BACK);
			break;
		case R.id.left:
			write(TURN_LEFT);
			break;
		case R.id.right:
			write(TURN_RIGHT);
			break;
		case R.id.stop:
			write(STOP);
			break;
		default:break;
		}
	}
	//When the Intent Activity return
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch(requestCode)
        {
        case REQUEST_BT_SETTING:
        case REQUEST_ENABLE_BT:
        {
        	if(resultCode == Activity.RESULT_OK)
        	{
        		//if the BT is enabled, then just init a session
        		Log.e(TAG, "++onResult++");
        		init();
        	} else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                toast("++BT not enbale++");
                finish();
            }
        }
        default:break;
        }
	}
	// The Handler that gets information , and handle it
	private final Handler handler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what)
			{
			case MESSAGE_CONNECT_SUCCSESS:
				Log.e(TAG, "++conect device success++");
				toast("++connect success++");
				dialog.dismiss();
				connected(mBTSocket);
				break;
			case MESSAGE_CONNECT_FAILED:
				Log.e(TAG, "++can not connect please cheak if the" +
						"samrt car opened");
				toast("++can not connect the bluetooth++");
				finish();
				dialog.dismiss();
				break;
			case MESSAGE_CONNECT_LOST:
				Log.e(TAG, "++connect lost, please cheak++");
				toast("++Socket Lost++");
				finish();
			case MESSAGE_STATE_CHANGE:
			default:
				break;
			}
		}
	};
	private void connected(BluetoothSocket socket)
	{
		connectedThread = new ConnectedThread(socket);
		connectedThread.start();
	}
	private void write(byte[] data)
	{
		if(connectedThread == null)
		{
			toast("++connect has not established++");
			finish();
		}
		else connectedThread.write(data);
	}
	//handle Socket connct
	private class ConnectThread extends Thread{
		private int mState;
		//private ClsUtils utils;
		public BluetoothDevice device;
		public ConnectThread(){
			mState = STATE_NONE;
		}
		@Override
		public void run()
		{
			mBTAdapter.cancelDiscovery();
			//utils = new ClsUtils();
			Log.e(TAG, "++run++");
			try {
				//connect by spp
				
				Method method = mBTDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
		        mBTSocket = (BluetoothSocket) method.invoke(mBTDevice, 1);
		        Log.e(TAG, "++Sokcet Create Success!++");
				mBTSocket.connect();
				mOutStream = mBTSocket.getOutputStream();
				handler.obtainMessage(MESSAGE_CONNECT_SUCCSESS).sendToTarget();
				SetState(STATE_CONNECTED);
			} catch (Exception e) {
				// TODO: handle exception
				handler.obtainMessage(MESSAGE_CONNECT_FAILED).sendToTarget();
			}
		}
		public synchronized void SetState(int state)
		{
			mState = state;
			handler.obtainMessage(MESSAGE_STATE_CHANGE, mState).sendToTarget();
		}
		public synchronized int GetState()
		{
			return mState;
		}
		//Check if the bluetooth is connect or not
		public boolean isConnected()
		{
			return mBTSocket.isConnected();
		}
	}
	//This class will run when the socket connected
	private class ConnectedThread extends Thread{
		private final BluetoothSocket mmSocket;
		private final OutputStream mmOutStream;
		private final InputStream mmInStream;
		
		public ConnectedThread(BluetoothSocket socket)
		{
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (Exception e) {
				// TODO: handle exception
			}
			mmOutStream = tmpOut;
			mmInStream = tmpIn;
		}
		@Override
		public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            String ttString;
            int bytes;
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    ttString = buffer.toString();
                    // Send the obtained bytes to the UI Activity   
                    sleep(1000);
                    Log.e(TAG, ttString);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    handler.obtainMessage(MESSAGE_CONNECT_LOST).sendToTarget();
                    break;
                } catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
		//write the cmd to the outputstream
		public  void write(byte[] cmd){
			try {
				mmOutStream.write(cmd);
				Log.e(TAG, ""+cmd[0]);
			} catch (IOException e) {
				// TODO: handle exception
				Log.e(TAG, "++write fialed++");
				handler.obtainMessage(MESSAGE_CONNECT_LOST).sendToTarget();
			}
		}
		//cancel the socket
		public void cancel(){
			try {
				mmSocket.close();
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	//The BroadCastReceiver that listens for setpin finish
	//and close the setting dialog
	private final BroadcastReceiver mReceiver = new BroadcastReceiver(){
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			String action = intent.getAction();
			if(action.equals("android.bluetooth.device.action.PAIRING_REQUEST"))
			{
				BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				Log.i(TAG,"dddd");
				try {
					ClsUtils.setPin(btDevice.getClass(), btDevice, PIN_CODE);
					ClsUtils.createBond(btDevice.getClass(), btDevice);
					ClsUtils.cancelPairingUserInput(btDevice.getClass(), btDevice);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
	};
}

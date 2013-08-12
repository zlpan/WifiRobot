package org.hustcse.wifirobot;

import java.io.*;
import java.net.*;
import java.sql.Date;
import java.text.SimpleDateFormat;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.MobileAnarchy.Android.Widgets.Joystick.JoystickMovedListener;
import com.MobileAnarchy.Android.Widgets.Joystick.JoystickView;

public class WifiRobotActivity extends Activity {
	/* 璁剧疆LOG鐨凾AG */
	private static String TAG = "ZRobot";
	/* 璁剧疆鏄惁闇�LOG */
	final static boolean D = true;	
	
	/* 榛樿鐨勪笁涓棰戞簮鐨勫湴鍧�*/
	private static String CAR_VIDEO_ADDR = "http://192.168.1.100:8080/?action=stream";
	private static String ARM_VIDEO_ADDR = "http://192.168.1.100:8090/?action=snapshot";
	private static String OPENCV_VIDEO_ADDR = "http://192.168.1.100/detection.jpg";

	/* 榛樿鐨勭洰鏍嘥CP鏈嶅姟鍣ㄧ殑IP鍦板潃 */
	
	private static String DIST_TCPIPADDR = "192.168.1.100";
	/* 榛樿鐨勭洰鏍嘥CP鏈嶅姟鍣ㄧ殑IP鍦板潃 */
	private static int DIST_TCPPORT = 2001;

	/* 鐢ㄤ簬璁剧疆鐣岄潰鐨刾reference */
	private SharedPreferences preferences;
	
	/* 瀹氫箟涓�釜鏄剧ず瑙嗛鐨勭被 */
	DrawVideo m_DrawVideo;

	/* TCP鏈嶅姟鍣ㄧ殑IP鍜孭ORT */
	private String dist_tcp_addr;
	private int dist_tcp_port;

	/* 瑙嗛褰撳墠鐨勭姸鎬�
	 * UPDATE : 瑙嗛鏂板抚鏇存柊
	 * ERROR  : 瑙嗛鏁版嵁閿欒
	 * END    : 瑙嗛缁撴潫 */
	/* MSG_ 寮�ご鐨勪笢瑗块兘琛ㄧずHandler鍙戦�鐨勬秷鎭�*/
	final static int MSG_VIDEO_UPDATE = 1;
	final static int MSG_VIDEO_ERROR = 2;
	final static int MSG_VIDEO_END = 3;

	/* 鍜宼cp_ctrl绫荤浉鍏宠仈
	 * 鐢ㄤ簬handler浼犻�娑堟伅鍒癆ctivity绫讳笂
	 * 鐢ㄤ簬鏇存柊UI涔嬬被鐨勪俊鎭�鍚﹀垯灏变細鎶ラ敊(涓婁笅鏂囬敊璇� */
	
	/* 鏄剧ずTOAST娑堟伅 */
	final static int MSG_DISPLAY_TOAST = 100;
	/* 淇preference閲岄潰鐨勯敊璇緭鍏ユ椂鐨勬秷鎭殑鍩哄噯鍊�*/
	final static int MSG_FIX_PREFERENCE = 1000;
	/* 淇preference鏃剁殑IP鐨勫亸绉婚噺 */
	final static int FIX_IP_PREFERENCE = 0;

	/* 杩涘害绛夊緟妗�	 * TCP杩炴帴绛夊緟
	 * 鍥惧儚鑾峰彇绛夊緟
	 * 瑙嗛鑾峰彇绛夊緟 */
	ProgressDialog mDialog_Connect, mDialog_ImageCap, mDialog_VideoCap;
	/* 瀵硅瘽妗嗗搴旂殑ID鍙�*/
	private static final int CONNECT_DIALOG_KEY = 0;
	private static final int IMGCAP_DIALOG_KEY = 1;
	private static final int VIDEOCAP_DIALOG_KEY = 2;

	/* 鍚勭BUTTON绛夊璞＄殑瀹氫箟 
	 * 璇峰弬瑙乺es/layout/main.xml */
	Button btn_image;
	Button btn_video_srcsel;
	Button btn_video;
	Button btn_follow_road_mode_ctrl;
	Button btn_set_camera2LCD;
	Button btn_control_mode;
	Button btn_connect;
	Button btn_laser_ctrl;

	ImageView img_camera;
	
	JoystickView joystick;
	JoystickView joystickArm;

	TextView txtAngle, txtSpeed, txtTCPState;

	/* 褰撳墠鐨勬帶鍒舵ā寮�鑷姩鎴栬�鎵嬪姩 */
	private boolean auto_control_mode = false;	
	
	/* 婵�厜鎺у埗鍛戒护鐩稿叧 */
	private final static int LASER_OFF = 0;
	private final static int LASER_ON = 1;
	private int laser_ctrl = LASER_OFF;


	/* video source select : 
	 * CAR 
	 * ARM
	 * OPENCV
	 * */	
	final static int MAX_VIDEO_SRC_CNT = 3;
	
	private int video_source_sel = 0;
	
	final static int CAR_VIDEO_SRC = 0;
	final static int ARM_VIDEO_SRC = 1;
	final static int OPENCV_VIDEO_SRC = 2;

	/* video source address array */
	private String[] video_addr = new String[MAX_VIDEO_SRC_CNT];
	/* current video address */
	private String cur_video_addr; 

	/* 鍥惧儚鎴栬�瑙嗛鐨勫噯澶囨儏鍐�*/
	private boolean image_ready_flag = false;
	private boolean video_ready_flag = false;

	/* 鑷畾涔夌殑tcp娑堟伅鐨勪紶閫掑崗璁浉鍏�	 * ctrl_code 鎺у埗瀛�	 * data_length 鏁版嵁闀垮害
	 * ctrl_data 鎺у埗鏁版嵁 */
	short tcp_ctrl_code;
	short tcp_data_length;
	byte[] tcp_ctrl_data = new byte[1024];

	/* 鑷畾涔夌殑TCP鎺у埗绫�*/
	tcp_ctrl tcp_ctrl_obj;

	Bitmap img_camera_bmp;
	
	/* 鐢ㄤ簬鎸囩ず褰撳墠瑙嗛鏁版嵁鐨勭姸鎬�	 * false : 鏃犺棰戞暟鎹鍦ㄦ樉绀�	 * true  : 瑙嗛鏁版嵁姝ｅ湪鏄剧ず */
	boolean video_flag = false;

	/* 鎺у埗灏忚溅鐨勮浆鍚戣搴︿互鍙婂墠杩涢�搴�*/
	int operate_angle_last = 0;
	int operate_speed_last = 0;
	int operate_angle = 0;
	int operate_speed = 0;

	/* 鏈�ぇ鐨勯�搴﹀彲璋冨崟浣�*/
	private final static int MAX_SPEED_UNIT = 10;
	/* 涓�釜閫熷害鍗曚綅瀵瑰簲鐨勫� */
	private final static int SPEED_SCALE = 5;
	
	/* 鏈烘鑷傜殑X,Y鍋忕Щ */
	int arm_x_offset = 0;
	int arm_y_offset = 0;
	int arm_x_offset_last = 0;
	int arm_y_offset_last = 0;
	private final static int MAX_ARM_UNIT = 10;
	private final static int ARM_X_SCALE = 9;
	private final static int ARM_Y_SCALE = 9;

	/* 褰撳墠鐨勪笂涓嬫枃 */
	private Context mContext;

	private static final int REQ_SYSTEM_SETTINGS = 0x0;

	/* 鐢ㄤ簬鑾峰彇鎵嬫満灞忓箷鐨勫ぇ灏忓垎杈ㄧ巼 鏂瑰悜*/
	Display display;
	private int screen_Width = 0;
	private int screen_Height = 0;
	private int screen_Orientation = 0;
	
	/* JoyStick绫荤殑閰嶇疆鍙傛暟绫�*/
	LayoutParams joyviewParams;
	LayoutParams joyviewParamsArm;

	/* 鐢ㄤ簬鑷姩閫傞厤鏃剁殑鎽囨潌 鎸夐挳 TextView绛�	 * 鐨勬寜灞忓箷缂╂斁姣斾緥*/
	private float joystick_scale = 3;
	
	private float btn_scale = 42;
	private float txtview_scale = 42;

	/* 鐢ㄤ簬璁板綍APP鍚姩鏃堕棿 鏂逛究璋冭瘯浣跨敤 */
	private long start_time = 0;
	private long end_time = 0;

	private long benchmark_start = 0;
	private long benchmark_end = 0;

	/* 鐢ㄤ簬璁板綍APP鍚姩闃舵浜轰负杈撳嚭LOG淇℃伅 */
	private static int MAX_PASS = 40;
	private long[] time_pass = new long[MAX_PASS + 1];
	private String[] pass_log = new String[MAX_PASS + 1];

	/* 鐢ㄤ簬璁板綍绯荤粺淇℃伅鐩稿叧 */
	private static int MAX_INFO_CNT = 40;
	private String[] SystemInfo = new String[MAX_INFO_CNT];
	private int SystemInfoCnt = 0;

	/* 璁板綍褰撳墠PASS鐨勭姸鎬�*/
	private int pass_cnt = 0;
	private String MYLOG_PATH_SD = "hrrobotlog";


	/** Called when the activity is first created. */
	/** 鎵�皳鐨勪富鍑芥暟 **/
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "program startup");
		init_log_time();

		start_time = System.currentTimeMillis();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		log_pass_time("Set ContentView OK");
		/* get the setting preference for update the setting information */
		preferences = PreferenceManager.getDefaultSharedPreferences(this);

		/* 鑾峰彇灞忓箷鐨勫搴﹂暱搴�*/
		display = getWindowManager().getDefaultDisplay();
		screen_Orientation = display.getOrientation();
		if ((screen_Orientation == Surface.ROTATION_0)
				|| (screen_Orientation == Surface.ROTATION_180)) {
			screen_Width = display.getHeight();
			screen_Height = display.getWidth();
		} else {
			screen_Width = display.getWidth();
			screen_Height = display.getHeight();
		}

		Log.i(TAG, "Screen Resolution:" + screen_Height + " X " + screen_Width);
		log_pass_time("screen info ok");

		/* 鍚勭鎸夐挳绛夊璞＄殑瀹氫箟鍒濆鍖�*/
		btn_video_srcsel = (Button) findViewById(R.id.button_video_src);
		btn_video = (Button) findViewById(R.id.button_video);
		btn_control_mode = (Button) findViewById(R.id.button_control);
		btn_connect = (Button) findViewById(R.id.button_connect);
		btn_laser_ctrl = (Button) findViewById(R.id.button_laser_ctrl);

		img_camera = (ImageView) findViewById(R.id.imageView_camera);

		/* car direction control joystick initialize */
		joystick = (JoystickView) findViewById(R.id.joystickView); 
		/* machine arm control joystick initialize */
		joystickArm = (JoystickView) findViewById(R.id.joystickARM); 

		txtAngle = (TextView) findViewById(R.id.TextViewX);
		txtSpeed = (TextView) findViewById(R.id.TextViewY);
		txtTCPState = (TextView) findViewById(R.id.TextViewTCPState);

		/* 璁剧疆鎸夐挳瀵硅薄鐨勫崐閫忔槑鏁堟灉 */
		btn_video_srcsel.getBackground().setAlpha(100); 
		btn_video.getBackground().setAlpha(100); 
		btn_control_mode.getBackground().setAlpha(100);
		btn_connect.getBackground().setAlpha(100);
		btn_laser_ctrl.getBackground().setAlpha(100);

		/* 鏍规嵁灞忓箷鐨勫ぇ灏忓拰鍒嗚鲸鐜囪嚜鍔ㄩ�閰嶆寜閽�鏂囨湰妗嗙瓑瀵硅薄鐨勫ぇ灏�*/
		
		/* 鎸夐挳閫傞厤 */
		btn_video_srcsel.setTextSize(screen_Width / btn_scale);
		btn_video.setTextSize(screen_Width / btn_scale);
		btn_control_mode.setTextSize(screen_Width / btn_scale);
		btn_connect.setTextSize(screen_Width / btn_scale);
		btn_laser_ctrl.setTextSize(screen_Width / btn_scale);

		/* TextView閫傞厤 */
		((TextView) findViewById(R.id.TextViewAngle)).setTextSize(screen_Width
				/ txtview_scale);
		((TextView) findViewById(R.id.TextViewSpeed)).setTextSize(screen_Width
				/ txtview_scale);
		((TextView) findViewById(R.id.TextViewTCPStateTxt))
				.setTextSize(screen_Width / txtview_scale);
		txtAngle.setTextSize(screen_Width / txtview_scale);
		txtSpeed.setTextSize(screen_Width / txtview_scale);
		txtTCPState.setTextSize(screen_Width / txtview_scale);

		/* 璁剧疆鎸夐挳绛夊璞＄殑鎸変笅鍚庡搷搴旂殑Listener */
		btn_video_srcsel.setOnClickListener(video_src_acquire_listener);
		btn_video.setOnClickListener(video_acquire_listener);

		btn_control_mode.setOnClickListener(ctrl_btn_listener);
		btn_connect.setOnClickListener(connect_listener);
		btn_laser_ctrl.setOnClickListener(laser_ctrl_listener);

		log_pass_time("all objects init ok");

		update_preference();
		
		/* TCP杩炴帴鍒濆鍖� 涓嶅厛寤虹珛杩炴帴,
		 * 杩欐牱瀛愬彲浠ュ姞蹇獳PP鐨勫惎鍔ㄩ�搴�
		 * 寤虹珛TCP杩炴帴(鏈垚鍔�闈炲父鑰楁椂 */
		tcp_ctrl_obj = new tcp_ctrl(getApplicationContext(),
				mHandler_UpdateUI, dist_tcp_addr, dist_tcp_port);
		log_pass_time("tcp ok");

		mContext = getApplicationContext();
		/* 鏍规嵁褰撳墠TCP鏄惁杩炴帴鍒癟CP鏈嶅姟鍣�		 * 鍐冲畾褰撳墠TCP鐘舵�骞朵笖鏄剧ず鍒板睆骞曚笂 */
		if (tcp_ctrl_obj.mTcp_ctrl_client.isSocketOK()) {
			txtTCPState.setText(R.string.tcpstate_online);
		} else {
			txtTCPState.setText(R.string.tcpstate_offline);
		}
		
		/* 鏍规嵁灞忓箷澶у皬鑷姩閫傞厤閬ユ帶灏忚溅鐨勭晫闈㈢殑鎽囨潌澶у皬 */
		joyviewParams = joystick.getLayoutParams();
		joyviewParams.width = (int) (screen_Width / joystick_scale);
		joyviewParams.height = (int) (screen_Width / joystick_scale);
		joystick.setLayoutParams(joyviewParams);
		/* 璁剧疆灏忚溅鎿嶄綔鎽囨潌杩愬姩鏃剁殑Listener */
		joystick.setOnJostickMovedListener(joystickctrl_listener);

		/* 鏍规嵁灞忓箷澶у皬鑷姩瑙嗛鎿嶄綔鏈烘鑷傜殑鐣岄潰鐨勬憞鏉嗗ぇ灏�*/
		joyviewParams = joystickArm.getLayoutParams();
		joyviewParams.width = (int) (screen_Width / joystick_scale);
		joyviewParams.height = (int) (screen_Width / joystick_scale);
		joystickArm.setLayoutParams(joyviewParams);
		/* 璁剧疆鏈烘鑷傛帶鍒舵憞鏉嗚繍鍔ㄦ椂鐨凩istener */
		joystickArm.setOnJostickMovedListener(joystickarm_listener);

		log_pass_time("joystick ok");

		end_time = System.currentTimeMillis();

		Log.i(TAG, "app startup use " + (end_time - start_time) + " ms");
		log_pass_time("program started");
		end_log_time();
		log_system_info();
		write_log2file("hrrobotup", false);
	}

	@Override
	protected void onResume() {
		/**
		 * 绋嬪簭浠庡悗鍙版仮澶嶅悗
		 * 寮哄埗璁剧疆涓烘í灞�		 */
		if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		}
		super.onResume();
	}

	/* 鍒涘缓涓�釜鎸変笅Menu閿脊鍑虹殑Menu閫夋嫨妗�
	 * menu 浣嶄簬res/menu/menu.xml */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/* 鏇存柊preference涓殑璁剧疆鏁版嵁 */
	public void update_preference() {
		int temp;
		try {
			video_addr[0] = preferences.getString(
					getResources().getString(R.string.videoaddr1),
					CAR_VIDEO_ADDR);
			video_addr[1] = preferences.getString(
					getResources().getString(R.string.videoaddr2),
					ARM_VIDEO_ADDR);
			video_addr[2] = preferences.getString(
					getResources().getString(R.string.videoaddr3),
					OPENCV_VIDEO_ADDR);
			dist_tcp_addr = preferences.getString(
					getResources().getString(R.string.distipaddr),
					DIST_TCPIPADDR);
			/* NOTICE 杩欓噷闇�娉ㄦ剰涓�			 * 闇�灏濊瘯璇诲彇TCP绔彛鍙�			 *
			 *  鐢变簬鏆傛椂鎴戜篃涓嶆竻妤氬浣曞垱寤轰竴涓彧鑳借緭鍏ユ暟瀛楃殑鏂囨湰妗�			 
			 *  * 鎵�互鍙兘鍦ㄨ繖閲屽垽鏂竴涓嬬敤鎴疯緭鍏ョ殑PORT鍙锋槸鍚︿负鏁板瓧
			 * 涓嶆槸鏁板瓧鐨勮瘽灏卞己鍒跺皢鏂囨湰妗嗗唴瀹规敼涓洪粯璁ょ殑PORT */
			try {
				temp = Integer.parseInt((preferences.getString(getResources()
						.getString(R.string.disttcpport), String
						.valueOf(DIST_TCPPORT))));
				dist_tcp_port = temp;
			} catch (Exception e) {
				/* 寮哄埗灏唒reference涓殑閿欒杈撳叆鎭㈠涓洪粯璁�*/
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(
						getResources().getString(R.string.disttcpport),
						String.valueOf(dist_tcp_port));
				editor.commit();
			}
		} catch (Exception e) { 
			Log.d(TAG, e.toString());
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int ItemId; 
		
		/* 鑾峰彇閫変腑鐨凪enu鑿滃崟鐨処D鍙�*/
		ItemId = item.getItemId() ;
		
		switch (ItemId){
		case R.id.Settings:
			/* 璺宠浆鍒板搴旂殑preference绫�*/
			startActivityForResult(new Intent(this, Preferences.class),
					REQ_SYSTEM_SETTINGS);
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
			/* 鎸変笅鐨勫鏋滄槸BACK锛屽悓鏃舵病鏈夐噸澶�
			 * 绋嬪簭寮哄埗閫�嚭
			 * TODO 缂轰箯璧勬簮閲婃斁? */
			Log.d(TAG, "Program Exit!");
			System.exit(0);
		}
		return super.onKeyDown(keyCode, event);
	}

	/* 寤虹珛TCP杩炴帴鎸夐挳瀵瑰簲鐨凩istener  */
	private OnClickListener connect_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			/* 鏇存柊瀵瑰簲鐨刾reference涓殑鍐呭
			 * 涓昏鏄悇绉嶈棰戝湴鍧�			 * TCP鏈嶅姟鍣ㄧ殑IP浠ュ強PORT */
			update_preference();
			showDialog(CONNECT_DIALOG_KEY);
		}
	};
	
	/* 瑙嗛婧愬垏鎹㈡寜閽殑listener */
	private OnClickListener video_src_acquire_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			process_video_src_select(v.getId());
		}
	};

	/* 鑾峰彇瑙嗛鎸夐挳鐨刲istener */
	private OnClickListener video_acquire_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			post_ctrl_btnclk_msg(v.getId());
		}
	};

	/* 鍏朵粬鎺у埗鎸夐挳鐨凩istener */
	private OnClickListener ctrl_btn_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			/* 鏍规嵁灏忚溅鎺у埗鎸夐挳鐨刬d鏉ュ鐞嗕簨浠�*/
			post_ctrl_btnclk_msg(v.getId());
		}
	};

	/* 婵�厜鎺у埗鎸夐挳鐨凩istener */
	private OnClickListener laser_ctrl_listener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			/* 鍙戦�婵�厜鎺у埗鍛戒护 */
			postLaserCtrlMsg(v.getId());
		}
	};
	
	/* 鏂瑰悜浠ュ強閫熷害鎺у埗鎽囨潌鐨凩istener瀹氫箟瀹炵幇 */
	private JoystickMovedListener joystickctrl_listener = new JoystickMovedListener() {
		@Override
		public void OnMoved(int pan, int tilt) {
			int operate_x = 0;
			int operate_y = 0;

			operate_x = pan;
			operate_y = -tilt;
			calc_speed_and_angle(operate_x, operate_y);
			txtAngle.setText(Integer.toString(operate_angle));
			txtSpeed.setText(Integer.toString(operate_speed));
			checkSendOperateCarMsg();
			/* 杩涚▼涓诲姩璁╁嚭鎺у埗鏉冿紝
			 * 杩欐牱鐨勮瘽,鍦ㄦ搷浣滄憞鏉嗘椂杩樻槸鍙互鏄剧ず鍔ㄦ�鍥惧儚鐨�			 * 铏界劧鏁堟灉涓嶅ソ */
			Thread.yield(); 
		}

		@Override
		public void OnReleased() {
			txtAngle.setText("released");
			txtSpeed.setText("released");
			Thread.yield();
		}

		@Override
		public void OnReturnedToCenter() {
			txtAngle.setText("stopped");
			txtSpeed.setText("stopped");
			operate_angle = 0;
			operate_speed = 0;
			checkSendOperateCarMsg();
			Thread.yield();
		};
	};

	/* 鏈烘鑷傛帶鍒剁殑鎽囨潌鐨刲istener瀹炵幇 */
	private JoystickMovedListener joystickarm_listener = new JoystickMovedListener() {
		@Override
		public void OnMoved(int pan, int tilt) {
			int operate_x = 0;
			int operate_y = 0;

			operate_x = pan;
			operate_y = -tilt;
			calc_arm_xy(operate_x, operate_y);

			checkSendOperateArmMsg();
			/* 杩涚▼涓诲姩璁╁嚭鎺у埗鏉冿紝
			 * 杩欐牱鐨勮瘽,鍦ㄦ搷浣滄憞鏉嗘椂杩樻槸鍙互鏄剧ず鍔ㄦ�鍥惧儚鐨�			 * 铏界劧鏁堟灉涓嶅ソ */
			Thread.yield(); 
		}

		@Override
		public void OnReleased() {
			Thread.yield();
		}

		@Override
		public void OnReturnedToCenter() {
			arm_x_offset = 0;
			arm_y_offset = 0;
			checkSendOperateArmMsg();
			Thread.yield();
		};
	};

	/*** OPERATE CAR ***/
	/* 鍙戦�鑾峰彇瑙掑害鎺у埗鍜岄�搴︽帶鍒剁殑鍛戒护 */
	private void postOperateCarMessage(int angle, int speed) {
		short ctrl_cmd;
		short ctrl_prefix;
		byte[] msg = new byte[4];

		/* 鍑嗗寰呭彂閫佺殑TCP娑堟伅鏁版嵁 */
		ctrl_prefix = ctrl_prefixs.encode_ctrlprefix(
				ctrl_prefixs.write_request, ctrl_prefixs.less_data_request,
				ctrl_prefixs.withoutack);
		ctrl_cmd = ctrlcmds.OPERATE_CAR;
		msg[0] = (byte) (angle & 0xff);
		msg[1] = (byte) ((angle >> 8) & 0xff);
		msg[2] = (byte) (speed & 0xff);
		msg[3] = (byte) ((speed >> 8) & 0xff);

		post_tcp_msg(ctrl_prefix, ctrl_cmd, msg);
	}
	
	/* 娴嬭瘯鏄惁瑙掑害鍜岄�搴︽病鏈夋敼鍙�骞跺彂閫佹帶鍒跺皬杞﹀懡浠�*/
	private void checkSendOperateCarMsg() {
		/* 妫�煡褰撳墠鐨勮搴︽垨鑰呴�搴︽槸鍚︽敼鍙�*/
		if (!((operate_angle == operate_angle_last) && (operate_speed == operate_speed_last))) {
			/* 褰撳墠socket鍙敤鎵嶈繘琛屾暟鎹彂閫�*/
			if ((tcp_ctrl_obj.mTcp_ctrl_client.isSocketOK())) { 
				postOperateCarMessage(operate_angle, operate_speed);
			}
			operate_angle_last = operate_angle;
			operate_speed_last = operate_speed;
		}
	}

	/* 閫氳繃褰撳墠鍧愭爣璁＄畻瑙掑害鍜岄�搴︿俊鎭�*/
	private void calc_speed_and_angle(int operate_x, int operate_y) {
		operate_speed = (int) Math.sqrt((operate_x * operate_x)
				+ (operate_y * operate_y));

		if (operate_y < 0) {
			operate_speed = -operate_speed;
		}

		if (operate_x == 0) {
			if (operate_y == 0) {
				operate_angle = 0;
			} else if (operate_y > 0) {
				operate_angle = 90;
			} else {
				operate_angle = -90;
			}
		} else if (operate_y == 0) {
			if (operate_x == 0) {
				operate_angle = 0;
			} else if (operate_x > 0) {
				operate_angle = 0;
			} else {
				operate_angle = 180;
			}
		} else {
			operate_angle = (int) ((Math.atan2(operate_y, operate_x) / Math.PI) * 180);
		}

		if (operate_speed == 0) {
			operate_angle = 0;
		} else if (operate_speed > 0) {
			operate_angle = 90 - operate_angle;
		} else {
			operate_angle = operate_angle + 90;
		}

		if (operate_speed > MAX_SPEED_UNIT) {
			operate_speed = MAX_SPEED_UNIT;
		} else if (operate_speed < -MAX_SPEED_UNIT) {
			operate_speed = -MAX_SPEED_UNIT;
		}
		operate_speed = operate_speed * SPEED_SCALE;

	}



	/*** OPERATE_ARM ***/
	/* 鍙戦�鑾峰彇鏈烘鑷俋Y鎺у埗鐨勫懡浠�*/
	private void postOperateArmMessage(int x, int y) {
		short ctrl_cmd;
		short ctrl_prefix;
		byte[] msg = new byte[4];

		ctrl_prefix = ctrl_prefixs.encode_ctrlprefix(
				ctrl_prefixs.write_request, ctrl_prefixs.less_data_request,
				ctrl_prefixs.withoutack);
		ctrl_cmd = ctrlcmds.OPERATE_ARM;
		msg[0] = (byte) (x & 0xff);
		msg[1] = (byte) ((x >> 8) & 0xff);
		msg[2] = (byte) (y & 0xff);
		msg[3] = (byte) ((y >> 8) & 0xff);

		post_tcp_msg(ctrl_prefix, ctrl_cmd, msg);
	}
	
	/* 娴嬭瘯鏈烘鑷傜殑XY鏄惁娌℃湁鏀瑰彉 骞跺彂閫佹帶鍒舵満姊拌噦鍛戒护 */
	private void checkSendOperateArmMsg() {
		if (!((arm_x_offset == arm_x_offset_last) && (arm_y_offset == arm_y_offset_last))) {
			if ((tcp_ctrl_obj.mTcp_ctrl_client.isSocketOK())) {
				/*
				 * 褰撳墠socket鍙敤鎵嶈繘琛屾暟鎹彂閫�				 */
				postOperateArmMessage(arm_x_offset, arm_y_offset);
			}
			arm_x_offset_last = arm_x_offset;
			arm_y_offset_last = arm_y_offset;
		}
	}
	
	/* 璁＄畻鏈烘鑷傜殑X,Y鍋忕Щ鍊�*/
	private void calc_arm_xy(int operate_x, int operate_y) {
		if (operate_x > MAX_ARM_UNIT) {
			operate_x = MAX_ARM_UNIT;
		} else if (operate_x < -MAX_ARM_UNIT) {
			operate_x = -MAX_ARM_UNIT;
		}

		if (operate_y > MAX_ARM_UNIT) {
			operate_y = MAX_ARM_UNIT;
		} else if (operate_y < -MAX_ARM_UNIT) {
			operate_y = -MAX_ARM_UNIT;
		}

		arm_x_offset = operate_x * ARM_X_SCALE;
		arm_y_offset = operate_y * ARM_Y_SCALE;
	}



	/* 鐢ㄤ簬tcp_ctrl绫荤殑handler 
	 * 涓昏鏄湁浜涢渶瑕佹洿鏂癠I涔嬬被鐨勬搷浣�*/
	private final Handler mHandler_UpdateUI = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_DISPLAY_TOAST:
				disp_toast((String) msg.obj);
				break;
			case (MSG_FIX_PREFERENCE + FIX_IP_PREFERENCE):
				String ip = (String) msg.obj;
				SharedPreferences.Editor editor = preferences.edit();
				editor.putString(getResources().getString(R.string.distipaddr),
						ip);
				editor.commit();
				break;
			default:
				break;
			}

		}
	};



	/* 鏍规嵁涓嶅悓鐨勮繘搴︽ID鍒涘缓杩涘害妗�*/
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNECT_DIALOG_KEY:
			/* 鍦ㄥ綋鍓嶇殑Activity涓嬪垱寤轰竴涓繘搴︽ */
			mDialog_Connect = new ProgressDialog(WifiRobotActivity.this);
			/* 璁剧疆杩涘害妗嗕笂鏄剧ず鐨勬秷鎭�*/
			mDialog_Connect.setMessage("Trying to connect to TCP server...");
			/* 璁剧疆杩涘害妗嗕负涓嶅彲鍙栨秷鐨�			
			 *  * 杩欐牱璁剧疆鐨勭洰鐨勭敱浜庡璇濇杩愯鏃跺悗鍙版湁杩涚▼鍦ㄨ繍琛�			
			 *   * 濡傛灉鍙栨秷鐨勮瘽,鍚庡彴鐨勮繘绋嬫病鏈夌粨鏉�浼氬鑷翠竴浜涙棤娉曢鏂欑殑闂 */
			mDialog_Connect.setCancelable(false);
			return mDialog_Connect;
		case IMGCAP_DIALOG_KEY:
			mDialog_ImageCap = new ProgressDialog(WifiRobotActivity.this);
			mDialog_ImageCap.setMessage("Trying to obtain image ...");
			mDialog_ImageCap.setCancelable(false);
			return mDialog_ImageCap;
		case VIDEOCAP_DIALOG_KEY:
			mDialog_VideoCap = new ProgressDialog(WifiRobotActivity.this);
			mDialog_VideoCap.setMessage("Trying to obtain video ...");
			mDialog_VideoCap.setCancelable(false);
			return mDialog_VideoCap;
		default:
			return null;
		}
	}



	/***
	 * 灏濊瘯杩炴帴杩涘害妗�	 * 灏濊瘯鑾峰彇杩滅▼鍥剧墖杩涘害妗�	 * 灏濊瘯鑾峰彇杩滅▼瑙嗛杩涘害妗�	 * ***/
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case CONNECT_DIALOG_KEY:
			ConnectProgressThread mConnectProgressThread = new ConnectProgressThread(
					progress_handler);
			mConnectProgressThread.start();
			break;
		case IMGCAP_DIALOG_KEY:
			ImageCapProgressThread mCapProgressThread = new ImageCapProgressThread(
					progress_handler, IMGCAP_DIALOG_KEY);
			mCapProgressThread.start();
			break;
		case VIDEOCAP_DIALOG_KEY:
			ImageCapProgressThread mCapProgressThread2 = new ImageCapProgressThread(
					progress_handler, VIDEOCAP_DIALOG_KEY);
			mCapProgressThread2.start();
			break;
		default:
			break;
		}

	}

	/* 澶勭悊鍚勭杩涘害妗嗙殑娑堟伅 */
	final Handler progress_handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.what <= VIDEOCAP_DIALOG_KEY) {
				dismissDialog(msg.what);
			}

			switch (msg.what) {
			case CONNECT_DIALOG_KEY:
				if (msg.obj != null) {
					disp_toast((String) msg.obj);
				}
				if (tcp_ctrl_obj.mTcp_ctrl_client.isSocketOK()) {
					txtTCPState.setText(R.string.tcpstate_online);
				} else {
					txtTCPState.setText(R.string.tcpstate_offline);
				}
				break;
			case IMGCAP_DIALOG_KEY:
				image_ready_flag = (Boolean) (msg.obj);
				if (image_ready_flag == true) {
					img_camera.setImageBitmap(img_camera_bmp);
				} else {
					disp_toast("Get remote image failed,please check the video address!");
				}
				break;
			case VIDEOCAP_DIALOG_KEY:
				video_ready_flag = (Boolean) (msg.obj);
				if (video_ready_flag == true) {
					img_camera.setImageBitmap(img_camera_bmp);
					/* 妫�煡鍒拌棰戞暟鎹甯�
					 * 灏卞彲浠ュ垱寤轰竴涓敤浜庢樉绀鸿棰戞暟鎹殑绫�*/
					m_DrawVideo = new DrawVideo(cur_video_addr,
							mHandler_video_process);
					m_DrawVideo.start();
					btn_video.setText(R.string.button_video_stop);
					video_flag = true;
				} else {
					disp_toast("Get remote video failed,please check the video address!");
				}
				break;
			case MSG_DISPLAY_TOAST:
				break;

			default:
				break;
			}
		}
	};

	/* 妫�煡TCP杩炴帴鏄惁宸茬粡杩炴帴鎴栬�
	 * 杩炴帴鐨処P鍜孭ort宸茬粡鏇存柊 灏遍渶瑕侀噸鏂拌繛鎺� */
	private class ConnectProgressThread extends Thread {
		Handler mHandler;
		String msg = null;

		ConnectProgressThread(Handler h) {
			mHandler = h;
		}

		@Override
		public void run() {
			if (tcp_ctrl_obj.mTcp_ctrl_client.updateIPandPort(dist_tcp_addr,
					dist_tcp_port)
					|| (tcp_ctrl_obj.mTcp_ctrl_client.isSocketOK() == false)) {
				/*
				 * 妫�煡鏄惁鏈夊涓嬫儏鍐�
				 * 1. TCP杩炴帴鐨処P鎴栬�绔彛鏈夋洿鏀�				 * 2. TCP杩炴帴娌℃湁寤虹珛
				 */
				/* 寮哄埗寤虹珛TCP杩炴帴 */
				tcp_ctrl_obj.mTcp_ctrl_client.tcp_connect(true);
			} else {
				msg = new String("Already Connected to TCP Server @"
						+ dist_tcp_addr + ":" + dist_tcp_port);
			}
			mHandler.obtainMessage(CONNECT_DIALOG_KEY, msg).sendToTarget();
		}
	}

	/* 妫�煡鏄惁鍥惧儚鎴栬�瑙嗛鏁版嵁鍑嗗濂戒簡 */
	private class ImageCapProgressThread extends Thread {
		Handler mHandler;
		boolean image_ok = false;
		int dialog_key;

		ImageCapProgressThread(Handler h, int id) {
			mHandler = h;
			dialog_key = id;
		}

		@Override
		public void run() {
			image_ok = get_remote_image(cur_video_addr);
			mHandler.obtainMessage(dialog_key, image_ok).sendToTarget();
		}
	}


	/*** LASER_CTRL ***/
	/* 鍙戦�婵�厜鎺у埗鐨凾CP娑堟伅 */
	private void postLaserCtrlMsg(int btn_id) {
		short ctrl_cmd;
		short ctrl_prefix;
		byte[] msg = new byte[1];
		Button btn;

		ctrl_prefix = ctrl_prefixs.encode_ctrlprefix(
				ctrl_prefixs.write_request, ctrl_prefixs.less_data_request,
				ctrl_prefixs.withoutack);
		ctrl_cmd = ctrlcmds.LASER_CTRL;

		btn = (Button) findViewById(btn_id);

		if (laser_ctrl == LASER_OFF) {
			btn.setText(R.string.button_laser_off);
			laser_ctrl = LASER_ON;
		} else {
			btn.setText(R.string.button_laser_on);
			laser_ctrl = LASER_OFF;
		}

		msg[0] = (byte) (laser_ctrl & 0xff);

		Log.d(TAG, "Switch Laser Ctrl " + " to  " + laser_ctrl);
		post_tcp_msg(ctrl_prefix, ctrl_cmd, msg);
	}


	/*** ADJUST_VIDEO_MODE ***/
	/* 鍙戦�鍒囨崲瑙嗛妯″紡鐨勬寚浠�*/
	private void postSwitchVideoModeMsg(int videomode) {
		short ctrl_cmd;
		short ctrl_prefix;
		byte[] msg = new byte[1];

		ctrl_prefix = ctrl_prefixs.encode_ctrlprefix(
				ctrl_prefixs.write_request, ctrl_prefixs.less_data_request,
				ctrl_prefixs.withoutack);
		ctrl_cmd = ctrlcmds.ADJUST_VIDEO_MODE;
		msg[0] = (byte) (videomode & 0xff);

		Log.d(TAG, "Switch Video Mode " + " to  " + videomode);
		post_tcp_msg(ctrl_prefix, ctrl_cmd, msg);
	}

	/* 妫�煡鏄惁鍙互鍙戦�鍒囨崲瑙嗛妯″紡 */
	private void checkSendSwitchVideoModeMsg(int videomode) {
		if (tcp_ctrl_obj.mTcp_ctrl_client.isSocketOK()) { 
			/* 褰撳墠socket鍙敤鎵嶈繘琛屾暟鎹彂閫�*/
			postSwitchVideoModeMsg(videomode);
		}
	}

	/* 澶勭悊閫夋嫨瑙嗛婧愮殑娑堟伅 */
	private void process_video_src_select(int btn_id) {
		Button btn;
		String toast_str;

		switch (btn_id) {
		case R.id.button_video_src:
			update_preference();
			btn = (Button) findViewById(R.id.button_video_src);
			video_source_sel += 1;
			if (video_source_sel >= MAX_VIDEO_SRC_CNT) {
				video_source_sel = 0;
			}
			/* 閫夋嫨姝ｇ‘瑙嗛婧�*/
			cur_video_addr = video_addr[video_source_sel]; 
			toast_str = new String(" Address : " + cur_video_addr);
			switch (video_source_sel) {
			case CAR_VIDEO_SRC:
				btn.setText(R.string.button_video_src_car);
				toast_str = "Switch to car video ," + toast_str;
				break;
			case ARM_VIDEO_SRC:
				btn.setText(R.string.button_video_src_arm);
				toast_str = "Switch to arm video ," + toast_str;
				break;
			case OPENCV_VIDEO_SRC:
				btn.setText(R.string.button_video_src_opencv);
				toast_str = "Switch to openCV video ," + toast_str;
				break;
			}
			/* 娴嬭瘯骞跺彂閫佸垏鎹㈢殑瑙嗛妯″紡 */
			checkSendSwitchVideoModeMsg(video_source_sel);

			// disp_toast(toast_str);

			/*
			 * 濡傛灉褰撳墠鐨勬鍦ㄩ噰闆嗚棰戞暟鎹�灏遍渶瑕佽繘琛屽垏鎹� 骞朵笖鍏堣灏嗕箣鍓嶇殑瑙嗛鎺愭帀
			 */
			if (video_flag == true) {
				/* 鍏堥�鍑轰箣鍓嶇殑瑙嗛婧�*/
				if (m_DrawVideo != null) {
					m_DrawVideo.exit_thread();
				}
				btn_video.setText(R.string.button_video_start);
				img_camera.setImageResource(R.drawable.zynq_logo);
				video_flag = false;
				/* 鍒囨崲涓烘柊鐨勮棰戞簮 */
				showDialog(VIDEOCAP_DIALOG_KEY);
			}

			break;
		default:
			return;
		}

	}

	/*** 
	 * ENTER_REAL_CONTROL_MODE
	 * ENTER_AUTO_NAV_MODE 
	 * IMAGE AND VIDEO 鐩稿叧
	 * ***/
	/* 澶勭悊瀵瑰皬杞︽帶鍒舵寜閽殑娑堟伅
	 * 鍥惧儚 瑙嗛 鑷姩/鎵嬪姩鍒囨崲 */
	private void post_ctrl_btnclk_msg(int btn_id) {
		short ctrl_cmd = 0;
		short ctrl_prefix = 0;
		byte[] msg = null;
		Button btn;
		
		ctrl_prefix = ctrl_prefixs.encode_ctrlprefix(
				ctrl_prefixs.write_request, ctrl_prefixs.less_data_request,
				ctrl_prefixs.withoutack);
		switch (btn_id) {
		case R.id.button_image:
			update_preference();
			showDialog(IMGCAP_DIALOG_KEY);
			return; /* no break, direct return */

		case R.id.button_video:
			btn = (Button) findViewById(R.id.button_video);
			update_preference();
			if (video_flag == false) {
				/* 閫夋嫨姝ｇ‘鐨勮棰戞簮鍦板潃 */
				cur_video_addr = video_addr[video_source_sel]; 
				showDialog(VIDEOCAP_DIALOG_KEY);
			} else {
				if (m_DrawVideo != null) {
					m_DrawVideo.exit_thread();
					// m_DrawVideo.stop();
				}
				btn.setText(R.string.button_video_start);
				img_camera.setImageResource(R.drawable.zynq_logo);
				video_flag = false;
			}
			return; /* no break, direct return */
			
			/*** 
			 * ENTER_REAL_CONTROL_MODE
			 * ENTER_AUTO_NAV_MODE 
			 * ***/
		case R.id.button_control:
			if (auto_control_mode == false) {
				ctrl_cmd = (ctrlcmds.ENTER_AUTO_NAV_MODE);
				btn_control_mode.setText(R.string.button_realcontrol);
				auto_control_mode = true;
			} else {
				ctrl_cmd = (ctrlcmds.ENTER_REAL_CONTROL_MODE);
				btn_control_mode.setText(R.string.button_autocontrol);
				auto_control_mode = false;
			}
			break;

		default:
			return;
		}
		post_tcp_msg(ctrl_prefix, ctrl_cmd, msg);
	}

	/* 灏濊瘯鑾峰彇杩滅▼鍥剧墖 */
	public boolean get_remote_image(String url_addr) {
		boolean flag = false;

		String m_video_addr = CAR_VIDEO_ADDR;
		HttpURLConnection m_video_conn = null;
		InputStream m_InputStream = null;
		HttpGet httpRequest;
		HttpClient httpclient = null;
		HttpResponse httpResponse;

		try {
			m_video_addr = url_addr;
			Log.d(TAG, "start get url");
			httpRequest = new HttpGet(m_video_addr);

			Log.d(TAG, "open connection");
			httpclient = new DefaultHttpClient();

			Log.d(TAG, "begin connect");
			httpResponse = httpclient.execute(httpRequest);
			Log.d(TAG, "get InputStream");
			if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				Log.d(TAG, "decodeStream");
				m_InputStream = httpResponse.getEntity().getContent();
				/* 浠庤幏鍙栫殑娴佷腑鏋勫缓鍑築MP鍥惧儚 */
				img_camera_bmp = BitmapFactory.decodeStream(m_InputStream);
			}
			Log.d(TAG, "decodeStream end");

			flag = true;
		} catch (Exception e) {
			Log.e(TAG, "Error In Get Image Msg:" + e.getMessage());
			flag = false;
		} finally {
			if (m_video_conn != null) {
				m_video_conn.disconnect();
			}
			if ((httpclient != null)
					&& (httpclient.getConnectionManager() != null)) {
				/* 鍙婃椂鍏抽棴httpclient閲婃斁璧勬簮 */
				httpclient.getConnectionManager().shutdown(); 
			}
		}

		return flag;
	}

	/* 寰堥噸瑕佺殑鍑芥暟
	 * 璋冪敤tcp_ctrl绫讳腑鐨勫嚱鏁版潵鍙戦�tcp娑堟伅 */
	private void post_tcp_msg(short ctrl_prefix, short ctrl_cmd, byte[] msg) {
		ctrl_frame mCtrl_frame = new ctrl_frame(ctrl_prefix, ctrl_cmd, msg);
		byte[] tcp_msg = new byte[4 + mCtrl_frame.datalength];
		mCtrl_frame.encode_frametobytes(tcp_msg);
		tcp_ctrl_obj.mTcp_ctrl_client.post_msg(tcp_msg);
		if (D) {
			Log.d(TAG, "The Sent TCP Message is As Follows:");
			mCtrl_frame.display_ctrl_frame();
		}
	}
	
	/* 鏄剧ずToast 娑堟伅 */
	private void disp_toast(String msg) {
		Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQ_SYSTEM_SETTINGS:
			systemsettingchange(resultCode, data);
			break;

		default:
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private boolean systemsettingchange(int resultCode, Intent data) {
		boolean ifSucess = true;

		if (resultCode == RESULT_OK) {

		} else {
			Log.i(TAG, "None settings change");
		}
		return ifSucess;
	}

		/* NO USE */
	private OnSharedPreferenceChangeListener sys_set_chg_listener = new OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			if (key == getResources().getString(R.string.videoaddr1)) {
				video_addr[0] = preferences.getString(key, CAR_VIDEO_ADDR);
			} else if (key == getResources().getString(R.string.videoaddr2)) {
				video_addr[1] = preferences.getString(key, ARM_VIDEO_ADDR);
			} else if (key == getResources().getString(R.string.videoaddr3)) {
				video_addr[2] = preferences.getString(key, OPENCV_VIDEO_ADDR);
			} else if ((key == getResources().getString(R.string.distipaddr))
					|| (key == getResources().getString(R.string.disttcpport))) {
				dist_tcp_addr = preferences.getString(
						getResources().getString(R.string.distipaddr),
						DIST_TCPIPADDR);
				dist_tcp_port = Integer.parseInt((preferences.getString(
						getResources().getString(R.string.disttcpport),
						String.valueOf(DIST_TCPPORT))));
				tcp_ctrl_obj.mTcp_ctrl_client.tcpreconnect(dist_tcp_addr,
						dist_tcp_port);
			}
		}
	};



	/**************USER LOG FUNCTIONS DEFINITION START**************/
	
	/* 璁板綍绋嬪簭姣忎竴涓樁娈电殑鏃堕棿浠ュ強鐢ㄦ埛鑷畾涔夌殑LOG娑堟伅
	 * Tag 琛ㄧず鐢ㄦ埛鑷畾涔夌殑LOG娑堟伅 */
	public void log_pass_time(String Tag) {
		long pass_time;

		/* 璁板綍杩欎釜PASS缁撴潫鏃堕棿 */
		benchmark_end = System.currentTimeMillis();
		if (true) {
			/* 鐢熸垚LOG娑堟伅 
			 * 骞跺瓨鏀惧湪棰勫畾涔夊ソ鐨凩OG淇℃伅鏁扮粍涓�*/
			if (pass_cnt > MAX_PASS) {
				return;
			} else {
				pass_time = benchmark_end - benchmark_start;
				time_pass[pass_cnt] = pass_time;
				pass_log[pass_cnt] = "PASS " + pass_cnt + " : " + Tag + ":"
						+ "costs " + pass_time + "ms";
				pass_cnt++;
			}
		}
		/* 璁惧畾杩欎釜PASS鐨勫紑濮嬫椂闂�*/
		benchmark_start = System.currentTimeMillis();
	}

	/* 鍒濆鍖栫敤浜庤褰昄OG鐨勫悇椤规椂闂�*/
	public void init_log_time() {
		benchmark_start = System.currentTimeMillis();
		benchmark_end = System.currentTimeMillis();
		start_time = System.currentTimeMillis();
		end_time = System.currentTimeMillis();
	}

	/* 缁撴潫PASS鏃跺仛鏈�悗鐨勬椂闂村拰娑堟伅璁板綍 */
	public void end_log_time() {
		end_time = System.currentTimeMillis();

		if (true) {
			if (pass_cnt > (MAX_PASS + 1)) {
				return;
			} else {
				long pass_time = end_time - start_time;
				time_pass[pass_cnt] = pass_time;
				pass_log[pass_cnt] = "Program Startup Costs " + pass_time
						+ "ms";
				pass_cnt++;
			}
		}
	}

	/* 璁板綍褰撳墠鎵嬫満鐨勬墍鏈夌殑閲嶈鐨勪俊鎭�*/
	public void log_system_info() {
		String ScreenInfo = "Screen Resolution:" + screen_Height + " X "
				+ screen_Width;
		String CpuInfo = "";
		String VersionInfo = "";

		CpuInfo = readfile2str("/proc/cpuinfo");
		VersionInfo = readfile2str("proc/version");

		SystemInfo[0] = CpuInfo;
		SystemInfo[1] = VersionInfo;
		SystemInfo[2] = ScreenInfo;
		SystemInfo[3] = getphoneinfo();

		SystemInfoCnt = 4;
	}

	/* 鑾峰彇鎵嬫満鐨勫唴閮ㄧ殑淇℃伅 
	 * 璇﹁绋嬪簭涓殑璇存槑 */
	public String getphoneinfo() {
		String phoneInfo = "Product: " + android.os.Build.PRODUCT;
		phoneInfo += "\n CPU_ABI: " + android.os.Build.CPU_ABI;
		phoneInfo += "\n TAGS: " + android.os.Build.TAGS;
		phoneInfo += "\n VERSION_CODES.BASE: "
				+ android.os.Build.VERSION_CODES.BASE;
		phoneInfo += "\n MODEL: " + android.os.Build.MODEL;
		phoneInfo += "\n SDK: " + android.os.Build.VERSION.SDK;
		phoneInfo += "\n VERSION.RELEASE: " + android.os.Build.VERSION.RELEASE;
		phoneInfo += "\n DEVICE: " + android.os.Build.DEVICE;
		phoneInfo += "\n DISPLAY: " + android.os.Build.DISPLAY;
		phoneInfo += "\n BRAND: " + android.os.Build.BRAND;
		phoneInfo += "\n BOARD: " + android.os.Build.BOARD;
		phoneInfo += "\n FINGERPRINT: " + android.os.Build.FINGERPRINT;
		phoneInfo += "\n ID: " + android.os.Build.ID;
		phoneInfo += "\n MANUFACTURER: " + android.os.Build.MANUFACTURER;
		phoneInfo += "\n USER: " + android.os.Build.USER;

		return phoneInfo;
	}

	/* 灏嗘枃浠跺唴瀹逛繚瀛樺埌瀛楃涓�	 * TODO 鍚庢湡涓轰簡闃叉鐢ㄦ埛鎵撳紑涓�釜寰堝ぇ鐨勬枃浠�	 * 杩欓噷鍙互鍔犱笂涓�釜璇诲彇澶氬皯琛屾暟鎹垨鑰呭瓧绗﹀悗鑷姩閫�嚭 */
	public String readfile2str(String file_path) {
		String res = "";

		File file = new File(file_path);
		if (file.exists()) {
			try {
				String temp;
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				while (((temp = bufferedReader.readLine()) != null)) {
					res = res + "\n" + temp;
				}
				fileReader.close();
				bufferedReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return res;
	}

	/* 灏嗙敤鎴疯嚜瀹氫箟鐨凩OG淇℃伅鍐欏叆鍒板浜庣殑LOG鏂囦欢
	 * LOG淇℃伅鍖呮嫭 : 鍚勪釜PASS鐨勪俊鎭�鎵嬫満淇℃伅
	 * log_file_name : log鐨勬枃浠跺悕(鍚庢湡浼氳嚜鍔ㄥ鍔犱竴浜涘叾浠栫殑闄勫睘淇℃伅濡傛椂闂存埑) */
	public void write_log2file(String log_file_name, boolean need_sd_log) {
		update_preference();
		if (need_sd_log) {
			SimpleDateFormat formatter = new SimpleDateFormat(
					"yyyy-MM-dd HH_mm_ss");
			Date curDate = new Date(System.currentTimeMillis());// 鑾峰彇褰撳墠鏃堕棿
			String log_file_date = formatter.format(curDate);
			String full_log_filename = log_file_name + "_" + log_file_date
					+ ".txt";
			String log_file_path = "";

			String sd_status = Environment.getExternalStorageState();
			if (!(sd_status.equals(Environment.MEDIA_MOUNTED))) {
				log_file_path = "/mnt/flash" + File.separator + MYLOG_PATH_SD;
				disp_toast("SD鍗℃病鏈夋寕杞� 鏃ュ織鏂囦欢灏嗗啓鍏ュ埌" + log_file_path + "鐩綍涓�");
			} else {
				log_file_path = Environment.getExternalStorageDirectory()
						+ File.separator + MYLOG_PATH_SD;
			}
			File log_file_Dir = new File(log_file_path);
			File log_file = new File(log_file_path, full_log_filename);
			try { 
				if (!log_file_Dir.exists()) {/* 鏂囦欢鎴栬�涓嶅瓨鍦ㄥ氨鍒涘缓鐩綍鍜屾枃浠�*/
					if (!log_file_Dir.mkdir()) {
						disp_toast("鍒涘缓鍚姩鏃ュ織鏂囦欢鐩綍澶辫触!");
						return;
					} else {
						if (!log_file.exists()) {
							log_file.createNewFile(); /* 鍒涘缓鏂囦欢 */
						}
					}
				}
				// 鍚庨潰杩欎釜鍙傛暟浠ｈ〃鏄笉鏄鎺ヤ笂鏂囦欢涓師鏉ョ殑鏁版嵁锛屼笉杩涜瑕嗙洊
				FileWriter filerWriter = new FileWriter(log_file, true);
				BufferedWriter bufWriter = new BufferedWriter(filerWriter);
				for (int cnt = 0; cnt < pass_cnt; cnt++) {
					bufWriter.write(pass_log[cnt]);
					bufWriter.newLine();
				}
				bufWriter.newLine();

				for (int cnt = 0; cnt < SystemInfoCnt; cnt++) {
					bufWriter.write(SystemInfo[cnt]);
					bufWriter.newLine();
				}
				bufWriter.close();
				filerWriter.close();
				disp_toast("鍚姩鏃ュ織鏂囦欢宸茬敓鎴愪綅浜�" + log_file.getAbsolutePath());
			} catch (Exception e) {
				Log.e(TAG, "Write Log File Failed! " + e.getMessage());
			}
		}
	}
	
	/**************USER LOG FUNCTIONS DEFINITION END**************/

	/* 澶勭悊DrawVideo绫荤殑Handler
	 * UI鐨勬洿鏂�*/
	@SuppressLint("HandlerLeak")
	private final Handler mHandler_video_process = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_VIDEO_UPDATE:
				img_camera.setImageBitmap(img_camera_bmp);
				break;
			case MSG_VIDEO_ERROR:
				((Button) findViewById(R.id.button_video))
						.setText(R.string.button_video_start);
				disp_toast("Getting remote video failed,please check the video address!");
				img_camera.setImageResource(R.drawable.zynq_logo);
				break;
			case MSG_VIDEO_END:
				((Button) findViewById(R.id.button_video))
						.setText(R.string.button_video_start);
				img_camera.setImageResource(R.drawable.zynq_logo);
				break;
			default:
				break;
			}
		}
	};
	/* 澶勭悊瑙嗛鏄剧ず鐨勫瓙绫�	 * 涓昏瀹炵幇鍘熺悊鏄�
	 * 1s鍐呰幏鍙栬嚦灏�4甯у浘鍍�	 * 骞跺嵆鏃舵樉绀哄嚭鏉ヨ揪鍒拌繛缁殑鏁堟灉 
	 * TODO: 鏈�ソ鏄兘澶熷疄鐜癕JPEG鐨勮В鐮�
	 * 杩欐牱瀛愯繛缁�鏈�ソ浜�*/
	class DrawVideo extends Thread {
		private String m_video_addr = CAR_VIDEO_ADDR;
		private Handler video_Handler;
		private HttpURLConnection m_video_conn;
		HttpGet httpRequest;
		HttpClient httpclient = null;
		HttpResponse httpResponse;
		Bitmap bmp = null;
		private boolean exit_flag = false;

		public DrawVideo(String url_addr, Handler handler) {
			m_video_addr = url_addr;
			video_Handler = handler;
		}
		public void exit_thread() {
			exit_flag = true;
		}
//		/* 妫�煡瑙嗛鏁版嵁鏄惁鍑嗗濂戒簡 */
//		public boolean testconnection() {
//			boolean flag = false;
//			try {
//				httpRequest = new HttpGet(m_video_addr);
//				httpclient = new DefaultHttpClient();
//				httpResponse = httpclient.execute(httpRequest);
//				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//					m_InputStream = httpResponse.getEntity().getContent();
//					/* 浠庤幏鍙栫殑娴佷腑鏋勫缓鍑築MP鍥惧儚 */
//					bmp = BitmapFactory.decodeStream(m_InputStream);
//				}
//				if (bmp == null) {
//					flag = false;
//				} else {
//					flag = true;
//				}
//			} catch (Exception e) {
//				flag = false;
//				Log.e(TAG, "Error In Get Video Msg:" + e.getMessage());
//			}
//			if (m_video_conn != null) {
//				m_video_conn.disconnect();
//			}
//			if ((httpclient != null)
//					&& (httpclient.getConnectionManager() != null)) {
//				/* 鍙婃椂鍏抽棴httpclient閲婃斁璧勬簮 */
//				httpclient.getConnectionManager().shutdown(); 
//			}
//			return flag;
//		}
		@Override
		public void run() {
			try {
				httpRequest = new HttpGet(m_video_addr);
				httpclient = new DefaultHttpClient();
				while (!exit_flag) {
					httpResponse = httpclient.execute(httpRequest);
					if (httpResponse.getStatusLine().getStatusCode() == 200) {
						MjpegInputStream jStream = new MjpegInputStream( httpResponse.getEntity().getContent());
						img_camera_bmp = BitmapFactory.decodeByteArray(jStream.readMjpegFrame(),0, jStream.mContentLength);
					}
					if (img_camera_bmp != null) {
						/*鑾峰彇鍒版暟鎹悗鍙婃椂閫氳繃handler鐨勬柟寮忔樉绀哄嚭鏉�*/
						video_Handler.obtainMessage(MSG_VIDEO_UPDATE)
								.sendToTarget();
					}
					/* 寤舵椂涓�鏃堕棿 */
					sleep(5);
				}
				exit_flag = false;
			} catch (Exception e) {
				video_flag = false;
				Log.e(TAG, "Error In Get Video Msg:" + e.getMessage());
				video_Handler.obtainMessage(MSG_VIDEO_ERROR).sendToTarget();
			} finally {
				if (m_video_conn != null) {
					m_video_conn.disconnect();
				}
				if ((httpclient != null)
						&& (httpclient.getConnectionManager() != null)) {
					httpclient.getConnectionManager().shutdown(); 
				}
				video_Handler.obtainMessage(MSG_VIDEO_END).sendToTarget();
			}
		}
	}
}

package kr.poturns.blink.internal.comm;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import kr.poturns.blink.db.BlinkDatabaseManager;
import kr.poturns.blink.db.SqliteManager;
import kr.poturns.blink.db.archive.App;
import kr.poturns.blink.db.archive.BlinkAppInfo;
import kr.poturns.blink.db.archive.BlinkLog;
import kr.poturns.blink.db.archive.Device;
import kr.poturns.blink.db.archive.Function;
import kr.poturns.blink.db.archive.Measurement;
import kr.poturns.blink.db.archive.MeasurementData;
import kr.poturns.blink.internal.BlinkLocalService;
import kr.poturns.blink.internal.DeviceAnalyzer;
import kr.poturns.blink.schema.Eye;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Blink 어플리케이션과 서비스 간의 통신을 도와주는 클래스</br>
 * 안드로이드 서비스 구조에서 ServiceConnection이며 어플리케이션에서 서비스를 호출할 수 있도록 매소드를 정의하고 있다.</br>
 * @author Jiwon.Kim
 * @author Yeonho.Kim
 * @since 2014.08.19
 * 
 */
public abstract class BlinkServiceInteraction implements ServiceConnection, IBlinkEventBroadcast {
	private final String tag = "BlinkServiceInteraction";
	
	private final Context CONTEXT;
	private final EventBroadcastReceiver EVENT_BR;
	private final IntentFilter FILTER;

	private IBlinkEventBroadcast mBlinkEventBroadcast;
	private IInternalOperationSupport mInternalOperationSupport;
	private IInternalEventCallback mIInternalEventCallback;
	private BlinkDatabaseManager mBlinkDatabaseManager;
	/**
	 * Application Info
	 */
	// 바인더 컨넥션시 획득
	private BlinkDevice mBlinkDevice;
	// 생성자에서 초기화
	private String mPackageName = "";
	private String mAppName = "";
	
	public BlinkAppInfo mBlinkAppInfo;
	public Local local;
	public Remote remote;
	Gson gson = new GsonBuilder().setPrettyPrinting().create();

	/**
	 * 생성자로 Boradcast와 Callback을 등록할 수 있다. 등록하고 싶지 않을 경우 null을 매개변수로 넘기면 된다.
	 * @param context : Android Context 객체
	 * @param iBlinkEventBroadcast : Broadcast를 받을 리스너
	 * @param iInternalEventCallback : 외부 데이터를 받을 콜백
	 */
	public BlinkServiceInteraction(Context context,
			IBlinkEventBroadcast iBlinkEventBroadcast,
			IInternalEventCallback iInternalEventCallback) {
		CONTEXT = context;
		EVENT_BR = new EventBroadcastReceiver();
		FILTER = new IntentFilter();

		FILTER.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); // 블루투스 탐색 시작
		FILTER.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // 블루투스 탐색 종료

		FILTER.addAction(BROADCAST_DEVICE_DISCOVERED);
		FILTER.addAction(BROADCAST_DEVICE_CONNECTED);
		FILTER.addAction(BROADCAST_DEVICE_DISCONNECTED);
		FILTER.addAction(BROADCAST_DEVICE_IDENTITY_CHANGED);

		FILTER.addAction(BROADCAST_CONFIGURATION_CHANGED);
		FILTER.addAction(BROADCAST_MESSAGE_RECEIVED_FOR_TEST);	// FOR TEST

		mBlinkEventBroadcast = iBlinkEventBroadcast;
		mIInternalEventCallback = iInternalEventCallback;
		mBlinkDatabaseManager = new BlinkDatabaseManager(context);
		
		/**
		 * Database Sync Event 받기
		 */
		CONTEXT.getContentResolver().registerContentObserver(SqliteManager.URI_OBSERVER_SYNC, false, mContentObserver);
		
		local = new Local();
		remote = new Remote();
		
		/**
		 * Setting Application Info
		 */
		mPackageName = context.getPackageName();
		mAppName = context.getApplicationInfo()
				.loadLabel(context.getPackageManager()).toString();
	}

	/**
	 *  Boradcast와 Callback을 등록하지 않는 생성자
	 * @param context
	 */
	public BlinkServiceInteraction(Context context) {
		this(context, null, null);
	}

	@Override
	public final void onServiceConnected(ComponentName name, IBinder service) {
		CONTEXT.registerReceiver(EVENT_BR, FILTER);

		if (service == null)
			onServiceFailed();
		else {
			mInternalOperationSupport = BlinkSupportBinder.asInterface(service);
			if (mInternalOperationSupport == null) {
				onServiceFailed();

			} else {
				try {
					mInternalOperationSupport.registerApplicationInfo(mPackageName, mAppName);
					mBlinkDevice = mInternalOperationSupport.getBlinkDevice();

					if (mIInternalEventCallback != null) {
						mInternalOperationSupport
								.registerCallback(mIInternalEventCallback);
					}
					// 어플리케이션 관련 정보 교환

				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}

			onServiceConnected(mInternalOperationSupport);
		}
	}

	@Override
	public final void onServiceDisconnected(ComponentName name) {
		CONTEXT.unregisterReceiver(EVENT_BR);
		onServiceDisconnected();
	}

	/**
	 * Blink 서비스와 바인드한다. 
	 * 서비스가 실행 중이지 않을 경우 자동으로 생성한다. 
	 * 바인드가 완료되면 onServiceConnected()가 호출된다.
	 */
	public final void startService() {
		Intent intent = new Intent(BlinkLocalService.INTENT_ACTION_NAME);
		intent.putExtra(BlinkLocalService.INTENT_EXTRA_SOURCE_PACKAGE,
				CONTEXT.getPackageName());

		CONTEXT.startService(intent);
		CONTEXT.bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	/**
	 * 언바인드한다. 다른 바인드된 어플리케이션이 있을 경우 종료되지 않는다.
	 */
	public final void stopService() {
		Intent intent = new Intent(BlinkLocalService.INTENT_ACTION_NAME);
		intent.putExtra(BlinkLocalService.INTENT_EXTRA_SOURCE_PACKAGE,
				CONTEXT.getPackageName());

		CONTEXT.unbindService(this);
		CONTEXT.unregisterReceiver(EVENT_BR);
		// CONTEXT.stopService(intent);
	}

	public final void startBroadcastReceiver() {
		CONTEXT.registerReceiver(EVENT_BR, FILTER);
	}

	public final void stopBroadcastReceiver() {
		CONTEXT.unregisterReceiver(EVENT_BR);
	}

	public final void requestConfigurationChange(String... keys) {
		if (keys != null) {
			for (String key : keys) {

			}
		}

		Intent intent = new Intent(BROADCAST_REQUEST_CONFIGURATION_CHANGE);
		CONTEXT.sendBroadcast(intent, PERMISSION_LISTEN_STATE_MESSAGE);
	}

	public final void setOnBlinkEventBroadcast(
			IBlinkEventBroadcast iBlinkEventBroadcast) {
		mBlinkEventBroadcast = iBlinkEventBroadcast;
	}

	private class EventBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
				onDiscoveryStarted();
				return;

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED
					.equals(action)) {
				onDiscoveryFinished();
				return;
			}

			BlinkDevice device = (BlinkDevice) intent
					.getSerializableExtra(EXTRA_DEVICE);

			if (BROADCAST_DEVICE_DISCOVERED.equals(action)) {
				onDeviceDiscovered(device);

			} else if (BROADCAST_DEVICE_CONNECTED.equals(action)) {
				onDeviceConnected(device);

			} else if (BROADCAST_DEVICE_DISCONNECTED.equals(action)) {
				onDeviceDisconnected(device);

			} else if (BROADCAST_DEVICE_IDENTITY_CHANGED.equals(action)) {
				onIdentityChanged(device.getIdentity());

			} else if (BROADCAST_CONFIGURATION_CHANGED.equals(action)) {

			} else if (BROADCAST_MESSAGE_RECEIVED_FOR_TEST.equals(action)) {
				Toast.makeText(CONTEXT, intent.getStringExtra("content"), Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * [ <b>OVERRIDE IT</b>, if you want to complement some operations. ]
	 * 
	 * <p>
	 * 블루투스 탐색이 시작되었을 때, 호출된다.
	 * 
	 */
	public void onDiscoveryStarted() {
	}

	/**
	 * [ <b>OVERRIDE IT</b>, if you want to complement some operations. ]
	 * 
	 * <p>
	 * 블루투스 탐색 수행시, 디바이스가 발견되었을 때 호출된다. <br>
	 * Override할 경우, 등록한 {@link IBlinkEventBroadcast}은 동작하지 않는다.
	 * <hr>
	 * 
	 * @param device
	 */
	public void onDeviceDiscovered(BlinkDevice device) {
		if (mBlinkEventBroadcast != null)
			mBlinkEventBroadcast.onDeviceDiscovered(device);
	}

	/**
	 * [ <b>OVERRIDE IT</b>, if you want to complement some operations. ]
	 * 
	 * <p>
	 * 블루투스 탐색이 종료되었을 때, 호출된다.
	 * 
	 */
	public void onDiscoveryFinished() {
	}

	/**
	 * [ <b>OVERRIDE IT</b>, if you want to complement some operations. ]
	 * 
	 * <p>
	 * 블루투스 디바이스가 연결되었을 때 호출된다. <br>
	 * Override할 경우, 등록한 {@link IBlinkEventBroadcast}은 동작하지 않는다.
	 * <hr>
	 * 
	 * @param device
	 */
	public void onDeviceConnected(BlinkDevice device) {
		if (mBlinkEventBroadcast != null)
			mBlinkEventBroadcast.onDeviceConnected(device);
	}

	/**
	 * [ <b>OVERRIDE IT</b>, if you want to complement some operations. ]
	 * 
	 * <p>
	 * 블루투스 디바이스가 해제되었을 때 호출된다. <br>
	 * Override할 경우, 등록한 {@link IBlinkEventBroadcast}은 동작하지 않는다.
	 * <hr>
	 * 
	 * @param device
	 */
	public void onDeviceDisconnected(BlinkDevice device) {
		if (mBlinkEventBroadcast != null)
			mBlinkEventBroadcast.onDeviceDisconnected(device);

	}

	/**
	 * 
	 * @param identity
	 */
	public void onIdentityChanged(DeviceAnalyzer.Identity identity) {
	}

	/**
	 * 
	 */
	public void onConfigurationChanged() {
	}

	/**
	 * Service에 Binding 되었을 때 호출된다.
	 * 
	 * @param iSupport
	 */
	public abstract void onServiceConnected(IInternalOperationSupport iSupport);

	/**
	 * Service에서 Unbinding 되었을 때 호출된다.
	 */
	public abstract void onServiceDisconnected();

	/**
	 * Service에서 Binding이 실패하였을 때 호출된다.
	 */
	public abstract void onServiceFailed();

	/**
	 * Database Interaction
	 */

	/**
	 * Database Sync가 발생했을 때 호출된다.
	 * Interaction에 가지고 있던 BlinkAppInfo를 최신화한다.
	 */
	private ContentObserver mContentObserver = new ContentObserver(new Handler()){
		public void onChange(boolean selfChange, Uri uri) {
			Log.i(tag, "Uri : "+uri);
			//새로운 BlinkApp이 추가되면 실행
			if(uri.equals(SqliteManager.URI_OBSERVER_SYNC)){
				Log.i(tag, "if : URI_OBSERVER_SYNC");
				mBlinkAppInfo = local.obtainBlinkApp();
			}
		};
	};
	
	/**
	 * BlinkAppInfo를 서비스에 등록한다.
	 * @param mBlinkAppInfo : 어플리케이션의 BlinkAppInfo (Function과 Measurement만 추가하면 된다.)
	 * @return
	 */
	public boolean registerBlinkApp(
			BlinkAppInfo mBlinkAppInfo) {
		mBlinkAppInfo.mDevice.Device = mBlinkDevice.getName();
		mBlinkAppInfo.mDevice.MacAddress = mBlinkDevice.getAddress();
		mBlinkAppInfo.mApp.PackageName = mPackageName;
		mBlinkAppInfo.mApp.AppName = mAppName;
		PackageManager mPackageManager = CONTEXT.getPackageManager();
		
		try {
			Bitmap bitmap = ((BitmapDrawable)mPackageManager.getApplicationIcon(mPackageName)).getBitmap();
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
			mBlinkAppInfo.mApp.AppIcon = stream.toByteArray();
		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			mBlinkAppInfo.mApp.AppIcon = null;
		}
		
		try {
			mInternalOperationSupport
					.registerBlinkApp(mBlinkAppInfo);
			mBlinkAppInfo = local.obtainBlinkApp();
			return true;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * 테스트 데이터 생성을 위한 임시 매소드</br>
	 * BlinkAppInfo를 서비스에 등록한다.
	 * 호출한 어플리케이션의 정보를 기본적으로 등록하지 않고 주어진 값으로 등록한다. 
	 * @param mBlinkAppInfo : 어플리케이션의 BlinkAppInfo
	 * @return
	 */
	public boolean registerExternalBlinkApp(
			BlinkAppInfo mBlinkAppInfo) {
		try {
			mInternalOperationSupport
					.registerBlinkApp(mBlinkAppInfo);
			return true;
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * 자신의 어플리케이션 BlinkAppInfo 객체를 얻어온다.
	 * @return
	 */
	public BlinkAppInfo obtainBlinkApp() {
		mBlinkAppInfo = local.obtainBlinkApp(mBlinkDevice.getName(), mPackageName);
		return mBlinkAppInfo;
	}
	
	/**
	 * BlinkLibrary의 ControllActivity를 여는 매소드 
	 */
	public void openControlActivity(){
		try {
			mInternalOperationSupport.openControlActivity();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Local로 동작하는 매소드를 가지고 있는 클래스</br>
	 * 서비스에 요청하지 않고 직접 DB로부터 데이터를 가져온다.
	 * @author mementohora
	 * 
	 */
	public class Local {
		/**
		 * 자신의 어플리케이션 BlinkAppInfo 객체를 얻어온다.
		 * @return
		 */
		public BlinkAppInfo obtainBlinkApp() {
			mBlinkAppInfo = obtainBlinkApp(mBlinkDevice.getName(), mPackageName);
			return mBlinkAppInfo;
		}

		/**
		 * 디바이스 이름과 패키지 이름을 통해 해당되는 BlinkAppInfo 객체를 얻는다.
		 * @param DeviceName : 얻고자 하는 BlinkApp의 디바이스 이름
		 * @param PackageName : 얻고자 하는 BlinkApp의 패키지 이름
		 * @return
		 */
		public BlinkAppInfo obtainBlinkApp(String DeviceName,
				String PackageName) {
			return mBlinkDatabaseManager.obtainBlinkApp(DeviceName,
					PackageName);
		}

		/**
		 * 등록되어 있는 모든 BlinkAppInfo를 가져온다.
		 * @return
		 */
		public List<BlinkAppInfo> obtainBlinkAppAll() {
			return mBlinkDatabaseManager.obtainBlinkApp();
		}

		/**
		 * MeasurementData를 객체 형태로 등록한다.</br>
		 * 반드시 등록한 BlinkAppInfo에 Measurement를 등록했어야 한다.
		 * @param obj : 등록할 데이터를 가지고 있는 객체
		 */
		public void registerMeasurementData(Object obj) {
			try {
				if(mBlinkAppInfo==null)obtainBlinkApp();
				mBlinkDatabaseManager.registerMeasurementData(
						mBlinkAppInfo, obj);
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		/**
		 * Class를 통해 데이터를 얻어온다.</br>
		 * Class와 함께 데이터를 반환 받을 타입을 매개변수로 넘겨야 한다.</br> 
		 * 기본적으로 SqliteManager.CONTAIN_DEFAULT로 동작한다. </br>
		 * {@code example : ArrayList<Eye> EyeList = mBlinkServiceInteraction.local.obtainMeasurementData(Eye.class,new TypeToken<ArrayList<Eye>>(){}.getType());}
		 * @param obj : 얻으려는 데이터의 클래스
		 * @param type : 반환받을 클래스 타입
		 * @return
		 */
		public <Object> Object obtainMeasurementData(Class<?> obj, Type type) {
			return obtainMeasurementData(obj, null, null,
					SqliteManager.CONTAIN_DEFAULT, type);
		}
		
		/**
		 * Class를 통해 데이터를 얻어온다.</br>
		 * Class와 함께 데이터를 반환 받을 타입을 매개변수로 넘겨야 한다.</br> 
		 * {@code example : ArrayList<Eye> EyeList = mBlinkServiceInteraction.local.obtainMeasurementData(Eye.class,SqliteManager.CONTAIN_FIELD,new TypeToken<ArrayList<Eye>>(){}.getType());}
		 * @param obj : 얻으려는 데이터의 클래스
		 * @param ContainType : 검색 타입 (SqliteManager.CONTAIN~)
		 * @param type : 반환받을 클래스 타입
		 * @return
		 */
		public <Object> Object obtainMeasurementData(Class<?> obj,
				int ContainType, Type type) {
			return obtainMeasurementData(obj, null, null, ContainType, type);
		}

		/**
		 * Class를 통해 데이터를 얻어온다.</br>
		 * Class와 함께 데이터를 반환 받을 타입을 매개변수로 넘겨야 한다.</br> 
		 * @param obj : 얻으려는 데이터의 클래스
		 * @param DateTimeFrom : 데이터 시작 일시
		 * @param DateTimeTo : 데이터 종료 일시
		 * @param ContainType : 검색 타입 (SqliteManager.CONTAIN~)
		 * @param type : 반환받을 클래스 타입
		 * @return
		 */
		public <Object> Object obtainMeasurementData(Class<?> obj,
				String DateTimeFrom, String DateTimeTo, int ContainType,
				Type type) {
			String json;
			try {
				json = mBlinkDatabaseManager.obtainMeasurementData(obj,
						DateTimeFrom, DateTimeTo, ContainType);
				return gson.fromJson(json, type);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}

		/**
		 * Measurement 리스트를 통해서 데이터를 얻어온다. 시간을 조건을 줄 수 있다.
		 * @param mMeasurementList : 검색할 데이터의 Measurement 리스트
		 * @param DateTimeFrom : 데이터 시작 일시
		 * @param DateTimeTo : 데이터 종료 일시
		 * @return
		 */
		public List<MeasurementData> obtainMeasurementData(
				List<Measurement> mMeasurementList, String DateTimeFrom,
				String DateTimeTo) {
			return mBlinkDatabaseManager.obtainMeasurementData(
					mMeasurementList, DateTimeFrom, DateTimeTo);
		}

		public void startFunction(Function function) {
			if (function.Type == Function.TYPE_ACTIVITY)
				CONTEXT.startActivity(new Intent(function.Action)
						.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
			else if (function.Type == Function.TYPE_SERIVCE)
				CONTEXT.startService(new Intent(function.Action));
			else if (function.Type == Function.TYPE_BROADCAST)
				CONTEXT.sendBroadcast(new Intent(function.Action));
		}

		/**
		 * 로그를 얻어온다.
		 * @param Device : 다바이스 이름
		 * @param App : 패키지 이름
		 * @param Type : 타입
		 * @param DateTimeFrom : 로그 시작 일시
		 * @param DateTimeTo : 로그 종료 일시
		 * @return
		 */
		public List<BlinkLog> obtainLog(String Device, String App, int Type,
				String DateTimeFrom, String DateTimeTo) {
			return mBlinkDatabaseManager.obtainLog(Device, App, Type,
					DateTimeFrom, DateTimeTo);
		}

		/**
		 * 로그를 얻어온다.
		 * @param Device : 다바이스 이름
		 * @param App : 패키지 이름
		 * @param DateTimeFrom : 로그 시작 일시
		 * @param DateTimeTo : 로그 종료 일시
		 * @return
		 */
		public List<BlinkLog> obtainLog(String Device, String App,
				String DateTimeFrom, String DateTimeTo) {
			return obtainLog(Device, App, -1, DateTimeFrom, DateTimeTo);
		}

		/**
		 * 로그를 얻어온다.
		 * @param Device : 다바이스 이름
		 * @param DateTimeFrom : 로그 시작 일시
		 * @param DateTimeTo : 로그 종료 일시
		 * @return
		 */
		public List<BlinkLog> obtainLog(String Device, String DateTimeFrom,
				String DateTimeTo) {
			return obtainLog(Device, null, -1, DateTimeFrom, DateTimeTo);
		}

		/**
		 * 로그를 얻어온다.
		 * @param DateTimeFrom : 로그 시작 일시
		 * @param DateTimeTo : 로그 종료 일시
		 * @return
		 */
		public List<BlinkLog> obtainLog(String DateTimeFrom, String DateTimeTo) {
			return obtainLog(null, null, -1, DateTimeFrom, DateTimeTo);
		}

		/**
		 * 모든 로그를 얻어온다.
		 * @return
		 */
		public List<BlinkLog> obtainLog() {
			return obtainLog(null, null, -1, null, null);
		}

		/**
		 * Device를 검색하는 쿼리로 조건을 매개변수로 받는다. 결과는 DeviceList에 저장된다.
		 * @param where
		 * @return
		 */
		public Local queryDevice(String where) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.queryDevice(where);
			return this;
		}

		/**
		 * App을 검색하는 쿼리로 조건을 매개변수로 받는다. 기본적으로 DeviceList에 저장되어 있는 Device 객체의 Id를 조건으로 설정한다. 결과는 AppList에 저장된다.
		 * @param where
		 * @return
		 */
		public Local queryApp(String where) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.queryApp(where);
			return this;
		}

		/**
		 * Function을 검색하는 쿼리로 조건을 매개변수로 받는다. 기본적으로 AppList에 저장되어 있는 App 객체의 Id를 조건으로 설정한다. 결과는 FunctionList에 저장된다.
		 * @param where
		 * @return
		 */
		public Local queryFunction(String where) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.queryFunction(where);
			return this;
		}

		/**
		 * Measurement을 검색하는 쿼리로 조건을 매개변수로 받는다. 기본적으로 AppList에 저장되어 있는 App 객체의 Id를 조건으로 설정한다. 결과는 MeasurementList에 저장된다
		 * @param where
		 * @return
		 */
		public Local queryMeasurement(String where) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.queryMeasurement(where);
			return this;
		}

		/**
		 * MeasurementData을 검색하는 쿼리로 조건을 매개변수로 받는다. 기본적으로 MeasurementList에 저장되어 있는 Measurement 객체의 Id를 조건으로 설정한다. 결과는 MeasurementDataList에 저장된다.
		 * @param where
		 * @return
		 */
		public Local queryMeasurementData(String where) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.queryMeasurementData(where);
			return this;
		}

		/**
		 * DeviceList를 리턴한다.
		 * @return
		 */
		public List<Device> getDeviceList() {
			// TODO Auto-generated method stub
			return mBlinkDatabaseManager.getDeviceList();
		}

		/**
		 * DeviceList를 변경한다.
		 * @param mDeviceList
		 */
		public void setDeviceList(List<Device> mDeviceList) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.setDeviceList(mDeviceList);
		}

		/**
		 * AppList를 리턴한다.
		 * @return
		 */
		public List<App> getAppList() {
			// TODO Auto-generated method stub
			return mBlinkDatabaseManager.getAppList();
		}
		
		/**
		 * AppList를 변경한다.
		 * @param mAppList
		 */
		public void setAppList(List<App> mAppList) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.setAppList(mAppList);
		}

		/**
		 * FunctionList를 리턴한다.
		 * @return
		 */
		public List<Function> getFunctionList() {
			// TODO Auto-generated method stub
			return mBlinkDatabaseManager.getFunctionList();
		}
		
		/**
		 * FunctionList를 변경한다.
		 * @param mFunctionList
		 */
		public void setFunctionList(List<Function> mFunctionList) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.setFunctionList(mFunctionList);
		}

		/**
		 * MeasurementList를 리턴한다.
		 * @return
		 */
		public List<Measurement> getMeasurementList() {
			// TODO Auto-generated method stub
			return mBlinkDatabaseManager.getMeasurementList();
		}
		
		/**
		 * MeasurementList를 변경한다.
		 * @param mMeasurementList
		 */
		public void setMeasurementList(List<Measurement> mMeasurementList) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.setMeasurementList(mMeasurementList);
		}

		/**
		 * MeasurementDataList를 리턴한다.
		 * @return
		 */
		public List<MeasurementData> getMeasurementDataList() {
			// TODO Auto-generated method stub
			return mBlinkDatabaseManager.getMeasurementDataList();
		}

		/**
		 * MeasurementDataList를 변경한다.
		 * @param mMeasurementDataList
		 */
		public void setMeasurementDataList(
				List<MeasurementData> mMeasurementDataList) {
			// TODO Auto-generated method stub
			mBlinkDatabaseManager.setMeasurementDataList(mMeasurementDataList);
		}
	}

	/**
	 * Remote와 통신할 수 있는 코드를 가지고 있는 클래스</br>
	 * 결과는 callback으로 넘겨진다.
	 * 
	 * @author mementohora
	 * 
	 */
	public class Remote {
		/**
		 * 요청 정책을 설정하는 매소드
		 * 현재 사용하지 않는다.
		 * @param requestPolicy
		 */
		private void setRequestPolicy(int requestPolicy) {
			try {
				mInternalOperationSupport.setRequestPolicy(requestPolicy);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		/**
		 * 외부 디바이스로 데이터 검색을 요청한다.
		 * 서비스가 클래스를 알고 있어야 하기 때문에 Blink 기본 schema에 있는 클래스만 사용 가능하다.
		 * 요청 결과는 콜백으로 반환된다.
		 * @param obj : 클래스 (기본 클래스만 사용 가능)
		 * @param RequestCode : 요청을 구분할 수 있는 코드로 콜백에서 responseCode와 동일하다.
		 */
		public void obtainMeasurementData(Class<?> obj, int RequestCode) {
			obtainMeasurementData(obj, null, null,
					SqliteManager.CONTAIN_DEFAULT, RequestCode);
		}

		/**
		 * 외부 디바이스로 데이터 검색을 요청한다.
		 * 서비스가 클래스를 알고 있어야 하기 때문에 Blink 기본 schema에 있는 클래스만 사용 가능하다.
		 * 요청 결과는 콜백으로 반환된다.
		 * @param obj : 클래스 (기본 클래스만 사용 가능)
		 * @param ContainType : 데이터 검색 타입
		 * @param RequestCode : 요청을 구분할 수 있는 코드로 콜백에서 responseCode와 동일하다.
		 */
		public void obtainMeasurementData(Class<?> obj, int ContainType, int RequestCode) {
			obtainMeasurementData(obj, null, null, ContainType, RequestCode);
		}

		/**
		 * 외부 디바이스로 데이터 검색을 요청한다.
		 * 서비스가 클래스를 알고 있어야 하기 때문에 Blink 기본 schema에 있는 클래스만 사용 가능하다.
		 * 요청 결과는 콜백으로 반환된다.
		 * @param obj : 클래스 (기본 클래스만 사용 가능)
		 * @param DateTimeFrom : 데이터 시작 일시
		 * @param DateTimeTo : 데이터 종료 일시
		 * @param ContainType : 데이터 검색 타입
		 * @param RequestCode : 요청을 구분할 수 있는 코드로 콜백에서 responseCode와 동일하다.
		 */
		public void obtainMeasurementData(Class<?> obj, String DateTimeFrom, String DateTimeTo, int ContainType, int RequestCode) {
			String ClassName = obj.getName();
			try {
				mInternalOperationSupport.obtainMeasurementData(ClassName,
						DateTimeFrom, DateTimeTo, ContainType, RequestCode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		/**
		 * MeasurementList를 기준으로 데이터를 검색한다. 
		 * @param mMeasurementList : 검색할 데이터의 Measurement 리스트
		 * @param DateTimeFrom : 데이터 시작 일시
		 * @param DateTimeTo : 데이터 종료 일시
		 * @param RequestType : 데이터 검색 타입
		 * @param RequestCode : 요청을 구분할 수 있는 코드로 콜백에서 responseCode와 동일하다.
		 * @return
		 */
		public void obtainMeasurementData(List<Measurement> mMeasurementList,
				String DateTimeFrom, String DateTimeTo, int RequestCode) {
			try {
				mInternalOperationSupport
						.obtainMeasurementDataById(mMeasurementList,
								DateTimeFrom, DateTimeTo, RequestCode);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * 외부 디바이스의 기능을 수행한다.
		 * @param function : 기능을 수행할 Function 객체
		 * @param requestCode : 요청을 구분할 수 있는 코드로 콜백에서 responseCode와 동일하다.
		 */
		public void startFunction(Function function, int requestCode) {
			try {
				mInternalOperationSupport.startFunction(function, requestCode);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sync 메시지 전송을 위한 임시 매소드
	 */
	public void sendSyncMessage(){
		try {
			Log.i("test", "btn_sendMessage");
			mInternalOperationSupport.sendSyncMessage();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

package kr.poturns.blink.internal;
import kr.poturns.blink.R;
import kr.poturns.blink.internal.comm.IInternalEventCallback;
import kr.poturns.blink.util.FileUtil;
import android.app.Service;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.RemoteCallbackList;
import android.util.Log;
import android.widget.Toast;

/**
 * Blink의 로컬 디바이스에서 베이스기능을 수행하는 백그라운드 서비스 Part.
 * 블루투스 통신으로 전달받은 데이터들을 다루는 가교역할을 한다({@link InterDeviceManager}).
 * 
 * 
 * @author Yeonho.Kim
 * @since 2014.07.12
 *
 */
abstract class BlinkLocalBaseService extends Service {

	// *** CONSTANT DECLARATION *** //
	public static final String PRIVATE_PROCESS_NAME = "kr.poturns.blink.internal.BlinkService";
	
	
	
	// *** LIFE CYCLE DECLARATION *** //
	protected final RemoteCallbackList<IInternalEventCallback> EVENT_CALLBACK_LIST = new RemoteCallbackList<IInternalEventCallback>();

	protected DeviceAnalyzer mDeviceAnalyzer;
	protected InterDeviceManager mInterDeviceManager;

	@Override
	public void onCreate() {
		Log.e("BlinkLocalBaseService", "onCreate()");
		super.onCreate();
		
		// For Service Debugging... 
		//android.os.Debug.waitForDebugger();
		
		// Blink 서비스에 필요한 기본 디렉토리 생성.
		FileUtil.createExternalDirectory();
		
		// Blink 서비스의 본 디바이스 정보 파악.
//		mDeviceAnalyzer = DeviceAnalyzer.getInstance(this);
//		
//		DeviceAnalyzer.Identity mIdentity = mDeviceAnalyzer.getCurrentIdentity();
//		if (DeviceAnalyzer.Identity.UNKNOWN.equals(mIdentity)) {
//			// Identity를 확인하고, 서비스가 정상적으로 동작할 수 없는 환경이면 종료한다.
//			Toast.makeText(this, R.string.internal_baseservice_unable_alert, Toast.LENGTH_LONG).show();
//			stopSelf();
//			return;
//		}
//		
//		// Device간 통신 모듈을 연결한다. 
//		mInterDeviceManager = InterDeviceManager.getInstance(this);
//		if (mInterDeviceManager == null) {
//			
//		}

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
//		DeviceAnalyzer.Identity identity = mDeviceAnalyzer.getCurrentIdentity();
		
//		return (DeviceAnalyzer.Identity.UNKNOWN.equals(identity))? 
//				START_NOT_STICKY : START_STICKY;
		return START_STICKY; 
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Log.e("BlinkLocalBaseService", "onConfigurationChanged()");
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		
	}
	
	@Override
	public void onTrimMemory(int level) {
		Log.e("BlinkLocalBaseService", "onTrimMemory()");
		// TODO Auto-generated method stub

		super.onTrimMemory(level);
	}
	
	@Override
	public void onLowMemory() {
		Log.e("BlinkLocalBaseService", "onLowMemory()");
		// TODO Auto-generated method stub

		super.onLowMemory();
	}
	
	@Override
	public void onTaskRemoved(Intent rootIntent) {
		Log.e("BlinkLocalBaseService", "onTaskRemoved()");
		// TODO Auto-generated method stub
		super.onTaskRemoved(rootIntent);

	}
	
	@Override
	public void onDestroy() {
		Log.e("BlinkLocalBaseService", "onDestroy()");
		// TODO Auto-generated method stub
		if (mInterDeviceManager != null)
			mInterDeviceManager.destroy();
		
		super.onDestroy();
	}
	
}

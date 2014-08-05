package kr.poturns.blink.internal;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import kr.poturns.blink.db.SqliteManager;
import kr.poturns.blink.db.archive.MeasurementData;
import kr.poturns.blink.db.archive.SystemDatabaseObject;
import kr.poturns.blink.service.IBlinkServiceBinder;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

/**
 * 
 * @author Yeonho.Kim
 *
 */
public final class BlinkLocalService extends BlinkLocalBaseService {
	private final String tag = "BlinkLocalService";
	public SqliteManager mSqliteManager = null;
	public static final String INTENT_ACTION_NAME = "";
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.i(tag, "onCreate");
		super.onCreate();
		mSqliteManager = SqliteManager.getSqliteManager(this);
	}
	
	protected IBlinkServiceBinder.Stub mBindingStub = new IBlinkServiceBinder.Stub() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		@Override
		public SystemDatabaseObject obtainSystemDatabase(String device,
				String app) throws RemoteException {
			// TODO Auto-generated method stub
			Log.i(tag, "obtainSystemDatabase");
			return mSqliteManager.obtainSystemDatabase(device, app);
		}

		@Override
		public void registerSystemDatabase(
				SystemDatabaseObject mSystemDatabaseObject)
				throws RemoteException {
			// TODO Auto-generated method stub
			Log.i(tag, "registerSystemDatabase");
			mSqliteManager.registerSystemDatabase(mSystemDatabaseObject);
		}

		@Override
		public void registerMeasurementData(
				SystemDatabaseObject mSystemDatabaseObject, String ClassName,
				String JsonObj) throws RemoteException {
			// TODO Auto-generated method stub
			try{
				Class<?> mClass = Class.forName(ClassName);
				Object obj = new Gson().fromJson(JsonObj, mClass);
				mSqliteManager.registerMeasurementData(mSystemDatabaseObject, obj);
			} catch(Exception e){
				e.printStackTrace();
			}
		}

		@Override
		public String obtainMeasurementData(String ClassName,
				String DateTimeFrom, String DateTimeTo, int ContainType)
				throws RemoteException {
			// TODO Auto-generated method stub
			try{
				Class<?> mClass = Class.forName(ClassName);
				Log.i(tag, "class name : "+mClass.getName());
				return mSqliteManager.obtainMeasurementData(mClass, DateTimeFrom, DateTimeTo, ContainType); 
			}catch (Exception e){
				e.printStackTrace();
				return null;
			}
		}

		
	};
	
	
	@Override
	public IBinder onBind(Intent intent) {
		
		if (InterDeviceManager.ACTION_NAME.equals(intent.getAction())) {
			return LinkStatusHandler.getInstance(mInterDeviceManager).getBinder();
		}
		
		return mBindingStub.asBinder();
	}
}

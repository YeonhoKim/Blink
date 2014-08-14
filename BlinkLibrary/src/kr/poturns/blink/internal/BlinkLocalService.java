package kr.poturns.blink.internal;

import java.util.List;

import kr.poturns.blink.db.SqliteManager;
import kr.poturns.blink.db.archive.DeviceAppLog;
import kr.poturns.blink.db.archive.DeviceAppMeasurement;
import kr.poturns.blink.db.archive.MeasurementData;
import kr.poturns.blink.db.archive.SystemDatabaseObject;
import kr.poturns.blink.internal.comm.IInternalOperationSupport;
import kr.poturns.blink.service.IBlinkServiceBinder;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * 
 * @author Yeonho.Kim
 *
 */
public final class BlinkLocalService extends BlinkLocalBaseService {
	private final String tag = "BlinkLocalService";
	public SqliteManager mSqliteManager = null;
	public static final String INTENT_ACTION_NAME = "kr.poturns.blink.internal.BlinkLocalService";
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		Log.i(tag, "onCreate");
		super.onCreate();
		mSqliteManager = SqliteManager.getSqliteManager(this);
	}
	
	class BlinkServiceBinder extends IBlinkServiceBinder.Stub {
		String device, app;
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		@Override
		public SystemDatabaseObject obtainSystemDatabase(String device,
				String app) throws RemoteException {
			// TODO Auto-generated method stub
			Log.i(tag, "obtainSystemDatabase");
			mSqliteManager.registerLog(device, app, mSqliteManager.LOG_OBTAIN_SYSTEMDATABASE, "");
			return mSqliteManager.obtainSystemDatabase(device, app);
		}

		@Override
		public void registerSystemDatabase(
				SystemDatabaseObject mSystemDatabaseObject)
				throws RemoteException {
			// TODO Auto-generated method stub
			Log.i(tag, "registerSystemDatabase");
			mSqliteManager.registerLog(device, app, mSqliteManager.LOG_REGISTER_SYSTEMDATABASE, "");
			mSqliteManager.registerSystemDatabase(mSystemDatabaseObject);
		}

		@Override
		public void registerMeasurementData(
				SystemDatabaseObject mSystemDatabaseObject, String ClassName,
				String JsonObj) throws RemoteException {
			// TODO Auto-generated method stub
			mSqliteManager.registerLog(device, app, mSqliteManager.LOG_REGISTER_MEASRUEMENT, "");
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
			mSqliteManager.registerLog(device, app, mSqliteManager.LOG_OBTAIN_MEASUREMENT, ClassName);
			try{
				Class<?> mClass = Class.forName(ClassName);
				Log.i(tag, "class name : "+mClass.getName());
				return mSqliteManager.obtainMeasurementData(mClass, DateTimeFrom, DateTimeTo, ContainType); 
			}catch (Exception e){
				e.printStackTrace();
				return null;
			}
		}

		@Override
		public List<SystemDatabaseObject> obtainSystemDatabaseAll()
				throws RemoteException {
			// TODO Auto-generated method stub
			mSqliteManager.registerLog(device, app, mSqliteManager.LOG_OBTAIN_SYSTEMDATABASE, "ALL");
			return mSqliteManager.obtainSystemDatabase();
		}

		@Override
		public List<MeasurementData> obtainMeasurementDataById(
				List<DeviceAppMeasurement> mDeviceAppMeasurementList,
				String DateTimeFrom, String DateTimeTo) throws RemoteException {
			// TODO Auto-generated method stub
			mSqliteManager.registerLog(device, app, mSqliteManager.LOG_OBTAIN_MEASUREMENT, "By Id");
			return mSqliteManager.obtainMeasurementData(mDeviceAppMeasurementList, DateTimeFrom, DateTimeTo);
		}

		@Override
		public void registerApplicationInfo(String device, String app)
				throws RemoteException {
			// TODO Auto-generated method stub
			this.device = device;
			this.app = app;
		}

		@Override
		public List<DeviceAppLog> obtainLog(String Device, String App,
				int Type, String DateTimeFrom, String DateTimeTo)
				throws RemoteException {
			// TODO Auto-generated method stub
			return mSqliteManager.obtainLog(Device, App, Type, DateTimeFrom, DateTimeTo);
		}

		@Override
		public void registerLog(String Device, String App, int Type,
				String Content) throws RemoteException {
			// TODO Auto-generated method stub
			mSqliteManager.registerLog(Device, App, Type, Content);
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		//return new BlinkServiceBinder().asBinder();
		return InterDeviceManager.getInstance(this).mInternalOperationSupporter.asBinder();
	}

}

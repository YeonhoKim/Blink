package kr.poturns.blink.internal.comm;

import java.util.List;

import kr.poturns.blink.db.BlinkDatabaseManager;
import kr.poturns.blink.db.SqliteManager;
import kr.poturns.blink.db.archive.BlinkLog;
import kr.poturns.blink.db.archive.Function;
import kr.poturns.blink.db.archive.Measurement;
import kr.poturns.blink.db.archive.MeasurementData;
import kr.poturns.blink.db.archive.SystemDatabaseObject;
import kr.poturns.blink.internal.BlinkLocalService;
import kr.poturns.blink.internal.ConnectionSupportBinder;
import android.content.Intent;
import android.os.RemoteException;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BlinkSupportBinder extends ConnectionSupportBinder {

	private final String tag = "BlinkDatabaseBinder";
	String mDeviceName, mPackageName, mAppName;
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	BlinkDatabaseManager mBlinkDatabaseManager;
	
	public BlinkSupportBinder(BlinkLocalService context) {
		super(context);
		mBlinkDatabaseManager = new BlinkDatabaseManager(context);
	}

	@Override
	public SystemDatabaseObject obtainSystemDatabase(String DeviceName,
			String PackageName) throws RemoteException {
		// TODO Auto-generated method stub
		Log.i(tag, "obtainSystemDatabase");
		mBlinkDatabaseManager.registerLog(DeviceName, PackageName, SqliteManager.LOG_OBTAIN_SYSTEMDATABASE, "");
		SystemDatabaseObject sdo = mBlinkDatabaseManager.obtainSystemDatabase(DeviceName, PackageName);
		Log.i(tag, sdo.toString());
		return mBlinkDatabaseManager.obtainSystemDatabase(DeviceName, PackageName);
	}

	@Override
	public void registerSystemDatabase(
			SystemDatabaseObject mSystemDatabaseObject)
			throws RemoteException {
		// TODO Auto-generated method stub
		Log.i(tag, "registerSystemDatabase");
		mBlinkDatabaseManager.registerLog(mDeviceName, mPackageName, mBlinkDatabaseManager.LOG_REGISTER_SYSTEMDATABASE, "");
		mBlinkDatabaseManager.registerSystemDatabase(mSystemDatabaseObject);
	}

	@Override
	public void registerMeasurementData(
			SystemDatabaseObject mSystemDatabaseObject, String ClassName,
			String JsonObj) throws RemoteException {
		// TODO Auto-generated method stub
		mBlinkDatabaseManager.registerLog(mDeviceName, mPackageName, mBlinkDatabaseManager.LOG_REGISTER_MEASRUEMENT, "");
		try{
			Class<?> mClass = Class.forName(ClassName);
			Object obj = new Gson().fromJson(JsonObj, mClass);
			mBlinkDatabaseManager.registerMeasurementData(mSystemDatabaseObject, obj);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public String obtainMeasurementData(String ClassName,
			String DateTimeFrom, String DateTimeTo, int ContainType)
			throws RemoteException {
		// TODO Auto-generated method stub
		mBlinkDatabaseManager.registerLog(mDeviceName, mPackageName, mBlinkDatabaseManager.LOG_OBTAIN_MEASUREMENT, ClassName);
		try{
			Class<?> mClass = Class.forName(ClassName);
			Log.i(tag, "class name : "+mClass.getName());
			return mBlinkDatabaseManager.obtainMeasurementData(mClass, DateTimeFrom, DateTimeTo, ContainType); 
		}catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<SystemDatabaseObject> obtainSystemDatabaseAll()
			throws RemoteException {
		// TODO Auto-generated method stub
		mBlinkDatabaseManager.registerLog(mDeviceName, mPackageName, mBlinkDatabaseManager.LOG_OBTAIN_SYSTEMDATABASE, "ALL");
		return mBlinkDatabaseManager.obtainSystemDatabase();
	}

	@Override
	public List<MeasurementData> obtainMeasurementDataById(
			List<Measurement> mMeasurementList,
			String DateTimeFrom, String DateTimeTo) throws RemoteException {
		// TODO Auto-generated method stub
		mBlinkDatabaseManager.registerLog(mDeviceName, mPackageName, mBlinkDatabaseManager.LOG_OBTAIN_MEASUREMENT, "By Id");
		return mBlinkDatabaseManager.obtainMeasurementData(mMeasurementList, DateTimeFrom, DateTimeTo);
	}

	@Override
	public void registerApplicationInfo(String DeviceName, String PackageName, String AppName)
			throws RemoteException {
		// TODO Auto-generated method stub
		this.mDeviceName = DeviceName;
		this.mPackageName = PackageName;
		this.mAppName = AppName;
	}

	@Override
	public List<BlinkLog> obtainLog(String DeviceName, String PackageName,
			int Type, String DateTimeFrom, String DateTimeTo)
			throws RemoteException {
		// TODO Auto-generated method stub
		return mBlinkDatabaseManager.obtainLog(DeviceName, PackageName, Type, DateTimeFrom, DateTimeTo);
	}

	@Override
	public void registerLog(String DeviceName, String PackageName, int Type,
			String Content) throws RemoteException {
		// TODO Auto-generated method stub
		mBlinkDatabaseManager.registerLog(DeviceName, PackageName, Type, Content);
	}

	@Override
	public void startFunction(Function mFunction) throws RemoteException {
		// TODO Auto-generated method stub
		if(mFunction.Type==Function.TYPE_ACTIVITY)
			CONTEXT.startActivity(new Intent(mFunction.Action).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		else if(mFunction.Type==Function.TYPE_SERIVCE)
			CONTEXT.startService(new Intent(mFunction.Action));
		else if(mFunction.Type==Function.TYPE_BROADCAST)
			CONTEXT.sendBroadcast(new Intent(mFunction.Action));
		
	}
}

package kr.poturns.blink.internal;

import kr.poturns.blink.external.ServiceControlActivity;
import kr.poturns.blink.internal.comm.BlinkDevice;
import kr.poturns.blink.internal.comm.BlinkProfile;
import kr.poturns.blink.internal.comm.IInternalOperationSupport;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.RemoteException;

/**
 * 
 * @author Yeonho.Kim
 * @since 2014.08.19
 * 
 */
public abstract class ConnectionSupportBinder extends IInternalOperationSupport.Stub {

	protected final BlinkLocalService CONTEXT;

	private BluetoothAssistant mAssistant;

	protected ConnectionSupportBinder(BlinkLocalService context)
			throws Exception {
		CONTEXT = context;

		if (context == null){
			throw new Exception();
		}
			
		mAssistant = BluetoothAssistant.getInstance(InterDeviceManager.getInstance(context));
	}

	@Override
	public void startDiscovery(int type) throws RemoteException {
		if (mAssistant == null) {
			return;
		}

		switch (InterDeviceManager.getInstance(CONTEXT).mStartDiscoveryType = type) {
		case BluetoothDevice.DEVICE_TYPE_CLASSIC:
		case BluetoothDevice.DEVICE_TYPE_DUAL:
		case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
			mAssistant.startDiscovery(BluetoothDevice.DEVICE_TYPE_CLASSIC);
			break;

		case BluetoothDevice.DEVICE_TYPE_LE:
			mAssistant.startDiscovery(BluetoothDevice.DEVICE_TYPE_LE);
			break;
		}
	}

	@Override
	public void stopDiscovery() throws RemoteException {
		if (mAssistant != null)
			mAssistant.stopDiscovery();
	}

	@Override
	public BlinkDevice[] obtainCurrentDiscoveryList() throws RemoteException {
		return ServiceKeeper.getInstance(CONTEXT).obtainDiscoveredDevices();
	}

	@Override
	public void startListeningAsServer() throws RemoteException {
		if (mAssistant != null)
			mAssistant.startListeningServer(false);
	}

	@Override
	public void stopListeningAsServer() throws RemoteException {
		if (mAssistant != null)
			mAssistant.stopListeningServer();
	}

	@Override
	public void connectDevice(BlinkDevice device) throws RemoteException {
		BluetoothDevice origin = device.obtainBluetoothDevice();

//		if (origin.getBondState() == BluetoothDevice.BOND_NONE)
//			origin.createBond();

		if (mAssistant != null)
			mAssistant.connectToDeviceFromClient(device,
					BlinkProfile.UUID_BLINK);
	}

	@Override
	public void disconnectDevice(BlinkDevice deviceX) throws RemoteException {
		if (mAssistant != null)
			mAssistant.disconnectDevice(deviceX);
	}

	@Override
	public BlinkDevice[] obtainConnectedDeviceList() throws RemoteException {
		return ServiceKeeper.getInstance(CONTEXT).obtainConnectedDevices();
	}

	@Override
	public void openControlActivity() throws RemoteException {
		Intent intent = new Intent(CONTEXT, ServiceControlActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_NO_HISTORY);
		CONTEXT.startActivity(intent);
	}

	@Override
	public void sendBlinkMessages(BlinkDevice target, String jsonMsg)
			throws RemoteException {
		ServiceKeeper.getInstance(CONTEXT).sendMessageToDevice(target, jsonMsg);
	}

	/**
	 * 사용자의 권한으로 Main Identity를 설정한다. 다른 디바이스와 연결 전에 설정해야한다.
	 * @param enable
	 */
	@Override
	public boolean grantMainIdentityFromUser(boolean enable) {
		return DeviceAnalyzer.getInstance(CONTEXT).grantMainIdentityFromUser(enable);
	}
}

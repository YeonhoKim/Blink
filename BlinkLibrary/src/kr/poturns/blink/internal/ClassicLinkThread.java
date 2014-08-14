package kr.poturns.blink.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import kr.poturns.blink.internal.comm.BluetoothDeviceExtended;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

/**
 * 
 * 스레드를 시작할 때는 start() 대신에 startListening()을 사용한다.
 * 스레드를 종료할 때는 stopListening()을 호출하면 된다.
 * 
 * @author Yeonho.Kim
 * @since 2014. 08. 01
 *
 */
public class ClassicLinkThread extends Thread {

	private final InterDeviceManager INTER_DEV_MANAGER;
	private final BluetoothAssistant ASSISTANT;
	private final BluetoothDeviceExtended DEVICE_X;
	
	private boolean isClient;
	private BluetoothSocket mBluetoothSocket;
	
	private ObjectInputStream mInputStream;
	private ObjectOutputStream mOutputStream;
	
	private boolean isRunning;
	private boolean isStopped;
	
	public ClassicLinkThread(BluetoothAssistant assistant, BluetoothSocket socket, boolean client) {
		this(assistant, new BluetoothDeviceExtended(socket.getRemoteDevice()), socket, client);
	}
	
	public ClassicLinkThread(BluetoothAssistant assistant, BluetoothDeviceExtended deviceX, BluetoothSocket socket, boolean client) {
		this(assistant, deviceX);
		
		isClient =  client;
		mBluetoothSocket = socket;
		
		init();
	}
	
	private ClassicLinkThread(BluetoothAssistant assistant, BluetoothDeviceExtended deviceX) {
		super(assistant.CONNECTION_GROUP, deviceX.getDevice().getName());

		INTER_DEV_MANAGER = assistant.INTER_DEV_MANAGER;
		ASSISTANT = assistant;
		DEVICE_X = deviceX;
	}
	
	private void init() {
		Log.d("ClassicLinkThread_init()", "");
		try {
			mInputStream = new ObjectInputStream(mBluetoothSocket.getInputStream());
			mOutputStream = new ObjectOutputStream(mBluetoothSocket.getOutputStream());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		isRunning = false;
	}
	
	
	@Override
	public void run() {
		Log.d("ClassicLinkThread_run()", "");
		while (isRunning) {
			try {
				String json = (String) mInputStream.readObject();

				Log.d("ClassicLinkThread_run()", "Read : " + json);
				ASSISTANT.onMessageReceivedFrom(json, DEVICE_X);
				
			} catch (IOException e) {
				e.printStackTrace();
				
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	@Deprecated
	public synchronized void start() {
		
	}
	
	public synchronized void startListening() {
		Log.d("ClassicLinkThread_startListening()", "");
		
		if (!isRunning || (isRunning = true)) 
			super.start();
		
		if (isStopped && (isStopped = false))
			this.notify();
	}
	
	public synchronized void stopListening() {
		if (isRunning && !isStopped && (isStopped = true)) {
			try {
				this.wait();
				
			} catch (InterruptedException e) {
				destroy();
			}
		}
	}
	
	public synchronized void destroy() {
		
		isRunning = false;
		
		try {
			if (mInputStream != null)
				mInputStream.close();
			
		} catch (IOException e) {
		} finally {
			mInputStream = null;
		}
		
		try {
			if (mOutputStream != null)
				mOutputStream.close();
			
		} catch (IOException e) {
		} finally {
			mInputStream = null;
		}
		
		try {
			if (mBluetoothSocket != null)
				mBluetoothSocket.close();
			
		} catch (IOException e) {
		} finally {
			mBluetoothSocket = null;
		}
	}

	public void sendMessageToDevice(Object obj) {
		
		if ((mOutputStream != null) && (obj != null)) {
			try {
				mOutputStream.writeObject(obj);
				mOutputStream.flush();
				
			} catch (IOException e) {
			}
		}
	}
	
}

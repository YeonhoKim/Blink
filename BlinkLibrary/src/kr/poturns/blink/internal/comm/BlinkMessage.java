package kr.poturns.blink.internal.comm;

import java.io.Serializable;

import android.bluetooth.BluetoothAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * implements {@link IBlinkMessagable}<br>
 * Blink Library를 사용하는 디바이스 간 통신을 위해서 사용되는 Message이며
 * 전송을 원하는 데이터를 Json 형태로 저장하며 송신/수신하는 디바이스 및 앱의 정보를 가진 일종의 프로토콜 클래스
 * Type : 메세지의 전송용도
 * Code : 메세지의 고유번호 (요청/응답 번호)
 * 
 * @author Yeonho.Kim
 * @author Ho.Kwon
 * 
 */
public class BlinkMessage implements Serializable, IBlinkMessagable {

	// *** CONSTANT DECLARATION *** //
	/**
	 * 
	 */
	private static final long serialVersionUID = 7552162163627874820L;
	
	

	// *** FIELD DECLARATION *** //
	private static final Gson JSON_CREATOR = new GsonBuilder().setPrettyPrinting().create();
	
	private String SourceAddress;
	private String SourceApplication;
	private String DestinationAddress;
	private String DestinationApplication;

	 
	 
	private int Type;
	private int Code; 
	private boolean Reliable;
	private long Timestamp;
	
	private String message;
	
	private BlinkMessage() {
		Type = 0;
		Code = 0;
		Reliable = false;
		Timestamp = 0;
	}

	/**
	 * BLE에서 사용할 수 있는 Message로 변환한다.
	 * @return
	 */
	@Override
	public Object toLeMessage() {
		return null;
	}


	@Override
	public String toClassicMessage() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	// *** BUILDER DECLARATION *** //
	/**
	 * BlinkMessage를 만드는 Builder Class
	 * Builder Class를 통해 BlinkMessage에 내부 정보를 넣고 객체를 return 받을 수 있다. 
	 * 
	 * @author Yeonho.Kim
	 * @author Ho.Kwon
	 */
	public static class Builder {

		private BlinkMessage mBlinkMessage;
		
		public Builder() {
			mBlinkMessage = new BlinkMessage();
		}
		
		/**
		 * 송신 디바이스를 설정한다.
		 * 
		 * @param device
		 * @return
		 */
		public Builder setSourceDevice(BlinkDevice device) {
			if (device != null)
				mBlinkMessage.SourceAddress = device.getAddress();
			return this;
		}
		
		/**
		 * 송신 디바이스를 설정한다.
		 * 
		 * @param device
		 * @return
		 */
		public Builder setSourceDevice(String deviceAddress) {
			if (deviceAddress == null || BluetoothAdapter.checkBluetoothAddress(deviceAddress))
				mBlinkMessage.SourceAddress = deviceAddress;
			return this;
		}
		
		/**
		 * 송신 애플리케이션을 설정한다.
		 * 
		 * @param packageName
		 * @return
		 */
		public Builder setSourceApplication(String packageName) {
			if (packageName != null)
				mBlinkMessage.SourceApplication = packageName;
			return this;
		}
		
		/**
		 * 수신 디바이스를 설정한다.
		 * <br> device가 null일 경우, Explicit Mode.
		 * 
		 * @param device
		 * @return
		 */
		public Builder setDestinationDevice(BlinkDevice device) {
			if (device != null)
				mBlinkMessage.DestinationAddress = device.getAddress();
			return this;
		}
		
		/**
		 * 수신 디바이스를 설정한다.
		 * <br> device가 null일 경우, Explicit Mode.
		 * 
		 * @param device
		 * @return
		 */
		public Builder setDestinationDevice(String deviceAddress) {
			if (deviceAddress == null || BluetoothAdapter.checkBluetoothAddress(deviceAddress))
				mBlinkMessage.DestinationAddress = deviceAddress;
			return this;
		}
		
		/**
		 * 수신 애플리케이션을 설정한다.
		 * <br> packageName이 null일 경우, Explicit Mode.
		 * 
		 * @param packageName
		 * @return
		 */
		public Builder setDestinationApplication(String packageName) {
			if (packageName != null)
				mBlinkMessage.DestinationApplication = packageName;
			return this;
		}
		
		/**
		 * 메세지의 속성을 설정한다.
		 * <p> TYPE_1 : 
		 * <br> TYPE_2 :
		 * <hr>
		 * @param type
		 * @return
		 */
		public Builder setType(int type) {
			mBlinkMessage.Type = type;
			return this;
		}
		
		/**
		 * 메세지의 요청/응답 번호를 설정한다.
		 * <p> TYPE_1 : 
		 * <br> TYPE_2 :
		 * <hr>
		 * @param type
		 * @return
		 */
		public Builder setCode(int Code) {
			mBlinkMessage.Code = Code;
			return this;
		}
		
		/**
		 * 메세지의 신뢰성 여부를 설정한다.
		 * <br> 신뢰성있는 메세지의 경우, 메세지 전달에 대한 응답이 없다면 결과를 알려준다.
		 * <br> 신뢰성없는 메세지의 경우, 메세지 전달에 대한 응답이 존재하지 않는다.
		 * 
		 * @param reliable
		 * @return
		 */
		public Builder setReliable(boolean reliable) {
			mBlinkMessage.Reliable = reliable;
			return this;
		}
		
		/**
		 * 
		 * @param message
		 * @return
		 */
		public Builder setMessage(String message) {
			mBlinkMessage.message = message;
			return this;
		}
		
		/**
		 * 
		 * @param object
		 * @return
		 */
		public Builder setMessage(Object object) {
			mBlinkMessage.message = JSON_CREATOR.toJson(object);
			return this;
		}
		
		/**
		 * {@link BlinkMessage} 객체를 반환한다.
		 * @return
		 */
		public BlinkMessage build() {
			mBlinkMessage.Timestamp = System.currentTimeMillis();
			return mBlinkMessage;
		}
		
		/**
		 * 
		 * @param json
		 * @return
		 */
		public static BlinkMessage restore(String json) {
			BlinkMessage mBlinkMessage = new BlinkMessage();
			
			return mBlinkMessage;
		}
	}
	
	

	// *** SETTER/GETTER DECLARATION *** //
	public void setDestinationAddress(String desinationAddress){
		this.DestinationAddress = desinationAddress;
	}
	public void setSourceAddress(String sourceAddress){
		this.SourceAddress = sourceAddress;
	}
	public String getSourceAddress() {
		return SourceAddress;
	}

	public String getSourceApplication() {
		return SourceApplication;
	}

	public String getDestinationAddress() {
		return DestinationAddress;
	}

	public String getDestinationApplication() {
		return DestinationApplication;
	}

	public int getType() {
		return Type;
	}

	public boolean isReliable() {
		return Reliable;
	}

	public long getTimestamp() {
		return Timestamp;
	}

	public String getMessage() {
		return message;
	}
	
	public <T> T getMessage(Class<T> classType) {
		return JSON_CREATOR.fromJson(message, classType);
	}

	public int getCode() {
	    return Code;
    }
}

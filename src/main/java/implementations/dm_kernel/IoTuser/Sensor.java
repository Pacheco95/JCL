package implementations.dm_kernel.IoTuser;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ScheduledFuture;

import javax.imageio.ImageIO;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.hopding.jrpicam.RPiCamera;

import implementations.collections.JCLHashMap;
import interfaces.kernel.JCL_IoT_Sensing_Model;
import interfaces.kernel.JCL_Sensor;
import mraa.Aio;
import mraa.Gpio;

public class Sensor implements Runnable{
	private int pin;
	private String alias;
	private long delay;
	private int size;
	private char dir;
	private Object lastValue;
	private int type;
	private int min;
	private int max;
	private String dataType;
	private JCLHashMap<Integer, JCL_Sensor> values;
	private ScheduledFuture<Sensor> future;
	
	public Sensor() {
		min = 0;
		max = 0;
		size = 0;
	}

	@Override
	public void run() {
		try{
			Object value = sensing();
			if (pin == 41 && Device.getPlatform().equals(JCL_IoT_Sensing_Model.RASPBERRY_PI_2_B))
				dataType = "jpeg";			

			setLastValue(value);

			if ( Device.getMqttClient().isConnected() ){
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ObjectOutputStream os = new ObjectOutputStream(out);
				os.writeObject(value);

				MqttMessage message = new MqttMessage(out.toByteArray());
				message.setQos(2);

				if (Device.getMqttClient().isConnected()){
					Device.getMqttClient().publish(this.alias, message);
				}
			}
			
			Device.saveAsGV(this, dataType);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public Object sensing(){
		Object value = null;
		try{
			if ( pin == 41 && Device.getPlatform().equals(JCL_IoT_Sensing_Model.RASPBERRY_PI_2_B) ) {
				RPiCamera piCamera = new RPiCamera("/home/pi/Pictures");
				piCamera.setWidth(2592);   // Set width property of RPiCamera
				piCamera.setHeight(1944); // Set height property of RPiCamera
				piCamera.setTimeout(200);
				
				while (value == null){
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ImageIO.write( piCamera.takeBufferedStill(), "jpg", baos );
					baos.flush();
					byte[] imageInByte = baos.toByteArray();
					baos.close();
					value = imageInByte;
				}
			}
			else if ( Device.getSensingModel().isPortDigital(getPin()) ){
				Gpio gpio = new Gpio(Device.getSensingModel().getGPIO(pin), true);			
				value = gpio.read();
				gpio.delete();
			}else{
				Aio aio = new Aio( Device.getSensingModel().getGPIO(pin) );
				value =  aio.read();
				aio.delete();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return value;
	}
	
	public void removeFuture(){
		if (future != null)
			future.cancel(false);
	}
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}

	public char getDir() {
		return dir;
	}

	public void setDir(char dir) {
		this.dir = dir;
	}

	public Object getLastValue() {
		return lastValue;
	}

	public void setLastValue(Object lastValue) {
		this.lastValue = lastValue;
	}
	
	public int getPin() {
		return pin;
	}
	
	public void setPin(int port) {
		this.pin = port;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}
	
	public int getMinAndIncrement(){
		return this.min++;
	}
	
	
	public int getMaxAndIncrement(){
		return this.max++;
	}

	public ScheduledFuture<Sensor> getFuture() {
		return future;
	}

	public void setFuture(ScheduledFuture<Sensor> future) {
		this.future = future;
	}

	public String getDataType() {
		return dataType;
	}

	public JCLHashMap<Integer, JCL_Sensor> getValues() {
		return values;
	}

	public void setValues(JCLHashMap<Integer, JCL_Sensor> values) {
		this.values = values;
	}
	
}
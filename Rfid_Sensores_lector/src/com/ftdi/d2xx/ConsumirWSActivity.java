package com.ftdi.d2xx;

//URL
//"http://www.cheesejedi.com/rest_services/get_big_cheese.php?puzzle=1");
//"http://172.17.19.179:8080/WebApp1/resources/controlar.tablauno");

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ftdi.D2xx;
import com.ftdi.D2xx.D2xxException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;
import android.widget.Toast;

public class ConsumirWSActivity extends Activity implements SensorEventListener {

	JSONArray jArray = null;
	String StringJson;
	String Item = "";
	String S1 = "";
	String S2 = "";
	String S3 = "";
	String S4 = "";
	String S5 = "";
	String S6 = "";
	String S7 = "";
	String S8 = "";
	TextView Sensor1, Sensor2, Sensor3, Sensor4, Sensor5, Sensor6, Sensor7,
			Sensor8, Item2;

	private SensorManager mSensorManager;
	Sensor accelerometer;
	Sensor magnetometer;
	Float azimut;
	Float giro;
	int grados;
	
	int devCount = 0;
	String etiqueta = "3D001C0E7D52";

	Panel Fondo;
	Bitmap imagen, imagen1;
	Canvas canvas = null;
	private int icon_nav_X = 350;
	private int icon_nav_Y = 741;
	private int fondo_X = -306;
	private int fondo_Y = -1766;

	private Timer timer;// timer recibir notificaciones json
	private Timer timer2; //timer lectura de etiquetas rfid

	final Handler mhandler = new Handler();
	
	//Hilo para la lectura de las etiquetas RFID//
	protected void miHilo() {
		Thread t = new Thread() {
			public void run() {
				mhandler.post(hilodeprueba);
			}
		};
		t.start();
	}

	final Runnable hilodeprueba = new Runnable() {
		public void run() {

			try {
				pausarRFID();
				devCount = D2xx.createDeviceInfoList();
				if (devCount > 0) {
					
					D2xx ftD2xx = new D2xx();
					try {
						
						// open our first device
						ftD2xx.openByIndex(0);
						// configure our port
						// reset to UART mode for 232 devices
						ftD2xx.setBitMode((byte) 0, D2xx.FT_BITMODE_RESET);
						// set 9600 baud
						ftD2xx.setBaudRate(9600);
						// set 8 data bits, 1 stop bit, no parity
						ftD2xx.setDataCharacteristics(D2xx.FT_DATA_BITS_8,
								D2xx.FT_STOP_BITS_1, D2xx.FT_PARITY_NONE);
						// set no flow control
						ftD2xx.setFlowControl(D2xx.FT_FLOW_NONE, (byte) 0x11,
								(byte) 0x13);
						// set latency timer to 16ms
						ftD2xx.setLatencyTimer((byte) 16);
						// set a read timeout of 5s
						// ftD2xx.setTimeouts(5000, 0);
						// purge buffers
						ftD2xx.purge((byte) (D2xx.FT_PURGE_TX | D2xx.FT_PURGE_RX));

						try {
							Thread.sleep(100);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

						int rxq;

						int[] devStatus = null;
						devStatus = ftD2xx.getStatus();
						rxq = devStatus[0];
						if (rxq > 0) {

							// read the data back!
							byte[] InData = new byte[rxq];
							ftD2xx.read(InData, rxq);

							etiqueta = (new String(InData));
							
							graficar();
							// 1 3D00428928DE
							// 2 3D0029EEA15B
							// 3 3D0074688DAC
							// 4 3D00762EFB9E
							// 5 3D00746882A3
							// 6 3D007440A3AA
							// 7 3D00226DE193
							// 8 3D001C0E7D52


						}
						// close the port
						ftD2xx.close();
						iniciarRFID();

					} catch (D2xxException e) {
						String s = e.getMessage();
						if (s != null) {
						}
					} // fin del catch
				} else {

				}// fin del If devcount > 0
			} // fin del try

			catch (D2xxException e) {
				String s = e.getMessage();
				if (s != null) {
					// device_information.setText(s);
				}
			} // fin del catch

		}
	};
	
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Fondo = new Panel(this);
		setContentView(Fondo);

		// **variables_acelerómetro***//

		mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		//*****Lectura RFid********//
		try {
			D2xx.setVIDPID(0x0403, 0xada1);
		} catch (D2xxException e) {
		}
		
		//***Buscar Dispositivos RFID*****//
		
		try {
			devCount = 0;
			devCount = D2xx.createDeviceInfoList();
			if (devCount > 0) {
				Toast.makeText(getApplicationContext(), "RFID conectado",
						Toast.LENGTH_SHORT).show();
			} else {
				//showDialog(DIALOG_ALERT);
			}

		} // fin del try
		catch (D2xxException e) {
			String s = e.getMessage();
			if (s != null) {

			}
		} // fin de catch
		
		
		

		// ***********************funciones****************************//
		iniciarLectura(); // iniciar timer para el lector json
		iniciarRFID();    // iniciar lectura RFID

	}

	public void iniciarLectura() {
		// consultar las notificaciones Json desde el serivor
		// cada 15 s.
		try {
			this.timer = new Timer();
			this.timer.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					new LongRunningGetIO().execute();
					System.out.println(S1);

				}
			}, 500, 15000);
		} catch (Exception e) {
		}
	} // fin de iniciarAlarma*/

	public void pausarLectura() {
		this.timer.cancel();

	}
	
	public void iniciarRFID() {
		try {
			this.timer2 = new Timer();
			this.timer2.scheduleAtFixedRate(new TimerTask() {
				public void run() {
					miHilo();

				}
			}, 0, 500);
		} catch (Exception e) {
		}
	} // fin de iniciarAlarma*/

	public void pausarRFID() {
		this.timer2.cancel();
		devCount = 0;

	}

	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, accelerometer,
				SensorManager.SENSOR_DELAY_UI);
		mSensorManager.registerListener(this, magnetometer,
				SensorManager.SENSOR_DELAY_UI);
	}

	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
		pausarLectura();
		devCount = 0;
		pausarRFID();
		finish();

	}
	
	public void graficar() {
		//if (etiqueta.contains("3D00428928DE")) {
		if (etiqueta.contains("928DE")) {	
			
			fondo_X = -306;
			fondo_Y = -1535;
			//fX = fX + 5;
			//mY = 20 - imagen.getHeight()/2;
			//imagen = BitmapFactory.decodeResource(getResources(),
				//	R.drawable.plano1);
		//lugar.setImageResource(R.drawable.plano1);
	//} else if (etiqueta.contains("3D0029EEA15B")){
	} else if (etiqueta.contains("EA15B")){
		
		fondo_X = -306;
		fondo_Y=-1060;
		//lugar.setImageResource(R.drawable.plano2);
			//fX = fX - 5;
			//mY = mY + 5;
			//imagen = BitmapFactory.decodeResource(getResources(),
				//	R.drawable.plano2);
	//} else if (etiqueta.contains("3D0074688DAC")) {
	} else if (etiqueta.contains("88DAC")) {	
		
		fondo_Y= -650;//imagen 3
		fondo_X= - 356;//imagen
		//lugar.setImageResource(R.drawable.plano3);
	} else if (etiqueta.contains("EFB9E")) {
	//} else if (etiqueta.contains("3D00762EFB9E")) {
		fondo_X= -643;
		fondo_Y= -586;
		
		//lugar.setImageResource(R.drawable.plano4);
	} else if (etiqueta.contains("3D00746882A3")) {
		//lugar.setImageResource(R.drawable.plano5);
	} else if (etiqueta.contains("3D007440A3AA")) {
		//lugar.setImageResource(R.drawable.plano6);
	} else if (etiqueta.contains("3D00226DE193")) {
		//lugar.setImageResource(R.drawable.pa3_u4);
	} else if (etiqueta.contains("3D001C0E7D52")) {
		//lugar.setImageResource(R.drawable.pa3_u5);
	} else {
		//lugar.setImageResource(R.drawable.ic_launcher);
	}
		
	
	
}


	private class LongRunningGetIO extends AsyncTask<Void, Void, String> {
		protected String getASCIIContentFromEntity(HttpEntity entity)
				throws IllegalStateException, IOException {
			InputStream in = entity.getContent();
			StringBuffer out = new StringBuffer();
			int n = 1;
			while (n > 0) {
				byte[] b = new byte[4096];
				n = in.read(b);
				if (n > 0)
					out.append(new String(b, 0, n));
			}
			return out.toString();
		}

		@Override
		protected String doInBackground(Void... params) {
			HttpClient httpClient = new DefaultHttpClient();
			HttpContext localContext = new BasicHttpContext();
			HttpGet httpGet = new HttpGet(
					"http://192.168.10.15:8080/Prime1/resources/loginbeanpack.dbalertas");
			String text = null;
			try {
				HttpResponse response = httpClient.execute(httpGet,
						localContext);
				HttpEntity entity = response.getEntity();
				text = getASCIIContentFromEntity(entity);
			} catch (Exception e) {
				return e.getLocalizedMessage();
			}
			return text;
			
			
		}

		protected void onPostExecute(String results) {
			if (results != null) {

				StringJson = results;

				try {
					jArray = new JSONArray(StringJson);

					for (int i = 0; i < jArray.length(); i++) {

						JSONObject e = jArray.getJSONObject(i);

						// Item.setText("ITEM: " + e.get("item"));

						Item = e.getString("item");
						S1 = e.getString("s1");
						S2 = e.getString("s2");
						S3 = e.getString("s3");
						S4 = e.getString("s4");
						S5 = e.getString("s5");
						S6 = e.getString("s6");
						S7 = e.getString("s7");
						S8 = e.getString("s8");

						if (S1.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 1", Toast.LENGTH_SHORT)
									.show();

						} else {

						}

						if (S2.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 2", Toast.LENGTH_SHORT)
									.show();

						} else {

						}

						if (S3.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 3", Toast.LENGTH_SHORT)
									.show();

						} else {

						}

						if (S4.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 3", Toast.LENGTH_SHORT)
									.show();

						} else {

						}
						if (S5.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 3", Toast.LENGTH_SHORT)
									.show();

						} else {

						}
						if (S6.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 3", Toast.LENGTH_SHORT)
									.show();

						} else {

						}
						if (S7.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 3", Toast.LENGTH_SHORT)
									.show();

						} else {

						}

						if (S8.contains("FUEGO")) {

							Toast.makeText(getApplicationContext(),
									"ALARMA en Sector 3", Toast.LENGTH_SHORT)
									.show();

						} else {

						}

					}

				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}

		}
	}

	class Panel extends SurfaceView implements SurfaceHolder.Callback {
		// private GraficarThread thread1;
		private ViewThread mThread;

		public Panel(Context context) {
			super(context);
			imagen = BitmapFactory.decodeResource(getResources(),
					R.drawable.plano_completo_2);

			imagen1 = BitmapFactory.decodeResource(getResources(),
					R.drawable.icon_nav);

			// imagen1 = imagen;
			getHolder().addCallback(this);
			mThread = new ViewThread(this);
		}

		public void doDraw(Canvas canvas) {

			// canvas.drawBitmap(imagen1, mX, mY, null);
			Paint p = new Paint();
			p.setColor(Color.GREEN);
			p.setAntiAlias(true);
			p.setTextSize(20);

			if (azimut != null)
				canvas.save();
			// canvas.rotate(grados, icon_nav_X, icon_nav_Y);
			canvas.scale(2.7f, 2.7f);
			canvas.drawBitmap(imagen, fondo_X, fondo_Y, p);
			canvas.restore();

			//

			if(S2.contentEquals("Fuego"))
			canvas.drawText("Alerta Sector1", icon_nav_X - 10,
					icon_nav_Y - 30, p);
			else{}
			if(S2.contentEquals("FUEGO"))
			canvas.drawText("Alerta Sector2", icon_nav_X - 10,
					icon_nav_Y - 90, p);
			else{}
			canvas.drawText(etiqueta, icon_nav_X-10, icon_nav_Y-30, p);
			canvas.save();
			canvas.rotate(grados-180, icon_nav_X, icon_nav_Y);
			canvas.drawBitmap(imagen1, icon_nav_X - (imagen1.getWidth() / 2),
					icon_nav_Y - (imagen1.getHeight() / 2), p);
			canvas.restore();
			// canvas.drawText(String.valueOf(grados), mX, mY-90, p);
			// canvas.drawText(String.valueOf(ancho), 390, 350, p);

			// canvas.drawRect(200, 500, 400, 700, p);
			// invalidate();

		}

		/*
		 * @Override public boolean onTouchEvent(MotionEvent event) {
		 * 
		 * mov_x = (int) event.getX(); mov_y = (int) event.getY(); return true;
		 * }
		 */

		public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2,
				int arg3) {
		}

		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub

			if (!mThread.isAlive()) {
				mThread = new ViewThread(this);
				mThread.setRunning(true);
				mThread.start();
			}
		}

		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			if (mThread.isAlive()) {
				mThread.setRunning(false);
			}
		}

	}

	public class ViewThread extends Thread {
		private Panel mPanel;
		private SurfaceHolder mHolder;
		private boolean mRun = false;

		public ViewThread(Panel panel) {
			mPanel = panel;
			mHolder = mPanel.getHolder();
		}

		public void setRunning(boolean run) {
			mRun = run;
		}

		@Override
		public void run() {
			// Canvas canvas = null;
			while (mRun) {
				canvas = mHolder.lockCanvas();
				if (canvas != null) {
					mPanel.doDraw(canvas);
					mHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	float[] mGravity;
	float[] mGeomagnetic;

	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			mGravity = event.values;
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			mGeomagnetic = event.values;
		if (mGravity != null && mGeomagnetic != null) {
			float R[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(R, I, mGravity,
					mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(R, orientation);
				azimut = orientation[0]; // orientation contains: azimut, pitch
											// and roll
				// giro = (-azimut*360/(2*3.14159f));
				giro = (-azimut * 360 / (2 * 3.14f)) / 3;
				grados = (giro.intValue() * 3);
			}
		}

	}

}
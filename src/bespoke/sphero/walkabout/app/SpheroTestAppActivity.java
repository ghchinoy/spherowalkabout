package bespoke.sphero.walkabout.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import orbotix.robot.app.StartupActivity;
import orbotix.robot.base.DeviceAsyncData;
import orbotix.robot.base.DeviceMessenger;
import orbotix.robot.base.DeviceSensorsAsyncData;
import orbotix.robot.base.FrontLEDOutputCommand;
import orbotix.robot.base.RGBLEDOutputCommand;
import orbotix.robot.base.Robot;
import orbotix.robot.base.RobotProvider;
import orbotix.robot.base.SetDataStreamingCommand;
import orbotix.robot.base.StabilizationCommand;
import orbotix.robot.sensor.AccelerometerData;
import orbotix.robot.sensor.AttitudeData;
import orbotix.robot.sensor.DeviceSensorsData;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

public class SpheroTestAppActivity extends MapActivity {
	
	/*
	 * ID for launching StartupActivity for result to connect
	 */
	private final static int STARTUP_ACTIVITY = 0;
	
	/*
	 * Sphero
	 */
	private Robot mRobot;
	
	private static String TAG = "BespokeSphero";

	// movement
	private static int X_DISTANCE = 50;
	private static int Y_DISTANCE = 50;
	
	// thresholds
	private static double THRESHOLD_RIGHT = 0.5;
	private static double THRESHOLD_LEFT = -0.5;
	private static double THRESHOLD_FORWARD = 0.5;
	private static double THRESHOLD_BACKWARD = -0.5;
	private static double THRESHOLD_ZOOM_OUT = 1.8;
	private static double THRESHOLD_ZOOM_IN = -0.1;
	
	private TextView xValue;
	private TextView yValue;
	private TextView zValue;
	private TextView pitchValue;
	private TextView yawValue;
	private TextView rollValue;
	private MapView myMap;
	private MapController controller;
	private TextView movementDescription;
	
	private ImageView helpImageView;

	private PowerManager.WakeLock screenWakeLock;

	
	private int zoomMax = 15;
	
	private LocationManager locationManager;
	private Location lastKnownLocation;
	private LocationListener locationListener;
	private Geocoder gc;
	
	private DeviceMessenger.AsyncDataListener mDataListener = new DeviceMessenger.AsyncDataListener() {
		@Override
		public void onDataReceived(DeviceAsyncData data) {
			if (data instanceof DeviceSensorsAsyncData) {
				ArrayList<DeviceSensorsData> datalist = ((DeviceSensorsAsyncData)data).getAsyncData();
				if (datalist != null) {
					for (DeviceSensorsData item : datalist) {
						AccelerometerData acc = item.getAccelerometerData();
						if (acc != null) {
							xValue.setText(String.format("%.3f", acc.getFilteredAcceleration().x));
							yValue.setText(String.format("%.3f", acc.getFilteredAcceleration().y));
							zValue.setText(String.format("%.3f", acc.getFilteredAcceleration().z));
							
							if (acc.getFilteredAcceleration().x > THRESHOLD_RIGHT) {
								movementDescription.setText("You're moving right.");
								controller.scrollBy(X_DISTANCE, 0);
							}
							if (acc.getFilteredAcceleration().x < THRESHOLD_LEFT) {
								movementDescription.setText("You're moving left.");
								controller.scrollBy(-X_DISTANCE, 0);
							}
							if (acc.getFilteredAcceleration().y > THRESHOLD_FORWARD) {
								movementDescription.setText("You're moving forward.");
								controller.scrollBy(0, -Y_DISTANCE);
							}
							if (acc.getFilteredAcceleration().y < THRESHOLD_BACKWARD) { 
								movementDescription.setText("You're moving backwards.");
								controller.scrollBy(0, Y_DISTANCE);
							}
							if (acc.getFilteredAcceleration().z > THRESHOLD_ZOOM_OUT) {
								movementDescription.setText("You're moving up.");
								controller.zoomOut();
							}
							if (acc.getFilteredAcceleration().z < THRESHOLD_ZOOM_IN) { 
								movementDescription.setText("You're moving down.");
								controller.zoomIn();
							}
							
						}
						AttitudeData att = item.getAttitudeData();
						if (att != null) {
							pitchValue.setText(String.format("%s", att.getAttitudeSensor().pitch)); //yaw, roll
							yawValue.setText(String.format("%s", att.getAttitudeSensor().yaw));
							rollValue.setText(String.format("%s", att.getAttitudeSensor().roll));
						}
					}
				}
			}	
		}
	};
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setupWidgets();
    }
   
    
    
    @Override
    protected void onStart() {
    	super.onStart();
    	
    	// get user location
    	findlocation();
    	
    	locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    	  
    	lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    	setMapCenter(lastKnownLocation);
 
    	// disable dimming
    	getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	
    	// obtain robot
    	Intent i = new Intent(this, StartupActivity.class);
    	startActivityForResult(i, STARTUP_ACTIVITY);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	
    	if (requestCode == STARTUP_ACTIVITY && resultCode == RESULT_OK) {
    		// get the connected robot
    		final String robot_id = data.getStringExtra(StartupActivity.EXTRA_ROBOT_ID);
    		Log.d(TAG, "Hi, there robot " + robot_id);
    		if (robot_id != null && !robot_id.equals("")) {
    			mRobot = RobotProvider.getDefaultProvider().findRobot(robot_id);
    			
    			StabilizationCommand.sendCommand(mRobot, false);
                FrontLEDOutputCommand.sendCommand(mRobot, 0.5f);
                startStreaming();
    		}
    	}
    	
    	// start blinking
    	//blink(false);
    	RGBLEDOutputCommand.sendCommand(mRobot, 0, 0, 0);
    }
    
    
    @Override
    protected void onStop() {
        super.onStop();
        
        if (screenWakeLock != null) {
            screenWakeLock.release();
            screenWakeLock = null;
        }
        
        // remove the flag that disables the screen dimming
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // remove use of the location listener
        locationManager.removeUpdates(locationListener);
        
        // turn stabilization back on
        StabilizationCommand.sendCommand(mRobot, true);

        // turn rear light off
        FrontLEDOutputCommand.sendCommand(mRobot, 0.0f);
        
        // unregister the async data listener to prevent a memory leak.
        DeviceMessenger.getInstance().removeAsyncDataListener(mRobot, mDataListener);

        // stop the streaming data when we leave
        SetDataStreamingCommand.sendCommand(mRobot, 0, 0, SetDataStreamingCommand.DATA_STREAMING_MASK_OFF, 0);

        // pause here for a tenth of a second to allow the previous commands to go through before we shutdown
        // the connection to the ball
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // disconnect from the ball
        RobotProvider.getDefaultProvider().removeAllControls();
        
        //mRobot = null;
    }
    
    
    public void reconnect() {
    	onStart();
    }
    
    public void recenterView() {
    	setMapCenter(lastKnownLocation);
    	controller.setZoom(zoomMax);
    }
    
    private void setupWidgets() {
    	
    	
    	myMap = (MapView)findViewById(R.id.my_map);
    	myMap.setSatellite(true);
    	
    	movementDescription = (TextView)findViewById(R.id.movementDescription);
    	
    	xValue = (TextView)findViewById(R.id.valueX);
    	yValue = (TextView)findViewById(R.id.valueY);
    	zValue = (TextView)findViewById(R.id.valueZ);
    	
    	pitchValue = (TextView)findViewById(R.id.valuePitch);
    	rollValue = (TextView)findViewById(R.id.valueRoll);
    	yawValue = (TextView)findViewById(R.id.valueYaw);
    	
    }
    
    private void findlocation() {
    	// get user location
    	locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    	
    	locationListener = new LocationListener() {
    	    public void onLocationChanged(Location location) {
    	      // Called when a new location is found by the network location provider.
    	      // 1. set map to location here
    	      // 2. stop listening
    	    	setMapCenter(location);
    	    	Log.d(TAG, "Found a location from the location service.");
    	    	locationManager.removeUpdates(this);
    	    }

    	    public void onStatusChanged(String provider, int status, Bundle extras) {}

    	    public void onProviderEnabled(String provider) {}

    	    public void onProviderDisabled(String provider) {}
    	  };
    }
    
    private void setMapCenter(Location location) {
    	
    	gc = new Geocoder(this);
    	
    	String placename = "";
    	
    	try {
			List<Address> place = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
			placename = "Location found: " + place.get(0).getLocality();
			
		} catch (IOException e) {
			placename = String.format("Location found (%s, %s)", location.getLatitude(), location.getLongitude());
		} catch (IndexOutOfBoundsException ioobe) {
			
		}
    	
    	Toast.makeText(this, placename, Toast.LENGTH_SHORT).show();
    	
    	Log.d(TAG, String.format("Location found (%s, %s)", location.getLatitude(), location.getLongitude()));
    	
    	controller = myMap.getController();
    	zoomMax = myMap.getMaxZoomLevel();
    	controller.setCenter(new GeoPoint((int)(location.getLatitude() * 1E6), (int)(location.getLongitude() * 1E6)));
    	controller.setZoom(zoomMax);
    	
    	// put an overlay on the map
    	Drawable marker = getResources().getDrawable(R.drawable.sphero_marker);
        int markerWidth = marker.getIntrinsicWidth();
        int markerHeight = marker.getIntrinsicHeight();
        marker.setBounds(0, markerHeight, markerWidth, 0);
        SpheroOverlay spheroOverlay = new SpheroOverlay(marker);
        myMap.getOverlays().add(spheroOverlay);
        spheroOverlay.addItem(locationToGeoPoint(location), "Sphero!", "This is where you're at.");
    }
    
    private GeoPoint locationToGeoPoint(Location location) {
    	return new GeoPoint((int)(location.getLatitude() * 1E6), (int)(location.getLongitude() * 1E6));
    }
    
    private void startStreaming(){

        if(mRobot != null){

            //Set up a bitmask containing the sensor information we want to stream
            final int mask =
                    SetDataStreamingCommand.DATA_STREAMING_MASK_ACCELEROMETER_X_FILTERED |
                    SetDataStreamingCommand.DATA_STREAMING_MASK_ACCELEROMETER_Y_FILTERED |
                    SetDataStreamingCommand.DATA_STREAMING_MASK_ACCELEROMETER_Z_FILTERED |
                    SetDataStreamingCommand.DATA_STREAMING_MASK_IMU_PITCH_ANGLE_FILTERED |
                    SetDataStreamingCommand.DATA_STREAMING_MASK_IMU_ROLL_ANGLE_FILTERED |
                    SetDataStreamingCommand.DATA_STREAMING_MASK_IMU_YAW_ANGLE_FILTERED;

            //Specify a divisor. The frequency of responses that will be sent is 400hz divided by this divisor.
            // 50
            final int divisor = 50;

            //Specify the number of frames that will be in each response. You can use a higher number to 
            // "save up" responses and send them at once with a lower frequency, but more packets per response.
            final int packet_frames = 1;

            //Total number of responses before streaming ends. 0 is infinite.
            final int response_count = 0;

            //Send this command to Sphero to start streaming
            SetDataStreamingCommand.sendCommand(mRobot, divisor, packet_frames, mask, response_count);

            //Set the AsyncDataListener that will process each response.
            DeviceMessenger.getInstance().addAsyncDataListener(mRobot, mDataListener);
        }
    }

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_reconnect:
			Log.d(TAG, "Reconnecting to robot...");
			reconnect();
			return true;
		case R.id.menu_reset:
			Log.d(TAG, "Recentering view...");
			recenterView();
			return true;
		case R.id.menu_sleep:
			//sleep();
			return true;
		case R.id.menu_findme:
			findlocation();
			return true;
		case R.id.menu_help:
			Log.d(TAG, "Displaying help...");
			startActivity(new Intent(this, HelpActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
    
}
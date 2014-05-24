package at.uni.as.colortracking;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import jp.ksksue.driver.serial.FTDriver;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;
import at.uni.as.colortracking.robot.Robot;
import at.uni.as.colortracking.robot.RobotEnviroment;
import at.uni.as.colortracking.tracking.Color;
import at.uni.as.colortracking.tracking.ColorTrackingUtil;
import at.uni.as.colortracking.tracking.TrackedBeacon;
import at.uni.as.colortracking.tracking.TrackedColor;
import at.uni.as.colortracking.util.CalibrationHelper;

public class MainActivity extends Activity implements CvCameraViewListener2,
		OnTouchListener {
	private static final String TAG = "ColorTracking::MainActivity";

	public static float CAMERA_H;
	public static float CAMERA_W;

	private CameraBridgeViewBase mOpenCvCameraView;
	private Robot robot;
	private RobotEnviroment environment;
	private CalibrationHelper cal;

	// Menu Items
	private MenuItem menuHomography = null;
	private MenuItem menuToggleTracking = null;
	private MenuItem menuCatchObject = null;
	private MenuItem menuMoveTo = null;
	private MenuItem menuCalibrateColors = null;
	private MenuItem menuCalibrateRobot = null;

	// flags
	private boolean trackingEnabled = false;
	private boolean catchingEnabled = false;
	private boolean calcHomography = false;
	
	
	private boolean calibration = false;
	private boolean submitTouchedColor = false;
	private Stack<Color> calibrationStack = new Stack<Color>();
	private BallCatcher ballCatcher;
	private CoordsMover coordsMover;
	

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			if(status == LoaderCallbackInterface.SUCCESS) {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			} else 
				super.onManagerConnected(status);
		}
	};

	public MainActivity() {
		Log.i(TAG, "Instantiated new " + this.getClass());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "called onCreate");
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.layout_main_acitivity);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
		mOpenCvCameraView.setOnTouchListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this,
				mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.i(TAG, "called onCreateOptionsMenu");

		this.menuToggleTracking = menu.add("Toggle Tracking");
		this.menuHomography = menu.add("Calc HOMOGRAPHY");
		this.menuCatchObject = menu.add("Toggle catch Object");
		this.menuMoveTo = menu.add("Move to...");
		this.menuCalibrateColors = menu.add("Cal. Color");
		this.menuCalibrateRobot = menu.add("Cal. Robot");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == this.menuToggleTracking) {
			if (trackingEnabled) {
				trackingEnabled = false;
				Toast.makeText(this, "stopped tracking", Toast.LENGTH_SHORT).show();
			} else {
				trackingEnabled = true;
				if(environment.getHomography() == null)
					Toast.makeText(this, "started tracking without homography", Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(this, "started tracking", Toast.LENGTH_SHORT).show();
			}

		} else if (item == this.menuHomography) {
			calcHomography = true;
		} else if (item == this.menuCatchObject) {
			if(robot == null || !robot.isConnected()) {
				catchingEnabled = false;
				Toast.makeText(getApplicationContext(), "no robot connection", Toast.LENGTH_SHORT);
				return true;
			}
			
			if (catchingEnabled) {
				catchingEnabled = false;
				Toast.makeText(this, "catch object disabled",Toast.LENGTH_SHORT).show();
			} else {
				ballCatcher = new BallCatcher(robot);
				robot.barUp();
						
				if (coordsMover != null && coordsMover.isMoveToCoordsEnabled()) {
					coordsMover.setMoveToCoordsEnabled(false);
					Toast.makeText(getApplicationContext(),"MoveTo mode disabled!", Toast.LENGTH_SHORT).show();
				}
				catchingEnabled = true;
				ballCatcher.setBallCatchingEnabled(catchingEnabled);
				Toast.makeText(this, "catch object enabled",Toast.LENGTH_SHORT).show();
			}
		} else if (item == this.menuMoveTo) {	
			if(robot == null || robot.isConnected()) {
				Toast.makeText(this, "robot not connected",Toast.LENGTH_SHORT).show();
				return true;
			}
			
			coordsMover = new CoordsMover(robot);
			final EditText input = new EditText(this);

			input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
			AlertDialog.Builder alert = getAlertWindow("Move To...",
					"Enter coord pair [x:y] \nOne per line.", input);
			alert.setPositiveButton("Ok", new MoveClickListener(input));
			alert.show();
		} else if(item == this.menuCalibrateColors){
			calibrationStack.clear();
			calibrationStack.addAll(Arrays.asList(Color.values()));
			
			if(!calibrationStack.isEmpty()) {
				calibration = true;
			}
		} else if(item == this.menuCalibrateRobot) {
			cal = new CalibrationHelper(robot, environment, mOpenCvCameraView, this);
			cal.calibrate();
		}

		return true;
	}

	public void onCameraViewStarted(int width, int height) {
		//get screen resolution
		MainActivity.CAMERA_H = width;
		MainActivity.CAMERA_W = height;
		
		environment = new RobotEnviroment();
		robot = new Robot(new FTDriver((UsbManager) getSystemService(USB_SERVICE)));
		if (!robot.isConnected())
			Toast.makeText(getApplicationContext(),"unable to connect to robot!", Toast.LENGTH_SHORT).show();
		else {
			robot.moveForward(15, 250);
			robot.moveBackward(15, 250);
		}
	}

	public void onCameraViewStopped() {
		// not used
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat image = inputFrame.rgba();
		StringBuilder screenInfo = new StringBuilder();
		
		if(cal != null && cal.isCalibrationDone()) {
			while(true) {
				robot.success();
			}
		}
		
		if(calcHomography) {
			environment.calcHomography(image);
			calcHomography = false;
		} else if(calibration) {
			Scalar rgb = new Scalar(image.get((int) (CAMERA_W / 2), (int) (CAMERA_H / 2))[0], 
									image.get((int) (CAMERA_W / 2), (int) (CAMERA_H / 2))[1], 
									image.get((int) (CAMERA_W / 2), (int) (CAMERA_H / 2))[2]);
			
			if(submitTouchedColor) {
				calibrationStack.pop().setRGB(rgb);
				
				if(calibrationStack.isEmpty())
					calibration = false;
				submitTouchedColor = false;
			} else {
				Core.putText(
						image,  calibrationStack.peek().name() + ": "
								+ String.valueOf(rgb.val[0]) + " | " 
								+ String.valueOf(rgb.val[1]) + " | "
								+ String.valueOf(rgb.val[2]),
						new Point(CAMERA_H / 2 + 10, CAMERA_W / 2 - 10),
						Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(
								255,255,255));
				
				Core.line(image, new Point(0.0, CAMERA_W / 2), new Point(CAMERA_H, CAMERA_W / 2) , new Scalar(255,255,255), 2);
				Core.line(image, new Point(CAMERA_H / 2, 0.0), new Point(CAMERA_H / 2, CAMERA_W) , new Scalar(255,255,255), 2);
			}
		} else if(trackingEnabled || catchingEnabled) {	
			Map<Color, List<TrackedColor>> trackedColors = ColorTrackingUtil.detectColors(image);
			image = ColorTrackingUtil.drawTrackedColors(image, trackedColors, environment.getHomography());
			if(image == null)
				return inputFrame.rgba();
			
			List<TrackedBeacon> beacons = RobotEnviroment.extractBeacons(trackedColors);
			
			if(environment.getHomography() != null) {
				Point position = RobotEnviroment.calcPosition(beacons, environment.getHomography());
				robot.setPosition(position);
			}
			
			// draw robot coordinates on screen
			if (robot.getPosition() != null) {
				screenInfo.append("Robot-Position: ");
				screenInfo.append(new DecimalFormat("#0.00").format(robot.getPosition().x));
				screenInfo.append("|");
				screenInfo.append(new DecimalFormat("#0.00").format(robot.getPosition().y));
				screenInfo.append("\n\n");
			}
			
			
			screenInfo.append("Beacons: \n");
			for(TrackedBeacon b : beacons) {
				screenInfo.append(b.getUpperColor().getColor().name());
				screenInfo.append(" | ");
				screenInfo.append(b.getLowerColor().getColor().name());
				screenInfo.append("\n");
			}
			
			if (catchingEnabled) {
				if(!ballCatcher.isDone()) {
					ballCatcher.catchBall(environment, trackedColors);
					ballCatcher.printStatus( image );
				} else {
					// MAYBE RESTART?
				}
			}
			
			printInfo(image, screenInfo.toString(), 0, 20);
		} 

		return image;
	}

	private void printInfo(Mat image, String string, int x, int y) {
		String[] lines = string.split("\n");
		
		for(int i=0; i < lines.length; i++)
			Core.putText(image, lines[i], new Point(x, y + 30 * i), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255.0, 255.0, 255.0));
	}

	private Builder getAlertWindow(String title, String message,
			EditText textbox) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setView(textbox);
		alert.setTitle(title);
		alert.setMessage(message);

		return alert;
	}

	protected class MoveClickListener implements
			DialogInterface.OnClickListener {
		private EditText input;

		public MoveClickListener(EditText input) {
			this.input = input;
		}

		public void onClick(DialogInterface dialog, int whichButton) {
			String value = input.getText().toString();
			if (value != null && value.length() > 0 && robot != null) {
				
				coordsMover.setTargetCoords(RobotEnviroment.parseCoordsList(value)); 
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if(event.getAction() != MotionEvent.ACTION_UP)
			return true;
		
		if(calibration)
			submitTouchedColor = true;
		
		return true;
	}
}

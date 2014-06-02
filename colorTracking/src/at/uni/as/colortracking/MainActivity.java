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
import at.uni.as.colortracking.util.RobotCalibrator;

public class MainActivity extends Activity implements CvCameraViewListener2,
		OnTouchListener {
	private static final String TAG = "ColorTracking::MainActivity";

	public static int CAMERA_H;
	public static int CAMERA_W;

	private CameraBridgeViewBase mOpenCvCameraView;
	private Robot robot;
	private RobotEnviroment environment;

	// Menu Items
	private MenuItem menuHomography = null;
	private MenuItem menuToggleTracking = null;
	private MenuItem menuCatchObject = null;
	private MenuItem menuMoveTo = null;
	private MenuItem menuCalibrateColors = null;
	private MenuItem menuCalibrateRobot = null;

	// flags
	private boolean trackingEnabled = false;
	private boolean calcHomography = false;
	
	private boolean calibration = false;
	private boolean submitTouchedColor = false;
	private Stack<Color> calibrationStack = new Stack<Color>();
	private BallCatcher ballCatcher;
	private RobotCalibrator robotCalibrator;
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
				if(robot != null)
					robot.setPosition(null);
					robot.setAngle(null);
			}

		} else if (item == this.menuHomography) {
			calcHomography = true;
		} else if (item == this.menuCatchObject) {
			if(robot == null || !robot.isConnected()) {
				Toast.makeText(this, "robot not connected",Toast.LENGTH_SHORT).show();
				return true;
			}
			
			if (ballCatcher != null && ballCatcher.isBallCatchingEnabled()) {
				ballCatcher.setBallCatchingEnabled(false);

				Toast.makeText(this, "catch object disabled",Toast.LENGTH_SHORT).show();
			} else {
				ballCatcher = new BallCatcher(robot, CAMERA_W, CAMERA_H);
				ballCatcher.setBallCatchingEnabled(true);
				
				Toast.makeText(this, "catch object enabled",Toast.LENGTH_SHORT).show();

				if (coordsMover != null && coordsMover.isMoveToCoordsEnabled()) {
					coordsMover.setMoveToCoordsEnabled(false);
					Toast.makeText(getApplicationContext(),"MoveTo mode disabled!", Toast.LENGTH_SHORT).show();
				}
			}
		} else if (item == this.menuMoveTo) {	
			if(robot == null || !robot.isConnected()) {
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
			/*if(robot == null || !robot.isConnected()) {
				Toast.makeText(this, "robot not connected",Toast.LENGTH_SHORT).show();
				return true;
			}*/
			
			robotCalibrator = new RobotCalibrator(robot, environment, new Point(CAMERA_W / 2, CAMERA_H / 2) );
		}

		return true;
	}

	public void onCameraViewStarted(int width, int height) {
		//get screen resolution
		MainActivity.CAMERA_W = width;
		MainActivity.CAMERA_H = height;
		
		environment = new RobotEnviroment();
		robot = new Robot(new FTDriver((UsbManager) getSystemService(USB_SERVICE)));
		if (!robot.isConnected())
			Toast.makeText(getApplicationContext(),"unable to connect to robot!", Toast.LENGTH_SHORT).show();
		else {
			robot.move(10);
			robot.move(-10);
			robot.barUp();
		}
	}

	public void onCameraViewStopped() {
		// not used
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat image = inputFrame.rgba();
		
		if(calcHomography) {
			
			//HOMOGRAPHY
			//===============================================================================
			environment.calcHomography(image);
			calcHomography = false;
		} else if(robotCalibrator != null && (robotCalibrator.isCalibrationFailed() || !robotCalibrator.isCalibrationRunning())) {
			robotCalibrator = null;
		} else if(robotCalibrator != null && robotCalibrator.isCalibrationRunning()) {
			
			//ROBOT CALIBRATION
			//===============================================================================
			robotCalibrator.calibrate(image);
		} else if(calibration) {
			
			//COLOR CALIBRATION
			//===============================================================================
			Scalar rgb = new Scalar(image.get((int) (CAMERA_H / 2), (int) (CAMERA_W / 2))[0], 
									image.get((int) (CAMERA_H / 2), (int) (CAMERA_W / 2))[1], 
									image.get((int) (CAMERA_H / 2), (int) (CAMERA_W / 2))[2]);
			
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
						new Point(CAMERA_W / 2 + 10, CAMERA_H / 2 - 10),
						Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(
								255,255,255));
				
				ColorTrackingUtil.drawCross(image, CAMERA_W, CAMERA_H, new Scalar(255.0, 255.0, 255.0), 2);
			}
		} else if(trackingEnabled) {
			
			//COLOR TRACKING
			//===============================================================================
			Map<Color, List<TrackedColor>> trackedColors = ColorTrackingUtil.detectColors(image); 
			
			image = ColorTrackingUtil.drawTrackedColors(image, trackedColors, environment.getHomography(), new Point(CAMERA_W / 2, CAMERA_H / 2));
			if(image == null)
				return inputFrame.rgba();
			
			List<TrackedBeacon> beacons = RobotEnviroment.extractBeacons(trackedColors);
			
			//LOCALIZATION
			//===============================================================================
			if(robot.getAngle() == null) {
				/*Double angle = RobotEnviroment.calcAngle(beacons, new Point(CAMERA_W / 2, CAMERA_H / 2));
				if(angle != null)
					robot.setAngle(angle);*/
				robot.setAngle(90.0);
			}
			if(environment.getHomography() != null && robot.getPosition() == null) {
				Point position = RobotEnviroment.calcPosition(beacons, environment.getHomography());
				if(position != null)
					robot.setPosition(position);
			}
			
			//BALL CATCHING
			//===============================================================================
			if (ballCatcher != null && ballCatcher.isBallCatchingEnabled()) {
				ballCatcher.catchBall(environment, trackedColors);
			} else if(ballCatcher != null && ballCatcher.isDone()) {
				ScreenInfo.getInstance().add( "BALL CATCHED" , ScreenInfo.POS_BOTTOM_LEFT, 2, ScreenInfo.COLOR_BLUE );
			}
			
			//COORD MOVING
			//===============================================================================
			if (coordsMover != null && coordsMover.isMoveToCoordsEnabled()) {				
				coordsMover.moveToCoords();
			}
			
			//ROBOT INFO
			//===============================================================================
			if (robot != null && robot.getPosition() != null) {
				StringBuilder screenInfo = new StringBuilder();
				screenInfo.append("Robot-Position: ");
				screenInfo.append(new DecimalFormat("#0.00").format(robot.getPosition().x));
				screenInfo.append("|");
				screenInfo.append(new DecimalFormat("#0.00").format(robot.getPosition().y));
				ScreenInfo.getInstance().add( screenInfo.toString(), ScreenInfo.POS_TOP_LEFT, ScreenInfo.COLOR_WHITE );
			}
			if (robot != null & robot.getAngle() != null) {
				StringBuilder screenInfo = new StringBuilder();
				screenInfo.append("Robot-Angle: ");
				screenInfo.append(new DecimalFormat("#0.00").format(robot.getAngle()));
				ScreenInfo.getInstance().add( screenInfo.toString(), ScreenInfo.POS_TOP_LEFT, ScreenInfo.COLOR_WHITE );
			}
			if(robot != null) {
				StringBuilder screenInfo = new StringBuilder();
				screenInfo.append("Robot-Calibration: ");
				screenInfo.append("move: ");
				screenInfo.append(new DecimalFormat("#0.00").format(Robot.Command.MOVE.getCal()));
				screenInfo.append(" |turn: ");
				screenInfo.append(new DecimalFormat("#0.00").format(Robot.Command.TURN.getCal()));
				ScreenInfo.getInstance().add( screenInfo.toString(), ScreenInfo.POS_TOP_LEFT, ScreenInfo.COLOR_WHITE );
			}
			
			
			
			ScreenInfo.getInstance().add( "Beacons: ", ScreenInfo.POS_TOP_LEFT, ScreenInfo.COLOR_WHITE );
			for(TrackedBeacon b : beacons) {
				StringBuilder screenInfo = new StringBuilder();
				screenInfo.append(b.getUpperColor().getColor().name());
				screenInfo.append(" | ");
				screenInfo.append(b.getLowerColor().getColor().name());
				ScreenInfo.getInstance().add( screenInfo.toString(), ScreenInfo.POS_TOP_LEFT, ScreenInfo.COLOR_WHITE );
			}
			
			ScreenInfo.getInstance().print( image );
			
			ColorTrackingUtil.drawScreenCenter(image, CAMERA_W, CAMERA_H, new Scalar(0.0, 0.0, 0.0), 1);
		} 

		return image;
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

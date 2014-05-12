package at.uni.as.colortracking;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

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
import android.content.res.Resources.NotFoundException;
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
import at.uni.as.colortracking.tracking.TrackedColor;

public class MainActivity extends Activity implements CvCameraViewListener2,
		OnTouchListener {
	private static final String TAG = "ColorTracking::MainActivity";

	private static float RES_DISP_H;
	private static float RES_DISP_W;

	private CameraBridgeViewBase mOpenCvCameraView;
	private Robot robot;
	private RobotEnviroment enviroment;

	// Menu Items
	private MenuItem menuHomography = null;
	private MenuItem menuStartLocalization = null;
	private MenuItem menuCatchObject = null;
	private MenuItem menuMoveTo = null;

	// flags
	private boolean trackingEnabled = false;
	private boolean calcHomography = false;
	

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
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

		this.menuStartLocalization = menu.add("Toggle Localization");
		this.menuHomography = menu.add("Calc HOMOGRAPHY");
		this.menuCatchObject = menu.add("Toggle catch Object");
		this.menuMoveTo = menu.add("Move to...");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == this.menuStartLocalization) {
			// ignore if no color data set
			if(enviroment.getHomography() == null) {
				Toast.makeText(this, "no homography", Toast.LENGTH_SHORT)
						.show();
				return true;
			}

			trackingEnabled = !trackingEnabled;
			
			// toggle tracking
			if (trackingEnabled)
				Toast.makeText(this, "started tracking", Toast.LENGTH_SHORT).show();
			else
				Toast.makeText(this, "stopped tracking", Toast.LENGTH_SHORT).show();

		} else if (item == this.menuHomography) {
			calcHomography = true;
		} else if (item == this.menuCatchObject) {
			if (robot != null) {
				if (robot.isCatchObjectEnabled()) {
					robot.setCatchObjectEnabled(false);

					Toast.makeText(this, "catch object disabled",
							Toast.LENGTH_SHORT).show();
				} else {
					robot.setCatchObjectEnabled(true);

					Toast.makeText(this, "catch object enabled",
							Toast.LENGTH_SHORT).show();

					if (robot.isMoveToCoordsEnabled()) {
						robot.setMoveToCoordsEnabled(false);
						Toast.makeText(getApplicationContext(),
								"MoveTo mode disabled!", Toast.LENGTH_SHORT)
								.show();
					}
				}
			}
		} else if (item == this.menuMoveTo) {
			final EditText input = new EditText(this);

			input.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
			AlertDialog.Builder alert = getAlertWindow("Move To...",
					"Enter coord pair [x:y] \nOne per line.", input);
			alert.setPositiveButton("Ok", new MoveClickListener(input));
			alert.show();
		}

		return true;
	}

	public void onCameraViewStarted(int width, int height) {
		//get screen resolution
		MainActivity.RES_DISP_H = width;
		MainActivity.RES_DISP_W = height;
		
		enviroment = new RobotEnviroment();
		robot = new Robot(new FTDriver((UsbManager) getSystemService(USB_SERVICE)));
		if (!robot.isConnected())
			Toast.makeText(getApplicationContext(),"unable to connect to robot!", Toast.LENGTH_SHORT).show();
		else {
			robot.moveForward(15, 250);
			robot.moveBackward(15, 250);
		}
		
		mOpenCvCameraView.setMaxFrameSize(50, 50);
        mOpenCvCameraView.enableFpsMeter();
	}

	public void onCameraViewStopped() {
		// not used
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat image = inputFrame.rgba();

		if(trackingEnabled) {
			try {
				//TODO: calc homography
				Map<Color, List<TrackedColor>> trackedColors = ColorTrackingUtil.detectColors(image);
				image = ColorTrackingUtil.drawTrackedColors(image, trackedColors);
				if(image == null)
					return inputFrame.rgba();
				
				Point position = RobotEnviroment.calcPosition(RobotEnviroment.extractBeacons(trackedColors), enviroment.getHomography());
				robot.setPosition(position);
				
				// draw robot coordinates on screen
				if (robot.getPosition() != null) {
					Core.putText(
							image,
							"robot: "
									+ String.valueOf( DecimalFormat.getIntegerInstance().format(robot.getPosition().x))
									+ ","
									+ String.valueOf( DecimalFormat.getIntegerInstance().format(robot.getPosition().y)),
							new Point(RES_DISP_H / 2, RES_DISP_W / 2),
							Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(
									50.0));
				}
				
				if (robot != null && robot.isConnected()) {				
					if (robot.isMoveToCoordsEnabled()) {
						//TODO: rework moveto
					} else if (robot.isCatchObjectEnabled()) {
						// add new catch target if not already set
						//TODO: catch object
					}
				}
			} catch (NotFoundException e) {
				// Toast.makeText(this, "could not calc ProbMap",
				// Toast.LENGTH_SHORT).show();
			}
		}

		return image;
	}

	private Builder getAlertWindow(String title, String message,
			EditText textbox) {
		// code for costum alertwindow
		// LayoutInflater inflater = (LayoutInflater)
		// getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		// View layout = inflater.inflate(R.layout.yourLayoutId, (ViewGroup)
		// findViewById(R.id.yourLayoutRoot));

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
				robot.setMoveToCoords(ColorTrackingUtil.parseCoordsList(value));

				if (robot.isMoveToCoordsEnabled()) {
					Toast.makeText(getApplicationContext(),
							"MoveTo mode enabled!", Toast.LENGTH_SHORT).show();
					if (robot.isCatchObjectEnabled()) {
						robot.setCatchObjectEnabled(false);
						Toast.makeText(getApplicationContext(),
								"CatchObject disabled!", Toast.LENGTH_SHORT)
								.show();
					}
				} else
					Toast.makeText(getApplicationContext(),
							"Failed to add Move-Coordinates",
							Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(),
						"Failed to add Move-Coordinates", Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return true;
	}
}

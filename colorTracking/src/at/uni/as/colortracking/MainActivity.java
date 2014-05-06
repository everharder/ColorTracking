package at.uni.as.colortracking;

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
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends Activity implements CvCameraViewListener2,
		OnTouchListener {
	private static final String TAG = "ColorTracking::MainActivity";

	private static final float RES_DISP_H = 1280;
	private static final float RES_DISP_W = 720;

	private static final float POS_TRACK_X = RES_DISP_H / 2;
	private static final float POS_TRACK_Y = RES_DISP_W / 2;

	private CameraBridgeViewBase mOpenCvCameraView;
	private Robot robot;
	private RobotEnviroment enviroment;
	private ColorTracking trackSingle;
	private ColorTracking trackBeacon;

	// Menu Items
	private MenuItem menuCalibrateSingleColor = null;
	private MenuItem menuCalibrateBeacon = null;
	private MenuItem menuHomography = null;
	private MenuItem menuStartTracking = null;
	private MenuItem menuResetTrackedObjects = null;
	private MenuItem menuCatchObject = null;

	// flags
	private boolean calibrationEnabled = true;
	private boolean newSingleColor = false;
	private boolean newBeacon = false;

	private boolean ignoreTouch = false;

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

		this.menuCalibrateSingleColor = menu.add("Add Single Color");
		this.menuCalibrateBeacon = menu.add("Add Beacon");
		this.menuStartTracking = menu.add("Toggle Tracking");
		this.menuHomography = menu.add("Calc HOMOGRAPHY");
		this.menuResetTrackedObjects = menu.add("Reset Tracks");
		this.menuCatchObject = menu.add("Toggle catch Object");

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);

		if (item == this.menuStartTracking) {
			// ignore if no color data set
			if (trackSingle.getTrackedColorCount() == 0
					&& trackBeacon.getTrackedColorCount() == 0) {
				Toast.makeText(this, "no colors tracked", Toast.LENGTH_SHORT)
						.show();
				return true;
			}

			// disable calibration buttons
			calibrationEnabled = !calibrationEnabled;
			setCalibrationMenuEnabled(calibrationEnabled);

			// toggle tracking

			trackSingle.setTrackingActive(!trackSingle.getTrackingActive());
			trackBeacon.setTrackingActive(!trackBeacon.getTrackingActive());
			if (trackSingle.getTrackingActive()
					|| trackBeacon.getTrackingActive())
				Toast.makeText(this, "started tracking", Toast.LENGTH_SHORT)
						.show();
			else
				Toast.makeText(this, "stopped tracking", Toast.LENGTH_SHORT)
						.show();

		} else if (item == this.menuResetTrackedObjects) {
			trackSingle.resetTrackedObjects();
			trackBeacon.resetTrackedObjects();

		} else if (item == this.menuHomography) {
			trackSingle.setCalcHomography(true);
			trackBeacon.setCalcHomography(true);

		} else if (item == this.menuCalibrateSingleColor) {
			final EditText input = new EditText(this);

			AlertDialog.Builder alert = getAlertWindow(
					"Calibrate Single Color", "Enter Color label.", input);
			alert.setPositiveButton("Ok", new SingleColorClickListener(input));
			alert.show();

		} else if (item == this.menuCalibrateBeacon) {
			final EditText input = new EditText(this);

			AlertDialog.Builder alert = getAlertWindow("Calibrate Beacon",
					"Enter Beacon Coords[x:y]", input);
			alert.setPositiveButton("Ok", new BeaconClickListener(input));
			alert.show();
		} else if (item == this.menuCatchObject) {
			if (robot != null) {
				if (robot.isCatchObjectEnabled()) {
					robot.setCatchObjectEnabled(false);
				} else {
					robot.setCatchObjectEnabled(true);
				}
			}
		}

		return true;
	}

	public void onCameraViewStarted(int width, int height) {
		trackSingle = new ColorTracking();
		trackBeacon = new ColorTracking();
		robot = new Robot(new FTDriver(
				(UsbManager) getSystemService(USB_SERVICE)));
		if (!robot.isConnected())
			Toast.makeText(getApplicationContext(),
					"unable to connect to robot!", Toast.LENGTH_SHORT).show();
		enviroment = new RobotEnviroment();

		if (robot.isConnected()) {
			robot.ledOn();
			robot.ledOff();
			robot.barDown();
			robot.barUp();
			robot.moveForward();
			robot.stop();
		}
	}

	public void onCameraViewStopped() {
		// not used
	}

	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Mat image = null;	 

		try {
			image = trackSingle.processImage(inputFrame.rgba(),
					(int) POS_TRACK_X, (int) POS_TRACK_Y);
			image = trackBeacon.processImage(image, (int) POS_TRACK_X,
					(int) POS_TRACK_Y);

			if (robot != null && trackSingle.getTrackingActive()) {
				if (trackBeacon.isHomographyEnabled()) {
					robot.setPosition(enviroment.locate(trackBeacon
							.getTrackedObjects()));

					if (robot.getPosition() != null && image != null) {
						Core.putText(
								image,
								"robot: "
										+ String.valueOf(robot.getPosition().x)
										+ ","
										+ String.valueOf(robot.getPosition().y),
								new Point(RES_DISP_H / 2, RES_DISP_W / 2),
								Core.FONT_HERSHEY_SIMPLEX, 0.75, new Scalar(
										50.0));
					}

					if (robot.isCatchObjectEnabled()) {
						// add new catch target if not already set
						if (!robot.isInCatchMode()) {
							// robot.ledOn();
							robot.setCatchObject(trackSingle
									.getTrackedObjects().get(0));
						} else {
							// robot.ledOff();
						}

						robot.catchObject();
					}
				}
			}
		} catch (NotFoundException e) {
			// Toast.makeText(this, "could not calc ProbMap",
			// Toast.LENGTH_SHORT).show();
		}

		if (image == null)
			return inputFrame.rgba();

		// Draw cross for color calibration.
		if (trackSingle.isWaitingForProbMap()
				|| trackBeacon.isWaitingForProbMap()) {
			Core.line(image, new Point(0, RES_DISP_W / 2), new Point(
					RES_DISP_H, POS_TRACK_Y), new Scalar(255, 255, 255));
			Core.line(image, new Point(RES_DISP_H / 2, 0), new Point(
					POS_TRACK_X, RES_DISP_W), new Scalar(255, 255, 255));

			// Toast.makeText(this,"Touch to calibrate the Color at the cross-center",
			// Toast.LENGTH_SHORT).show();
		}

		return image;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_UP && ignoreTouch) {
			ignoreTouch = false;
			return true;
		}

		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			if (!trackSingle.isWaitingForProbMap()
					&& !trackBeacon.isWaitingForProbMap())
				return true;

			if (!trackSingle.getCalcProbMap() && newSingleColor) {
				trackSingle.setCalcProbMap(true);
				newSingleColor = false;
				menuCalibrateSingleColor.setEnabled(true);

				Toast.makeText(this, "ColorTracking added", Toast.LENGTH_SHORT)
						.show();
			} else if (!trackBeacon.getCalcProbMap() && newBeacon) {
				if (trackBeacon.getNewTracking() != null
						&& trackBeacon.getNewTracking().getTracks().size() == trackBeacon
								.getNewTracking().getTrackCount()) {
					newBeacon = false;
					menuCalibrateBeacon.setEnabled(true);

					Toast.makeText(this, "ColorTracking added",
							Toast.LENGTH_SHORT).show();
				} else {
					trackBeacon.setCalcProbMap(true);
				}
			}
			return true;
		default:
			return true;
		}
	}

	public void setCalibrationMenuEnabled(boolean enabled) {
		menuCalibrateSingleColor.setEnabled(enabled);
		menuCalibrateBeacon.setEnabled(enabled);
		menuHomography.setEnabled(enabled);
		menuResetTrackedObjects.setEnabled(enabled);

		calibrationEnabled = enabled;
	}

	private Builder getAlertWindow(String title, String message,
			EditText textbox) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setView(textbox);
		alert.setTitle(title);
		alert.setMessage(message);

		return alert;
	}

	protected class BeaconClickListener implements
			DialogInterface.OnClickListener {
		private EditText input;

		public BeaconClickListener(EditText input) {
			this.input = input;
		}

		public void onClick(DialogInterface dialog, int whichButton) {
			String value = input.getText().toString();
			if (value != null && value.length() > 0
					&& ColorTrackingUtil.hasCoordFormat(value)) {
				trackBeacon.trackColor(value, 2);

				newBeacon = true;
				// menuCalibrateBeacon.setEnabled(false);

				ignoreTouch = false;
			} else {
				Toast.makeText(getApplicationContext(),
						"Failed to add colorTrack", Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	protected class SingleColorClickListener implements
			DialogInterface.OnClickListener {
		private EditText input;

		public SingleColorClickListener(EditText input) {
			this.input = input;
		}

		public void onClick(DialogInterface dialog, int whichButton) {
			String value = input.getText().toString();
			if (value != null && value.length() > 0) {
				trackSingle.trackColor(value, 1);

				newSingleColor = true;
				menuCalibrateSingleColor.setEnabled(false);

				ignoreTouch = false;
			} else {
				Toast.makeText(getApplicationContext(),
						"Failed to add colorTrack", Toast.LENGTH_SHORT).show();
				finish();
			}
		}

	}
}

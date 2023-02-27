package com.example.opencvfacedetector

import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class MainActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    lateinit var cascFile: File
    var faceDetector: CascadeClassifier? = null
    lateinit var mOpenCvCameraView: CameraBridgeViewBase

    // Initialize OpenCV manager.
    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    mOpenCvCameraView.enableView()
                    // Load the Cascade Classifier
                    val iStream: InputStream =
                        resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
                    val cascadeDir = getDir("cascade", Context.MODE_PRIVATE)
                    cascFile = File(cascadeDir, "haarcasecade_frontalface_alt2.xml")
                    try {
                        val fos = FileOutputStream(cascFile)
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        bytesRead = iStream.read(buffer)
                        while (bytesRead != -1) {
                            fos.write(buffer, 0, bytesRead)
                            bytesRead = iStream.read(buffer)
                        }
                        iStream.close()
                        fos.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    faceDetector = CascadeClassifier(cascFile!!.absolutePath)
                    if (faceDetector!!.empty()) {
                        faceDetector = null
                    } else {
                        cascadeDir.delete()
                    }
                    mOpenCvCameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}
    override fun onCameraViewStopped() {}

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame):
        Mat? {
        val frame = inputFrame.rgba()
        Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGRA2BGR)
        // Perform Face Detection
        val faceDetections = MatOfRect()
        faceDetector!!.detectMultiScale(frame, faceDetections)
        for (rect in faceDetections.toArray()) {
            Imgproc.rectangle(
                frame,
                Point(rect.x.toDouble(), rect.y.toDouble()),
                Point(
                    (rect.x + rect.width).toDouble(),
                    (
                        rect.y +
                            rect.height
                        ).toDouble(),
                ),
                Scalar(255.0, 0.0, 0.0),
            )
        }
        return frame
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 0)
        }
        mOpenCvCameraView = findViewById(R.id.javaCameraView)
        mOpenCvCameraView.setCameraPermissionGranted()
        mOpenCvCameraView.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView.setCvCameraViewListener(this)
    }

    public override fun onPause() {
        super.onPause()
        mOpenCvCameraView.disableView()
    }
    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(
                OpenCVLoader.OPENCV_VERSION_3_0_0,
                this,
                mLoaderCallback,
            )
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }
    public override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView.disableView()
    }
    companion object {
        private const val TAG = "MainActivity"
    }
}

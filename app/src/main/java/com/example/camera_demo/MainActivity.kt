package com.example.camera_demo

import android.Manifest.permission.CAMERA
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var cameraId: String
    private lateinit var cameraHandler: Handler
    private lateinit var handlerThread: HandlerThread
    private lateinit var captureRequest: CaptureRequest.Builder
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSession: CameraCaptureSession

    private val stateCallback =
        CameraStateCallback { cameraDevice -> startCameraPreview(cameraDevice) }

    private val captureStateCallback =
        CaptureStateCallback { cameraSession -> updatePreview(cameraSession) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView.surfaceTextureListener = this
        buttonCapture.setOnClickListener { captureImage() }
    }

    override fun onResume() {
        super.onResume()
        startCameraThread()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        prepareCamera()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean = false

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

    private fun startCameraThread() {
        handlerThread = HandlerThread("THREAD_NAME")
        handlerThread.start()
        cameraHandler = Handler(handlerThread.looper)
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            //100 là request code, có thể là 1 số nguyên bất kỳ
            ActivityCompat.requestPermissions(
                this, arrayOf(CAMERA), 100
            )
        }
    }

    private fun prepareCamera() {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]
        requestCameraPermission()
        if (ActivityCompat.checkSelfPermission(this, CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId, stateCallback, null)
        }
    }

    private fun startCameraPreview(cameraDevice: CameraDevice) {
        this.cameraDevice = cameraDevice
        val surfaceTexture = textureView.surfaceTexture
        val surface = Surface(surfaceTexture)
        captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequest.addTarget(surface)
        cameraDevice.createCaptureSession(listOf(surface), captureStateCallback, null)
    }

    private fun updatePreview(session: CameraCaptureSession) {
        captureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
        cameraCaptureSession = session
    }

    private fun captureImage() {
        val imageReader = ImageReader.newInstance(400, 400, ImageFormat.JPEG, 1)
        val outputSurfaces =
            mutableListOf<Surface>(imageReader.surface)
        val captureRequest =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        captureRequest.addTarget(imageReader.surface)
        captureRequest.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO)
        val imageAvailableListener = ImageAvailableListener { reader ->
            displayImage(reader.acquireLatestImage())
            startCameraPreview(cameraDevice)
        }
        imageReader.setOnImageAvailableListener(imageAvailableListener, null)
        cameraDevice.createCaptureSession(
            outputSurfaces,
            CaptureStateCallback { cameraSession ->
                cameraSession.capture(
                    captureRequest.build(),
                    CameraCaptureListener(),
                    cameraHandler
                )
            },
            cameraHandler
        )
    }

    private fun displayImage(outputImage: Image) {
        val buffer: ByteBuffer = outputImage.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.position(0)
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        imageView.setImageBitmap(bitmap)
        imageView.rotation = 90f
    }
}

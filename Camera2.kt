

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.support.v4.content.ContextCompat
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import java.util.Collections.singletonList


class Camera2(private val textureView: TextureView) {

    private val cameraManager: CameraManager = textureView.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraFacing = CameraCharacteristics.LENS_FACING_BACK
    private var previewSize: Size? = null
    private var cameraId = "-1"
    private var backgroundHandler: Handler? = null
    private var backgroundThread: HandlerThread? = null
    private var cameraDevice: CameraDevice? = null
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var captureRequestBuilder: CaptureRequest.Builder? = null
    private var isFlashEnabled = false

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {


        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            setupCamera()
            openCamera()

        }
    }

    private fun setupCamera() {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cameraFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)
                if (cameraFacing == this.cameraFacing) {
                    val streamConfigurationMap = cameraCharacteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    previewSize = streamConfigurationMap.getOutputSizes(SurfaceTexture::class.java)[0]
                    this.cameraId = cameraId
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    fun onResume() {
        openBackgroundThread()
        if (textureView.isAvailable) {
            setupCamera()
            openCamera()
        } else textureView.surfaceTextureListener = surfaceTextureListener


    }

    fun onPauseOrOnStop() {
        closeCamera()
        closeBackgroundThread()

    }


    private fun closeCamera() {
        if (cameraCaptureSession != null) {
            cameraCaptureSession!!.close()
            cameraCaptureSession = null
        }

        if (cameraDevice != null) {
            cameraDevice!!.close()
            cameraDevice = null
        }
    }

    private fun closeBackgroundThread() {
        if (backgroundHandler != null) {
            backgroundThread!!.quitSafely()
            backgroundThread = null
            backgroundHandler = null
        }
    }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(textureView.context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    this@Camera2.cameraDevice = camera
                    createPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    this@Camera2.cameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                }

            }, backgroundHandler)
        }
    }

    private fun openBackgroundThread() {
        backgroundThread = HandlerThread("camera_background_thread")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun createPreviewSession() {
        try {
            val surfaceTexture = textureView.surfaceTexture
            surfaceTexture.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)
            val previewSurface = Surface(surfaceTexture)
            captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder!!.addTarget(previewSurface)

            cameraDevice!!.createCaptureSession(singletonList(previewSurface),
                    object : CameraCaptureSession.StateCallback() {

                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            if (cameraDevice == null) {
                                return
                            }

                            try {
                                this@Camera2.cameraCaptureSession = cameraCaptureSession
                                buildDefaultCaptureRequest()

                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }

                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {

                        }
                    }, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun buildDefaultCaptureRequest() {
//        captureRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE,
//                CaptureRequest.CONTROL_AE_MODE_ON)
        captureRequestBuilder!!.set(CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_OFF)

        cameraCaptureSession!!.setRepeatingRequest(captureRequestBuilder!!.build(), null, backgroundHandler)

        isFlashEnabled = false
    }

    // When enable flash is called a flash would be fired for each capture
    private fun launchFlash() {
        if (isFlashEnabled) {
            cameraCaptureSession!!.setRepeatingRequest(captureRequestBuilder!!.build(), null, backgroundHandler)
        } else return


    }

    private fun enableFlash() {
//        captureRequestBuilder!!.set(CaptureRequest.CONTROL_AE_MODE,
//                CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH)
        captureRequestBuilder!!.set(CaptureRequest.FLASH_MODE,
                CaptureRequest.FLASH_MODE_SINGLE)


        isFlashEnabled = true
    }

    private fun lockPreview() {
        try {
            cameraCaptureSession!!.capture(captureRequestBuilder!!.build(),
                    null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun unlockPreview() {
        try {
            cameraCaptureSession!!.setRepeatingRequest(captureRequestBuilder!!.build(),
                    null, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    // This method switches Camera Lens Front or Back then restarts camera.
    fun switchCamera() {
        onPauseOrOnStop()
        cameraFacing = if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK)
            CameraCharacteristics.LENS_FACING_FRONT
        else CameraCharacteristics.LENS_FACING_BACK
        onResume()


    }


    fun setFlash(b: Boolean) {
        if (textureView.context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            if (b && cameraFacing == CameraCharacteristics.LENS_FACING_BACK)
                enableFlash()
            else if (cameraFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                Log.e("Camera2", "Front Camera Flash isn't supported yet.")
                return
            } else buildDefaultCaptureRequest()
        }

    }


    fun isFlashEnabled() = isFlashEnabled && cameraFacing == CameraCharacteristics.LENS_FACING_BACK

    fun takePhoto(onPictureListener: OnPictureListener) {
        launchFlash()
        lockPreview()

        if (textureView.isAvailable) {
            try {
                onPictureListener.onPictureTaken(textureView.bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                unlockPreview()
            }
        }
    }
    
    interface OnPictureListener
    {
        fun onPictureTaken(bitmap:Bitmap)
    }
    // Use below method if you encounter stretched camera problem.
    // While there is no absolute guarantee that the primal code ensures most use-cases. Some devices can face this problem upon rendition.

//    private fun chooseOptimalSize(outputSizes: Array<Size>, width: Int, height: Int): Size {
//        val preferredRatio = height / width.toDouble()
//        var currentOptimalSize = outputSizes[0]
//        var currentOptimalRatio = currentOptimalSize.width / currentOptimalSize.height.toDouble()
//        for (currentSize in outputSizes) {
//            val currentRatio = currentSize.width / currentSize.height.toDouble()
//            if (Math.abs(preferredRatio - currentRatio) < Math.abs(preferredRatio - currentOptimalRatio)) {
//                currentOptimalSize = currentSize
//                currentOptimalRatio = currentRatio
//            }
//        }
//        return currentOptimalSize
//    }
}




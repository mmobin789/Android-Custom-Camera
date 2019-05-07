package mobin.customcamera.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View

@Deprecated("used when given support below api 21")
class CameraPreview(context: Context) : SurfaceView(context), SurfaceHolder.Callback {


    private var onPictureListener: (Bitmap) -> Unit = {}

    private val pictureCallback = Camera.PictureCallback { data, camera ->
        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
        onPictureListener(bitmap)


    }

    fun setPictureListener(onPictureListener: (Bitmap) -> Unit) {
        this.onPictureListener = onPictureListener
    }

    private var camera: Camera? = null
    private var cameraFront = false
    private var previewSize: Camera.Size? = null

    init {
        camera = Camera.open()
        camera!!.setDisplayOrientation(90)


        holder.addCallback(this)
    }


    private fun findFrontFacingCamera(): Int {

        var cameraId = -1
        // Search for the front facing camera
        val numberOfCameras = Camera.getNumberOfCameras()
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i
                cameraFront = true
                break
            }
        }
        return cameraId

    }

//    private fun setCameraDisplayOrientation(activity: AppCompatActivity, cameraId: Int) {
//        val info = android.hardware.Camera.CameraInfo()
//        android.hardware.Camera.getCameraInfo(cameraId, info)
//        val rotation = activity.getWindowManager().getDefaultDisplay()
//                .getRotation()
//        var degrees = 0
//        when (rotation) {
//            Surface.ROTATION_0 -> degrees = 0
//            Surface.ROTATION_90 -> degrees = 90
//            Surface.ROTATION_180 -> degrees = 180
//            Surface.ROTATION_270 -> degrees = 270
//        }
//
//        var result: Int
//        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//            result = (info.orientation + degrees) % 360;
//            result = (360 - result) % 360;  // compensate the mirror
//        } else {  // back-facing
//            result = (info.orientation - degrees + 360) % 360;
//        }
//        camera!!.setDisplayOrientation(result)
//    }

    private fun refreshCamera() {
        if (holder.surface == null) {
            // preview surface does not exist
            return
        }
        // stop preview before making changes
        try {
            camera!!.stopPreview()
        } catch (e: Exception) {
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        //setCamera(camera)
        try {
            val params = camera!!.parameters
            params.setPreviewSize(previewSize!!.width, previewSize!!.height)
            camera!!.parameters = params
            camera!!.setPreviewDisplay(holder)
            camera!!.startPreview()
        } catch (e: Exception) {
            Log.e("CustomCam", "Error starting camera preview: " + e.message)
        }

    }

    private fun findBackFacingCamera(): Int {
        var cameraId = -1
        //Search for the back facing camera
        //get the number of cameras
        val numberOfCameras = Camera.getNumberOfCameras()
        //for every camera check
        for (i in 0 until numberOfCameras) {
            val info = Camera.CameraInfo()
            Camera.getCameraInfo(i, info)
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i
                cameraFront = false
                break

            }

        }
        return cameraId
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        refreshCamera()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        camera!!.setPreviewDisplay(holder)
        camera!!.startPreview()
    }

    fun switchCamera() {
        val cameras = Camera.getNumberOfCameras()
        if (cameras > 1) {
            releaseCamera()
            chooseCamera()
        }

    }

    fun takePhoto() {
        camera!!.takePicture(null, null, pictureCallback)
    }

    private fun chooseCamera() {
        //if the camera preview is the front
        if (cameraFront) {
            val cameraId = findBackFacingCamera()
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview

                camera = Camera.open(cameraId)
                camera!!.setDisplayOrientation(90)
                refreshCamera()
            }
        } else {
            val cameraId = findFrontFacingCamera()
            if (cameraId >= 0) {
                //open the backFacingCamera
                //set a picture callback
                //refresh the preview
                camera = Camera.open(cameraId)
                camera!!.setDisplayOrientation(90)
                refreshCamera()
            }
        }
    }

    fun pauseCamera() {
        releaseCamera()
    }

    fun resumeCamera() {
        if (camera == null) {
            camera = Camera.open()
            camera!!.setDisplayOrientation(90)
            refreshCamera()
        }
    }

    private fun releaseCamera() {
        // stop and release camera
        if (camera != null) {
            camera?.stopPreview()
            camera?.setPreviewCallback(null)
            camera?.release()
            camera = null
        }
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val ASPECT_TOLERANCE = 0.1
        val targetRatio = h.toDouble() / w

        if (sizes == null) return null

        var optimalSize: Camera.Size? = null
        var minDiff = java.lang.Double.MAX_VALUE

        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = Math.abs(size.height - h).toDouble()
            }
        }

        if (optimalSize == null) {
            minDiff = java.lang.Double.MAX_VALUE
            for (size in sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = Math.abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)
        val height = View.resolveSize(suggestedMinimumWidth, heightMeasureSpec)
        setMeasuredDimension(width, height)
        val supportedPreviewSizes = camera!!.parameters.supportedPreviewSizes
        if (supportedPreviewSizes != null)
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height)

    }
}
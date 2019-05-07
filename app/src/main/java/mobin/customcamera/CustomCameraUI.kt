package mobin.customcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_custom_camera_ui.*
import mobin.customcamera.core.Camera2
import mobin.ui.R

class CustomCameraUI : AppCompatActivity() {

    // private lateinit var cameraPreview: CameraPreview
    private lateinit var camera2: Camera2
//    @Inject
//    lateinit var mPresenter: CustomCameraPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_camera_ui)
        init()
    }

    private fun init() {

        // initCameraApi()
        //  activityComponent.inject(this)


        //  textView_skip.visibility = View.GONE

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            initCamera2Api()
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 3)
            else initCamera2Api()

        }

    }

    private fun initCamera2Api() {

        camera2 = Camera2(this,camera_view)

        iv_rotate_camera.setOnClickListener {
            camera2.switchCamera()
        }

        iv_capture_image.setOnClickListener { v ->
            v.isEnabled = false
            camera2.takePhoto {
                Toast.makeText(v.context, "Picture Taken !!", Toast.LENGTH_SHORT).show()
                v.isEnabled = true

            }


        }

        iv_camera_flash_on.setOnClickListener {
            camera2.setFlash(Camera2.FLASH.ON)
            it.alpha = 1f
            iv_camera_flash_auto.alpha = 0.4f
            iv_camera_flash_off.alpha = 0.4f
        }


        iv_camera_flash_auto.setOnClickListener {
            iv_camera_flash_off.alpha = 0.4f
            iv_camera_flash_on.alpha = 0.4f
            it.alpha = 1f
            camera2.setFlash(Camera2.FLASH.AUTO)
        }

        iv_camera_flash_off.setOnClickListener {
            camera2.setFlash(Camera2.FLASH.OFF)
            it.alpha = 1f
            iv_camera_flash_on.alpha = 0.4f
            iv_camera_flash_auto.alpha = 0.4f

        }

//        iv_gallery.setOnClickListener {
//            pickFromGallery()
//        }

    }


    override fun onPause() {
        //  cameraPreview.pauseCamera()
        camera2.close()
        super.onPause()
    }

    override fun onResume() {
        // cameraPreview.resumeCamera()
        camera2.onResume()
        super.onResume()
    }


}

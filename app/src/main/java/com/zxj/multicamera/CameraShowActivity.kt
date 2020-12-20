package com.zxj.multicamera

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera_show.*
import kotlinx.android.synthetic.main.camera_item.view.*
import java.util.concurrent.Executors

class CameraShowActivity : AppCompatActivity() {
    val handler = Handler(Looper.getMainLooper())
    private val cameraManager: CameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as CameraManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_show)
        val cameras = intent.getStringArrayExtra("cameras")
        cameras?.forEachIndexed { _, s ->
            s?.let {
                if (it.startsWith("logical id: ")) {
                    Log.e("zxj", "open: $s")
                    val cameraId = it.replace("logical id: ", "")
                    val inflate = LayoutInflater.from(this).inflate(R.layout.camera_item, null, false)
                    container.addView(inflate)
                    inflate.desc_tv.text = it
                    inflate.tv.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                            val get = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                            get?.let {
                                val previewSizes = it.getOutputSizes(SurfaceTexture::class.java).filter {
                                    it.width / it.height == 4 / 3
                                }
                                with(previewSizes[previewSizes.size / 2]) {
                                    p0.setDefaultBufferSize(width, height)
                                }
                                openLogicalCamera(cameraId, listOf(Surface(p0)))
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
                        }

                        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                            return true
                        }

                        override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                        }

                    }

                } else {

                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openLogicalCamera(id: String, surfaces: List<Surface>) {

        cameraManager.openCamera(id, Executors.newSingleThreadExecutor(),
                object : CameraDevice.StateCallback() {
                    override fun onOpened(p0: CameraDevice) {
                        val request = p0.createCaptureRequest(TEMPLATE_PREVIEW).apply {
                            for (surface in surfaces) {
                                addTarget(surface)
                            }
                        }.build()
                        p0.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                p0.setRepeatingRequest(request, object : CameraCaptureSession.CaptureCallback() {

                                }, handler)
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {
                            }

                        }, handler)

                    }

                    override fun onDisconnected(p0: CameraDevice) {

                    }

                    override fun onError(p0: CameraDevice, p1: Int) {
                    }

                })
    }
}
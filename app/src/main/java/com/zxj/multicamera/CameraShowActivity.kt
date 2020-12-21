package com.zxj.multicamera

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW
import android.hardware.camera2.CameraDevice.TEMPLATE_STILL_CAPTURE
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.hardware.camera2.params.SessionConfiguration.SESSION_REGULAR
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_camera_show.*
import kotlinx.android.synthetic.main.camera_item.view.*
import java.util.concurrent.Executors

class CameraShowActivity : AppCompatActivity() {
    val handler = Handler(Looper.getMainLooper())
    var captureSessions: MutableMap<CameraCaptureSession, Pair<CameraDevice, Pair<String, List<String>?>>> = mutableMapOf()
    private val cameraManager: CameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as CameraManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_show)
        val cameras = intent.getBundleExtra("cameras")!!

        for (s in cameras.keySet()) {
            val stringArrayList = cameras.getStringArrayList(s)
            if (stringArrayList.isNullOrEmpty()) {
                val inflate = LayoutInflater.from(this).inflate(R.layout.camera_item, null, false)
                container.addView(inflate)
                inflate.desc_tv.text = s
                handler.postDelayed(Runnable {
                    openLogicalCamera(s, listOf(inflate.sv.holder.surface))
                }, 1000)

            } else {
                val surfaces = stringArrayList.map {
                    val inflate = LayoutInflater.from(this).inflate(R.layout.camera_item, null, false)
                    container.addView(inflate)
                    inflate.desc_tv.text = it
                    inflate.sv.holder.surface
                }
                handler.postDelayed(Runnable {
                    openPhysicalCamera(s, stringArrayList, surfaces)
                }, 1000)
            }
        }
        take.setOnClickListener {
            captureSessions.forEach { t, u ->
                val cameraId = u.second.first
                val get = cameraManager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                val outputSizes = get.getOutputSizes(ImageFormat.JPEG).first()
                val imageReader = ImageReader.newInstance(outputSizes.width, outputSizes.height, ImageFormat.JPEG, 1).apply {
                    setOnImageAvailableListener({
                        Log.e("zxj", "image: ${cameraId}")
                    }, handler)
                }
                val build = u.first.createCaptureRequest(TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(imageReader.surface)
                }.build()
                t.setSingleRepeatingRequest(build, Executors.newSingleThreadExecutor(), object : CameraCaptureSession.CaptureCallback() {})
                if (!u.second.second.isNullOrEmpty()) {
                    u.second.second!!.forEach {
                        val cameraId1 = it
                        val get1 = cameraManager.getCameraCharacteristics(cameraId1).get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
                        val outputSizes1 = get1.getOutputSizes(ImageFormat.JPEG).first()
                        val imageReader1 = ImageReader.newInstance(outputSizes1.width, outputSizes1.height, ImageFormat.JPEG, 1).apply {
                            setOnImageAvailableListener({
                                Log.e("zxj", "image: ${cameraId}")
                            }, handler)
                        }
                        val build1 = u.first.createCaptureRequest(TEMPLATE_STILL_CAPTURE).apply {
                            addTarget(imageReader1.surface)
                        }.build()
                        t.setSingleRepeatingRequest(build1, Executors.newSingleThreadExecutor(), object : CameraCaptureSession.CaptureCallback() {})
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun openLogicalCamera(id: String, surfaces: List<Surface>) {

        cameraManager.openCamera(id, Executors.newSingleThreadExecutor(),
                object : CameraDevice.StateCallback() {
                    override fun onOpened(p0: CameraDevice) {
                        val cameraDevice = p0
                        val request = p0.createCaptureRequest(TEMPLATE_PREVIEW).apply {
                            for (surface in surfaces) {
                                addTarget(surface)
                            }
                        }.build()
                        p0.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                p0.setRepeatingRequest(request, object : CameraCaptureSession.CaptureCallback() {

                                }, handler)
                                captureSessions[p0] = Pair(cameraDevice, Pair(id, null))
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

    @SuppressLint("MissingPermission")
    fun openPhysicalCamera(logicalId: String, physicalIds: List<String>, surfaces: List<Surface>) {
        cameraManager.openCamera(logicalId, Executors.newSingleThreadExecutor(),
                object : CameraDevice.StateCallback() {
                    override fun onOpened(p0: CameraDevice) {
                        val cameraDevice = p0
                        val request = p0.createCaptureRequest(TEMPLATE_PREVIEW, physicalIds.toSet()).apply {
                            surfaces.forEach { addTarget(it) }
                        }.build()
                        val takeCaptureRequest = p0.createCaptureRequest(TEMPLATE_STILL_CAPTURE, physicalIds.toSet()).apply {
                            surfaces.forEach { addTarget(it) }
                        }.build()
                        val outputConfigurations = physicalIds.mapIndexed { index, s ->
                            OutputConfiguration(surfaces[index]).apply {
                                setPhysicalCameraId(s)
                            }
                        }
                        val sessionConfig = SessionConfiguration(SESSION_REGULAR, outputConfigurations, Executors.newSingleThreadExecutor(), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(p0: CameraCaptureSession) {
                                p0.setRepeatingRequest(request, object : CameraCaptureSession.CaptureCallback() {

                                }, handler)
                                captureSessions[p0] = Pair(cameraDevice, Pair(logicalId, physicalIds))
                            }

                            override fun onConfigureFailed(p0: CameraCaptureSession) {

                            }
                        })
                        p0.createCaptureSession(sessionConfig)
                    }

                    override fun onDisconnected(p0: CameraDevice) {
                    }

                    override fun onError(p0: CameraDevice, p1: Int) {
                    }

                })
    }
}
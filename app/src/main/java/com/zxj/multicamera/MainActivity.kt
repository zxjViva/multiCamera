package com.zxj.multicamera

import android.content.Intent
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.os.bundleOf
import com.zxj.camerainfoselector.CameraInfoSelector
import com.zxj.camerainfoselector.DuoCamera
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.logical_item.view.*
import kotlinx.android.synthetic.main.physics_item.view.*

class MainActivity : AppCompatActivity() {
    private val checkedMap: HashMap<CheckBox, DuoCamera> = hashMapOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 2)
        open.setOnClickListener {
            val result = checkedMap.map {
                if (it.key.isChecked) {
                    return@map it.value
                } else {
                    return@map null
                }
            }.filterNotNull()
            val map: Bundle = bundleOf()
            for (duoCamera in result) {
                if (map.containsKey(duoCamera.logicalCameraId) && !duoCamera.physicalCameraIds.isNullOrEmpty()) {
                    map.getStringArrayList(duoCamera.logicalCameraId)?.addAll(duoCamera.physicalCameraIds)
                } else {
                    map.putStringArrayList(duoCamera.logicalCameraId, arrayListOf<String>())
                    if (!duoCamera.physicalCameraIds.isNullOrEmpty()) {
                        map.getStringArrayList(duoCamera.logicalCameraId)?.addAll(duoCamera.physicalCameraIds)
                    }
                }
            }
            startActivity(Intent(this, CameraShowActivity::class.java).apply {
                putExtra("cameras", map)
            })
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == 0) {
            initCamera()
        }
    }

    private fun initCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val info = CameraInfoSelector(cameraManager).select()
        info.forEach {
            val logicalItem = View.inflate(this, R.layout.logical_item, null) as LinearLayout
            logicalItem.logical_name.text = "logical id: ${it.cameraId} focal:${it.focalLengths} size:${it.size} facing:${it.lenFacing}"
            checkedMap[logicalItem.logical_name] = DuoCamera(it.cameraId, null)
            container.addView(logicalItem)
            for (physicalCamera in it.physicalCameras) {
                val physicsItem = View.inflate(this, R.layout.physics_item, null)
                physicsItem.physics_name.text = "physics id: ${physicalCamera.cameraId} focal:${physicalCamera.focalLengths} size:${physicalCamera.size}"
                checkedMap[physicsItem.physics_name] = DuoCamera(it.cameraId, setOf(physicalCamera.cameraId))
                logicalItem.addView(physicsItem)
            }
        }
    }
}
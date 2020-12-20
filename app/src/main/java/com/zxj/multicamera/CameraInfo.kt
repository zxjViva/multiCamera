package com.zxj.camerainfoselector

import android.util.SizeF

class CameraInfo(val cameraId: String) {
    var physicalCameras: MutableList<PhysicalCamera> = mutableListOf()
    var focalLengths = ""
    var size: SizeF? = SizeF(0f, 0f)
    var lenFacing: String = "back"
}

data class PhysicalCamera(val focalLengths: String, val cameraId: String, val size: SizeF?)

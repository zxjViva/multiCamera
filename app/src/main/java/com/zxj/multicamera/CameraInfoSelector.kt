package com.zxj.camerainfoselector

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

class CameraInfoSelector(private val cameraManager: CameraManager) {
    fun select(): List<LogicalCameraInfo> {
        val cameraInfos = mutableListOf<LogicalCameraInfo>()
        for (id in cameraManager.cameraIdList) {
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(id)
            val lenFacing = cameraCharacteristics.get(
                    CameraCharacteristics.LENS_FACING
            )
            val focalLengths = cameraCharacteristics.get(
                    CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
            ) ?: floatArrayOf(0F)
            val sensorSize = cameraCharacteristics.get(
                    CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
            )
            val cameraInfo = LogicalCameraInfo(id)
            cameraInfos.add(cameraInfo)
            cameraInfo.size = sensorSize
            cameraInfo.focalLengths = focalLengths.joinToString()
            cameraInfo.lenFacing =
                    if (lenFacing == CameraCharacteristics.LENS_FACING_BACK) "back" else "font"
            cameraCharacteristics.physicalCameraIds.forEach {
                val cameraCharacteristics1 = cameraManager.getCameraCharacteristics(it)
                val focalLengths1 = cameraCharacteristics1.get(
                        CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS
                ) ?: floatArrayOf(0F)
                val sensorSize = cameraCharacteristics1.get(
                        CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE
                )
                cameraInfo.physicalCameras.add(
                        PhysicalCamera(
                                focalLengths1.joinToString(),
                                it,
                                sensorSize
                        )
                )
            }
        }
        return cameraInfos
    }
}
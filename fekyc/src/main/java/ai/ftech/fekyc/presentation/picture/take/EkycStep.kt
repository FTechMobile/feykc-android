package ai.ftech.fekyc.presentation.picture.take

import ai.ftech.fekyc.domain.model.ekyc.PHOTO_INFORMATION
import ai.ftech.fekyc.domain.model.ekyc.PHOTO_TYPE
import ai.ftech.fekyc.domain.model.ekyc.PhotoInfo

object EkycStep {
    private const val PASSPORT_STEP_COUNT = 2
    private const val PAPERS_STEP_COUNT = 3
    private var stepList = mutableListOf<PhotoInfo>()
    private var photoType: PHOTO_TYPE = PHOTO_TYPE.SSN

    fun setType(type: PHOTO_TYPE) {
        photoType = type
    }

    fun getType() = photoType

    fun isDoneStep(): Boolean {
        return stepList.size == if (isTypePassport()) PASSPORT_STEP_COUNT else PAPERS_STEP_COUNT
    }

    fun getCurrentStep(): PHOTO_INFORMATION {
        return if (isTypePassport()) {
            when (stepList.size) {
                0 -> PHOTO_INFORMATION.PAGE_NUMBER_2
                1 -> PHOTO_INFORMATION.FACE
                else -> PHOTO_INFORMATION.PAGE_NUMBER_2
            }
        } else {
            when (stepList.size) {
                0 -> PHOTO_INFORMATION.FRONT
                1 -> PHOTO_INFORMATION.BACK
                2 -> PHOTO_INFORMATION.FACE
                else -> PHOTO_INFORMATION.FRONT
            }
//            when (stepList.size) {
//                0 -> PHOTO_INFORMATION.FACE
//                1 -> PHOTO_INFORMATION.BACK
//                2 -> PHOTO_INFORMATION.FRONT
//                else -> PHOTO_INFORMATION.FRONT
//            }
        }
    }

    fun add(photoType: PHOTO_TYPE, path: String, retakePhotoType: PHOTO_INFORMATION?) {
        val photoInfo = PhotoInfo().apply {
            this.photoType = photoType
            this.url = path
            this.photoInformation = retakePhotoType ?: getCurrentStep()
        }
        if (retakePhotoType == null)
            stepList.add(photoInfo)
        else {
            when (retakePhotoType) {
                PHOTO_INFORMATION.FRONT -> stepList[0] = photoInfo
                PHOTO_INFORMATION.BACK -> stepList[1] = photoInfo
                PHOTO_INFORMATION.FACE ->stepList[2] = photoInfo
                else -> stepList[0] = photoInfo
            }
        }
    }

    fun getPaperList(): MutableList<PhotoInfo> {
        if (getType() == PHOTO_TYPE.SSN || getType() == PHOTO_TYPE.DRIVER_LICENSE) {
            if (stepList.size > 2) {
                val paperFront = stepList[0]
                val paperBack = stepList[1]
                return mutableListOf(paperFront, paperBack)
            }
        } else if (getType() == PHOTO_TYPE.PASSPORT) {
            val passportItem = stepList.firstOrNull()
            if (passportItem != null) {
                return mutableListOf(passportItem)
            }
        }
        return mutableListOf()
    }

    fun getPortraitItem(): PhotoInfo {
        return stepList.last()
    }

    fun clear() {
        stepList.clear()
    }

    private fun isTypePassport(): Boolean {
        return photoType == PHOTO_TYPE.PASSPORT
    }
}

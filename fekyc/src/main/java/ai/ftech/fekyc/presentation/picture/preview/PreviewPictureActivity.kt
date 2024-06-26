package ai.ftech.fekyc.presentation.picture.preview

import ai.ftech.fekyc.base.common.StatusBar
import ai.ftech.fekyc.base.extension.setOnSafeClick
import ai.ftech.fekyc.R
import ai.ftech.fekyc.common.FEkycActivity
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.imageloader.ImageLoaderFactory
import ai.ftech.fekyc.common.widget.toolbar.ToolbarView
import ai.ftech.fekyc.domain.event.FinishActivityEvent
import ai.ftech.fekyc.domain.model.ekyc.PHOTO_INFORMATION
import ai.ftech.fekyc.presentation.dialog.WARNING_TYPE
import ai.ftech.fekyc.presentation.dialog.WarningCaptureDialog
import ai.ftech.fekyc.presentation.picture.take.EkycStep
import ai.ftech.fekyc.presentation.picture.take.TakePictureActivity
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.utils.ShareFlowEventBus
import android.annotation.SuppressLint
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class PreviewPictureActivity : FEkycActivity(R.layout.fekyc_preview_picture_activity) {
    companion object {
        const val SEND_PREVIEW_IMAGE_KEY = "SEND_PREVIEW_IMAGE_KEY"
        const val SEND_MESSAGE_KEY = "SEND_MESSAGE_KEY"
        const val RETAKE_PHOTO_TYPE = "retakePhotoType"
    }

    private lateinit var tbvHeader: ToolbarView
    private lateinit var ivImageSrc: ImageView
    private lateinit var tvMessage: TextView
    private lateinit var tvPreviewPictureTransId: TextView
    private lateinit var btnTakeAgain: Button

    private val viewModel by viewModels<PreviewPictureViewModel>()
    private val imageLoader = ImageLoaderFactory.glide()

    override fun setupStatusBar(): StatusBar {
        return StatusBar(color = R.color.fbase_color_black, isDarkText = false)
    }

    override fun onResume() {
        super.onResume()
        if (warningDialog == null) {
            val type = getWarningType()
            warningDialog = WarningCaptureDialog(type)
        }
    }

    override fun onPause() {
        super.onPause()
        warningDialog = null
    }

    override fun onPrepareInitView() {
        super.onPrepareInitView()
        viewModel.imagePreviewPath = intent.getStringExtra(SEND_PREVIEW_IMAGE_KEY)
        viewModel.message = intent.getStringExtra(SEND_MESSAGE_KEY)
    }

    @SuppressLint("SetTextI18n")
    override fun onInitView() {
        super.onInitView()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tbvHeader = findViewById(R.id.tbvPreviewPictureHeader)
        tvMessage = findViewById(R.id.tvPreviewPictureErrorMessage)
        ivImageSrc = findViewById(R.id.ivPreviewPictureImageSrc)
        btnTakeAgain = findViewById(R.id.btnPreviewPictureTakeAgain)
        tvPreviewPictureTransId = findViewById(R.id.tvPreviewPictureTransId)

        val indexRetake = intent.getIntExtra(RETAKE_PHOTO_TYPE, -1)
        Log.e("indexRetake", indexRetake.toString())
        viewModel.retakePhotoType = when (indexRetake) {
            0 -> PHOTO_INFORMATION.FRONT
            1 -> PHOTO_INFORMATION.BACK
            2 -> PHOTO_INFORMATION.FACE
            else -> null
        }

        tbvHeader.setTitle(getToolbarTitleByEkycType())
        tvMessage.text = viewModel.message

        tvPreviewPictureTransId.text = "Trans ID: ${FTechEkycManager.getTransactionID()}"

        tbvHeader.setListener(object : ToolbarView.IListener {
            override fun onLeftIconClick() {
                showConfirmDialog {
                    lifecycleScope.launch {
                        val event = FinishActivityEvent()
                        ShareFlowEventBus.emitEvent(event)
                    }
                }
            }

            override fun onRightIconClick() {
                warningDialog?.showDialog(
                    supportFragmentManager,
                    warningDialog!!::class.java.simpleName
                )
            }
        })

        imageLoader.loadImage(
            activity = this,
            url = viewModel.imagePreviewPath,
            view = ivImageSrc,
            ignoreCache = true
        )

        btnTakeAgain.setOnSafeClick {
            finish()
        }
    }

    private fun getWarningType(): WARNING_TYPE {
        return when (EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FRONT,
            PHOTO_INFORMATION.BACK,
            PHOTO_INFORMATION.PAGE_NUMBER_2 -> WARNING_TYPE.PAPERS
            PHOTO_INFORMATION.FACE -> WARNING_TYPE.PORTRAIT
        }
    }

    private fun getToolbarTitleByEkycType(): String {
        return when (viewModel.retakePhotoType ?: EkycStep.getCurrentStep()) {
            PHOTO_INFORMATION.FRONT -> getAppString(R.string.fekyc_take_picture_take_front)
            PHOTO_INFORMATION.BACK -> getAppString(R.string.fekyc_take_picture_take_back)
            PHOTO_INFORMATION.FACE -> getAppString(R.string.fekyc_take_picture_image_portrait)
            PHOTO_INFORMATION.PAGE_NUMBER_2 -> getAppString(R.string.fekyc_take_picture_take_passport)
        }
    }

}

package ai.ftech.fekyc.presentation.info

import ai.ftech.fekyc.base.extension.observer
import ai.ftech.fekyc.base.extension.setOnSafeClick
import ai.ftech.fekyc.R
import ai.ftech.fekyc.common.FEkycActivity
import ai.ftech.fekyc.common.action.FEkycActionResult
import ai.ftech.fekyc.common.getAppString
import ai.ftech.fekyc.common.widget.bottomsheetpickerdialog.BottomSheetPickerDialog
import ai.ftech.fekyc.common.widget.datepicker.DatePickerDialog
import ai.ftech.fekyc.common.widget.toolbar.ToolbarView
import ai.ftech.fekyc.domain.APIException
import ai.ftech.fekyc.domain.event.EkycEvent
import ai.ftech.fekyc.domain.model.ekyc.EkycFormInfo
import ai.ftech.fekyc.domain.model.ekyc.EkycInfo
import ai.ftech.fekyc.presentation.dialog.ConfirmDialog
import ai.ftech.fekyc.presentation.model.BottomSheetPicker
import ai.ftech.fekyc.publish.FTechEkycManager
import ai.ftech.fekyc.utils.KeyboardUtility
import ai.ftech.fekyc.utils.ShareFlowEventBus
import ai.ftech.fekyc.utils.TimeUtils
import android.annotation.SuppressLint
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.util.*

class EkycInfoActivity : FEkycActivity(R.layout.fekyc_ekyc_info_activity) {
    companion object {
        const val SESSION_ID = "SESSION_ID"
        const val EKYC_INFO = "EKYC_INFO"
    }

    /**
     * view
     */
    private lateinit var constRoot: ConstraintLayout
    private lateinit var tbvHeader: ToolbarView
    private lateinit var tvTypePapres: TextView
    private lateinit var rvUserInfo: RecyclerView
    private lateinit var btnCompleted: Button
    private lateinit var tvEkycInfoTransId: AppCompatTextView

    /**
     * data
     */
    private val viewModel by viewModels<EkycInfoViewModel>()

    private val adapter = FormInfoAdapter().apply {
        listener = object : FormInfoAdapter.IListener {
            override fun onClickItem(item: EkycFormInfo, edt: EditText) {
                if (item.fieldType !== null) {
                    showBottomSheetDialog(item, edt)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Suppress("UNCHECKED_CAST")
    override fun onInitView() {
        super.onInitView()
        viewModel.currentSessionId = intent.getStringExtra(SESSION_ID)
        viewModel.ekycInfo = intent.getSerializableExtra(EKYC_INFO) as EkycInfo
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        constRoot = findViewById(R.id.constEkycInfoRoot)
        tbvHeader = findViewById(R.id.tbvEkycInfoHeader)
        tvTypePapres = findViewById(R.id.tvEkycInfoEkycTypeContent)
        rvUserInfo = findViewById(R.id.rvEkycInfoList)
        btnCompleted = findViewById(R.id.btnEkycInfoCompleted)
        tvEkycInfoTransId = findViewById(R.id.tvEkycInfoTransId)

        tvEkycInfoTransId.text = "Trans ID: ${FTechEkycManager.getTransactionID()}"
        tbvHeader.setListener(object : ToolbarView.IListener {
            override fun onLeftIconClick() {
                onBackPressed()
            }
        })

        btnCompleted.setOnSafeClick {
            showLoading()
            val dataInfo = (adapter.dataList as List<FormInfoAdapter.FormInfoDisplay>).map { it.data }
            viewModel.submitInfo(dataInfo)
        }

        rvUserInfo.layoutManager = LinearLayoutManager(this)
        rvUserInfo.adapter = adapter
        tvTypePapres.text = String.format("${viewModel.ekycInfo?.identityType}: ${viewModel.ekycInfo?.identityName}")
        adapter.resetData(viewModel.ekycInfo?.form ?: emptyList())
    }

    override fun onObserverViewModel() {
        super.onObserverViewModel()

        observer(viewModel.submitInfo) {

            when (it?.resultStatus) {

                FEkycActionResult.RESULT_STATUS.SUCCESS -> {
                    hideLoading()
                    if (it.data == true) {
                        lifecycleScope.launch {

                            val event = EkycEvent().apply {
                                this.code = 0
                                this.message = "Ekyc thành công!"
                            }

                            ShareFlowEventBus.emitEvent(event)
                        }
                        finish()
                    }
                }

                FEkycActionResult.RESULT_STATUS.ERROR -> {
                    hideLoading()
                    lifecycleScope.launchWhenStarted {
                        val apiException = it.exception as APIException
//                        if (apiException.code != APIException.EXPIRE_SESSION_ERROR) {
//                            val event = EkycEvent().apply {
//                                this.code = apiException.code
//                                this.message = apiException.message.toString()
//                            }
//                            ShareFlowEventBus.emitEvent(event)
//                            finish()
//                        }
                        showError(apiException.message) {}
                    }
                }

                else -> {
                    Log.e(TAG, getAppString(R.string.fekyc_unknown_error))
                }
            }
        }
    }

    private fun showBottomSheetDialog(ekycInfo: EkycFormInfo, editText: EditText) {
        when (ekycInfo.fieldType) {
            EkycFormInfo.FIELD_TYPE.DATE -> {
                showDatePickerDialog(ekycInfo)
            }
            EkycFormInfo.FIELD_TYPE.COUNTRY -> {
                showCityDialog(ekycInfo)
            }
            EkycFormInfo.FIELD_TYPE.NATIONAL -> {
                showNationDialog(ekycInfo)
            }
            EkycFormInfo.FIELD_TYPE.GENDER -> {
                showGenderDialog(ekycInfo)
            }
            EkycFormInfo.FIELD_TYPE.STRING, EkycFormInfo.FIELD_TYPE.NUMBER -> {
                editText.requestFocus()
                editText.setSelection(editText.text.length)
                KeyboardUtility.showKeyBoard(this, editText)
            }
            else -> {}
        }
    }

    private fun showDatePickerDialog(ekycInfo: EkycFormInfo) {
        ekycInfo.fieldValue = ""
        DatePickerDialog.Builder()
            .setTitle(getAppString(R.string.fekyc_ekyc_info_select_time))
            .setCurrentCalendar(
                try {
                    TimeUtils.getCalendarFromDateString(
                        if (ekycInfo.fieldValue.isNullOrEmpty()) TimeUtils.dateToDateString(
                            Calendar.getInstance(),
                            TimeUtils.ISO_SHORT_DATE_FOMAT
                        ) else ekycInfo.fieldValue,
                        TimeUtils.ISO_SHORT_DATE_FOMAT
                    )
                } catch (e: Exception) {
                    TimeUtils.getCalendarFromDateString(
                        if (ekycInfo.fieldValue.isNullOrEmpty()) TimeUtils.dateToDateString(
                            Calendar.getInstance(),
                            TimeUtils.ISO_SHORT_DATE_FOMAT
                        ) else ekycInfo.fieldValue,
                        TimeUtils.ISO_YEAR_FOMAT
                    )
                }
            )
            .setDateType(ekycInfo.dateType)
            .setDatePickerListener {
                val time = TimeUtils.dateToDateString(it, TimeUtils.ISO_SHORT_DATE_FOMAT)
                adapter.updateField(ekycInfo.id!!, time)
            }.show(supportFragmentManager)
    }

    private fun showCityDialog(ekycInfo: EkycFormInfo) {
        val list = viewModel.cityList.map { city ->
            BottomSheetPicker().apply {
                this.id = city.id.toString()
                this.title = city.name
                this.isSelected = (ekycInfo.fieldValue == city.name)
            }
        }

        BottomSheetPickerDialog.Builder()
            .setTitle(getAppString(R.string.fekyc_ekyc_info_select_issued_by))
            .setListPicker(list)
            .setChooseItemListener {
                adapter.updateField(ekycInfo.id!!, it.title.toString())
            }
            .show(supportFragmentManager)
    }

    private fun showNationDialog(ekycInfo: EkycFormInfo) {
        val list = viewModel.nationList.map { nation ->
            BottomSheetPicker().apply {
                this.id = nation.id.toString()
                this.title = nation.name
                this.isSelected = (ekycInfo.fieldValue == nation.name)
            }
        }

        BottomSheetPickerDialog.Builder()
            .setTitle(getAppString(R.string.fekyc_ekyc_info_select_issued_by))
            .setListPicker(list)
            .setChooseItemListener {
                adapter.updateField(ekycInfo.id!!, it.title.toString())
            }
            .show(supportFragmentManager)
    }

    private fun showGenderDialog(ekycInfo: EkycFormInfo) {
        val list = mutableListOf<BottomSheetPicker>()

        list.add(BottomSheetPicker().apply {
            this.id = ekycInfo.id.toString()
            this.title = getAppString(R.string.fekyc_ekyc_info_gender_male)
            this.isSelected =
                (ekycInfo.fieldValue == getAppString(R.string.fekyc_ekyc_info_gender_male))
        })

        list.add(BottomSheetPicker().apply {
            this.id = ekycInfo.id.toString()
            this.title = getAppString(R.string.fekyc_ekyc_info_gender_female)
            this.isSelected =
                (ekycInfo.fieldValue == getAppString(R.string.fekyc_ekyc_info_gender_female))
        })

        BottomSheetPickerDialog.Builder()
            .setTitle(getAppString(R.string.fekyc_ekyc_info_select_gender))
            .setListPicker(list)
            .hasSearch(false)
            .setChooseItemListener {
                adapter.updateField(ekycInfo.id!!, it.title.toString())
            }
            .show(supportFragmentManager)
    }

    override fun onResume() {
        super.onResume()
        FTechEkycManager.notifyActive(this)
    }

    override fun onPause() {
        super.onPause()
        FTechEkycManager.notifyInactive(this)
    }
}

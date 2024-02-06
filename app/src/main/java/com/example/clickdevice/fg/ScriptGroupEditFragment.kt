package com.example.clickdevice.fg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clickdevice.activity.ScriptGroupEditActivity
import com.example.clickdevice.adapter.ScriptGroupEditAdapter
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.databinding.DialogAddCmdBinding
import com.example.clickdevice.databinding.FgScriptGroupEditBinding
import com.example.clickdevice.dialog.CmdDialogHelper
import com.example.clickdevice.dialog.DialogHelper

class ScriptGroupEditFragment : Fragment() {


    private var  cmdDialogHelper: CmdDialogHelper?=null
    private var binding: FgScriptGroupEditBinding? = null

    private val mActivity: ScriptGroupEditActivity by lazy { activity as ScriptGroupEditActivity }

    private val mAdapter= ScriptGroupEditAdapter<ScriptCmdBean>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FgScriptGroupEditBinding.inflate(inflater, container, false)
        binding!!.initView()
        initEvent()
        cmdDialogHelper= CmdDialogHelper(requireActivity())
        return binding!!.root
    }


    private fun FgScriptGroupEditBinding.initView() {
        btnComplete.setOnClickListener {
            onComplete()
        }
        btnBack.setOnClickListener {
            onBack()
        }
        btnChild.setOnClickListener {
            mActivity.showScriptList()
        }
        btnInsertCmd.setOnClickListener {
            showInsertCmdDialog()
        }

        rvScriptEdit.adapter=mAdapter
        rvScriptEdit.layoutManager=LinearLayoutManager(requireContext())
        mAdapter.editCallBack={ index,item->
            showInsertCmdDialog(name = item.name, script = item.data)
        }
        mAdapter.deleteCallBack={ index,item->
            DialogHelper.DeleteDialogShow(requireContext(),"删除命令", "确定要删除命令吗？") {
                removeCmd(item.name)

            }
        }
    }


    private var tempScript=ScriptCmdBean.BuildNoneCMD()

    private fun showInsertCmdDialog(name:String="", script:ScriptCmdBean=ScriptCmdBean.BuildNoneCMD()){
        val inflate = DialogAddCmdBinding.inflate(layoutInflater, null, false)
        val showDialog = DialogHelper.showDialog(requireContext(), inflate.root)
        tempScript=script
        inflate.btnCancel.setOnClickListener {
            showDialog.dismiss()
        }
        inflate.editName.setText(name)
        inflate.tvCmMsg.setText(tempScript.actionTypeName)
        inflate.btnDetermine.setOnClickListener {
            val newName= inflate.editName.text.toString()
            putCmd(newName,tempScript)
            showDialog.dismiss()
        }
        inflate.tvCmdType.setOnClickListener {
            cmdDialogHelper?.showSelectCmdDialog(tempScript) {
                tempScript = it
                inflate.tvCmMsg.setText(tempScript.actionTypeName)
            }
        }
    }


    private fun removeCmd(name:String){
        mActivity.removeCmd(name)
    }
    private fun putCmd(name:String,script:ScriptCmdBean){
        mActivity.putCmd(name,script)
    }

    private fun initEvent(){
        mActivity.mLiveData.observe(this) {
            binding?.editName?.setText(it.name)
            val data=it.actionMap.entries.map { ScriptGroupEditAdapter.Item<ScriptCmdBean>(name = it.key, msg = it.value.content, data = it.value) }
            mAdapter.setData(data)
        }
    }

    private fun onComplete() {
        mActivity.onComplete(binding?.editName?.text.toString())
    }

    private fun onBack() {
        mActivity.finish()
    }


}
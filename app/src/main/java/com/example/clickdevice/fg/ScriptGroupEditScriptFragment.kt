package com.example.clickdevice.fg

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clickdevice.Util
import com.example.clickdevice.activity.ScriptGroupEditActivity
import com.example.clickdevice.adapter.ScriptGroupEditAdapter
import com.example.clickdevice.bean.ActionScript
import com.example.clickdevice.bean.ScriptCmdBean
import com.example.clickdevice.bean.toSimpleScriptGroup
import com.example.clickdevice.databinding.DialogListScriptGroupActionBinding
import com.example.clickdevice.databinding.FgScriptGroupEditScriptBinding
import com.example.clickdevice.dialog.DialogHelper
import com.google.gson.Gson

class ScriptGroupEditScriptFragment : Fragment() {

    private var binding: FgScriptGroupEditScriptBinding? = null
    private val mActivity: ScriptGroupEditActivity by lazy { requireActivity() as ScriptGroupEditActivity }
    private val mAdapter = ScriptGroupEditAdapter<String>()
    private var actionScript: ActionScript? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            FgScriptGroupEditScriptBinding.inflate(inflater, container, false)
        binding!!.initView()
        initEvent()
        return binding!!.root
    }


    private fun FgScriptGroupEditScriptBinding.initView() {
        btnBack.setOnClickListener {
            mActivity.goBack()
        }

        btnOutJson.setOnClickListener {
            actionScript?.apply {
                val json = Gson().toJson(actionScript)
                try {
                    Util.copyText(json, requireContext())
                    Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {

                }
            }
        }

        btnInputJson.setOnClickListener {
            DialogHelper.EditDialogShow(requireContext(), "导入数据", "") {
                val gson = Gson()
                try {
                    if (it.isEmpty()) {
                        Toast.makeText(requireContext(), "数据为空", Toast.LENGTH_LONG).show()
                        return@EditDialogShow
                    }
                    val fromJson = gson.fromJson(it, ActionScript::class.java)
                    if (fromJson != null) {
                        actionScript?.apply {
                            name = fromJson.name
                            script = fromJson.script
                            setScript(this)
                        }
                    }else{
                        Toast.makeText(requireContext(), "数据格式不正确", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "导入数据失败", Toast.LENGTH_LONG).show()
                }
            }
        }

        editName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                actionScript?.name = s.toString()
            }
        })
        rvScriptEdit.adapter = mAdapter
        rvScriptEdit.layoutManager = LinearLayoutManager(requireContext())
        mAdapter.editCallBack = { index, item ->
            showCmdDialog { key ->
                actionScript?.apply {
                    script.set(index, key)
                    setScript(this)
                }
            }
        }
        mAdapter.isShowPosition = true
        mAdapter.deleteCallBack = { index, item ->
            DialogHelper.DeleteDialogShow(requireContext(), "删除命令", "确定要删除该命令吗？") {
                actionScript?.apply {
                    script.removeAt(index)
                    setScript(this)
                }
            }
        }
        mAdapter.itemClickCallBack = { index, item ->
            Toast.makeText(requireContext(), "选择要的插入命令", Toast.LENGTH_SHORT).show()
            showCmdDialog { key ->
                actionScript?.apply {
                    script.add(index, key)
                    setScript(this)
                }
            }
        }
        btnInsertCmd.setOnClickListener {
            showCmdDialog { key ->
                actionScript?.apply {
                    script.add(key)
                    setScript(this)
                }
            }
        }
    }


    private fun showCmdDialog(call: (String) -> Unit) {
        val inflate = DialogListScriptGroupActionBinding.inflate(layoutInflater)
        val showDialog = DialogHelper.showDialog(requireContext(), inflate.root)
        inflate.tvTitle.text = "选择命令"
        val scriptGroupEditAdapter = ScriptGroupEditAdapter<String>()
        inflate.recyclerView.adapter = scriptGroupEditAdapter
        inflate.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        inflate.btnCancel.setOnClickListener {
            showDialog.dismiss()
        }

        val cmdData = getCmds().map {
            ScriptGroupEditAdapter.Item<String>(
                name = it.key,
                msg = it.value.actionTypeName,
                data = it.key
            )
        }
        scriptGroupEditAdapter.setData(cmdData)
        scriptGroupEditAdapter.btnVisibility = false
        scriptGroupEditAdapter.itemClickCallBack = { index, item ->
            call(item.name)
            showDialog.dismiss()
        }
    }


    private fun initEvent() {

    }

    private fun getCmds(): Set<Map.Entry<String, ScriptCmdBean>> {
        return mActivity.getCmds().entries
    }

    fun setScript(script: ActionScript) {
        this.actionScript = script
        binding?.editName?.setText(script.name)
        val data = script.script.map {
            val msg = mActivity.getActionScriptMsg(it)
            ScriptGroupEditAdapter.Item<String>(name = it, msg = msg, data = it)
        }
        mAdapter.setData(data)

    }

}
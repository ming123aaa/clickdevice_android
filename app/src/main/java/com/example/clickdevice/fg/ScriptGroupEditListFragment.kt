package com.example.clickdevice.fg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.clickdevice.activity.ScriptGroupEditActivity
import com.example.clickdevice.adapter.ScriptGroupEditAdapter
import com.example.clickdevice.bean.ActionScript
import com.example.clickdevice.databinding.FgScriptGroupEditBinding
import com.example.clickdevice.databinding.FgScriptGroupEditListBinding
import com.example.clickdevice.dialog.DialogHelper
import java.util.Arrays

class ScriptGroupEditListFragment : Fragment() {
    private var binding: FgScriptGroupEditListBinding? = null
    private val mActivity: ScriptGroupEditActivity by lazy { activity as ScriptGroupEditActivity }
    private val mAdapter = ScriptGroupEditAdapter<ActionScript>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FgScriptGroupEditListBinding.inflate(inflater, container, false)
        binding!!.initView()
        initEvent()
        return binding!!.root
    }

    private fun FgScriptGroupEditListBinding.initView() {
        btnBack.setOnClickListener {
            mActivity.goBack()
        }
        btnAdd.setOnClickListener {
            mActivity.newScript()
        }

        rvScriptEdit.adapter = mAdapter
        rvScriptEdit.layoutManager = LinearLayoutManager(requireContext())

        mAdapter.editCallBack = { index, item ->
            mActivity.editScript(item.data)
        }

        mAdapter.deleteCallBack = { index, item ->
            DialogHelper.DeleteDialogShow(requireContext(), "删除脚本", "确定要删除脚本吗？") {
                mActivity.deleteScript(item.data)
            }

        }
    }


    private fun initEvent() {
        mActivity.mLiveData.observe(this) {
            val actionScript = it.actionScript
            val data = actionScript.map {
                ScriptGroupEditAdapter.Item<ActionScript>(
                    name = it.name,
                    msg = it.script.toString(),
                    data = it
                )
            }
            mAdapter.setData(data)
        }

    }
}
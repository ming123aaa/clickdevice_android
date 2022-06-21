package com.example.clickdevice.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clickdevice.R;
import com.example.clickdevice.bean.RecordScriptCmd;
import com.example.clickdevice.databinding.ItemRecordCmdBinding;
import com.example.clickdevice.db.RecordScriptBean;

import java.util.List;

public class RecordCMDAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<RecordScriptCmd> mData;
    private Context context;

    public RecordCMDAdapter(List<RecordScriptCmd> mData, Context context) {
        this.mData = mData;
        this.context = context;
    }


    public List<RecordScriptCmd> getmData() {
        return mData;
    }

    public void setmData(List<RecordScriptCmd> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecordCMDViewHolder(LayoutInflater.from(context).inflate(R.layout.item_record_cmd, null));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RecordCMDViewHolder) {
            ((RecordCMDViewHolder) holder).itemRecordCmdBinding.tvName.setText(getDescribe(position));
        }
    }

    private String getDescribe(int position) {
        RecordScriptCmd recordScriptCmd = mData.get(position);
        String s = position + ".  ";
        switch (recordScriptCmd.type) {
            case Gesture:
                s += "手势执行" + recordScriptCmd.duration + "ms";
                break;
            case Delay:
                s += "延时" + recordScriptCmd.delayed + "ms";
                break;

        }
        return s;
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    private static class RecordCMDViewHolder extends RecyclerView.ViewHolder {
        public ItemRecordCmdBinding itemRecordCmdBinding;

        public RecordCMDViewHolder(@NonNull View itemView) {
            super(itemView);
            itemRecordCmdBinding = ItemRecordCmdBinding.bind(itemView);
        }
    }
}

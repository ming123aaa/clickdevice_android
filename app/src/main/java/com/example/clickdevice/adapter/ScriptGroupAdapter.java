package com.example.clickdevice.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clickdevice.R;
import com.example.clickdevice.databinding.ItemScriptGroupBinding;
import com.example.clickdevice.db.ScriptGroupBean;

import java.util.List;

public class ScriptGroupAdapter extends RecyclerView.Adapter {

    private List<ScriptGroupBean> mData;
    private Context context;
    private ClickListener clickListener;

    public ClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public ScriptGroupAdapter(List<ScriptGroupBean> mData, Context context) {
        this.mData = mData;
        this.context = context;

    }

    public List<ScriptGroupBean> getmData() {
        return mData;
    }

    public void setmData(List<ScriptGroupBean> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecordScriptViewHolder(LayoutInflater.from(context).inflate(R.layout.item_script_group, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RecordScriptViewHolder) {
            ((RecordScriptViewHolder) holder).itemScriptBinding.setScriptData(mData.get(position));

            if (clickListener != null) {
                ((RecordScriptViewHolder) holder).itemScriptBinding.btnDelete.setOnClickListener(v -> {
                    clickListener.delete(mData.get(position));
                });
                ((RecordScriptViewHolder) holder).itemScriptBinding.btnEdit.setOnClickListener(v -> {
                    clickListener.edit(mData.get(position));
                });
                ((RecordScriptViewHolder) holder).itemScriptBinding.btnSelect.setOnClickListener(v -> {
                    clickListener.select(mData.get(position));
                });
                ((RecordScriptViewHolder) holder).itemScriptBinding.tvCreateDesktop.setOnClickListener(v -> {
                    clickListener.createDesktop(mData.get(position));
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    private static class RecordScriptViewHolder extends RecyclerView.ViewHolder {
        ItemScriptGroupBinding itemScriptBinding;

        public RecordScriptViewHolder(@NonNull View itemView) {
            super(itemView);
            itemScriptBinding = ItemScriptGroupBinding.bind(itemView);
        }
    }

    public interface ClickListener {
        void delete(ScriptGroupBean recordScriptBean);

        void edit(ScriptGroupBean recordScriptBean);

        void select(ScriptGroupBean recordScriptBean);

        void createDesktop(ScriptGroupBean recordScriptBean);
    }
}

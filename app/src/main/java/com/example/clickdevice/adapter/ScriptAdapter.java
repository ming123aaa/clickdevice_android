package com.example.clickdevice.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clickdevice.R;
import com.example.clickdevice.bean.ScriptCmdBean;
import com.example.clickdevice.databinding.ItemScriptBinding;
import com.example.clickdevice.db.ScriptDataBean;

import java.util.List;

public class ScriptAdapter extends RecyclerView.Adapter {
    private static final String TAG = "ScriptAdapter";
    private List<ScriptDataBean> mData;
    private Context context;
    private ClickListener clickListener;

    public ClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public ScriptAdapter(List<ScriptDataBean> mData, Context context) {
        this.mData = mData;
        this.context = context;

    }

    public List<ScriptDataBean> getmData() {
        return mData;
    }

    public void setmData(List<ScriptDataBean> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ScriptViewHolder(LayoutInflater.from(context).inflate(R.layout.item_script, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ScriptViewHolder) {
            ((ScriptViewHolder) holder).itemScriptBinding.setScriptData(mData.get(position));

            if (clickListener != null) {
                ((ScriptViewHolder) holder).itemScriptBinding.btnDelete.setOnClickListener(v -> {
                    clickListener.delete(mData.get(position));
                });
                ((ScriptViewHolder) holder).itemScriptBinding.btnEdit.setOnClickListener(v -> {
                    clickListener.edit(mData.get(position));
                });
                ((ScriptViewHolder) holder).itemScriptBinding.btnSelect.setOnClickListener(v -> {
                    clickListener.select(mData.get(position));
                });
                ((ScriptViewHolder) holder).itemScriptBinding.tvCreateDesktop.setOnClickListener(v -> {
                    clickListener.createDesktop(mData.get(position));
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    private class ScriptViewHolder extends RecyclerView.ViewHolder {
        ItemScriptBinding itemScriptBinding;

        public ScriptViewHolder(@NonNull View itemView) {
            super(itemView);
            itemScriptBinding = ItemScriptBinding.bind(itemView);
        }
    }

    public interface ClickListener {
        void delete(ScriptDataBean scriptDataBean);

        void edit(ScriptDataBean scriptDataBean);

        void select(ScriptDataBean scriptDataBean);

        void createDesktop(ScriptDataBean scriptDataBean);
    }
}

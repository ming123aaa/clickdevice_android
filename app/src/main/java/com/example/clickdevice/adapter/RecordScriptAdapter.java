package com.example.clickdevice.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clickdevice.databinding.ItemRecordScriptBinding;
import com.example.clickdevice.db.RecordScriptBean;

import java.util.List;
import com.example.clickdevice.R;

public class RecordScriptAdapter extends RecyclerView.Adapter {

    private List<RecordScriptBean> mData;
    private Context context;
    private ClickListener clickListener;

    public ClickListener getClickListener() {
        return clickListener;
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public RecordScriptAdapter(List<RecordScriptBean> mData, Context context) {
        this.mData = mData;
        this.context = context;

    }

    public List<RecordScriptBean> getmData() {
        return mData;
    }

    public void setmData(List<RecordScriptBean> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new RecordScriptViewHolder(LayoutInflater.from(context).inflate(R.layout.item_record_script, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof RecordScriptViewHolder) {
            ((RecordScriptViewHolder) holder).itemScriptBinding.setScriptData(mData.get(position));

            if (clickListener != null) {
                ((RecordScriptViewHolder) holder).itemScriptBinding.btnDelete.setOnClickListener(v -> {
                    clickListener.delete(mData.get(position));
                });
                ((RecordScriptViewHolder) holder).itemScriptBinding.btnEdit.setOnClickListener(v->{
                    clickListener.edit(mData.get(position));
                });
                ((RecordScriptViewHolder) holder).itemScriptBinding.btnSelect.setOnClickListener(v->{
                    clickListener.select(mData.get(position));
                });
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    private static class RecordScriptViewHolder extends RecyclerView.ViewHolder {
        ItemRecordScriptBinding itemScriptBinding;

        public RecordScriptViewHolder(@NonNull View itemView) {
            super(itemView);
            itemScriptBinding = ItemRecordScriptBinding.bind(itemView);
        }
    }

    public interface ClickListener {
        void delete(RecordScriptBean recordScriptBean);

        void edit(RecordScriptBean recordScriptBean);

        void select(RecordScriptBean recordScriptBean);
    }
}

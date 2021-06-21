package com.example.clickdevice.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.clickdevice.bean.ScriptCmdBean;
import com.example.clickdevice.R;

import java.util.List;

public class CMDAdapter extends RecyclerView.Adapter {
    private List<ScriptCmdBean> mData;
    private Context context;
    private CmdListener cmdListener;

    public List<ScriptCmdBean> getmData() {
        return mData;
    }

    public void setmData(List<ScriptCmdBean> mData) {
        this.mData = mData;
        notifyDataSetChanged();
    }

    public CmdListener getCmdListener() {
        return cmdListener;
    }

    public void setCmdListener(CmdListener cmdListener) {
        this.cmdListener = cmdListener;
    }

    public CMDAdapter(List<ScriptCmdBean> mData, Context context) {
        this.mData = mData;
        this.context = context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CMDViewHolder(LayoutInflater.from(context).inflate(R.layout.item_cmd, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof CMDViewHolder) {
            ((CMDViewHolder) holder).textView.setText(mData.get(position).getContent());
            if (cmdListener != null) {
                ((CMDViewHolder) holder).btn_insert.setOnClickListener(v -> {
                    cmdListener.insert(holder);
                });
                ((CMDViewHolder) holder).btn_edit.setOnClickListener(v -> {
                    cmdListener.edit(holder);
                });
            }

        }
        Log.e("TAG", "onBindViewHolder: "+position );
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public class CMDViewHolder extends RecyclerView.ViewHolder {
        public ImageView imageView;
        public TextView textView;
        public Button btn_insert, btn_edit;

        public CMDViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.img_icon);
            textView = itemView.findViewById(R.id.tv_content);
            btn_edit = itemView.findViewById(R.id.btn_edit);
            btn_insert = itemView.findViewById(R.id.btn_insert);
        }
    }

    public interface CmdListener {
        void insert(RecyclerView.ViewHolder holder);

        void edit(RecyclerView.ViewHolder holder);
    }
}

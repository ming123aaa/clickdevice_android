package com.example.clickdevice.adapter;

import android.graphics.Canvas;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class SimpleCallbackHelp {

    private static final String TAG = "SimpleCallbackHelp";

    public static <T> ItemTouchHelper.SimpleCallback get(final List<T> list, final RecyclerView.Adapter myAdapter) {
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP |
                ItemTouchHelper.DOWN | ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                ItemTouchHelper.START | ItemTouchHelper.END) {
            private boolean needChange;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();//当前ViewHolder的position
                int toPosition = target.getAdapterPosition();//目标ViewHolder的position

                //交换位置
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(list, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(list, i, i - 1);
                    }
                }

                myAdapter.notifyItemMoved(fromPosition, toPosition);
                needChange=true;
                return true;
            }

            @Override
            public void onSelectedChanged(@Nullable  RecyclerView.ViewHolder viewHolder, int actionState) {

//                if (actionState==ItemTouchHelper.ACTION_STATE_IDLE&&needChange){
//                    myAdapter.notifyDataSetChanged();
//                    needChange=false;
//                }
            }



            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                list.remove(position);//删除数据
                myAdapter.notifyItemRemoved(position);
                Log.e(TAG, "onSwiped: "+direction+" p"+position );

            }


            @Override
            public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    //滑动时改变Item的透明度
                    final float alpha = 1 - Math.abs(dX) / (float) viewHolder.itemView.getWidth();
                    viewHolder.itemView.setAlpha(alpha);
                    viewHolder.itemView.setTranslationX(dX);
                }
            }
        };

    }
}

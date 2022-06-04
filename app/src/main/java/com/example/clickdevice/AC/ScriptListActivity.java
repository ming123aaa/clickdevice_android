package com.example.clickdevice.AC;

import androidx.appcompat.app.AppCompatActivity;
import androidx.arch.core.util.Function;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.Transformations;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.Ohuang.ilivedata.LiveDataBus;
import com.Ohuang.ilivedata.MyLiveData;
import com.example.clickdevice.MyApp;
import com.example.clickdevice.R;
import com.example.clickdevice.adapter.ScriptAdapter;
import com.example.clickdevice.db.AppDatabase;
import com.example.clickdevice.db.ScriptDataBean;
import com.example.clickdevice.dialog.DialogHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ScriptListActivity extends AppCompatActivity {
    private static final String TAG = "ScriptListActivity";
    private RecyclerView recyclerView;
    private ScriptAdapter scriptAdapter;
    private List<ScriptDataBean> mData;
    private ExecutorService singleThreadExecutor;
    private MutableLiveData<String> liveData = new MutableLiveData<>();
    private LiveData<List<ScriptDataBean>> listLiveData = Transformations.switchMap(liveData, new Function<String, LiveData<List<ScriptDataBean>>>() {
        @Override
        public LiveData<List<ScriptDataBean>> apply(String input) {
            return appDatabase.getScriptDao().loadLiveDataOfAllScriptDataBean();
        }
    });
    private AppDatabase appDatabase;
    private LifecycleOwner lifecycleOwner;
    private Context context;
    private Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_script_list);
        lifecycleOwner = this;
        context = this;
        recyclerView = findViewById(R.id.rv_script_list);
        singleThreadExecutor = Executors.newSingleThreadExecutor();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        mData = new ArrayList<>();
        scriptAdapter = new ScriptAdapter(mData, context);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(scriptAdapter);

        appDatabase = ((MyApp) getApplication()).getAppDatabase();
        liveData.postValue("s");
        listLiveData.observe(this, new Observer<List<ScriptDataBean>>() {
            @Override
            public void onChanged(List<ScriptDataBean> scriptDataBeans) {
                mData = scriptDataBeans;
                scriptAdapter.setmData(mData);

            }
        });
        scriptAdapter.setClickListener(new ScriptAdapter.ClickListener() {
            @Override
            public void delete(ScriptDataBean scriptDataBean) {
             dialog  = DialogHelper.DeleteDialogShow(ScriptListActivity.this, "删除脚本", "你确定要删除" + scriptDataBean.getName() + "?",
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                singleThreadExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        appDatabase.getScriptDao().deleteScriptDataBean(scriptDataBean);
                                    }
                                });
                                dialog.dismiss();
                            }
                        });
            }

            @Override
            public void edit(ScriptDataBean scriptDataBean) {
                Intent intent=new Intent(ScriptListActivity.this,ScriptEditActivity.class);
                intent.putExtra("isNew", false);
                MyLiveData.getInstance().with("ScriptDataBean",ScriptDataBean.class).setValue(scriptDataBean);
                startActivity(intent);

            }

            @Override
            public void select(ScriptDataBean scriptDataBean) {
               LiveDataBus.get().with("json",String.class).setValue(scriptDataBean.getScriptJson());
               LiveDataBus.get().with("scriptName",String.class).setValue(scriptDataBean.getName());
               finish();
            }
        });
    }





    public void createNewScript(View view) {
        Intent intent = new Intent(this, ScriptEditActivity.class);
        intent.putExtra("isNew", true);
        startActivity(intent);

    }
}
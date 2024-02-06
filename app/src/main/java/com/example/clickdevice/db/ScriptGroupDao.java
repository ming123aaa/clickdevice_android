package com.example.clickdevice.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;
@Dao
public interface ScriptGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScriptGroupBean(ScriptGroupBean... scriptGroupBeans);


    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateScriptGroupBean(ScriptGroupBean... scriptGroupBeans);

    @Delete
    void deleteScriptGroupBean(ScriptGroupBean... scriptGroupBeans);

    @Query("SELECT * FROM script_group")
    List<ScriptGroupBean> loadAllScriptGroupBean();

    @Query("SELECT * FROM script_group")
    LiveData<List<ScriptGroupBean>> loadLiveDataOfAllScriptGroupBean();

    //分页查询 size为每次查询数据的个数  page为第几个数据开始
    @Query("SELECT * FROM script_group order by id limit :size offset :page")
    LiveData<List<ScriptGroupBean>> loadLiveDataOfScriptGroupBeanForPage(int size, int page);

    //分页查询 size为每次查询数据的个数  page为第几个数据开始
    @Query("SELECT * FROM script_group order by id limit :size offset :page")
    List<ScriptGroupBean> loadScriptGroupBeanForPage(int size, int page);


    @Query("SELECT * FROM script_group WHERE id=:iID")
    ScriptGroupBean findBeanById(int iID);
}

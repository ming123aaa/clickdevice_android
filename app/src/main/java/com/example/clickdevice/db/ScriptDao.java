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
public interface ScriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertScriptDataBean(ScriptDataBean... scriptDataBeans);


    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateScriptDataBean(ScriptDataBean... scriptDataBeans);

    @Delete
    void deleteScriptDataBean(ScriptDataBean... scriptDataBeans);

    @Query("SELECT * FROM script")
    List<ScriptDataBean> loadAllScriptDataBean();

    @Query("SELECT * FROM script")
    LiveData<List<ScriptDataBean>> loadLiveDataOfAllScriptDataBean();

    //分页查询 size为每次查询数据的个数  page为第几个数据开始
    @Query("SELECT * FROM script order by id limit :size offset :page")
    LiveData<List<ScriptDataBean>> loadLiveDataOfScriptDataBeanForPage(int size, int page);

    //分页查询 size为每次查询数据的个数  page为第几个数据开始
    @Query("SELECT * FROM script order by id limit :size offset :page")
    List<ScriptDataBean> loadScriptDataBeanForPage(int size, int page);


    @Query("SELECT * FROM script WHERE id=:iID")
    ScriptDataBean findBeanById(int iID);
}

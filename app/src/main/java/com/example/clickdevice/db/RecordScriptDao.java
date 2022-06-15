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
public interface RecordScriptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertRecordScriptBean(RecordScriptBean... RecordScriptBeans);


    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateRecordScriptBean(RecordScriptBean... RecordScriptBeans);

    @Delete
    void deleteRecordScriptBean(RecordScriptBean... RecordScriptBeans);

    @Query("SELECT * FROM record")
    List<RecordScriptBean> loadAllRecordScriptBean();

    @Query("SELECT * FROM record")
    LiveData<List<RecordScriptBean>> loadLiveDataOfAllRecordScriptBean();

    //分页查询 size为每次查询数据的个数  page为第几个数据开始
    @Query("SELECT * FROM record order by id limit :size offset :page")
    LiveData<List<RecordScriptBean>> loadLiveDataOfRecordScriptBeanForPage(int size, int page);

    //分页查询 size为每次查询数据的个数  page为第几个数据开始
    @Query("SELECT * FROM record order by id limit :size offset :page")
    List<RecordScriptBean> loadRecordScriptBeanForPage(int size, int page);
}

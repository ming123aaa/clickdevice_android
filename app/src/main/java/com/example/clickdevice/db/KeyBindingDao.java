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
public interface KeyBindingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertKeyBindingBean(KeyBindingBean... keyBindingBeans);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    int updateKeyBindingBean(KeyBindingBean... keyBindingBeans);

    @Delete
    void deleteKeyBindingBean(KeyBindingBean... keyBindingBeans);

    @Query("SELECT * FROM key_binding")
    List<KeyBindingBean> loadAllKeyBindingBean();

    @Query("SELECT * FROM key_binding")
    LiveData<List<KeyBindingBean>> loadLiveDataOfAllKeyBindingBean();

    @Query("SELECT * FROM key_binding WHERE id=:iID")
    KeyBindingBean findBeanById(int iID);

    @Query("SELECT COUNT(*) FROM key_binding")
    int getKeyBindingCount();
}
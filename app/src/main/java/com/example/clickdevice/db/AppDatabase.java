package com.example.clickdevice.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ScriptDataBean.class,RecordScriptBean.class} ,version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase appDatabase;
    public abstract ScriptDao getScriptDao();

    public abstract RecordScriptDao getRecordScriptDao();

    public static Migration migration_1_2=new Migration(1,2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `createTime` TEXT, `updateTime` TEXT, `scriptJson` TEXT)");
        }
    };

    public static AppDatabase getInstance(Context context){
        if (appDatabase==null){
          appDatabase=Room.databaseBuilder(context,AppDatabase.class,"script_info.db")
                  .addMigrations(migration_1_2)

                    .build();
        }
        return appDatabase;
    }



}

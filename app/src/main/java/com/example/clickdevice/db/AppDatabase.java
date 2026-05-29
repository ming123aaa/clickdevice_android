package com.example.clickdevice.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {ScriptDataBean.class, RecordScriptBean.class,ScriptGroupBean.class, KeyBindingBean.class}, version = 6, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase appDatabase;

    public abstract ScriptDao getScriptDao();

    public abstract RecordScriptDao getRecordScriptDao();

    public abstract ScriptGroupDao getScriptGroupDao();

    public abstract KeyBindingDao getKeyBindingDao();

    public static Migration migration_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `record` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `createTime` TEXT, `updateTime` TEXT, `scriptJson` TEXT)");
        }
    };
    public static Migration migration_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `script_group` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `createTime` TEXT, `updateTime` TEXT, `scriptJson` TEXT)");
        }
    };

    public static Migration migration_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `script_group` ADD COLUMN `xCoefficient` REAL NOT NULL DEFAULT 1.0");
            database.execSQL("ALTER TABLE `script_group` ADD COLUMN `yCoefficient` REAL NOT NULL DEFAULT 1.0");
        }
    };

    public static Migration migration_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `script` ADD COLUMN `xCoefficient` REAL NOT NULL DEFAULT 1.0");
            database.execSQL("ALTER TABLE `script` ADD COLUMN `yCoefficient` REAL NOT NULL DEFAULT 1.0");
            database.execSQL("ALTER TABLE `record` ADD COLUMN `xCoefficient` REAL NOT NULL DEFAULT 1.0");
            database.execSQL("ALTER TABLE `record` ADD COLUMN `yCoefficient` REAL NOT NULL DEFAULT 1.0");
        }
    };

    public static Migration migration_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `key_binding` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `keyName` TEXT, `keyDescription` TEXT, `scriptType` TEXT, `scriptId` INTEGER NOT NULL DEFAULT 0, `scriptName` TEXT, `scriptParams` TEXT, `textColor` INTEGER NOT NULL DEFAULT -16777216, `textSize` INTEGER NOT NULL DEFAULT 16, `windowX` INTEGER NOT NULL DEFAULT 0, `windowY` INTEGER NOT NULL DEFAULT 0, `windowLocked` INTEGER NOT NULL DEFAULT 0, `createTime` TEXT, `updateTime` TEXT)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (appDatabase == null) {
            appDatabase = Room.databaseBuilder(context, AppDatabase.class, "script_info.db")
                    .addMigrations(migration_1_2)
                    .addMigrations(migration_2_3)
                    .addMigrations(migration_3_4)
                    .addMigrations(migration_4_5)
                    .addMigrations(migration_5_6)
                    .build();
        }
        return appDatabase;
    }


}

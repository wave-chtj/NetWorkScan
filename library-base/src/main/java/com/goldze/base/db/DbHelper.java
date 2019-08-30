package com.goldze.base.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.goldze.base.entity.ModelTypeEntity;

import java.util.ArrayList;
import java.util.List;

import me.goldze.mvvmhabit.utils.KLog;

public class DbHelper extends SQLiteOpenHelper {
    private SQLiteDatabase db = null;
    private Cursor cursor = null;
    private String TAG = DbHelper.class.getSimpleName();
    private boolean isInit;
    public static final String DB_NAME_CONN_READER_TYPE = "connDB";//数据库名称
    public static final String TABLE_READER_TYPE = "connadr";//访问表
    public static final String TABLE_CONN_ADDR = "readerType";//机型表
    private List<String> createString;//多张表

    public DbHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory,
                    int version) {
        super(context, dbName, factory, version);
    }

    // 建表
    @Override
    public void onCreate(SQLiteDatabase db) {
        this.db = db;
        for (int i = 0; i < createString.size(); i++) {
            db.execSQL(createString.get(i));
        }
    }

    public void inItDb() {
        if (isInit)
            return;
        initTable();
        this.isInit = true;
    }

    // 增
    public void insertConn(ContentValues values, String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        db.insert(tableName, null, values);
        Log.i(TAG, "增加一行");
        db.close();
    }

    // 删除访问地址
    public void deleteConn(String addr, String tableName) {
        // if (db == null) {
        SQLiteDatabase db = getWritableDatabase();
        // }
        db.delete(tableName, "addr=?", new String[]{String.valueOf(addr)});
        Log.i(TAG, "删除一行");
    }

    // 删除机型
    public void deleteModelType(String typeName, String tableName) {
        // if (db == null) {
        SQLiteDatabase db = getWritableDatabase();
        // }
        db.delete(tableName, "typeName=?", new String[]{String.valueOf(typeName)});
        Log.i(TAG, "删除一行");
    }

    // 更新某一行
    public void update(ContentValues values, String string, String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        db.update(tableName, values, "id=?", new String[]{string});
        db.close();
        Log.i(TAG, "更新一行");
    }

    // 按addr查询
    public Cursor queryByAddr(String addr) {
        // 解析游标
        SQLiteDatabase db = getWritableDatabase();
        System.out.println("addr---->" + addr);
        cursor = db.query(TABLE_CONN_ADDR, null, "addr=?", new String[]{addr},
                null, null, null);
        Log.i(TAG, "按addr查询一行");
        return cursor;
    }

    /**
     * 查询访问地址是否存在
     *
     * @param addr
     * @return
     */
    public boolean checkExistByAddr(String addr) {
        boolean isExist = false;//是否在数据库找到了对应的地址
        List<String> byAddrList = new ArrayList<>();
        // 解析游标
        SQLiteDatabase db = getWritableDatabase();
        System.out.println("addr---->" + addr);
        cursor = db.query(TABLE_CONN_ADDR, null, "addr=?", new String[]{addr},
                null, null, null);
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                byAddrList.add(cursor.getString(cursor.getColumnIndex("addr")));
                cursor.moveToNext();
            }
        }
        if (byAddrList == null || byAddrList.size() <= 0) {
            isExist = false;//未找到
        } else {
            isExist = true;
        }
        return isExist;
    }


    // 按typeName查询
    public Cursor queryByModelType(String typeName) {
        SQLiteDatabase db = getWritableDatabase();
        System.out.println("typeName---->" + typeName);
        cursor = db.query(TABLE_READER_TYPE, null, "typeName=?", new String[]{typeName},
                null, null, null);
        Log.i(TAG, "按typeName查询一行");
        return cursor;
    }

    /**
     * 按照机型名称查看是否重复
     * @param typeName 机型名称
     * @return 是否重复
     */
    public boolean checkExistByTypeName(String typeName){
        boolean isExist = false;//是否在数据库找到了对应的机型
        List<String> byAddrList = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        System.out.println("typeName---->" + typeName);
        cursor = db.query(TABLE_READER_TYPE, null, "typeName=?", new String[]{typeName},
                null, null, null);
        Log.i(TAG, "按typeName查询一行");
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                byAddrList.add(cursor.getString(cursor.getColumnIndex("typeName")));
                cursor.moveToNext();
            }
        }
        if (byAddrList == null || byAddrList.size() <= 0) {
            isExist = false;//未找到
        } else {
            isExist = true;
        }
        return isExist;
    }

    /**
     * 按typeCommand查询是否存在相同命令
     * @param typeCommand 命令
     * @return 是否存在
     */
    public boolean checkExistTypeCommand(String typeCommand) {
        boolean isExist = false;//是否在数据库找到了对应的机型
        List<String> byTypeCommandList = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        System.out.println("typeCommand---->" + typeCommand);
        cursor = db.query(TABLE_READER_TYPE, null, "typeCommand=?", new String[]{typeCommand},
                null, null, null);
        Log.i(TAG, "按typeCommand查询一行");
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                byTypeCommandList.add(cursor.getString(cursor.getColumnIndex("typeCommand")));
                cursor.moveToNext();
            }
        }
        if (byTypeCommandList == null || byTypeCommandList.size() <= 0) {
            isExist = false;//未找到
        } else {
            isExist = true;
        }
        return isExist;
    }

    //按表名查询全部数据
    public Cursor query(String tableName) {
        SQLiteDatabase db = getWritableDatabase();
        cursor = db.query(tableName, null, null, null, "id", null, "id");
        Log.i(TAG, "查询所有");
        return cursor;
    }

    /**
     * 查询全部机型
     * @return
     */
    public List<ModelTypeEntity> getModelTypeList() {
        List<ModelTypeEntity> modelTypeEntityList=new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        cursor = db.query(TABLE_READER_TYPE, null, null, null, "id", null, "id");
        Log.i(TAG, "查询所有机型");
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String id = cursor.getString(cursor.getColumnIndex("id"));
                String typeName = cursor.getString(cursor.getColumnIndex("typeName"));
                String command = cursor.getString(cursor.getColumnIndex("typeCommand"));
                String androidVersion = cursor.getString(cursor.getColumnIndex("typeCommand"));    KLog.e(TAG, "获取一些内置存储机型-->list value:" + typeName + ",typeCommand:" + command + ",androidVersion:" + androidVersion);
                modelTypeEntityList.add(new ModelTypeEntity(id,typeName,command,androidVersion));
                cursor.moveToNext();
            }
        }
        return modelTypeEntityList;
    }

    /**
     * 获取数据库的全部访问地址
     * @return
     */
    public List<String> getAddrList() {
        List<String> addrList = new ArrayList<>();
        SQLiteDatabase db = getWritableDatabase();
        cursor = db.query(TABLE_CONN_ADDR, null, null, null, "id", null, "id");
        Log.i(TAG, "查询所有访问地址");
        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String addrContent = cursor.getString(cursor.getColumnIndex("addr"));
                KLog.e(TAG, "获取一些内置存储访问地址-->list value:" + addrContent);
                addrList.add(addrContent);
                cursor.moveToNext();
            }
        }
        return addrList;
    }

    public boolean deleteDatabase(Context context) {
        return context.deleteDatabase(DB_NAME_CONN_READER_TYPE);
    }

    // 按sql语句操作数据库
    public void handleBySql(String TabName) {
        String sqsl = "update sqlite_sequence set seq=0 where addr='" + TabName + "'";
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sqsl);
        Log.i(TAG, "执行sql语句");


    }

    /**
     * 判断某张表是否存在
     *
     * @return
     */
    public boolean tabbleIsExist(String tableName) {
        boolean result = false;
        if (tableName == null) {
            return false;
        }
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = this.getReadableDatabase();
            String sql = "select count(*) as c from Sqlite_master  where type ='table' and name ='" + tableName.trim() + "' ";
            cursor = db.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    result = true;
                }
            }

        } catch (Exception e) {
            // TODO: handle exception
        }
        return result;
    }

    // 关闭数据库
    public void close() {
        if (db != null) {
            db.close();
            db = null;
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //清空搜索记录
    public void deleteAllRecords() {
        db = this.getWritableDatabase();
        db.execSQL("delete from TabOther");
        db.close();
    }

    /**
     * 创建表
     *
     * @return
     */
    public void initTable() {
        createString = new ArrayList<>();
        createString.add("Create Table " + TABLE_CONN_ADDR + "( id INTEGER PRIMARY KEY AUTOINCREMENT, addr text )");
        createString.add("Create Table " + TABLE_READER_TYPE + "( id INTEGER PRIMARY KEY AUTOINCREMENT, typeName text,typeCommand text,androidVersion text )");
    }
}
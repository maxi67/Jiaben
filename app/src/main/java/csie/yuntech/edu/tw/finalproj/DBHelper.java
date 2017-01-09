package csie.yuntech.edu.tw.finalproj;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by ASUS on 2016/12/21.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static DBHelper instance = null;
    //防止重複建立資料庫。首次呼叫時instance變數為null，建完後也給變數值，之後呼叫有值就不另建資料庫
    public static DBHelper getInstance(Context ctx){
        if (instance == null){
            instance = new DBHelper(ctx, "db_test.db", null, 1);
        }
        return instance;
    }

    private DBHelper(Context context, String name,
                       SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    //建立資料表於資料庫
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE  TABLE " + Item.DATABASE_TABLE+
                " (_id INTEGER PRIMARY KEY  NOT NULL , " + //ID
                "name VARCHAR , " + //品項
                "date VARCHAR , " + //日期
                "kind VARCHAR , " + //類型
                "cost INTEGER)"); //金額

//        db.execSQL("CREATE  TABLE " + Item.INFO_TABLE+
//                " (_id INTEGER PRIMARY KEY  NOT NULL , " + //ID
//                "year INTEGER , " +
//                "month INTEGER , " +
//                "budget VARCHAR)");
    }

    //資料庫的版本更新
    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
    }

}
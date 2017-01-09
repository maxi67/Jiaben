package csie.yuntech.edu.tw.finalproj;

/**
 * Created by ASUS on 2016/12/21.
 */

public class Item {

    // Labels DB
    public  static final String DATABASE_TABLE = "db_table";
    public  static final String INFO_TABLE = "info_table";

    // Labels  of Table Columns' names
    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name"; //品項
    public static final String KEY_DATE = "date"; //日期
    public static final String KEY_KIND = "kind"; //類型
    public static final String KEY_COST = "cost"; //金額

    static boolean firstTime = true;
    public static int CURRENT_YEAR = 2016; //當年
    public static int CURRENT_MONTH = 11; //當月(0 ~ 11)
    public static String MONTH_BUDGET = "2000"; //預設預算

}

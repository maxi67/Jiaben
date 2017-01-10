package csie.yuntech.edu.tw.finalproj;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import static csie.yuntech.edu.tw.finalproj.R.array.food;
import static csie.yuntech.edu.tw.finalproj.R.id.record_date;

public class MainActivity extends AppCompatActivity {

    //Views
    private DBHelper _helper; //DB initialize
    TabHost tabHost;
    EditText record_name, record_$$, search_name;
    DatePickerDialog.OnDateSetListener dateSetListener;
    Calendar myCalendar;
    ImageButton ask_shell;
    Button btn_record_date, btn_record_save, btn_search_go, btn_count_$change;
    Spinner record_spinner, ask_spinner;
    ListView search_list;
    TextView txv_count_sum, txv_count_daysLeft, txv_count_$left, txv_count_word, ask_txv_eat;
    private Cursor c, c2;
    private SimpleCursorAdapter adapter;

    //Variables
    private boolean firstTime;
    public int CURRENT_YEAR; //當年
    public int CURRENT_MONTH; //當月(1 ~ 12)
    public int MONTH_BUDGET; //預設預算
    private String input_date = ""; // YYYY/MM/DD
    String ask_kind = "";
  //  private String ss;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myCalendar = Calendar.getInstance();  //取得手機當前日期
        CURRENT_YEAR = myCalendar.get(Calendar.YEAR);
        CURRENT_MONTH = myCalendar.get(Calendar.MONTH) + 1;

        _helper = DBHelper.getInstance(this); //資料庫物件初始化
        firstTime = isFirstLaunchApp();

        //=============tabHost===================
        tabHost = (TabHost)findViewById(R.id.tabhost);
        tabHost.setup();

        addTabHost("tag1", "支出", R.drawable.out, R.id.tab1);
        initializeTab1();
        addTabHost("tag2", "查詢", R.drawable.find, R.id.tab2);
        initializeTab2();
        addTabHost("tag3", "統計", R.drawable.count, R.id.tab3);
        initializeTab3();
        addTabHost("tag4", "吃啥", R.drawable.eatwhat, R.id.tab4);
        initializeTab4();
        //==================================

    }

    @Override
    protected void onResume() {
        super.onResume();

        //更新剩餘日期(當月最大值 - 當天日期)
        String daysLeft = Integer.toString(myCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) - myCalendar.get(Calendar.DAY_OF_MONTH));
        txv_count_daysLeft.setText(daysLeft);

        if(firstTime) { //初次使用App
            firstTime = false;
            final View item = LayoutInflater.from(this).inflate(R.layout.item_view_input, null);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.welcometoApp)
                    .setMessage("我們會進行每月的預算控管，請先輸入本月預算")
                    .setView(item)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText editText = (EditText) item.findViewById(R.id.editText);
                            if (editText.getText().toString().length() == 0) {
                                editText.setError("不能為空");
                                return;
                            }
                            reloadCount(Integer.valueOf(editText.getText().toString()));
                            ChangeState(CURRENT_YEAR + "", CURRENT_MONTH + "", MONTH_BUDGET, 0);
                        }
                    })
                    .show();
        }
        else {
            //如果進到下個月，重新詢問預算
            final int getMonth = myCalendar.get(Calendar.MONTH) + 1;
            if (getMonth != CURRENT_MONTH) {

                if(getMonth == 0) //進入新的一年
                {
                    CURRENT_YEAR = myCalendar.get(Calendar.YEAR);
                }
                final View item = LayoutInflater.from(this).inflate(R.layout.item_view_input, null);
                new AlertDialog.Builder(this)
                        .setTitle("請輸入本月預算")
                        .setView(item)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText editText = (EditText) item.findViewById(R.id.editText);
                                if (editText.getText().toString().length() == 0) {
                                    editText.setError("不能為空");
                                    return;
                                }
                                reloadCount(Integer.valueOf(editText.getText().toString()));
                                ChangeState(CURRENT_YEAR + "", CURRENT_MONTH + "", MONTH_BUDGET, 1);
                                CURRENT_MONTH = getMonth;
                            }
                        })
                        .show();
            }
            else
                reloadCount(MONTH_BUDGET);
        }
    }

    public void initializeTab1(){
        //元件宣告
        LinearLayout content_view = (LinearLayout) findViewById(R.id.tab1);
        getLayoutInflater().inflate(R.layout.tab1_content, content_view, true);
        record_spinner = (Spinner)content_view.findViewById(R.id.record_spinner);  //(kind)
        btn_record_date = (Button)content_view.findViewById(record_date);        //(date)
        btn_record_save = (Button)content_view.findViewById(R.id.record_save);
        record_name = (EditText)content_view.findViewById(R.id.record_name);    //(name)
        record_$$ = (EditText)content_view.findViewById(R.id.record_$$);        //(cost)

        //下拉式選單[種類(kind)]=========Spinner===============================
        ArrayAdapter<CharSequence> foodList = ArrayAdapter.createFromResource(MainActivity.this,
                food,
                android.R.layout.simple_spinner_dropdown_item);
        record_spinner.setAdapter(foodList);
        //============================================================

        //按鈕、日期選擇[日期(date)]==================DatePicker======================
        btn_record_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myCalendar = Calendar.getInstance();  //取得手機當前日期
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this,dateSetListener,
                        myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                input_date = String.format("%04d/%02d/%02d", year, monthOfYear + 1,dayOfMonth);
             //   input_date = year + "/" + (monthOfYear + 1) + "/" + dayOfMonth;
                btn_record_date.setText(input_date); //將選取結果show在按鈕上
            }
        };
        //=============================================================

        //按鈕[儲存]=======================================================
        btn_record_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取得資料
                String input_name = record_name.getText().toString();
                String input_cost = record_$$.getText().toString();
                String input_kind = getResources().getStringArray(food)[record_spinner.getSelectedItemPosition()];

                //檢查資料(防止空值)
                if(input_name.length() == 0){
                    Toast.makeText(MainActivity.this, "品項名稱不得為空", Toast.LENGTH_SHORT).show();
                    return;
                }
                //檢查資料(防止空值)
                if(input_cost.length() == 0){
                    Toast.makeText(MainActivity.this, "金額不得為空", Toast.LENGTH_SHORT).show();
                    return;
                }
                //檢查資料(防止空值)
                if(input_date.length() == 0){
                    Toast.makeText(MainActivity.this, "未選擇日期", Toast.LENGTH_SHORT).show();
                    return;
                }

                //設定存進資料庫的容器(參數: 資料表欄名稱 / 資料值，id是主鍵會自己遞增生成，不用另外寫)
                ContentValues values = new ContentValues();
                values.put(Item.KEY_NAME, input_name);
                values.put(Item.KEY_DATE, input_date);
                values.put(Item.KEY_KIND, input_kind);
                values.put(Item.KEY_COST, input_cost);
                //資料庫新增資料語法 (參數: 資料表名稱 /好像是如果第三個參數沒給值要放什麼 /要放的值)
                _helper.getWritableDatabase().insert(Item.DATABASE_TABLE, null, values);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("已成功存檔")
                        .setPositiveButton("OK", null)
                        .show();

                //Update listView
                c = _helper.getReadableDatabase()
                        .query(Item.DATABASE_TABLE, null, Item.KEY_NAME + "!= 'state'", null, null, null, "date");
                adapter.changeCursor(c);
            }
        });
        //==================================================================
    }

    public void initializeTab2(){
        //元件宣告
        LinearLayout content_view = (LinearLayout) findViewById(R.id.tab2);
        getLayoutInflater().inflate(R.layout.tab2_content, content_view, true);
        search_name = (EditText) content_view.findViewById(R.id.search_name);
        btn_search_go = (Button)content_view.findViewById(R.id.search_go);

        //插入標題列
        LinearLayout content_view2 = (LinearLayout)content_view.findViewById(R.id.list_title);
        getLayoutInflater().inflate(R.layout.list_style, content_view2, true);
        View item_view = content_view2.getChildAt(0);

        ((TextView)item_view.findViewById(R.id.item_name)).setText("品項");
        TextView title_date = (TextView)item_view.findViewById(R.id.item_date);
        title_date.setText("時間");
//        title_date.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                final View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view_select_month, null);
//                new AlertDialog.Builder(MainActivity.this)
//                        .setTitle("請選擇時間")
//                        .setView(item)
//                        .setNegativeButton("Cancel", null)
//                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                EditText editText = (EditText) item.findViewById(R.id.editText);
//                                if (editText.getText().toString().length() == 0)
//                                {
//                                    editText.setError("不能為空");
//                                    return;
//                                }
//                                reloadCount(Integer.valueOf(editText.getText().toString()));
//                                ChangeState(CURRENT_YEAR + "", CURRENT_MONTH + "", MONTH_BUDGET, 0);
//                            }
//                        })
//                        .show();
//            }
//        });
        TextView title_kind = (TextView)item_view.findViewById(R.id.item_kind);
        title_kind.setText("種類");
        TextView title_cost = (TextView)item_view.findViewById(R.id.item_cost);
        title_cost.setText("金額");

        search_list = (ListView)content_view.findViewById(R.id.search_list);

        //從資料庫抓清單
        c = _helper.getReadableDatabase()
                .query(Item.DATABASE_TABLE, null, Item.KEY_NAME + "!= 'state'", null, null, null, "date");
        adapter = new SimpleCursorAdapter(this,
                R.layout.list_style, c,
                new String[]{"name", "date", "kind", "cost"},
                new int[]{R.id.item_name, R.id.item_date, R.id.item_kind, R.id.item_cost}, 1);
        search_list.setAdapter(adapter);

        //=====================查詢=========================================
        btn_search_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(search_name.getText().toString().length() != 0){
         //           String name = search_name.getText().toString().trim();
                }else{
                    Toast.makeText(MainActivity.this, "名稱不得為空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //刪除功能===========================================================
        search_list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //get _id
                c.moveToPosition(position);
                final int _id = c.getInt(0);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("提醒") //對話框標題
                        .setMessage("你確定要刪除嗎?") //對話框內容
                        .setNegativeButton("Cancel", null) //按下按鈕的動作，null表示沒動作
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                _helper.getReadableDatabase().delete(Item.DATABASE_TABLE, "_id=" + _id, null);
                                Toast.makeText(MainActivity.this, "已成功刪除", Toast.LENGTH_SHORT).show();

                                //Update listView
                                c = _helper.getReadableDatabase()
                                        .query(Item.DATABASE_TABLE, null, Item.KEY_NAME + "!= 'state'", null, null, null, "date");
                                adapter.changeCursor(c);
                            }
                        })
                        .setCancelable(false) //不可以點對話框以外區域取消
                        .show();
                return false;
            }
        });
    }

    public void initializeTab3(){
        //元件宣告
        LinearLayout content_view = (LinearLayout) findViewById(R.id.tab3);
        getLayoutInflater().inflate(R.layout.tab3_content, content_view, true);
        btn_count_$change = (Button)content_view.findViewById(R.id.count_$change); //預算變更
        txv_count_sum = (TextView)content_view.findViewById(R.id.count_sum);    //預算
        txv_count_$left = (TextView)content_view.findViewById(R.id.count_$left);
        txv_count_daysLeft = (TextView)content_view.findViewById(R.id.count_daysLeft);
        txv_count_word = (TextView)content_view.findViewById(R.id.count_word);

        btn_count_$change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view_input, null);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("請輸入本月預算")
                        .setView(item)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText editText = (EditText) item.findViewById(R.id.editText);
                                if (editText.getText().toString().length() == 0)
                                {
                                    editText.setError("不能為空");
                                    return;
                                }
                                reloadCount(Integer.valueOf(editText.getText().toString()));
                                ChangeState(CURRENT_YEAR + "", CURRENT_MONTH + "", MONTH_BUDGET, 0);
                            }
                        })
                        .show();
            }
        });
    }

    public void initializeTab4() {
        //元件宣告
        LinearLayout content_view = (LinearLayout) findViewById(R.id.tab4);
        getLayoutInflater().inflate(R.layout.tab4_content, content_view, true);
        ask_spinner = (Spinner)content_view.findViewById(R.id.ask_spinner);  //(kind)
        ask_txv_eat = (TextView)content_view.findViewById(R.id.txv_eat);
        ask_shell = (ImageButton)content_view.findViewById(R.id.img_btn_shell);

        //下拉式選單[種類(kind)]=========Spinner===============================
        ArrayAdapter<CharSequence> foodList = ArrayAdapter.createFromResource(MainActivity.this,
                food,
                android.R.layout.simple_spinner_dropdown_item);
        ask_spinner.setAdapter(foodList);
        //==============================

        //================spinner==========================

        ask_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ask_shell.setVisibility(View.VISIBLE);
                ask_kind = getResources().getStringArray(food)[i];
                String ss = "kind LIKE '" + ask_kind + "'";
                c2 = _helper.getReadableDatabase()
                        .query(Item.DATABASE_TABLE, new String[]{"name"},
                                ss,
                                null, null, null, null);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        //按鈕(亂數)=================================================
        ask_shell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ask_txv_eat.setVisibility(View.VISIBLE);
                //需要截取資料庫資訊之程式片段
                if (c2 != null){
                    if(c2.getCount() != 0) {
                        c2.moveToFirst();    //將指標移到第一筆資料
                        int numOfData = c2.getCount();
                        int num = (int) (Math.random() * numOfData);
                        c2.moveToPosition(num);
                        String food = c2.getString(0);
                        ask_txv_eat.setText("您吃" + food + "吧！");
                    }
                    else{
                        ask_txv_eat.setText("您未登記" + ask_kind + "的相關資訊\n海螺不知道您都吃什麼");
                    }
                }
            }
        });
        //=========================================
    }

    //TabHost標籤建立(標籤名/標籤顯示文字/顯示圖片ID/標籤畫面ID)
    public void addTabHost(String label, String title, int iconId, int contentId){
        View tab = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        ImageView image = (ImageView) tab.findViewById(R.id.icon);
        TextView text = (TextView) tab.findViewById(R.id.text);
        text.setText(title);
        image.setImageResource(iconId);
        TabHost.TabSpec spec = tabHost.newTabSpec(label).setIndicator(tab).setContent(contentId);
        tabHost.addTab(spec);
    }

    //更新預算介面(本月預算、剩餘額度)
    public void reloadCount(int newBudget){
        //紀錄當前的年/月(抓資料的語法)
        String monthKeyword = String.format("date LIKE '%04d/%02d%%'", CURRENT_YEAR, CURRENT_MONTH);

        //更新預算
        MONTH_BUDGET = newBudget;
        txv_count_sum.setText(newBudget+"");

        //統計當月總支出
        Cursor c3 = _helper.getReadableDatabase()
                .query(Item.DATABASE_TABLE, //table
                        new String[] {"cost"}, //columns
                        monthKeyword, //WHERE
                        null, null, null, null); //selectionArgs, groupBy, having, orderBy

        if (c3 != null) {
            c3.moveToFirst();    //將指標移到第一筆資料

            int sum = 0;
            for (int i = 0; i < c3.getCount(); i++) { //計算總支出
                sum += c3.getInt(0);
                c3.moveToNext();
            }
            sum = newBudget - sum;
            txv_count_$left.setText(sum + "");
            c3.close();
        }
    }

    public boolean isFirstLaunchApp(){
        c = _helper.getReadableDatabase(). //查找有無狀態列，若無，getCount() == 0，表示第一次執行App
                query(Item.DATABASE_TABLE, null, "name LIKE 'state'", null, null, null, null);

        if (c.getCount() == 0) //第一次開啟App，新建state資料，並回傳true
            return true;
        else {
            c.moveToFirst();
            MONTH_BUDGET = Integer.valueOf(c.getString(4));
            return false;
        }
    }

    //更新狀態(當前年，當前月，當前預算，mode: 0 新增/1 修改)
    public void ChangeState(String year, String month, int budget, int mode){

            //設定存進資料庫的容器(參數: 資料表欄名稱 / 資料值，id是主鍵會自己遞增生成，不用另外寫)
            ContentValues values = new ContentValues();
            values.put(Item.KEY_NAME, "state"); //VARCHAR
            values.put(Item.KEY_DATE, year); //VARCHAR year month(YYYY/MM)
            values.put(Item.KEY_KIND, month); //VARCHAR first (Y/N)
            values.put(Item.KEY_COST, budget); //INTEGER budget

        if(mode == 0) //資料庫新增資料語法 (參數: 資料表名稱 /好像是如果第三個參數沒給值要放什麼 /要放的值)
            _helper.getWritableDatabase().insert(Item.DATABASE_TABLE, null, values);
        if(mode == 1) //資料庫更新資料語法 (參數: 資料表名稱 /要放的值 /WHERE /WhereArgs)
            _helper.getWritableDatabase().update(Item.DATABASE_TABLE, values, Item.KEY_ID + "= 1", null);
    }
}

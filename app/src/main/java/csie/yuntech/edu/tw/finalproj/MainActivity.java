package csie.yuntech.edu.tw.finalproj;
//2017-01-11 11:58

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.Calendar;

import static csie.yuntech.edu.tw.finalproj.R.array.food;
import static csie.yuntech.edu.tw.finalproj.R.array.food_random;
import static csie.yuntech.edu.tw.finalproj.R.id.record_date;

public class MainActivity extends AppCompatActivity {

    //Views
    private DBHelper _helper; //DB initialize
    TabHost tabHost;
    EditText record_name, record_$$;
    DatePickerDialog.OnDateSetListener dateSetListener;
    TimePickerDialog.OnTimeSetListener timeSetListener;
    Calendar myCalendar, notifc;
    ImageButton ask_shell;
    Button btn_record_date, btn_record_save, btn_count_$change, btn_notify_time, btn_showAll;
    Spinner record_spinner, ask_spinner;
    ListView search_list;
    TextView txv_show, txv_cost, txv_count_sum, txv_count_daysLeft, txv_count_$left, txv_count_word, ask_txv_eat;

    private Cursor c, c2;
    private SimpleCursorAdapter adapter;

    //Variables
    private boolean firstTime, changeFlag = false; //第一次執行App/ 跑去修改資料
    public int CURRENT_YEAR; //當年
    public int CURRENT_MONTH; //當月(1 ~ 12)
    public int MONTH_BUDGET; //預設預算
    private int daysLeft;
    private String input_date = ""; // YYYY/MM/DD
    String ask_kind = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myCalendar = Calendar.getInstance();  //取得手機當前日期
        notifc = Calendar.getInstance();
        CURRENT_YEAR = myCalendar.get(Calendar.YEAR);
        CURRENT_MONTH = myCalendar.get(Calendar.MONTH) + 1;

        _helper = DBHelper.getInstance(this); //資料庫物件初始化
        firstTime = isFirstLaunchApp();

        //=============tabHost===================
        tabHost = (TabHost)findViewById(R.id.tabhost);
        tabHost.setup();

        addTabHost("tag1", getResources().getString(R.string.tab_expense), R.drawable.out, R.id.tab1);
        initializeTab1();
        addTabHost("tag2", getResources().getString(R.string.tab_search), R.drawable.find, R.id.tab2);
        initializeTab2();
        addTabHost("tag3", getResources().getString(R.string.tab_count), R.drawable.count, R.id.tab3);
        initializeTab3();
        addTabHost("tag4", getResources().getString(R.string.tab_eatWhat), R.drawable.eatwhat, R.id.tab4);
        initializeTab4();

        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if(tabId.equals("tag2"))
                    txv_show.setText("");
            }
        });
        //==================================
    }

    @Override
    protected void onResume() {
        super.onResume();

        //更新剩餘日期(當月最大值 - 當天日期)
        daysLeft = myCalendar.getActualMaximum(Calendar.DAY_OF_MONTH) - myCalendar.get(Calendar.DAY_OF_MONTH);
        txv_count_daysLeft.setText(Integer.toString(daysLeft)); //更新介面

        if(firstTime) { //初次使用App
            firstTime = false;
            final View item = LayoutInflater.from(this).inflate(R.layout.item_view_inputnumber, null);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.welcomeToApp)
                    .setMessage("我們會進行每月的預算控管，請先輸入本月預算")
                    .setView(item)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            EditText editText = (EditText) item.findViewById(R.id.edt_input);
                            if (editText.getText().toString().length() == 0) {
                                Toast.makeText(MainActivity.this, getResources().getString(R.string.noEmpty), Toast.LENGTH_SHORT).show();
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
                final View item = LayoutInflater.from(this).inflate(R.layout.item_view_inputnumber, null);
                new AlertDialog.Builder(this)
                        .setTitle("請輸入本月預算")
                        .setView(item)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText editText = (EditText) item.findViewById(R.id.edt_input);
                                if (editText.getText().toString().length() == 0) {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.noEmpty), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                reloadCount(Integer.valueOf(editText.getText().toString()));
                                ChangeState(CURRENT_YEAR + "", CURRENT_MONTH + "", MONTH_BUDGET, 1);
                                CURRENT_MONTH = getMonth;
                            }
                        })
                        .show();
            }else
                reloadCount(MONTH_BUDGET);
        }

        if(changeFlag) {
            //Update ListView
            updateTable();
            changeFlag = false;
        }

    }

    private void setAlarm(Calendar target){
        // ===============建立NotificationCompat.Builder物件=================
        Intent intent = new Intent();
        intent.setClass(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this,0,intent,0);
        AlarmManager alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, target.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    public void initializeTab1(){
        //元件宣告
        LinearLayout content_view = (LinearLayout) findViewById(R.id.tab1);
        getLayoutInflater().inflate(R.layout.tab1_content, content_view, true);
        record_spinner = (Spinner)content_view.findViewById(R.id.record_spinner); //(kind)
        btn_record_date = (Button)content_view.findViewById(record_date); //(date)
        btn_record_save = (Button)content_view.findViewById(R.id.record_save);
        btn_notify_time = (Button)content_view.findViewById(R.id.record_notify_time);
        record_name = (EditText)content_view.findViewById(R.id.record_name); //(name)
        record_$$ = (EditText)content_view.findViewById(R.id.record_$$); //(cost)

        //=================通知時間設定============================
        btn_notify_time.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myCalendar = Calendar.getInstance(); //取得當前時間，做為打開選取器的預設值
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, timeSetListener,
                        myCalendar.get(Calendar.HOUR_OF_DAY),
                        myCalendar.get(Calendar.MINUTE),true);
                timePickerDialog.show();
            }
        });

        timeSetListener = new TimePickerDialog.OnTimeSetListener(){
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                notifc.set(Calendar.HOUR_OF_DAY, hour);
                notifc.set(Calendar.MINUTE, minute);
                notifc.set(Calendar.SECOND, 0);
                myCalendar = Calendar.getInstance();
                if(notifc.compareTo(myCalendar) <= 0){ //兩物件相同得0，不同得開始不同位元之ASCII前-後值(<=0 表示鬧鐘[時:分]晚於或相同於現在)
                    int day = notifc.get(Calendar.DAY_OF_MONTH);
                    notifc.set(Calendar.DAY_OF_MONTH, day + 1); //隔天開始同時間才會叫
                }
                btn_notify_time.setText(String.format("%02d:%02d", hour, minute));  //將選取結果show在按鈕上
                setAlarm(notifc);
            }
        };

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
                input_date = String.format("%04d/%02d/%02d", year, monthOfYear + 1, dayOfMonth);
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

                if(input_name.length() == 0){ //檢查資料(防止空值)
                    Toast.makeText(MainActivity.this, "品項名稱不得為空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(input_cost.length() == 0){ //檢查資料(防止空值)
                    Toast.makeText(MainActivity.this, "金額不得為空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(input_date.length() == 0){ //檢查資料(防止空值)
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
                        .setTitle(getResources().getString(R.string.save_ok)) //成功存檔
                        .setPositiveButton(getResources().getString(R.string.ok), null)
                        .show();

                //Update ListView
                updateTable();
                reloadCount(MONTH_BUDGET);
            }
        });
        //==================================================================
    }

    public void initializeTab2(){
        //元件宣告
        LinearLayout content_view = (LinearLayout) findViewById(R.id.tab2);
        getLayoutInflater().inflate(R.layout.tab2_content, content_view, true);
        btn_showAll = (Button)content_view.findViewById(R.id.btn_showAll);
        txv_show = (TextView)content_view.findViewById(R.id.txv_show);
        //初始化清單[ListView] ==============================================
        search_list = (ListView)content_view.findViewById(R.id.search_list);

        c = _helper.getReadableDatabase() //從資料庫抓清單
                .query(Item.DATABASE_TABLE, null, Item.KEY_NAME + "!= 'state'", null, null, null, Item.KEY_DATE);
        adapter = new SimpleCursorAdapter(this,
                R.layout.list_style, c,
                new String[]{Item.KEY_NAME, Item.KEY_DATE, Item.KEY_KIND, Item.KEY_COST},
                new int[]{R.id.item_name, R.id.item_date, R.id.item_kind, R.id.item_cost}, 1);
        search_list.setAdapter(adapter);
        //=============================================================

        //插入標題列===================================================
        LinearLayout content_view2 = (LinearLayout)content_view.findViewById(R.id.list_title);
        getLayoutInflater().inflate(R.layout.list_style, content_view2, true);
        View item_view = content_view2.getChildAt(0);

        TextView title_name = (TextView)item_view.findViewById(R.id.item_name);
        title_name.setText(getResources().getString(R.string.title_name));
        title_name.setTextSize(18);
        title_name.setTextColor(0xff882288);
        title_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view_inputketword, null);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getResources().getString(R.string.plzTypeKeyword))
                        .setView(item)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                EditText search_name = (EditText) item.findViewById(R.id.edt_input);
                                if (search_name.getText().toString().length() == 0)
                                {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.noEmpty), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String name = Item.KEY_NAME + " LIKE '%" + search_name.getText().toString() + "%'";
                                //從資料庫抓清單
                                c = _helper.getReadableDatabase()
                                        .query(Item.DATABASE_TABLE, null, name, null, null, null, Item.KEY_DATE);
                                adapter.changeCursor(c);

                                if(c != null) {
                                    if(c.getCount() == 0)
                                        txv_show.setText("查無結果");
                                    else
                                        txv_show.setText("搜尋" + search_name.getText().toString() + "的結果如下:");
                                }
                            }
                        })
                        .show();
            }
        });

        TextView title_date = (TextView)item_view.findViewById(R.id.item_date);
        title_date.setText(getResources().getString(R.string.title_date));
        title_date.setTextSize(18);
        title_date.setTextColor(0xff882288);
        title_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view_select_month, null);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("請輸入時間")
                        .setView(item)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                final EditText edt_year = (EditText) item.findViewById(R.id.editText_year);
                                final EditText edt_month = (EditText) item.findViewById(R.id.editText_month);
                                edt_year.setText(CURRENT_YEAR+"");
                                edt_month.setText(CURRENT_MONTH+"");

                                int newMonth = Integer.valueOf(edt_month.getText().toString());
                                if(newMonth < 1)
                                {
                                    edt_month.setText("01");
                                }else
                                    edt_month.setText(newMonth+"");
                                if(newMonth > 12)
                                {
                                    edt_month.setText("12");
                                }else
                                    edt_month.setText(newMonth+"");

                                String year = String.format("%04d",Integer.valueOf(edt_year.getText().toString()));
                                String month = String.format("%02d",Integer.valueOf(edt_month.getText().toString()));
                                if (year.length() == 0 || month.length() == 0)
                                {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.noEmpty), Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                String date = Item.KEY_DATE +" LIKE '" + year + "/" + month + "%'";
                                //從資料庫抓清單
                                c = _helper.getReadableDatabase()
                                        .query(Item.DATABASE_TABLE, null, date, null, null, null, Item.KEY_DATE);
                                adapter.changeCursor(c);

                                if(c != null) {
                                    c.moveToFirst();

                                    int sum = 0;
                                    for (int i = 0; i < c.getCount(); i++) { //計算總支出
                                        sum += c.getInt(4);
                                        c.moveToNext();
                                    }
                                    txv_show = (TextView)findViewById(R.id.txv_show);
                                    String show = year + "年" + month + "月總花費: " + sum;
                                    txv_show.setText(show);
                                }
                            }
                        })
                        .show();
            }
        });

        TextView title_kind = (TextView)item_view.findViewById(R.id.item_kind);
        title_kind.setText(getResources().getString(R.string.title_kind));
        title_kind.setTextSize(18);
        title_kind.setTextColor(0xff882288);
        title_kind.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("請選擇類型")
                        .setNegativeButton("Cancel", null)
                        .setItems(food, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //取array單項內容
                                String kind = getResources().getStringArray(R.array.food)[which];

                                c = _helper.getReadableDatabase()
                                        .query(Item.DATABASE_TABLE, null, Item.KEY_KIND + " LIKE '%" + kind + "%'",
                                                null, null, null, null);
                                adapter.changeCursor(c);

                                if(c != null) {
                                    String show = "總共" + c.getCount() + "份"+ kind;
                                    txv_show.setText(show);
                                }
                            }
                        })
                        .setNegativeButton("Cancel", null) //按下按鈕的動作，null表示沒動作
                        .show();
            }
        });

        TextView title_cost = (TextView)item_view.findViewById(R.id.item_cost);
        title_cost.setText(getResources().getString(R.string.title_cost));
        title_cost.setTextSize(18);
        title_cost.setTextColor(0xff882288);

        //顯示全部[Button]============================================================
        btn_showAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Update ListView
                updateTable();
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
                        .setTitle("請選擇動作") //對話框標題
                        .setNegativeButton("Cancel", null) //按下按鈕的動作，null表示沒動作
                        .setItems(R.array.actions, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            //    String kind = getResources().getStringArray(R.array.actions)[which];

                                if(which == 0){ //修改
                                    changeFlag = true; //標記以便回來時可以在onResume()更新表格

                                    //new一個intent物件，並指定Activity切換的class
                                    Intent intent = new Intent();
                                    intent.setClass(MainActivity.this, ChangeDataActivity.class);
                                    intent.putExtra("id",_id);//可放所有基本類別
                                    //切換Activity
                                    startActivity(intent);
                                }
                                else if(which == 1){ //刪除
                                    _helper.getReadableDatabase().delete(Item.DATABASE_TABLE, "_id=" + _id, null);
                                    Toast.makeText(MainActivity.this, "已成功刪除", Toast.LENGTH_SHORT).show();

                                    //Update ListView
                                    updateTable();
                                }
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
        txv_cost = (TextView)content_view.findViewById(R.id.cost_sum);
        txv_count_$left = (TextView)content_view.findViewById(R.id.count_$left);
        txv_count_daysLeft = (TextView)content_view.findViewById(R.id.count_daysLeft);
        txv_count_word = (TextView)content_view.findViewById(R.id.count_word);

        btn_count_$change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View item = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_view_inputnumber, null);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("請輸入本月預算")
                        .setView(item)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                EditText editText = (EditText) item.findViewById(R.id.edt_input);
                                if (editText.getText().toString().length() == 0)
                                {
                                    Toast.makeText(MainActivity.this, getResources().getString(R.string.noEmpty), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                reloadCount(Integer.valueOf(editText.getText().toString()));
                                ChangeState(CURRENT_YEAR + "", CURRENT_MONTH + "", MONTH_BUDGET, 1);
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
                food_random,
                android.R.layout.simple_spinner_dropdown_item);
        ask_spinner.setAdapter(foodList);
        //==============================

        //================spinner==========================
        ask_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                ask_shell.setVisibility(View.VISIBLE);
                if(i == 5) //全部類型
                    c2 = _helper.getReadableDatabase()
                            .query(Item.DATABASE_TABLE, new String[]{Item.KEY_NAME},
                                    Item.KEY_NAME + "!= 'state'",
                                    null, null, null, null);
                else {
                    ask_kind = getResources().getStringArray(food)[i];
                    String ss = Item.KEY_KIND + " LIKE '" + ask_kind + "'";
                    c2 = _helper.getReadableDatabase()
                            .query(Item.DATABASE_TABLE, new String[]{Item.KEY_NAME},
                                    ss,
                                    null, null, null, null);
                }
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
                        ask_txv_eat.setText("吃" + food + "吧！");
                    }
                    else{
                        ask_txv_eat.setText("未登記" + ask_kind + "的相關資訊\n海螺不知道您都吃什麼");
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
                        new String[] {Item.KEY_COST}, //columns
                        monthKeyword, //WHERE
                        null, null, null, null); //selectionArgs, groupBy, having, orderBy

        if (c3 != null) {
            c3.moveToFirst();    //將指標移到第一筆資料

            int sum = 0;
            for (int i = 0; i < c3.getCount(); i++) { //計算總支出
                sum += c3.getInt(0);
                c3.moveToNext();
            }
            c3.close();
            int left = newBudget - sum;
            txv_cost.setText(sum+"");
            txv_count_$left.setText(left + "");

            //Update Comment
            if(left > MONTH_BUDGET * 0.75 && daysLeft > 20){
                txv_count_word.setText(getResources().getString(R.string.comment_1));
            }else if(left > MONTH_BUDGET * 0.75 && daysLeft > 10){
                txv_count_word.setText(getResources().getString(R.string.comment_2));
            }else if(left > MONTH_BUDGET * 0.5 && daysLeft > 20){
                txv_count_word.setText(getResources().getString(R.string.comment_3));
            }else if(left > MONTH_BUDGET * 0.5 && daysLeft > 10){
                txv_count_word.setText(getResources().getString(R.string.comment_4));
            }else if(left > MONTH_BUDGET * 0.25 && daysLeft > 20){
                txv_count_word.setText(getResources().getString(R.string.comment_5));
            }else if(left > MONTH_BUDGET * 0.25 && daysLeft > 10){
                txv_count_word.setText(getResources().getString(R.string.comment_6));
            }else if(left <= MONTH_BUDGET * 0.25 && daysLeft > 20){
                txv_count_word.setText(getResources().getString(R.string.comment_7));
            }else if(left < 0){
                txv_count_word.setText(getResources().getString(R.string.comment_8));
            }else
                txv_count_word.setText(getResources().getString(R.string.comment_9));
        }
    }

    //更新顯示所有資料
    public void updateTable(){
        c = _helper.getReadableDatabase()
                .query(Item.DATABASE_TABLE, null, Item.KEY_NAME + "!= 'state'", null, null, null, "date");
        adapter.changeCursor(c);
        txv_show.setText("");
    }

    public boolean isFirstLaunchApp(){
        c = _helper.getReadableDatabase(). //查找有無狀態列，若無，getCount() == 0，表示第一次執行App
                query(Item.DATABASE_TABLE, null, Item.KEY_NAME + " LIKE 'state'", null, null, null, null);

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
            _helper.getWritableDatabase().update(Item.DATABASE_TABLE, values, Item.KEY_NAME + " LIKE 'state'", null);
    }
}
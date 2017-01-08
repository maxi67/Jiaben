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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import static csie.yuntech.edu.tw.finalproj.R.id.record_date;

public class MainActivity extends AppCompatActivity {

    //Views
    private DBHelper _helper; //DB initialize
    TabHost tabHost;
    EditText record_name, record_$$, search_name;
    DatePickerDialog.OnDateSetListener dateSetListener;
    Calendar myCalendar;
    Button btn_record_date, btn_record_save, btn_search_go, btn_count_$change;
    Spinner record_spinner;
    ListView search_list;
    TextView txv_count_sum, txv_count_daysleft, txv_count_$left, txv_count_word;
    private Cursor c;
    private SimpleCursorAdapter adapter;

    LinearLayout content_view; //要放標題列
    View item_view;

    //Variables
    private String input_date; // YYYY/MM/DD
    static int $sum = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //資料庫物件初始化
        _helper = DBHelper.getInstance(this);

        myCalendar = Calendar.getInstance();  //取得手機當前日期
        //預設時間
        input_date = myCalendar.get(Calendar.YEAR) + "/"
                  + (myCalendar.get(Calendar.MONTH) + 1) + "/"
                  +  myCalendar.get(Calendar.DAY_OF_MONTH);

        Toast.makeText(MainActivity.this, input_date, Toast.LENGTH_SHORT).show();
        record_name = (EditText) findViewById(R.id.record_name);    //(name)
        record_$$ = (EditText) findViewById(R.id.record_$$);        //(cost)
        search_name = (EditText) findViewById(R.id.search_name);    //

        btn_record_date = (Button)findViewById(record_date);        //(date)
        btn_record_save = (Button)findViewById(R.id.record_save);
        btn_search_go = (Button)findViewById(R.id.search_go);
        btn_count_$change = (Button)findViewById(R.id.count_$change); //預算變更

        txv_count_sum = (TextView)findViewById(R.id.count_sum);    //預算
        txv_count_$left = (TextView)findViewById(R.id.count_$left);
        txv_count_daysleft = (TextView)findViewById(R.id.count_daysleft);
        txv_count_word = (TextView)findViewById(R.id.count_word);

        record_spinner = (Spinner)findViewById(R.id.record_spinner);  //(kind)

        //插入標題列
        content_view = (LinearLayout)findViewById(R.id.list_title);
        getLayoutInflater().inflate(R.layout.list_style, content_view, true);
        item_view = content_view.getChildAt(0);

        TextView title_name = (TextView)item_view.findViewById(R.id.item_name);
        title_name.setText("品項");
        TextView title_date = (TextView)item_view.findViewById(R.id.item_date);
        title_date.setText("時間");
        TextView title_kind = (TextView)item_view.findViewById(R.id.item_kind);
        title_kind.setText("種類");
        TextView title_cost = (TextView)item_view.findViewById(R.id.item_cost);
        title_cost.setText("金額");

        search_list = (ListView)findViewById(R.id.search_list);

        //從資料庫抓清單
        c = _helper.getReadableDatabase()
                .query(Item.DATABASE_TABLE, null, null, null, null, null, null);
        adapter = new SimpleCursorAdapter(this,
                R.layout.list_style, c,
                new String[]{"name", "date", "kind", "cost"},
                new int[]{R.id.item_name, R.id.item_date, R.id.item_kind, R.id.item_cost}, 1);
        search_list.setAdapter(adapter);

        //=============tabHost===================
        tabHost = (TabHost)findViewById(R.id.tabhost);
        tabHost.setup();

        addTabHost("tag1", "支出", R.drawable.out, R.id.tab1);
        addTabHost("tag2", "查詢", R.drawable.find, R.id.tab2);
        addTabHost("tag3", "統計", R.drawable.count, R.id.tab3);
        addTabHost("tag4", "吃啥", R.drawable.eatwhat, R.id.tab4);

//        TabHost.TabSpec spec = tabHost.newTabSpec("tag1");
//        spec.setContent(R.id.tab1);
//        spec.setIndicator("支出");
//        tabHost.addTab(spec);
//
//        spec = tabHost.newTabSpec("tag2");
//        spec.setContent(R.id.tab2);
//        spec.setIndicator("餐點查詢");
//        tabHost.addTab(spec);
//
//        spec = tabHost.newTabSpec("tag3");
//        spec.setContent(R.id.tab3);
//        spec.setIndicator("開銷統計");
//        tabHost.addTab(spec);
//
//        spec = tabHost.newTabSpec("tag4");
//        spec.setContent(R.id.tab4);
//        spec.setIndicator("問問神奇海螺");
//        tabHost.addTab(spec);
        //==================================

        //種類(kind)=========spinner===============================
        ArrayAdapter<CharSequence> foodList = ArrayAdapter.createFromResource(MainActivity.this,
                R.array.food,
                android.R.layout.simple_spinner_dropdown_item);
        record_spinner.setAdapter(foodList);
        //============================================================

        //日期(date)==================DatePicker======================
        btn_record_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog dialog = new DatePickerDialog(MainActivity.this,dateSetListener,
                        myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                input_date = year + "/" + (monthOfYear + 1) + "/" + dayOfMonth;
                btn_record_date.setText(input_date);
            }
        };
        //=============================================================

        //儲存鈕=======================================================
        btn_record_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取得資料
                String input_name = record_name.getText().toString();
                String input_cost = record_$$.getText().toString();
                String input_kind = getResources().getStringArray(R.array.food)[record_spinner.getSelectedItemPosition()];

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
                        .query(Item.DATABASE_TABLE, null, null, null, null, null, null);
                adapter.changeCursor(c);
            }
        });
        //==================================================================

        //=====================查詢=========================================
        btn_search_go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(search_name.getText()!=null){
                    String name = search_name.getText().toString().trim();
                }else{
                    Toast.makeText(MainActivity.this, "名稱不得為空", Toast.LENGTH_SHORT).show();
                }
            }
        });
        //==================================================================

        //====================預算==========================================
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String s) {
                if(s.equals("tab3")){

                }
            }
        });

        btn_count_$change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View budget = LayoutInflater.from(MainActivity.this).inflate(R.layout.budget_change, null);
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("修改預算")
                        .setView(budget)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                EditText edt_budget = (EditText)findViewById(R.id.edt_budget);
                            }
                        }).show();
            }
        });

    }

    public void addTabHost(String label, String title, int iconId, int contentId){
        View tab = LayoutInflater.from(this).inflate(R.layout.custom_tab, null);
        ImageView image = (ImageView) tab.findViewById(R.id.icon);
        TextView text = (TextView) tab.findViewById(R.id.text);
        text.setText(title);
        image.setImageResource(iconId);
        TabHost.TabSpec spec = tabHost.newTabSpec(label).setIndicator(tab).setContent(contentId);
        tabHost.addTab(spec);
    }

}

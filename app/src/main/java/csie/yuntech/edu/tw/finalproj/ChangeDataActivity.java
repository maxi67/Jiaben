package csie.yuntech.edu.tw.finalproj;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.Calendar;

import static csie.yuntech.edu.tw.finalproj.R.array.food;

public class ChangeDataActivity extends AppCompatActivity {

    private DBHelper myHelper;
    private Cursor c;
    Spinner change_spinner;
    Button btn_date, btn_save, btn_cancel;
    EditText change_name, change_cost;
    Calendar myCalendar;
    DatePickerDialog.OnDateSetListener dateSetListener;

    String input_date = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_data);

        //取得傳遞過來的id(要修改的資料)
        Intent intent = ChangeDataActivity.this.getIntent();
        final int _id = intent.getIntExtra("id", 0);

        myHelper = DBHelper.getInstance(this); //資料庫物件初始化
        myCalendar = Calendar.getInstance();  //取得手機當前日期

        change_spinner = (Spinner)findViewById(R.id.change_spinner);  //(kind)
        btn_date = (Button)findViewById(R.id.change_date); //(date)
        btn_save = (Button)findViewById(R.id.change_save);
        btn_cancel= (Button)findViewById(R.id.change_cancel);
        change_name = (EditText)findViewById(R.id.change_name); //(name)
        change_cost = (EditText)findViewById(R.id.change_cost); //(cost)

        //下拉式選單[種類(kind)]=========Spinner===============================
        ArrayAdapter<CharSequence> foodList = ArrayAdapter.createFromResource(ChangeDataActivity.this,
                food,
                android.R.layout.simple_spinner_dropdown_item);
        change_spinner.setAdapter(foodList);
        //============================================================

        //設定初值=================================================================
        c = myHelper.getReadableDatabase()
                .query(Item.DATABASE_TABLE, null, Item.KEY_ID + "= " + _id, null, null, null, null);
        if(c != null)
        {
            c.moveToFirst();
            change_name.setText(c.getString(1));
            btn_date.setText(c.getString(2));
            input_date = c.getString(2);
            change_cost.setText(c.getString(4));
            for(int i = 0; i < 5; i++)
                if(getResources().getStringArray(food)[i].equals(c.getString(3))) {
                    change_spinner.setSelection(i);
                    break;
                }
        }
        //==========================================================================

        //按鈕、日期選擇[日期(date)]==================DatePicker======================
        btn_date.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //    myCalendar = Calendar.getInstance();  //取得手機當前日期
                int year = Integer.valueOf(c.getString(2).substring(0, 4));
                int month = Integer.valueOf(c.getString(2).substring(5, 7));
                int day = Integer.valueOf(c.getString(2).substring(8, 10));
                DatePickerDialog dialog = new DatePickerDialog(ChangeDataActivity.this, dateSetListener,
                        year, month - 1, day);
                dialog.show();
            }
        });

        dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int monthOfYear, int dayOfMonth) {
                input_date = String.format("%04d/%02d/%02d", year, monthOfYear + 1, dayOfMonth);
                btn_date.setText(input_date); //將選取結果show在按鈕上
            }
        };
        //=============================================================

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取得資料
                String input_name = change_name.getText().toString();
                String input_cost = change_cost.getText().toString();
                String input_kind = getResources().getStringArray(food)[change_spinner.getSelectedItemPosition()];

                if(input_name.length() == 0){ //檢查資料(防止空值)
                    Toast.makeText(ChangeDataActivity.this, "品項名稱不得為空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(input_cost.length() == 0){ //檢查資料(防止空值)
                    Toast.makeText(ChangeDataActivity.this, "金額不得為空", Toast.LENGTH_SHORT).show();
                    return;
                }
                if(input_date.length() == 0){ //檢查資料(防止空值)
                    Toast.makeText(ChangeDataActivity.this, "未選擇日期", Toast.LENGTH_SHORT).show();
                    return;
                }

                //Update Database
                ContentValues values = new ContentValues();
                values.put(Item.KEY_ID, _id);
                values.put(Item.KEY_NAME, input_name);
                values.put(Item.KEY_DATE, input_date);
                values.put(Item.KEY_KIND, input_kind);
                values.put(Item.KEY_COST, input_cost);
                myHelper.getWritableDatabase().update(Item.DATABASE_TABLE, values, Item.KEY_ID + "=" + _id, null);

                ChangeDataActivity.this.finish(); //返回原Activity
            }
        });

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChangeDataActivity.this.finish(); //返回原Activity
            }
        });
    }
}

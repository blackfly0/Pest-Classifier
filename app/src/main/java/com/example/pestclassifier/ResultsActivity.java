package com.example.pestclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class ResultsActivity extends AppCompatActivity {
    private static final String TAG = "ResultsActivity";
    private TableLayout tableLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);
        tableLayout = findViewById(R.id.tableLayout);

        Intent intent = getIntent();
        String results = intent.getStringExtra("RESULTS");
        try {
            JSONObject obj = new JSONObject(results);
            JSONArray arr = obj.getJSONArray("predictions");
            setTableView(arr);
        } catch (Throwable t) {
            Log.e("My App", "Could not parse malformed JSON");
        }
    }

    private void setTableView(JSONArray arrObj) {
        for(int i = 0; i < arrObj.length(); i++){

            try {
                JSONArray temp = arrObj.getJSONArray(i);
                String className = (String)temp.get(0);
                Double score = (Double)temp.get(1);
                addRows(Integer.toString(i), className, Double.toString(score));
                //Log.d(TAG, "********* result:" + className + Double.toString(score) );
            } catch (Throwable t) {
                Log.e("My App", "Could not parse malformed JSON");
            }
        }
    }

    private void addRows(String index, String className, String score){
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

//        TextView textview1 = new TextView(this);
//        textview1.setText(index);
//        textview1.setTextColor(Color.BLACK);
        TextView textview2 = new TextView(this);
        textview2.setText(className);
        textview2.setTextColor(Color.BLACK);
        TextView textview3 = new TextView(this);
        textview3.setText(score);
        textview3.setTextColor(Color.BLACK);

//        row.addView(textview1);
        row.addView(textview2);
        row.addView(textview3);
        tableLayout.addView(row, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
    }
}
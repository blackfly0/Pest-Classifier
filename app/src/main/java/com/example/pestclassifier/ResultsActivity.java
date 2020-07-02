package com.example.pestclassifier;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class ResultsActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        tableLayout = findViewById(R.id.tableLayout);
        addRows();
    }

    private void addRows(){
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));

        TextView textview1 = new TextView(this);
        textview1.setText("1st col");
        textview1.setTextColor(Color.BLACK);
        TextView textview2 = new TextView(this);
        textview2.setText("2st col");
        textview2.setTextColor(Color.BLACK);
        TextView textview3 = new TextView(this);
        textview3.setText("3st col");
        textview3.setTextColor(Color.BLACK);

        row.addView(textview1);
        row.addView(textview2);
        row.addView(textview3);
        tableLayout.addView(row, new TableLayout.LayoutParams(TableLayout.LayoutParams.FILL_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
    }
}
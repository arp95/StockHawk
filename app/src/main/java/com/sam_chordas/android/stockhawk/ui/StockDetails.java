package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.sam_chordas.android.stockhawk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Calendar;

public class StockDetails extends AppCompatActivity {

    String StartDate = null ,EndDate = null , symbol = null , url = null;
    TextView textView;
    ArrayList<String> dates;
    ArrayList<String> values;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_graph);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(android.support.v7.app.ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.stock_details));
        actionBar.setDisplayHomeAsUpEnabled(true);


        // taking the start and the end dates to fetch the detail of the stock
        dates = new ArrayList<String>();
        values = new ArrayList<String>();
        textView = (TextView) findViewById(R.id.text);
        Date currentDate = Calendar.getInstance().getTime();
        java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US);
        EndDate = simpleDateFormat.format(currentDate);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        StartDate = simpleDateFormat.format(cal.getTime());

        textView.setText(getResources().getString(R.string.text_view));
        Intent intent = getIntent();
        symbol = intent.getStringExtra(getResources().getString(R.string.symbol));
        url = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20yahoo.finance.historicaldata%20where%20symbol%20%3D%20%22" + symbol + "%22%20and%20startDate%20%3D%20%22" + StartDate + "%22%20and%20endDate%20%3D%20%22" + EndDate + "%22&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=";

        ConnectivityManager conn = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
            new FetchDetailTask().execute(url);
        } else
            Toast.makeText(this, getResources().getString(R.string.no_internet), Toast.LENGTH_LONG).show();
    }

    //performing the action to fetch json string from the api through a seperate thread
    public class FetchDetailTask extends AsyncTask<String, Void, String> {
        private final String LOG_TAG = FetchDetailTask.class.getSimpleName();
        //performs the required action to fetch json string
        @Override
        protected String doInBackground(String... params) {
            try {
                return downloadUrl(params[0]);
            } catch (IOException e) {
                return "invalid !";
            }
        }
        //fetches json string from the inputBuffer
        // this is where you get the json string and do whatever you want to do over here to fetch the info .
        @Override
        protected void onPostExecute(String s) {
            if (s != null) {
                textView.setText(s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONObject jsonObject1 = jsonObject.getJSONObject("query");
                    JSONObject jsonObject2 = jsonObject1.getJSONObject("results");
                    JSONArray jsonObject3 = jsonObject2.getJSONArray("quote");

                    for (int i = 0; i < jsonObject3.length(); i++) {
                        JSONObject jsonObject4 = jsonObject3.getJSONObject(i);
                        String date = jsonObject4.getString("Date");
                        String value = jsonObject4.getString("Adj_Close");

                        dates.add(date);
                        values.add(value);
                    }

                    // drawing the required chart from the values given

                    ArrayList<BarEntry> entries = new ArrayList<BarEntry>();
                    ArrayList<String> labels = new ArrayList<String>();

                    for(int i=0;i<values.size();i++)
                    {
                        entries.add(new BarEntry(Float.parseFloat(values.get(i)),i));
                    }
                    BarDataSet barDataSet = new BarDataSet(entries , getResources().getString(R.string.stock_description));

                    for(int i=0;i<dates.size();i++)
                    {
                        labels.add(dates.get(i));
                    }

                    BarChart barChart = new BarChart(getApplicationContext());
                    setContentView(barChart);

                    BarData barData = new BarData(labels , barDataSet);
                    barChart.setData(barData);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        String result = null;
        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.connect();
            int response = conn.getResponseCode();
            is = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            result = sb.toString();
        } catch (Exception e) {
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception squish) {
            }
        }
        return result;
    }
}

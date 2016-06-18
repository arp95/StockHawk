package com.sam_chordas.android.stockhawk.rest;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by sam_chordas on 10/8/15.
 */
public class Utils {

    private static String LOG_TAG = Utils.class.getSimpleName();
    public static boolean showPercent = true;

    //getting content from the JSON data
    public static ArrayList quoteJsonToContentVals(Context context , String JSON){

        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject = null;
        JSONArray resultsArray = null;
        try{

            //creating json object rom the string
            jsonObject = new JSONObject(JSON);
            if (jsonObject != null && jsonObject.length() != 0){
                jsonObject = jsonObject.getJSONObject("query");
                int count = Integer.parseInt(jsonObject.getString("count"));
                if (count == 1){
                    jsonObject = jsonObject.getJSONObject("results").getJSONObject("quote");

                    //checking if any key has null value , if yes dont add to the db
                    if(!jsonObject.isNull("Change") && !jsonObject.isNull("symbol") && !jsonObject.isNull("Bid") && !jsonObject.isNull("ChangeinPercent"))
                        batchOperations.add(buildBatchOperation(jsonObject));
                    else
                        sendBroadcast(context);
                } else{
                    resultsArray = jsonObject.getJSONObject("results").getJSONArray("quote");
                    if (resultsArray != null && resultsArray.length() != 0){
                        for (int i = 0; i < resultsArray.length(); i++){

                            jsonObject = resultsArray.getJSONObject(i);

                            //checking if any key has null value , if yes dont add to the db
                            if(!jsonObject.isNull("Change") && !jsonObject.isNull("symbol") && !jsonObject.isNull("Bid") && !jsonObject.isNull("ChangeinPercent"))
                                batchOperations.add(buildBatchOperation(jsonObject));
                            else
                                sendBroadcast(context);
                        }
                    }
                }
            }
        } catch (JSONException e){
        }
        return batchOperations;
    }

    public static void sendBroadcast(Context context)
    {
        Intent intent = new Intent("messageToBeSent");
        intent.putExtra("message" , context.getResources().getString(R.string.invalid_Stock));
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    //getting the bid price from the string
    public static String truncateBidPrice(String bidPrice){

        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    //getting round value from the string
    public static String truncateChange(String change, boolean isPercentChange){

        String weight = change.substring(0,1);
        String ampersand = "";
        if (isPercentChange){
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round =0.00;
        round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    //getting the required data from the JSON object
    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject){

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
        QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }
        }catch (JSONException e){
            e.printStackTrace();
        }
        return builder.build();
    }

    //checking for the network
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo availableNetwork = cm.getActiveNetworkInfo();
        return availableNetwork != null && availableNetwork.isConnectedOrConnecting();
    }

    //getting the date from the given string
    public static String convertDate(String inputDate){

        StringBuilder outputFormattedDate = new StringBuilder();
        outputFormattedDate.append(inputDate.substring(6))
                            .append("/")
                            .append(inputDate.substring(4, 6))
                            .append("/")
                            .append(inputDate.substring(2, 4));

        return outputFormattedDate.toString();
    }
}

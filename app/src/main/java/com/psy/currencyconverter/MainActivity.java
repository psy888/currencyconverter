package com.psy.currencyconverter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.zip.DataFormatException;

public class MainActivity extends AppCompatActivity {

    //ТИПЫ КОНВЕРТАЦИИ
    //UAH->USD = UAH sum/ USD curs (100/26.8) !!!!(uah curse/usd curse)*sum
    //UAH->EURO = UAH sum/ EURO curs
    //EURO->UAH = EURO sum * EURO curs
    //USD->UAH = rate_USD sum * USD curs
    //EURO->USD = (rate_EURO sum * rate_EURO sell.curs) / rate_USD curs
    //USD-> EURO = (rate_USD sum * rate_USD sell.curs) / Euro curs
/*
    private static final int CODE_USD = 100;
    private static final int CODE_EURO = 200;
    private static final int CODE_UAH = 300;

    private static final int TYPE_NBU = 0;
    private static final int TYPE_BUY = 1;
    private static final int TYPE_SELL = 2;
*/



    EditText etInputSum;
    TextView tvResult;
    Spinner spConvertFrom;
    Spinner spConvertTo;
    RadioGroup mRadioGroup;
    RadioButton rNbu;
    RadioButton rBuy;
    RadioButton rSell;
    EditText etExchangeRate;
    Button btnConvert;

    ArrayAdapter<Currency> mAdapter;
    Currency mFrom;
    Currency mTo;
    int currencyType;

    Currency[] currencies;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(savedInstanceState==null) {
            Currency UAH = new Currency(getResources().getString(R.string.spinner_item_uah), 1, 1, 1);
            Currency USD = new Currency(getResources().getString(R.string.spinner_item_usd), 26.76, 26.8, 26.95);
            Currency EURO = new Currency(getResources().getString(R.string.spinner_item_eur), 30.32, 30.35, 33.55);
            currencies = new Currency[]{UAH, USD, EURO};
        }else{
            currencies = (Currency[]) savedInstanceState.getSerializable("currencies");
        }
        mFrom = currencies[0];
        mTo = currencies[1];
        //Restore state
        if(savedInstanceState != null)
        {
//            rate_USD = savedInstanceState.getDoubleArray("rate_USD");
//            rate_EURO = savedInstanceState.getDoubleArray("rate_EURO");
        }
        setContentView(R.layout.activity_main);

        etInputSum = findViewById(R.id.input_sum);
        tvResult = findViewById(R.id.output_result);
        if(savedInstanceState!=null)
        {
            tvResult.setText(savedInstanceState.getString("resultTvVal",""));
        }





        etExchangeRate = findViewById(R.id.exchangeRate);


        //---------------SPINNERS------------------------------
        //Find spinner
        spConvertFrom = findViewById(R.id.spConvertFrom);
        spConvertTo = findViewById(R.id.spConvertTo);
        //Create adapter
        mAdapter = new ArrayAdapter(this,R.layout.spinner_item,currencies);
        mAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Set adapter
        spConvertFrom.setAdapter(mAdapter);
        spConvertTo.setAdapter(mAdapter);
        spConvertTo.setSelection(1);
        //Create instance of listener
        AdapterView.OnItemSelectedListener spinnerListener = new SpinnerListener();
        //Set listener
        spConvertFrom.setOnItemSelectedListener(spinnerListener);
        spConvertTo.setOnItemSelectedListener(spinnerListener);

        //--------------Radio buttons-------------------
        mRadioGroup = findViewById(R.id.rgType);
        //find Radio buttons
        rNbu = findViewById(R.id.rNbu);
        rNbu.setChecked(true);
        if(savedInstanceState!=null)
        {
            int rbId = savedInstanceState.getInt("checkedRb");
            RadioButton rb = findViewById(rbId);
            rb.setChecked(true);
        }
        rBuy = findViewById(R.id.rBuy);
        rSell = findViewById(R.id.rSell);

        //Create instance of listener
        CompoundButton.OnCheckedChangeListener radioBListener = new RadioBListener();
        //set Listener
        rNbu.setOnCheckedChangeListener(radioBListener);
        rBuy.setOnCheckedChangeListener(radioBListener);
        rSell.setOnCheckedChangeListener(radioBListener);


        RadioGroup.LayoutParams rglp = new RadioGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rglp.setMarginStart(getResources().getDisplayMetrics().widthPixels/3);
        rNbu.setLayoutParams(rglp);
        rBuy.setLayoutParams(rglp);
        rSell.setLayoutParams(rglp);

        // ----------- Exchange Rate  EditView -----------------

        etExchangeRate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                try {
                    String str = v.getText().toString();
                    str = str.replaceAll(",",".");

                    double newRate = Double.parseDouble(str);
                    if(mFrom.getRate(currencyType)>1&&mTo.getRate(currencyType)>1) {
                        Toast.makeText(MainActivity.this,R.string.wrong_currency,Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    else if(mFrom.getRate(currencyType)==1)
                    {
                        mTo.setRate(currencyType,newRate);
                    }
                    else if(mTo.getRate(currencyType)==1)
                    {
                        mFrom.setRate(currencyType,newRate);
                    }
                }catch (NumberFormatException e){
                    Log.e("ERROR", e.getMessage());
                    Toast.makeText(MainActivity.this, R.string.wrong_input,Toast.LENGTH_SHORT).show();
                    return false;
                }finally {
                    updateRateUI();
                }

                return false;
            }
        });

        //---------------BTN---------------------
        //find btn
        btnConvert = findViewById(R.id.btnConvert);
        //Set listener
        btnConvert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                double inputSum = (!etInputSum.getText().toString().isEmpty())?Double.parseDouble(etInputSum.getText().toString()):0;//??
                double result = mFrom.getRate(currencyType)/mTo.getRate(currencyType)*inputSum;
                tvResult.setText(String.format("%4.2f" , result));
            }
        });

    }

    void updateRateUI()
    {
        String str = "";
        if(mFrom.getRate(currencyType)>1) { //mFrom NOT UAH
//            str = String.valueOf(mFrom.getRate(currencyType) / mTo.getRate(currencyType));
            double curRate = mFrom.getRate(currencyType) / mTo.getRate(currencyType);
            str = String.format("%4.2f" , curRate);
        }
        else if(mFrom.getRate(currencyType)<mTo.getRate(currencyType))
        {
            str = String.format("%4.2f" , mTo.getRate(currencyType));

        }

        etExchangeRate.setText(str);
    }

    class RadioBListener implements CompoundButton.OnCheckedChangeListener
    {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            int selectedRadioButton = buttonView.getId();
            switch (selectedRadioButton)
            {
                case R.id.rNbu:
                    currencyType = 0;
                    break;
                case R.id.rBuy:
                    currencyType = 1;
                    break;
                case R.id.rSell:
                    currencyType = 2;
                    break;
            }
            updateRateUI();
        }
    }

    /**
     * Spinner Listener
     */
    class SpinnerListener implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            int spinnerId = parent.getId(); //ToDo: fix null pointer exception
//            Log.d("CONVERT____________" , "spinnerId = " + spinnerId + " == " + R.id.spConvertFrom);
            switch (spinnerId)
            {
                case R.id.spConvertFrom:
                    mFrom = mAdapter.getItem(position);
//                    Log.d("CONVERT____________" , String.valueOf(mFrom));
                    break;
                case R.id.spConvertTo:
                    mTo = mAdapter.getItem(position);
//                    Log.d("CONVERT____________" , String.valueOf(mTo));
                    break;
            }
            updateRateUI();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("currencies", currencies);
        outState.putString("resultTvVal", tvResult.getText().toString());
        int rbId = mRadioGroup.getCheckedRadioButtonId();
        outState.putInt("checkedRb", rbId);
    }

}

class Currency
{
    String mName;
    double[] rate = new double[3]; //[0]nbu, [1]buy, [2]sell

    public Currency(String name, double nbu, double buy, double sell)
    {
        mName = name;
        rate[0]= nbu;
        rate[1]= buy;
        rate[2]= sell;
    }

    @Override
    public String toString() {
        return mName;
    }

    double getRate(int i){
        return rate[i];
    }

    void setRate (int i, double val) {rate[i] = val;}


}
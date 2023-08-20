package com.glowingsoft.carplaterecognizer.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;

public class SettingActivity extends AppCompatActivity {

    EditText apitoken,regionCodeEdit;
    Button applychanged;
    String SHARED_PREF_NAME ="user_pref";
    SharedPreferences pref;
    ImageButton imageButton;
    TextView currenttoken, messageText;
    SharedPreferences.Editor editor;
    ConstraintLayout currentlayout,editlayout;
    String token = "";
    //    String token = "ddecd03711e795147f3feb345ec198eff5d957b6";
    String defaultUrl="https://api.platerecognizer.com/v1/plate-reader/";
    String regioncode="";
    CheckBox checkBox;
    EditText urlEdit;
    Spinner resizeImageEdit;
    ArrayAdapter<CharSequence> adapter;

    ImageView backButton, settingsButton, toastIcon;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);



        currentlayout=findViewById(R.id.current_token_layout);
        editlayout=findViewById(R.id.edit_current_token_layout);
        imageButton=findViewById(R.id.setting_edit_btn);
        currenttoken=findViewById(R.id.current_token);
        checkBox=findViewById(R.id.checkBox);
        resizeImageEdit = findViewById(R.id.spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        adapter = ArrayAdapter.createFromResource(SettingActivity.this,
                R.array.size, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        resizeImageEdit.setAdapter(adapter);
        urlEdit=findViewById(R.id.default_url);
        urlEdit.setText(defaultUrl);

        backButton = findViewById(R.id.back_button);
        settingsButton = findViewById(R.id.settings_button);

        settingsButton.setVisibility(View.GONE);


        backButton.setOnClickListener(view-> {
            onBackPressed();
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentlayout.setVisibility(View.GONE);
                editlayout.setVisibility(View.VISIBLE);
            }
        });

        pref =getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE);
        editor = pref.edit();;

        apitoken=findViewById(R.id.token_Code);
        regionCodeEdit=findViewById(R.id.editText2);
        regioncode=pref.getString("RegionCode","");
        regionCodeEdit.setText(regioncode);
        applychanged=findViewById(R.id.button);
        String settoken="";
        String edittedtoken ="",gettoken="";
        edittedtoken=pref.getString("CarToken", "");
        apitoken.setText(edittedtoken);
        if (edittedtoken.isEmpty())
        {
            currenttoken.setText("No Token");
            apitoken.setText(token);
        }
        if (!edittedtoken.isEmpty()) {
            gettoken = apitoken.getText().toString();
            settoken = gettoken.substring(gettoken.length() - 6);
            currenttoken.setText("**********" + settoken);
        }
        applychanged.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (apitoken.equals("") || apitoken.length()<1){
                    apitoken.setError("Add Token");
                }else {
                    String currentToken = apitoken.getText().toString();
                    editor.putString("CarToken", currentToken).apply();
                    String regioncodes =regionCodeEdit.getText().toString();
                    editor.putString("RegionCode",regioncodes).apply();
                    String lastDigits = currentToken.substring(currentToken.length()-6);
                    editor.putString("LastDigits",lastDigits).apply();
                    String reziseString=resizeImageEdit.getSelectedItem().toString();
                    int resizeInt=Integer.parseInt(reziseString);
                    editor.putInt("Resize",resizeInt).apply();
                    String baseurl = urlEdit.getText().toString();
                    editor.putString("BaseUrl",baseurl);

                    // save inputed spinner position to sharedpreferences
                    int userChoice = resizeImageEdit.getSelectedItemPosition();
                    editor.putInt("userChoiceSpinner", userChoice);
                    editor.commit();
                    finish();
                    showToast("Saved Successfully");
                }
                if(checkBox.isChecked()) {
                    editor.putBoolean("checked", true);
                    editor.apply();
                }else{
                    editor.putBoolean("checked", false);
                    editor.apply();
                }
            }
        });
    }

    private void showToast(String message) {
        // inflate the custom toast layout
        View toastView = getLayoutInflater().inflate(R.layout.custom_toast_layout, null);

        // set the message text
        messageText = toastView.findViewById(R.id.toast_text);
        messageText.setText(message);

        // set the app icon
        toastIcon = toastView.findViewById(R.id.toast_icon);
        toastIcon.setImageResource(R.drawable.app_logo);

        // create the toast and set its custom layout
        Toast toast = new Toast(SettingActivity.this);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(toastView);

        // set the toast position
        toast.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 30);

        // show the toast
        toast.show();

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(pref.contains("checked") && pref.getBoolean("checked",false) == true) {
            checkBox.setChecked(true);
        }else {
            checkBox.setChecked(false);
        }
        String baseurl=pref.getString("BaseUrl","https://api.platerecognizer.com/v1/plate-reader/");
        urlEdit.setText(baseurl);

        int spinnerValue = pref.getInt("userChoiceSpinner",-1);
        if(spinnerValue != -1)
            // set the value of the spinner
            resizeImageEdit.setSelection(spinnerValue);
    }
}
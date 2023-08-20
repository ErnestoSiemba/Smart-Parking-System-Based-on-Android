package com.glowingsoft.carplaterecognizer.ui;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import cz.msebera.android.httpclient.Header;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlots;
import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.api.WebRequest;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.enums.EPickType;
import com.vansuita.pickimage.listeners.IPickResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity  implements IPickResult,View.OnClickListener {
    ImageView imageView,emptyImage, toastIcon, backImageView, settingsImageView, bouncingCircleImage;
    TextView plate_txt,region_txt,vihical_txt, resultsText, messageText, simulationText, carNumberPlateText, carOwnerText, gateStatus;
    LinearLayout simulationLayout;
    Context context;
    ImageButton editResult;
    Button nextImage, secureExitButton;
    ProgressBar progressBar;
    SharedPreferences sharedPreferences;
    String SHARED_PREF_NAME ="user_pref";
    String token = "";
    String countrycode="";
    Date date;
    DateFormat df;
    String plate_type="",region_type="",car_type="",last_digits="",timeStamp="";
    CardView plateCard,regionCard,vihicalCard;
    String imagepath;
    Button loginButton;
    FirebaseDatabase firebaseDatabase;
    FirebaseAuth mAuth;

    BasicUtils utils = new BasicUtils();

    // Authentication
    private BiometricManager biometricManager;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!utils.isNetworkAvailable(getApplication())) {
            showToast("No network available. Turn on mobile data or WIFI");
        }

        context=this;
        setContentView(R.layout.activity_main);
        sharedPreferences=getSharedPreferences(SHARED_PREF_NAME,MODE_PRIVATE);
        date = new Date();
        df = new SimpleDateFormat("MM/dd/");
        // Use London time zone to format the date in
        df.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        nextImage=findViewById(R.id.next_image);
        nextImage.setOnClickListener(this);
        progressBar=findViewById(R.id.homeprogress);
        plate_txt = findViewById(R.id.car_plate);
        vihical_txt = findViewById(R.id.vihicle_type);
        emptyImage=findViewById(R.id.empty_image);
        plateCard=findViewById(R.id.cardView);
        vihicalCard=findViewById(R.id.cardView2);
        editResult=findViewById(R.id.setting_edit_btn);
        imageView = findViewById(R.id.imageView);
        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        simulationText = findViewById(R.id.simulation_text);
        simulationLayout = findViewById(R.id.simulation_layout);
        carNumberPlateText = findViewById(R.id.car_number_plate_text);
        carOwnerText = findViewById(R.id.car_owner_text);
        gateStatus = findViewById(R.id.gate_status_text);
        bouncingCircleImage = findViewById(R.id.bouncing_circle_image);
        editResult.setOnClickListener(this);
        imageView.setOnClickListener(this);
        backImageView.setOnClickListener(this);
        settingsImageView.setOnClickListener(this);
        settingsImageView.setVisibility(View.GONE);



        firebaseDatabase = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();

        resultsText = findViewById(R.id.results_text);

//        settingsImageView.setVisibility(View.GONE);


        secureExitButton = findViewById(R.id.secure_exit_button);
        secureExitButton.setOnClickListener(this);


        loginButton = findViewById(R.id.login_button);
        loginButton.setOnClickListener(this);

        try {
            // Check that Biometric Authentication is available
            biometricManager = BiometricManager.from(this);
            switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
                case BiometricManager.BIOMETRIC_SUCCESS:
                    Log.d("MY_APP_TAG", "App can authenticate using biometrics.");
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                    Log.e("MY_APP_TAG", "No biometric features available on this device.");
                    break;
                case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                    Log.e("MY_APP_TAG", "Biometric features are currently unavailable.");
                    break;
                case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                    // Prompts the user to create credentials that your app accepts.
                    final Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                    enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
                    startActivityForResult(enrollIntent, 1);
                    break;
            }

        } catch(Exception ex) {
            showToast("Biometrics are not supported on this device");
        }





        // Setting up biometric authentication
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(MainActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                showToast("Authentication Error");
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                // Launch the activity you want to execute
                Intent restrictedIntent = new Intent(MainActivity.this, Restricted.class);
                startActivity(restrictedIntent);
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                showToast("Authentication Succeeded!");
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed",
                                Toast.LENGTH_SHORT)
                        .show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm Access to Restricted Activity")
                .setSubtitle("Confirm exit with your biometric credentials")
                .setNegativeButtonText("Use account password")
                .build();


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
        Toast toast = new Toast(MainActivity.this);
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
        finish();
    }

    //dialog box setup
    @SuppressLint("WrongConstant")
    PickSetup setup = new PickSetup()
            .setTitle("Choose")
            .setCancelText("Cancel")
            .setFlip(true)
            .setMaxSize(50)
            .setWidth(50)
            .setHeight(50)
            .setProgressText("Loading Image")
            .setPickTypes(EPickType.GALLERY, EPickType.CAMERA)
            .setCameraButtonText("Camera")
            .setGalleryButtonText("Gallery")
            .setIconGravity(Gravity.TOP)
            .setButtonOrientation(LinearLayoutCompat.HORIZONTAL)
            .setSystemDialog(false)
            .setGalleryIcon(R.drawable.photo)
            .setCameraIcon(R.drawable.cam);

    //to change token value
    @Override
    protected void onResume() {
        super.onResume();
//        String editvisibility=plate_txt.getText().toString();
        if( sharedPreferences.contains("checked") && sharedPreferences.getBoolean("checked", false)) {
            editResult.setVisibility(View.VISIBLE);
        }
        else {
            editResult.setVisibility(View.GONE);
        }
        last_digits=sharedPreferences.getString("LastDigits", "");
//        Toast.makeText(MainActivity.this, last_digits, Toast.LENGTH_SHORT).show();
        token=sharedPreferences.getString("CarToken", "");
        if (token.equals("")){
            showToast("Token Not Found");
        }else {
            WebRequest.client.addHeader("Authorization","Token "+token);
        }
    }
    //pick result method to get image after getting image form gallary or camera
    @Override
    public void onPickResult(PickResult r) {
        if (r.getError() == null) {
            RequestParams params=new RequestParams();
            String file=r.getPath();
            String compressed=compressImage(file);
            countrycode=sharedPreferences.getString("RegionCode","");
            String baseurl=sharedPreferences.getString("BaseUrl","https://api.platerecognizer.com/v1/plate-reader/");


            Log.d("response", "filepath: "+file+" ");
            try {
                params.put("upload", new File(compressed));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            params.put("regions",countrycode);
            Log.d("response", "image to upload: "+params+" ");
            WebRequest.post(context,baseurl,params,new JsonHttpResponseHandler()
            {
                @Override
                public void onStart() {
                    progressBar.setVisibility(View.VISIBLE);
                    plate_txt.setText(null);
                    vihical_txt.setText(null);
                    imageView.setImageResource(R.drawable.upload_icon);

                    Log.d("response", "onStart: ");
                    super.onStart();
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    super.onSuccess(statusCode, headers, response);

                    Log.d("response ",response.toString()+" ");
                    try {
                        //image path
                        imagepath="https://us-east-1.linodeobjects.com/platerec-api/uploads/"+df.format(date)+response.getString("filename");
                        //json array or results
                        JSONArray Jsresults = response.getJSONArray("results");
                        if (Jsresults.length()>0)
                        {
                            for (int i = 0; i < Jsresults.length(); i++) {
                                JSONObject tabObj = Jsresults.getJSONObject(i);
                                String numberPlate = tabObj.getString("plate");

                                // Extract the first three characters
                                String firstThreeCharacters = numberPlate.substring(0, 3);

                                // Extract the remaining characters
                                String remainingCharacters = numberPlate.substring(3);

                                // Format the number plate with a space
                                String formattedNumberPlate = firstThreeCharacters + " " + remainingCharacters;

                                plate_txt.setText(formattedNumberPlate);
                                vihical_txt.setText(tabObj.getJSONObject("vehicle").getString("type"));
                                timeStamp = response.getString("timestamp");

                                // Rest of your code...
                                Picasso.with(context)
                                        .load(imagepath)
                                        .into(imageView, new Callback() {
                                            @Override
                                            public void onSuccess() {
                                                progressBar.setVisibility(View.GONE);
                                            }
                                            @Override
                                            public void onError() {

                                            }
                                        });
                                plateCard.setVisibility(View.VISIBLE);
                                vihicalCard.setVisibility(View.VISIBLE);
                                nextImage.setVisibility(View.VISIBLE);
                                simulationText.setVisibility(View.VISIBLE);
                                simulationLayout.setVisibility(View.VISIBLE);
                                emptyImage.setVisibility(View.GONE);
                                resultsText.setVisibility(View.GONE);
//                                showToast(plate_txt.getText().toString().toUpperCase());


                                firebaseDatabase.getReference()
                                        .child("BookedSlots")
                                        .orderByChild("numberPlate")
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                BookedSlots bookedSlots;
                                                String actualPlate;
                                                String userId;
                                                String targetNumberPlate;
                                                String plate;


                                                // iterate over the number plates to get the first one
                                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
//                                                    if (snapshot.getChildrenCount() > 1) {
//                                                        showToast("The table has more than one record.");
//                                                    } else if (snapshot.getChildrenCount() == 1) {
//                                                        showToast("The table has exactly one record");
//                                                    }
                                                    bookedSlots = dataSnapshot.getValue(BookedSlots.class);
                                                    userId = bookedSlots.getUserID();
                                                    actualPlate = bookedSlots.getNumberPlate();
                                                    targetNumberPlate = formattedNumberPlate.toUpperCase();
                                                    plate = actualPlate.trim();

                                                    if (dataSnapshot.exists()) {
                                                        // check if the number plates are the same
                                                        if (snapshot.getChildrenCount() >= 1) {
                                                            boolean newPlate = plate.equals(targetNumberPlate);
                                                            if (newPlate && bookedSlots.hasPaid == 1) {
                                                                showToast("Number Plates Match and the Driver Paid");
                                                                carNumberPlateText.setText("Number Plate Is: " + plate);
                                                                carOwnerText.setText("User ID Is: " + userId);
                                                                gateStatus.setText("THE GATE IS OPEN");
                                                                bouncingCircleImage.setVisibility(View.VISIBLE);
                                                                bouncingCircleImage.setColorFilter(ContextCompat.getColor(MainActivity.this, R.color.default_color));
                                                            } else if (newPlate && bookedSlots.hasPaid == 0) {
                                                                showToast("Associated Driver of this number plate has not yet paid!");
                                                                carNumberPlateText.setText("Number Plate Is: " + plate);
                                                                carOwnerText.setText("User ID Is: " + userId);
                                                                gateStatus.setText("THE GATE WILL REMAIN CLOSED");
                                                                bouncingCircleImage.setVisibility(View.VISIBLE);
                                                                bouncingCircleImage.setColorFilter(Color.RED);

                                                            } else {
                                                                showToast("Driver has not yet paid for parking");
                                                                gateStatus.setText("THE GATE WILL REMAIN CLOSED");
                                                                carOwnerText.setText("Associated user is NULL");
                                                                carNumberPlateText.setText("Number Plate Mismatch");
                                                                gateStatus.setText("THE GATE WILL REMAIN CLOSED");
                                                                bouncingCircleImage.setVisibility(View.VISIBLE);
                                                                bouncingCircleImage.setColorFilter(Color.RED);
                                                            }

                                                        } else if (snapshot.getChildrenCount() == 0) {
                                                            showToast("No number plates have booked yet");
                                                        } else {
                                                            showToast("Number Plates do not Match");
                                                            carOwnerText.setText("Associated user is NULL");
                                                            carNumberPlateText.setText("Number Plate Mismatch");
                                                            gateStatus.setText("THE GATE WILL REMAIN CLOSED");
                                                            bouncingCircleImage.setVisibility(View.VISIBLE);
                                                            bouncingCircleImage.setColorFilter(Color.RED);

                                                        }
                                                    } else {
                                                        showToast("Record Does not Exist");
                                                    }


                                                }

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });





                            }


                        }


                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.d("response1", "onFailure: " + errorResponse + " ");
                    progressBar.setVisibility(View.GONE);
                    editResult.setVisibility(View.GONE);
                    plateCard.setVisibility(View.GONE);
                    vihicalCard.setVisibility(View.GONE);
                    nextImage.setVisibility(View.GONE);
                    simulationText.setVisibility(View.GONE);
                    simulationLayout.setVisibility(View.GONE);
                    emptyImage.setVisibility(View.VISIBLE);
                    resultsText.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, errorResponse+"", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                    super.onFailure(statusCode, headers, throwable, errorResponse);
                    Log.d("response2", "onFailure: "+errorResponse+" ");
                    progressBar.setVisibility(View.GONE);
                    editResult.setVisibility(View.GONE);
                    plateCard.setVisibility(View.GONE);
                    vihicalCard.setVisibility(View.GONE);
                    nextImage.setVisibility(View.GONE);
                    simulationText.setVisibility(View.GONE);
                    simulationLayout.setVisibility(View.GONE);
                    emptyImage.setVisibility(View.VISIBLE);
                    resultsText.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this,errorResponse.toString()+"",Toast.LENGTH_LONG).show();
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Log.d("response3", "onFailure: "+responseString+" ");
                    progressBar.setVisibility(View.GONE);
                    editResult.setVisibility(View.GONE);
                    plateCard.setVisibility(View.GONE);
                    vihicalCard.setVisibility(View.GONE);
                    nextImage.setVisibility(View.GONE);
                    simulationText.setVisibility(View.GONE);
                    simulationLayout.setVisibility(View.GONE);
                    emptyImage.setVisibility(View.VISIBLE);
                    resultsText.setVisibility(View.VISIBLE);
                    showToast("No internet connection");
                }
            });
        } else {
            //Handle possible errors
            //TODO: do what you have to do with r.getError();
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        //check if request code is for inserting new list then perform insertion
        if (requestCode == 123 && resultCode == RESULT_OK) {
            String plate = data.getStringExtra("car_plate");
            String region = data.getStringExtra("region_code");
            String car = data.getStringExtra("car_type");
            Log.d("response", "onActivityResult: "+plate+" ");
            plate_txt.setText(plate);
            region_txt.setText(region);
            vihical_txt.setText(car);
            showToast("Results Saved");
        }

    }




    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.settings_button) {
            startActivity(new Intent(MainActivity.this, SettingActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        if (v.getId() == R.id.back_button) {
            onBackPressed();
        }
        if (v.getId() == R.id.login_button) {
//            String passNumberPlate = plate_txt.getText().toString();
//            Intent intent = new Intent(MainActivity.this, Register.class);
//
//            if (!passNumberPlate.isEmpty()) {
//                intent.putExtra("NUMBER PLATE", passNumberPlate.toUpperCase());
//                startActivity(intent);
//            } else {
//                startActivity(intent);
//            }
            startActivity(new Intent(MainActivity.this, Login.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }
        if (v.getId() == R.id.secure_exit_button) {
            // Prompt appears when a user clicks
            biometricPrompt.authenticate(promptInfo);
        }
        if (v.getId()==R.id.imageView)
        {
            if (token.isEmpty()) {
                showToast("Go to Settings to set your Token");
                return;
            }
            PickImageDialog.build(setup).show(MainActivity.this);
        }

        if (v.getId()==R.id.setting_edit_btn)
        {

            plate_type = plate_txt.getText().toString();
            car_type=vihical_txt.getText().toString();
            if (plate_type.isEmpty())
            {
                showToast("Nothing to edit now!");
            }
            else {
                Intent intent = new Intent(MainActivity.this, EditActivity.class);
                intent.putExtra("car_plate", plate_type);
                intent.putExtra("car_type", car_type);
                startActivityForResult(intent, 123);
            }
        }
        if (v.getId()==R.id.next_image)
        {
            PickImageDialog.build(setup).show(MainActivity.this);
        }

    }

    public String compressImage(String filePath) {

        int resized=sharedPreferences.getInt("Resize", -1);

        Bitmap scaledBitmap = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

        float maxHeight =resized*7.0f;
        float maxWidth = resized*12.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

        if (actualHeight > maxHeight || actualWidth > maxWidth)
        {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;

            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;
            }
        }
        options.inSampleSize = calculateInSampleSize(options, actualWidth,
                actualHeight);
        //      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;
        //      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];
        try {
            //          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth,
                    actualHeight,Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }
        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));
        //      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);
            Log.d("EXIF", "Exif: " + orientation);
            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 3) {
                matrix.postRotate(180);
                Log.d("EXIF", "Exif: " + orientation);
            } else if (orientation == 8) {
                matrix.postRotate(270);
                Log.d("EXIF", "Exif: " + orientation);
            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileOutputStream out = null;
        String filename = getFilename(this);
        try {
            out = new FileOutputStream(filename);
            //          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, resized, out);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return filename;
    }

    public static String getFilename(Context context) {
        File file = new File(context.getFilesDir().getPath(), ".Foldername/PlateRecognizerHistory");

        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");

        return uriSting;

    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        final int height = options.outHeight;

        final int width = options.outWidth;

        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int heightRatio = Math.round((float) height/ (float)
                    reqHeight);

            final int widthRatio = Math.round((float) width / (float) reqWidth);

            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

        }       final float totalPixels = width * height;

        final float totalReqPixelsCap = reqWidth * reqHeight * 2;

        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }
        return inSampleSize;
    }


}

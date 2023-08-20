package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlots;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.ui.MainActivity;
import com.glowingsoft.carplaterecognizer.ui.Restricted;
import com.glowingsoft.carplaterecognizer.utils.AppConstants;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.notifications.AlarmUtils;
import com.glowingsoft.carplaterecognizer.utils.notifications.NotificationHelper;
import com.glowingsoft.carplaterecognizer.utils.notifications.NotificationReceiver;
import com.glowingsoft.carplaterecognizer.utils.pdfs.InvoiceGenerator;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hover.sdk.api.Hover;
import com.hover.sdk.api.HoverParameters;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.concurrent.Executor;

import ir.androidexception.andexalertdialog.AndExAlertDialog;
import ir.androidexception.andexalertdialog.AndExAlertDialogListener;

public class BookingDetails extends AppCompatActivity {

    ImageView backImageView, settingsImageView, shareInvoicePdfImageView, toastIcon;
    TextView messageText;
    LinearLayout openInvoicePdfLayout;
    FloatingActionButton fabCheckOutButton, fabPayButton;
    TextView placeText, wheelerText, amountText, checkoutDateText, checkoutTimeText, endDateText, endTimeText, startDateText, startTimeText, numberPlateText;

    SupportMapFragment supportMapFragment;
    GoogleMap gMap;
    LatLng globalLatLng = null;
    MarkerOptions options;

    BookedSlots bookingSlot;
    User userObj;
    NotificationHelper mNotificationHelper;

    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;

    String UUID;
    Boolean run_once = false;

    AppConstants globalClass;
    ParkingArea parkingArea;


    BasicUtils utils = new BasicUtils();


    // Authentication
    private BiometricManager biometricManager;
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_details);

        Hover.initialize(this);
        Hover.setBranding("BRAND NAME", R.drawable.app_logo, BookingDetails.this);

        initComponents();
        attachListeners();

        if (!utils.isNetworkAvailable(getApplication())) {
            showToast("No network available. Turn on mobile data or WIFI");
        }


    }


    private void initComponents() {

        userObj = new User();

        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);

        fabCheckOutButton = findViewById(R.id.checkoutBtn);
        fabPayButton = findViewById(R.id.payBtn);

        // setting the color of icons in the fab
        int color = ContextCompat.getColor(BookingDetails.this, R.color.white);
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        // set the color state list as the tint for the FABs
        fabCheckOutButton.setImageTintList(colorStateList);
        fabPayButton.setImageTintList(colorStateList);


        placeText = findViewById(R.id.placeText);
        numberPlateText = findViewById(R.id.numberPlateText);
        wheelerText = findViewById(R.id.wheelerText);
        startDateText = findViewById(R.id.startDateText);
        startTimeText = findViewById(R.id.startTimeText);
        endDateText = findViewById(R.id.endDateText);
        endTimeText = findViewById(R.id.endTimeText);
        checkoutDateText = findViewById(R.id.checkoutDateText);
        checkoutTimeText = findViewById(R.id.checkoutTimeText);
        amountText = findViewById(R.id.amountText);
        shareInvoicePdfImageView = findViewById(R.id.shareInvoicePdf);
        openInvoicePdfLayout = findViewById(R.id.openInvoicePdf);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        Bundle bundle = getIntent().getExtras();
        UUID = bundle.getString("UUID");


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

        } catch (Exception ex) {
            showToast("Biometrics are not supported on this device");
        }





    }



    private void setupBiometricAuthentication() {
        // Setting up biometric authentication
        executor = ContextCompat.getMainExecutor(getApplicationContext());
        biometricPrompt = new BiometricPrompt(BookingDetails.this,
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
                // perform the checkout logic after successfull authentication
                checkoutData();
                showToast("Authentication Succeeded!");
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                showToast("Authentication failed");
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric confirmation to prove phone ownership")
                .setSubtitle("Confirm checkout with your biometric credentials")
                .setNegativeButtonText("Use account password")
                .build();
    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            String[] sessionTextArr = data.getStringArrayExtra("session_messages");
            String uuid = data.getStringExtra("uuid");
            showToast("Payment Successful");

            fabPayButton.setVisibility(View.GONE);
            fabCheckOutButton.setVisibility(View.VISIBLE);
            bookingSlot.hasPaid = 1;
        } else if (requestCode == 0 && resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Error: " + data.getStringExtra("error"), Toast.LENGTH_LONG).show();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            fabPayButton.setVisibility(View.GONE);
            fabCheckOutButton.setVisibility(View.VISIBLE);
            bookingSlot.hasPaid = 1;
        }

    }

    private void attachListeners() {
        backImageView.setOnClickListener(view->{
            onBackPressed();
        });

        openInvoicePdfLayout.setOnClickListener(view-> {
            InvoiceGenerator invoiceGenerator = new InvoiceGenerator();
            showToast("Open PDF File");
            invoiceGenerator.downloadFile(bookingSlot.userID,UUID,BookingDetails.this,getApplication());
            invoiceGenerator.openFile(BookingDetails.this);
        });

        shareInvoicePdfImageView.setOnClickListener(view-> {
            InvoiceGenerator invoiceGenerator = new InvoiceGenerator();
            showToast("Share PDF File");
            invoiceGenerator.downloadFile(bookingSlot.userID,UUID,BookingDetails.this,getApplication());
            invoiceGenerator.shareFile(BookingDetails.this);
        });

        fabCheckOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(utils.isNetworkAvailable(getApplication())) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(BookingDetails.this);
                    builder.setCancelable(true);
                    builder.setTitle("Confirm Checkout");
                    if(userObj.userType==2)
                        builder.setMessage("Confirm to checkout the user vehicle?");
                    else
                        builder.setMessage("Confirm checkout for this area?");
                    builder.setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                   setupBiometricAuthentication();
                                    // Prompt appears when a user clicks
                                    biometricPrompt.authenticate(promptInfo);

                                }
                            });
                    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }else{
                    showToast("No network available");
                }
            }
        });

        fabPayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(utils.isNetworkAvailable(getApplication())){
                    if(parkingArea.availableSlots>0) {
                        new AndExAlertDialog.Builder(BookingDetails.this)
                                .setTitle("Confirm Payment")
                                .setMessage("Confirm to proceed with payment?")
                                .setPositiveBtnText("Confirm")
                                .setNegativeBtnText("Cancel")
                                .setCancelableOnTouchOutside(true)
                                .OnPositiveClicked(input -> {
                                    firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                                    if (userObj.getUserType() == 2) {
                                        fabPayButton.setVisibility(View.GONE);
                                        fabCheckOutButton.setVisibility(View.GONE);
                                        bookingSlot.hasPaid = 1;
                                        payData();
                                    } else {
                                        // its a driver, pay using hover api
                                        Intent i = new HoverParameters.Builder(BookingDetails.this)
                                                .request("31936be4")
                                                .style(R.style.myHoverTheme)
                                                .buildIntent();
                                        startActivityForResult(i, 0);
                                        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

                                    }

                                })
                                .OnNegativeClicked(new AndExAlertDialogListener() {
                                    @Override
                                    public void OnClick(String input) {

                                    }
                                })
                                .setButtonTextColor(ContextCompat.getColor(BookingDetails.this, R.color.default_color))
                                .setImage(R.drawable.baseline_done_outline_24, 20)
                                .build();
                    }else{
                        showToast("Failed! Slots are full");
                    }
                }else{
                    showToast("No network available");

                }
            }
        });

        firebaseDatabase.getReference().child("BookedSlots").child(UUID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingSlot=snapshot.getValue(BookedSlots.class);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
                startTimeText.setText(simpleDateFormat.format(bookingSlot.startTime));
                endTimeText.setText(simpleDateFormat.format(bookingSlot.endTime));
                checkoutTimeText.setText(simpleDateFormat.format(bookingSlot.checkoutTime));
                simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
                startDateText.setText(simpleDateFormat.format(bookingSlot.startTime));
                endDateText.setText(simpleDateFormat.format(bookingSlot.endTime));
                checkoutDateText.setText(simpleDateFormat.format(bookingSlot.checkoutTime));
                numberPlateText.setText(bookingSlot.numberPlate);
                wheelerText.setText(String.valueOf(bookingSlot.wheelerType) + " Wheeler");
                if(bookingSlot.hasPaid==0){
                    amountText.setText(String.valueOf(bookingSlot.amount).concat(" (Not Paid)"));
                }else{
                    amountText.setText(String.valueOf(bookingSlot.amount).concat(" (Paid)"));
                }

                updatePayCheckoutUI();



                if(!run_once){
                    run_once=true;
                    attachParkingListeners();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        firebaseDatabase.getReference().child("BookedSlots").child(UUID).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(snapshot.getKey().equals("hasPaid")){
                    try{
                        bookingSlot.hasPaid=snapshot.getValue(int.class);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    Log.e(String.valueOf(BookingDetails.this.getClass()),"Fetched updated BookedSlots:"+  String.valueOf(snapshot.getKey())+snapshot.getValue(int.class));
                }
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

    }

    private void checkoutData() {
        Calendar calendar=new GregorianCalendar();
        bookingSlot.checkout=1;
        bookingSlot.checkoutTime=calendar.getTime();
        parkingArea.deallocateSpace();
        parkingArea.deallocateSlot(bookingSlot.slotNo);
        firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    firebaseDatabase.getReference("BookedSlots").child(UUID).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful()){
                                showToast("Checkout Success");
                                Intent notifyIntent = new Intent(getApplicationContext(), NotificationReceiver.class);
                                AlarmUtils.cancelAlarm(BookingDetails.this,notifyIntent,bookingSlot.notificationID);
                                Intent intent;
                                if(userObj.userType==3)
                                    intent = new Intent(BookingDetails.this, DriverContainer.class);
                                else
                                    intent = new Intent(BookingDetails.this, DriverContainer.class);
                                intent.putExtra("FRAGMENT_NO", 0);
                                startActivity(intent);
                                finish();
                            }else{
                                showToast("Checkout Failed!");
                                parkingArea.allocateSpace();
                                firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                            }
                        }
                    });
                }
            }
        });
    }

    private void updatePayCheckoutUI() {
        if(bookingSlot.hasPaid==1){
            fabCheckOutButton.setVisibility(View.VISIBLE);
            fabPayButton.setVisibility(View.GONE);
        }else if(bookingSlot.hasPaid==0){
            fabPayButton.setVisibility(View.VISIBLE);
            fabCheckOutButton.setVisibility(View.GONE);
        }
        if(bookingSlot.checkout!=0){
            fabCheckOutButton.setVisibility(View.GONE);
            fabPayButton.setVisibility(View.GONE);
        }
    }

    private void attachParkingListeners(){
        firebaseDatabase.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        if(snapshot.getKey().equals("availableSlots") || snapshot.getKey().equals("occupiedSlots") || snapshot.getKey().equals("totalSlots")){
                            try{
                                parkingArea.setData(snapshot.getKey(),snapshot.getValue(int.class));
                                updatePayCheckoutUI();
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                            Log.e(String.valueOf(BookingDetails.this.getClass()),"Fetched updated parking Area:"+  String.valueOf(snapshot.getKey())+snapshot.getValue(int.class));
                        }
                    }
                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
                    @Override
                    public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });

        firebaseDatabase.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        ParkingArea parkingArea = snapshot.getValue(ParkingArea.class);
                        setAddValues(parkingArea);
                        Log.e(String.valueOf(BookingDetails.this.getClass()),"Fetched parking Area:"+ String.valueOf(snapshot.getKey()));
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void setAddValues(ParkingArea parkingArea) {
        this.parkingArea=parkingArea;
        placeText.setText(parkingArea.name);
        globalLatLng=new LatLng(parkingArea.latitude,parkingArea.longitude);
        options=new MarkerOptions().position(globalLatLng)
                .title(parkingArea.name);
        supportMapFragment=(SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                gMap=googleMap;
                gMap.clear();
                gMap.addMarker(options);
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(globalLatLng,30));
            }
        });
    }

    private void payData() {
        bookingSlot.notificationID=Math.abs((int) Calendar.getInstance().getTimeInMillis());
        bookingSlot.slotNo=parkingArea.allocateSlot(bookingSlot.numberPlate);
        firebaseDatabase.getReference("BookedSlots").child(UUID).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                    showToast("Payment Success");
                    File file = new File(BookingDetails.this.getExternalCacheDir(), File.separator + "invoice.pdf");
                    InvoiceGenerator invoiceGenerator=new InvoiceGenerator(bookingSlot,parkingArea,UUID,userObj,file);
                    invoiceGenerator.create();
                    invoiceGenerator.uploadFile(BookingDetails.this,getApplication());
                    Intent intent;
                    if(userObj.userType==3)
                        intent = new Intent(BookingDetails.this, DriverContainer.class);
                    else
                        intent = new Intent(BookingDetails.this, DriverContainer.class);
                    intent.putExtra("FRAGMENT_NO", 0);
                    startActivity(intent);
                    finish();
                }else{
                    showToast("Payment Failed");
                    parkingArea.deallocateSpace();
                    parkingArea.deallocateSlot(bookingSlot.slotNo);
                    firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
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
        Toast toast = new Toast(BookingDetails.this);
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


}
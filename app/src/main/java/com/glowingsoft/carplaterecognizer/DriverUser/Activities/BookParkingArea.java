package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlots;
import com.glowingsoft.carplaterecognizer.HelperClasses.NumberPlate;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.HelperClasses.User;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.notifications.NotificationHelper;
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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

public class BookParkingArea extends AppCompatActivity {

    private static final int SCAN_PERMISSION_ALL = 109;

    Spinner numberPlateSpinner;
    TextView placeText,wheelerText,amountText,endDateText,endTimeText, messageText;
    ImageView toastIcon;
    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;

    SupportMapFragment supportMapFragment;
    GoogleMap gMap;
    LatLng globalLatLng=null;
    MarkerOptions options;

    List<Integer> numberPlateWheeler = new ArrayList<Integer>();
    List<String> numberPlateNumber = new ArrayList<String>();

    Calendar calendar;

    BookedSlots bookingSlot = new BookedSlots();
    ParkingArea parkingArea;
    User userObj;
    NotificationHelper mNotificationHelper;
    String[] PERMISSIONS = {
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
    };
    ImageView backImageView, settingsImageView;
    FloatingActionButton fabBookButton;
    LinearLayout endDate,endTime;
    BasicUtils utils = new BasicUtils();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_parking_area);
        
        initComponents();
        attachListeners();
        if (!utils.isNetworkAvailable(getApplication())) {
            showToast("No network available. Turn on mobile data or WIFI");
        }

        addItemsOnSpinner();
        addListenerOnSpinnerItemSelection();
        askCameraFilePermission();
    }
    
    private void initComponents() {
        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);

        fabBookButton = findViewById(R.id.bookBtn);

        // setting the color of icons in the fab
        int color = ContextCompat.getColor(BookParkingArea.this, R.color.white);
        ColorStateList colorStateList = ColorStateList.valueOf(color);
        // set the color state list as the tint for the FABs
        fabBookButton.setImageTintList(colorStateList);

        // initialize firebase
        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();


        placeText = findViewById(R.id.placeText);
        numberPlateSpinner = findViewById(R.id.vehicleSelect);
        endDate = findViewById(R.id.endDate);
        endTime = findViewById(R.id.endTime);
        endDateText = findViewById(R.id.endDateText);
        endTimeText = findViewById(R.id.endTimeText);

        wheelerText = findViewById(R.id.wheelerText);
        amountText = findViewById(R.id.amountText);
        mNotificationHelper=new NotificationHelper(this);

        calendar=new GregorianCalendar();
        bookingSlot.startTime=bookingSlot.endTime=bookingSlot.checkoutTime=calendar.getTime();
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
        endTimeText.setText(simpleDateFormat.format(bookingSlot.endTime));
        simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
        endDateText.setText(simpleDateFormat.format(bookingSlot.endTime));
        bookingSlot.readNotification=0;
        bookingSlot.readBookedNotification=1;
        bookingSlot.hasPaid=0;
        bookingSlot.userID=mAuth.getCurrentUser().getUid();

        Bundle bundle = getIntent().getExtras();
        bookingSlot.placeID=bundle.getString("UUID");
        final ParkingArea parkingArea = (ParkingArea) getIntent().getSerializableExtra("ParkingArea");
        Log.e(String.valueOf(BookParkingArea.this.getClass()),"Fetched parking Area:"+ parkingArea.name+" "+bookingSlot.placeID);

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
    
    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

        endDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker(endDateText);
            }
        });
        endTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTimePicker(endTimeText);
            }
        });

        fabBookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(numberPlateSpinner.getSelectedItemPosition()==0){
                    showToast("Please select a vehicle!");
                }else if(bookingSlot.endTime.equals(bookingSlot.startTime)){
                    showToast("Please set the end time!");
                }else if(!bookingSlot.timeDiffValid()){
                    showToast("Less time difference (<15 minutes)!");
                }else{
                    AlertDialog.Builder builder = new AlertDialog.Builder(BookParkingArea.this);
                    builder.setCancelable(true);
                    builder.setTitle("Confirm Booking");
                    builder.setMessage("Confirm Booking for this area?");
                    builder.setPositiveButton("Confirm",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if(parkingArea.availableSlots>0) {
                                        parkingArea.allocateSpace();
                                        firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                                        showToast("Booking Successful");
                                        saveData();
                                        onBackPressed();
                                    }else{
                                        showToast("Failed! Slots are Full");
                                    }
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
                }
            }
        });

        firebaseDatabase.getReference().child("ParkingAreas").child(bookingSlot.placeID)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        if(snapshot.getKey().equals("availableSlots") || snapshot.getKey().equals("occupiedSlots") || snapshot.getKey().equals("totalSlots")){
                            parkingArea.setData(snapshot.getKey(),snapshot.getValue(int.class));
                            Log.e(String.valueOf(BookParkingArea.this.getClass()),"Fetched updated parking Area:"+ String.valueOf(snapshot.getKey())+snapshot.getValue(int.class));
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

                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
        
    }

    private void calcRefreshAmount() {
        bookingSlot.calcAmount(parkingArea);
        String amountStr=String.valueOf(bookingSlot.amount);
        amountText.setText(amountStr);
    }

    private void setAddValues(ParkingArea parkingArea) {
        this.parkingArea=parkingArea;
        placeText.setText(parkingArea.name);
    }

    private void saveData() {
        bookingSlot.notificationID=Math.abs((int)Calendar.getInstance().getTimeInMillis());
        final String key=firebaseDatabase.getReference("BookedSlots").push().getKey();
        bookingSlot.slotNo=parkingArea.allocateSlot(bookingSlot.numberPlate);
        firebaseDatabase.getReference("BookedSlots").child(key).setValue(bookingSlot).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                    showToast("Booking Successful");
//                    File file = new File(BookParkingArea.this.getExternalCacheDir(), File.separator + "invoice.pdf");
//                    InvoiceGenerator invoiceGenerator=new InvoiceGenerator(bookingSlot,parkingArea,key,userObj,file);
//                    invoiceGenerator.create();
//                    invoiceGenerator.uploadFile(BookParkingArea.this,getApplication());
//                    Intent intent = new Intent(BookParkingArea.this, DriverContainer.class);
//                    intent.putExtra("FRAGMENT_NO", 0);
//                    startActivity(intent);
//                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);  //slide from left to right
//                    finish();

                    onBackPressed();
                }else{
                    showToast("Failed to process your booking. Try again later!");
                    parkingArea.deallocateSpace();
                    parkingArea.deallocateSlot(bookingSlot.slotNo);
                    firebaseDatabase.getReference("ParkingAreas").child(bookingSlot.placeID).setValue(parkingArea);
                }
            }
        });
    }

    private void showDatePicker(final TextView button) {
        DatePickerDialog.OnDateSetListener dateSetListener=new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, final int date) {
                calendar.set(Calendar.YEAR,year);
                calendar.set(Calendar.MONTH,month);
                calendar.set(Calendar.DAY_OF_MONTH,date);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("dd-MM-yyyy");
                button.setText(simpleDateFormat.format(calendar.getTime()));
                bookingSlot.endTime = bookingSlot.checkoutTime = calendar.getTime();
                calcRefreshAmount();
            }
        };
        DatePickerDialog datePickerDialog=new DatePickerDialog(BookParkingArea.this,dateSetListener,calendar.get(Calendar.YEAR),calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker(final TextView button) {
        TimePickerDialog.OnTimeSetListener timeSetListener= new TimePickerDialog.OnTimeSetListener() {
            @Override
            public void onTimeSet(TimePicker timePicker, int hour, int minute) {
                calendar.set(Calendar.HOUR_OF_DAY,hour);
                calendar.set(Calendar.MINUTE,minute);
                calendar.set(Calendar.SECOND, 0);
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm a");
                bookingSlot.endTime = bookingSlot.checkoutTime = calendar.getTime();
                if(bookingSlot.endTime.after(bookingSlot.startTime)){
                    button.setText(simpleDateFormat.format(calendar.getTime()));
                    bookingSlot.endTime = bookingSlot.checkoutTime = calendar.getTime();
                    calcRefreshAmount();
                }else{
                    bookingSlot.endTime = bookingSlot.checkoutTime = bookingSlot.startTime;
                    showToast("Please select a time after present time!");
                }
            }
        };
        TimePickerDialog timePickerDialog=new TimePickerDialog(BookParkingArea.this,timeSetListener,calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),false);
        timePickerDialog.show();
    }

    public void addItemsOnSpinner() {
        numberPlateWheeler.add(0);
        numberPlateNumber.add("Select a vehicle");
        firebaseDatabase.getReference().child("NumberPlates").orderByChild("userID").equalTo(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            NumberPlate numberPlate = dataSnapshot.getValue(NumberPlate.class);
                            if(numberPlate.isDeleted==0){
                                numberPlateWheeler.add(numberPlate.wheelerType);
                                numberPlateNumber.add(numberPlate.numberPlate);
                            }
                        }
                        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(BookParkingArea.this,
                                android.R.layout.simple_spinner_item, numberPlateNumber);
                        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        numberPlateSpinner.setAdapter(dataAdapter);
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public void addListenerOnSpinnerItemSelection() {
        numberPlateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if(position!=0){
                    bookingSlot.numberPlate= numberPlateNumber.get(position);
                    bookingSlot.wheelerType= numberPlateWheeler.get(position);
                    calcRefreshAmount();
                    String wheelerTypeStr=String.valueOf(bookingSlot.wheelerType);
                    wheelerText.setText(wheelerTypeStr);
                    Toast.makeText(BookParkingArea.this, String.valueOf(numberPlateSpinner.getSelectedItem())+String.valueOf(position), Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void askCameraFilePermission() {
        if (!hasPermissions(BookParkingArea.this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(BookParkingArea.this, PERMISSIONS, SCAN_PERMISSION_ALL);
        }else{
//            openCamera();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (data != null) {
                saveData();
            } else {
                showToast("Failed to Save Data. Try again!");
            }
        }
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
        Toast toast = new Toast(BookParkingArea.this);
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
package com.glowingsoft.carplaterecognizer.DriverUser.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlotKey;
import com.glowingsoft.carplaterecognizer.HelperClasses.BookedSlots;
import com.glowingsoft.carplaterecognizer.HelperClasses.ParkingArea;
import com.glowingsoft.carplaterecognizer.R;
import com.glowingsoft.carplaterecognizer.RegisterLogin.Login;
import com.glowingsoft.carplaterecognizer.utils.BasicUtils;
import com.glowingsoft.carplaterecognizer.utils.adapters.BookingHistoryAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserHistory extends AppCompatActivity {
    BasicUtils utils = new BasicUtils();
    ImageView backImageView, settingsImageView, toastIcon;

    FirebaseAuth mAuth;
    FirebaseDatabase firebaseDatabase;
    TextView emptyTextView, messageText;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;

    List<BookedSlotKey> bookedSlotKeyList = new ArrayList<BookedSlotKey>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_history);

        initComponents();
        attachListeners();

        if (!utils.isNetworkAvailable(getApplication())) {
            showToast("No network available. Turn on mobile data or WIFI");
        }
    }

    private void initComponents() {
        backImageView = findViewById(R.id.back_button);
        settingsImageView = findViewById(R.id.settings_button);
        settingsImageView.setVisibility(View.GONE);

        mAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();

        emptyTextView=findViewById(R.id.empty_view);
        recyclerView = (RecyclerView) findViewById(R.id.user_history_recycler_view);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(UserHistory.this);
        recyclerView.setLayoutManager(layoutManager);
    }

    private void attachListeners() {
        backImageView.setOnClickListener(view-> {
            onBackPressed();
        });

        firebaseDatabase.getReference().child("BookedSlots").orderByChild("userID").equalTo(mAuth.getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                            BookedSlots bookedSlot = dataSnapshot.getValue(BookedSlots.class);
                            BookedSlotKey bookedSlotKey=new BookedSlotKey(bookedSlot,dataSnapshot.getKey());
                            bookedSlotKeyList.add(bookedSlotKey);
                        }
                        mAdapter = new BookingHistoryAdapter(bookedSlotKeyList);
                        recyclerView.setAdapter(mAdapter);
                        if(bookedSlotKeyList.isEmpty()){
                            recyclerView.setVisibility(View.GONE);
                            emptyTextView.setVisibility(View.VISIBLE);
                        }else{
                            recyclerView.setVisibility(View.VISIBLE);
                            emptyTextView.setVisibility(View.GONE);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
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
        Toast toast = new Toast(UserHistory.this);
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
package com.example;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Firebase Database State
    private DatabaseReference firebaseDbRef;
    private boolean isFirebaseAvailable = false;

    // UI Modules Layout Containers
    private View sectionAnnouncements;
    private View sectionCctv;
    private View sectionSocial;
    private View sectionPayments;
    private View sectionMap;

    // Bottom Navigation Bar
    private BottomNavigationView bottomNavigationView;

    // CCTV Simulation Component Resources
    private TextView cctvTimestamp;
    private TextView cctvFpsBitrate;
    private TextView cctvCamTitleTag;
    private View cctvRecIndicator;
    private ProgressBar cctvLoader;
    private RelativeLayout cctvScreenBg;
    private View cctvMotionTicker;
    private final Handler cctvHandler = new Handler(Looper.getMainLooper());
    private Runnable cctvRunnable;
    private int selectedCamId = 1;

    // RSVP Counters State
    private int rsvpCountEvent1 = 28;
    private int rsvpCountEvent2 = 15;
    private TextView socialCounter1;
    private TextView socialCounter2;

    // Payment Section State Variables
    private Spinner spinPaymentBlock, spinPaymentHouse;
    private Button btnCekIuran, btnSubTabIuran, btnSubTabRonda;
    private View subContainerIuran, subContainerRonda;
    private View billStatusContainer;
    private TextView billTargetTitle, billMonthStatus;
    private Button btnPayNowSim;
    private TextView txPaymentHistory;

    // Incident / Security siskamling Logs State
    private EditText etReporterName, etReporterMessage;
    private Button btnSubmitReport;
    private TextView txSecurityReports;
    private List<String> incidentReports = new ArrayList<>();

    // Map Section Variables
    private Spinner spinMapBlock, spinMapNum;
    private MapCanvasView customCourierMapView;
    private TextView txMapGuideInstructions;

    // Registry of Local Paid / Unpaid Houses tracking
    private final Map<String, Boolean> housePaymentsRegistry = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind layout views
        sectionAnnouncements = findViewById(R.id.section_announcements);
        sectionCctv = findViewById(R.id.section_cctv);
        sectionSocial = findViewById(R.id.section_social);
        sectionPayments = findViewById(R.id.section_payments);
        sectionMap = findViewById(R.id.section_map);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Track local payment defaults (mocked database)
        initializePaymentsDatabase();

        // Configure Bottom Navigation tabs transition routing
        setupBottomNavigation();

        // Module - Announcement details
        setupAnnouncementListeners();

        // Module - CCTV Stream Clock Handler Tick loops
        setupCctvFeedSimulation();

        // Module - Social RSVPs
        setupSocialEvents();

        // Module - Payments and Security schedule
        setupPaymentAndRondaDashboard();

        // Module - Interactive Courier schematic mapping
        setupCourierMapConsole();

        // Initialize Firebase Realtime Database and synchronize
        initFirebaseConnection();
    }

    private void initializePaymentsDatabase() {
        for (int i = 1; i <= 20; i++) {
            housePaymentsRegistry.put(String.format(Locale.getDefault(), "F-%02d", i), i % 3 != 0);
            housePaymentsRegistry.put(String.format(Locale.getDefault(), "G-%02d", i), i % 4 != 0);
        }
    }

    private void initFirebaseConnection() {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:513155315152:android:56fd0e3684aad47a48e6e1")
                    .setDatabaseUrl("https://rt02grahacikarang-default-rtdb.firebaseio.com")
                    .setProjectId("rt02grahacikarang")
                    .build();

            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this, options);
            }
            FirebaseDatabase db = FirebaseDatabase.getInstance();
            try {
                db.setPersistenceEnabled(true);
            } catch (Exception e) {
                // Ignore if already set
            }
            firebaseDbRef = db.getReference();
            isFirebaseAvailable = true;
            syncDataFromFirebase();
        } catch (Throwable t) {
            isFirebaseAvailable = false;
            Log.e("FirebaseInit", "Firebase setup bypassed. Local persistence will be used.", t);
        }
    }

    private void syncDataFromFirebase() {
        if (!isFirebaseAvailable || firebaseDbRef == null) return;

        // 1. Sync Payments Registry
        firebaseDbRef.child("payments").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String key = ds.getKey();
                    Boolean val = ds.getValue(Boolean.class);
                    if (key != null && val != null) {
                        housePaymentsRegistry.put(key, val);
                    }
                }
                verifyActiveBillStatus();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 2. Sync Social RSVPs
        firebaseDbRef.child("rsvp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer e1 = snapshot.child("event1").getValue(Integer.class);
                Integer e2 = snapshot.child("event2").getValue(Integer.class);
                if (e1 != null) {
                    rsvpCountEvent1 = e1;
                    socialCounter1.setText(getString(R.string.rsvp_counter_format, rsvpCountEvent1));
                }
                if (e2 != null) {
                    rsvpCountEvent2 = e2;
                    socialCounter2.setText(getString(R.string.rsvp_counter_format, rsvpCountEvent2));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 3. Sync Incident/Security reports
        firebaseDbRef.child("reports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> loadedReports = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String r = ds.getValue(String.class);
                    if (r != null) {
                        loadedReports.add(0, r);
                    }
                }
                if (!loadedReports.isEmpty()) {
                    incidentReports = loadedReports;
                    updateSecurityReportsDisplay();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateSecurityReportsDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(incidentReports.size(), 4); i++) {
            sb.append(incidentReports.get(i));
        }
        if (incidentReports.size() < 4) {
            sb.append(getString(R.string.default_report_1));
            sb.append(getString(R.string.default_report_2));
        }
        txSecurityReports.setText(sb.toString());
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            sectionAnnouncements.setVisibility(View.GONE);
            sectionCctv.setVisibility(View.GONE);
            sectionSocial.setVisibility(View.GONE);
            sectionPayments.setVisibility(View.GONE);
            sectionMap.setVisibility(View.GONE);

            int id = item.getItemId();
            if (id == R.id.nav_announcements) {
                sectionAnnouncements.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_cctv) {
                sectionCctv.setVisibility(View.VISIBLE);
                simulateCameraSwitch(selectedCamId);
                return true;
            } else if (id == R.id.nav_social) {
                sectionSocial.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_payments) {
                sectionPayments.setVisibility(View.VISIBLE);
                return true;
            } else if (id == R.id.nav_map) {
                sectionMap.setVisibility(View.VISIBLE);
                updateMapRoute();
                return true;
            }
            return false;
        });
    }

    private void setupAnnouncementListeners() {
        findViewById(R.id.btn_ann_read_1).setOnClickListener(v -> showDetailedAnnouncement(
                getString(R.string.ann_title_1),
                getString(R.string.ann_body_1)
        ));

        findViewById(R.id.btn_ann_read_2).setOnClickListener(v -> showDetailedAnnouncement(
                getString(R.string.ann_title_2),
                getString(R.string.ann_body_2)
        ));

        findViewById(R.id.btn_ann_read_3).setOnClickListener(v -> showDetailedAnnouncement(
                getString(R.string.ann_title_3),
                getString(R.string.ann_body_3)
        ));
    }

    private void showDetailedAnnouncement(String title, String body) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(body)
                .setIcon(R.drawable.ic_announcement)
                .setPositiveButton(getString(R.string.understand), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void setupCctvFeedSimulation() {
        cctvTimestamp = findViewById(R.id.cctv_timestamp);
        cctvFpsBitrate = findViewById(R.id.cctv_fps_bitrate);
        cctvCamTitleTag = findViewById(R.id.cctv_cam_title_tag);
        cctvRecIndicator = findViewById(R.id.cctv_rec_indicator);
        cctvLoader = findViewById(R.id.cctv_loader);
        cctvScreenBg = findViewById(R.id.cctv_screen_background);
        cctvMotionTicker = findViewById(R.id.cctv_motion_ticker);

        final MaterialCardView btnCardCam1 = findViewById(R.id.btn_cam_1);
        final MaterialCardView btnCardCam2 = findViewById(R.id.btn_cam_2);
        final MaterialCardView btnCardCam3 = findViewById(R.id.btn_cam_3);
        final MaterialCardView btnCardCam4 = findViewById(R.id.btn_cam_4);

        final TextView txCam1 = findViewById(R.id.tx_cam_1);
        final TextView txCam2 = findViewById(R.id.tx_cam_2);
        final TextView txCam3 = findViewById(R.id.tx_cam_3);
        final TextView txCam4 = findViewById(R.id.tx_cam_4);

        btnCardCam1.setOnClickListener(v -> {
            selectedCamId = 1;
            cctvCamTitleTag.setText(getString(R.string.cam_01_title));
            highlightButton(btnCardCam1, txCam1, btnCardCam2, txCam2, btnCardCam3, txCam3, btnCardCam4, txCam4);
            simulateCameraSwitch(1);
        });

        btnCardCam2.setOnClickListener(v -> {
            selectedCamId = 2;
            cctvCamTitleTag.setText(getString(R.string.cam_02_title));
            highlightButton(btnCardCam2, txCam2, btnCardCam1, txCam1, btnCardCam3, txCam3, btnCardCam4, txCam4);
            simulateCameraSwitch(2);
        });

        btnCardCam3.setOnClickListener(v -> {
            selectedCamId = 3;
            cctvCamTitleTag.setText(getString(R.string.cam_03_title));
            highlightButton(btnCardCam3, txCam3, btnCardCam1, txCam1, btnCardCam2, txCam2, btnCardCam4, txCam4);
            simulateCameraSwitch(3);
        });

        btnCardCam4.setOnClickListener(v -> {
            selectedCamId = 4;
            cctvCamTitleTag.setText(getString(R.string.cam_04_title));
            highlightButton(btnCardCam4, txCam4, btnCardCam1, txCam1, btnCardCam2, txCam2, btnCardCam3, txCam3);
            simulateCameraSwitch(4);
        });

        cctvRunnable = new Runnable() {
            private int counterCount = 0;
            private final Random rnd = new Random();

            @Override
            public void run() {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                cctvTimestamp.setText(sdf.format(new Date()));

                float mbps = 2.2f + rnd.nextFloat() * 0.6f;
                cctvFpsBitrate.setText(String.format(Locale.getDefault(), "FHD 1080p | %.2f Mbps", mbps));

                cctvRecIndicator.setVisibility(cctvRecIndicator.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);

                counterCount++;
                if (counterCount % 12 == 0) {
                    cctvMotionTicker.setVisibility(View.VISIBLE);
                } else if (counterCount % 12 == 3) {
                    cctvMotionTicker.setVisibility(View.GONE);
                }

                cctvHandler.postDelayed(this, 1000);
            }
        };
        cctvHandler.post(cctvRunnable);
    }

    private void highlightButton(MaterialCardView active, TextView activeText,
                                 MaterialCardView i1, TextView t1,
                                 MaterialCardView i2, TextView t2,
                                 MaterialCardView i3, TextView t3) {
        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        int dividerLineColor = ContextCompat.getColor(this, R.color.divider_line);
        int textDarkColor = ContextCompat.getColor(this, R.color.text_dark);

        active.setStrokeWidth(5);
        active.setStrokeColor(primaryColor);
        active.setCardElevation(4f);
        activeText.setTextColor(primaryColor);
        activeText.getPaint().setFakeBoldText(true);

        i1.setStrokeWidth(2);
        i1.setStrokeColor(dividerLineColor);
        i1.setCardElevation(1f);
        t1.setTextColor(textDarkColor);
        t1.getPaint().setFakeBoldText(false);

        i2.setStrokeWidth(2);
        i2.setStrokeColor(dividerLineColor);
        i2.setCardElevation(1f);
        t2.setTextColor(textDarkColor);
        t2.getPaint().setFakeBoldText(false);

        i3.setStrokeWidth(2);
        i3.setStrokeColor(dividerLineColor);
        i3.setCardElevation(1f);
        t3.setTextColor(textDarkColor);
        t3.getPaint().setFakeBoldText(false);
    }

    private void simulateCameraSwitch(int camId) {
        cctvLoader.setVisibility(View.VISIBLE);
        cctvRecIndicator.setVisibility(View.INVISIBLE);

        if (camId == 1) cctvScreenBg.setBackgroundColor(Color.parseColor("#0F172A"));
        else if (camId == 2) cctvScreenBg.setBackgroundColor(Color.parseColor("#1B2A4A"));
        else if (camId == 3) cctvScreenBg.setBackgroundColor(Color.parseColor("#0B261A"));
        else cctvScreenBg.setBackgroundColor(Color.parseColor("#2A120B"));

        cctvHandler.postDelayed(() -> {
            cctvLoader.setVisibility(View.GONE);
            cctvRecIndicator.setVisibility(View.VISIBLE);
        }, 1200);
    }

    private void setupSocialEvents() {
        socialCounter1 = findViewById(R.id.social_counter_1);
        socialCounter2 = findViewById(R.id.social_counter_2);

        findViewById(R.id.btn_rsvp_1).setOnClickListener(v ->
                triggerRsvpDialog(getString(R.string.event_name_1), 1)
        );

        findViewById(R.id.btn_rsvp_2).setOnClickListener(v ->
                triggerRsvpDialog(getString(R.string.event_name_2), 2)
        );
    }

    private void triggerRsvpDialog(final String eventName, final int eventNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(getString(R.string.rsvp_dialog_title));
        builder.setMessage(getString(R.string.event_label, eventName));

        View inputView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_rsvp_input, null);
        builder.setView(inputView);

        final EditText etRsvpName = inputView.findViewById(R.id.et_rsvp_name);
        final EditText etRsvpHouse = inputView.findViewById(R.id.et_rsvp_house);

        builder.setPositiveButton(getString(R.string.confirm_attendance), (dialog, which) -> {
            String nameInput = etRsvpName.getText().toString().trim();
            String houseInput = etRsvpHouse.getText().toString().trim();

            if (nameInput.isEmpty() || houseInput.isEmpty()) {
                Toast.makeText(MainActivity.this, getString(R.string.validation_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            if (eventNum == 1) {
                rsvpCountEvent1++;
                socialCounter1.setText(getString(R.string.rsvp_counter_format, rsvpCountEvent1));
                if (isFirebaseAvailable && firebaseDbRef != null) {
                    firebaseDbRef.child("rsvp").child("event1").setValue(rsvpCountEvent1);
                }
            } else {
                rsvpCountEvent2++;
                socialCounter2.setText(getString(R.string.rsvp_counter_format, rsvpCountEvent2));
                if (isFirebaseAvailable && firebaseDbRef != null) {
                    firebaseDbRef.child("rsvp").child("event2").setValue(rsvpCountEvent2);
                }
            }

            Toast.makeText(MainActivity.this, getString(R.string.rsvp_thank_you, nameInput, houseInput), Toast.LENGTH_LONG).show();
            dialog.dismiss();
        });

        builder.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void setupPaymentAndRondaDashboard() {
        spinPaymentBlock = findViewById(R.id.spin_payment_block);
        spinPaymentHouse = findViewById(R.id.spin_payment_house);
        btnCekIuran = findViewById(R.id.btn_cek_iuran);
        btnPayNowSim = findViewById(R.id.btn_pay_now_sim);
        billStatusContainer = findViewById(R.id.bill_status_container);
        billTargetTitle = findViewById(R.id.bill_target_title);
        billMonthStatus = findViewById(R.id.bill_month_status);
        txPaymentHistory = findViewById(R.id.tx_payment_history);

        btnSubTabIuran = findViewById(R.id.btn_sub_tab_iuran);
        btnSubTabRonda = findViewById(R.id.btn_sub_tab_ronda);
        subContainerIuran = findViewById(R.id.sub_container_iuran);
        subContainerRonda = findViewById(R.id.sub_container_ronda);

        etReporterName = findViewById(R.id.et_reporter_name);
        etReporterMessage = findViewById(R.id.et_reporter_message);
        btnSubmitReport = findViewById(R.id.btn_submit_report);
        txSecurityReports = findViewById(R.id.tx_security_reports);

        String[] blocks = {getString(R.string.block_f_label), getString(R.string.block_g_label)};
        ArrayAdapter<String> blockAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, blocks);
        spinPaymentBlock.setAdapter(blockAdapter);

        ArrayList<String> houses = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            houses.add(String.format(Locale.getDefault(), "No. %02d", i));
        }
        ArrayAdapter<String> houseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, houses);
        spinPaymentHouse.setAdapter(houseAdapter);

        int primaryColor = ContextCompat.getColor(this, R.color.primary);
        int textDarkColor = ContextCompat.getColor(this, R.color.text_dark);

        btnSubTabIuran.setOnClickListener(v -> {
            btnSubTabIuran.setBackgroundColor(primaryColor);
            btnSubTabIuran.setTextColor(Color.WHITE);

            btnSubTabRonda.setBackgroundColor(Color.TRANSPARENT);
            btnSubTabRonda.setTextColor(textDarkColor);

            subContainerIuran.setVisibility(View.VISIBLE);
            subContainerRonda.setVisibility(View.GONE);
        });

        btnSubTabRonda.setOnClickListener(v -> {
            btnSubTabRonda.setBackgroundColor(primaryColor);
            btnSubTabRonda.setTextColor(Color.WHITE);

            btnSubTabIuran.setBackgroundColor(Color.TRANSPARENT);
            btnSubTabIuran.setTextColor(textDarkColor);

            subContainerRonda.setVisibility(View.VISIBLE);
            subContainerIuran.setVisibility(View.GONE);
        });

        btnCekIuran.setOnClickListener(v -> verifyActiveBillStatus());
        btnPayNowSim.setOnClickListener(v -> showQrisSimulatedModal());

        btnSubmitReport.setOnClickListener(v -> {
            String repName = etReporterName.getText().toString().trim();
            String repMsg = etReporterMessage.getText().toString().trim();

            if (repName.isEmpty() || repMsg.isEmpty()) {
                Toast.makeText(MainActivity.this, getString(R.string.validation_report_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String logTime = timeSdf.format(new Date());

            String newReport = String.format(Locale.getDefault(), "🟢 [%s] %s: %s\n", logTime, repName.toUpperCase(Locale.getDefault()), repMsg);
            incidentReports.add(0, newReport);

            if (isFirebaseAvailable && firebaseDbRef != null) {
                String reportKey = firebaseDbRef.child("reports").push().getKey();
                if (reportKey != null) {
                    firebaseDbRef.child("reports").child(reportKey).setValue(newReport);
                }
            }

            updateSecurityReportsDisplay();
            etReporterMessage.setText("");
            Toast.makeText(MainActivity.this, getString(R.string.report_submitted_toast), Toast.LENGTH_SHORT).show();
        });
    }

    private void verifyActiveBillStatus() {
        String blk = spinPaymentBlock.getSelectedItemPosition() == 0 ? "F" : "G";
        String num = String.format(Locale.getDefault(), "%02d", spinPaymentHouse.getSelectedItemPosition() + 1);
        String houseKey = blk + "-" + num;

        billStatusContainer.setVisibility(View.VISIBLE);
        billTargetTitle.setText(getString(R.string.bill_detail_title, houseKey));

        boolean isPaid = housePaymentsRegistry.containsKey(houseKey) && Boolean.TRUE.equals(housePaymentsRegistry.get(houseKey));

        if (isPaid) {
            billMonthStatus.setText(getString(R.string.status_paid));
            billMonthStatus.setTextColor(ContextCompat.getColor(this, R.color.primary));
            btnPayNowSim.setVisibility(View.GONE);
        } else {
            billMonthStatus.setText(getString(R.string.status_unpaid));
            billMonthStatus.setTextColor(ContextCompat.getColor(this, R.color.active_red));
            btnPayNowSim.setVisibility(View.VISIBLE);
        }
    }

    private void showQrisSimulatedModal() {
        AlertDialog.Builder qrisBuilder = new AlertDialog.Builder(MainActivity.this);
        View qrisLayout = LayoutInflater.from(this).inflate(R.layout.dialog_qris, null);
        qrisBuilder.setView(qrisLayout);

        TextView txQrisHouse = qrisLayout.findViewById(R.id.tx_qris_house);
        TextView txQrisBillingId = qrisLayout.findViewById(R.id.tx_qris_billing_id);

        final String blk = spinPaymentBlock.getSelectedItemPosition() == 0 ? "F" : "G";
        final String num = String.format(Locale.getDefault(), "%02d", spinPaymentHouse.getSelectedItemPosition() + 1);
        final String houseKey = blk + "-" + num;

        txQrisHouse.setText(getString(R.string.qris_house_detail, houseKey));
        txQrisBillingId.setText(getString(R.string.qris_billing_id, houseKey, new Random().nextInt(900)));

        final AlertDialog qrDialog = qrisBuilder.show();

        Button btnCompleteTx = qrisLayout.findViewById(R.id.btn_complete_transaction);
        btnCompleteTx.setOnClickListener(v -> {
            housePaymentsRegistry.put(houseKey, true);
            if (isFirebaseAvailable && firebaseDbRef != null) {
                firebaseDbRef.child("payments").child(houseKey).setValue(true);
            }

            verifyActiveBillStatus();

            SimpleDateFormat dSdf = new SimpleDateFormat("dd/05", Locale.getDefault());
            String dateLabel = dSdf.format(new Date());
            String oldLog = txPaymentHistory.getText().toString();
            String newLog = String.format(Locale.getDefault(), "1. %s (Bp/Ibu Pemilik) - Lunas Juni • %s\n%s", houseKey, dateLabel, oldLog);
            txPaymentHistory.setText(newLog);

            Toast.makeText(MainActivity.this, getString(R.string.payment_success_toast, houseKey), Toast.LENGTH_LONG).show();
            qrDialog.dismiss();
        });
    }

    private void setupCourierMapConsole() {
        spinMapBlock = findViewById(R.id.spin_map_block);
        spinMapNum = findViewById(R.id.spin_map_num);
        customCourierMapView = findViewById(R.id.custom_courier_map_view);
        txMapGuideInstructions = findViewById(R.id.tx_map_guide_instructions);

        String[] mapBlks = {getString(R.string.block_f_label), getString(R.string.block_g_label)};
        ArrayAdapter<String> mapBlkAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mapBlks);
        spinMapBlock.setAdapter(mapBlkAdapter);

        ArrayList<String> mapNums = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            mapNums.add(String.format(Locale.getDefault(), "No. %02d", i));
        }
        ArrayAdapter<String> mapNumAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mapNums);
        spinMapNum.setAdapter(mapNumAdapter);

        spinMapBlock.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMapRoute();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        spinMapNum.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateMapRoute();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        customCourierMapView.setOnBlockSelectionListener((block, houseNum) -> {
            try {
                int blkPos = block.equalsIgnoreCase("F") ? 0 : 1;
                int numPos = Integer.parseInt(houseNum) - 1;

                spinMapBlock.setSelection(blkPos);
                spinMapNum.setSelection(numPos);

                boolean houseStatus = housePaymentsRegistry.containsKey(block + "-" + houseNum) && Boolean.TRUE.equals(housePaymentsRegistry.get(block + "-" + houseNum));
                String statusStr = houseStatus ? getString(R.string.status_paid_simple) : getString(R.string.status_unpaid_simple);

                txMapGuideInstructions.setText(getString(R.string.map_selection_instruction, block, houseNum, statusStr));

            } catch (Exception e) {
                Log.e("MapSelection", "Error processing block selection", e);
            }
        });
    }

    private void updateMapRoute() {
        String blkIn = spinMapBlock.getSelectedItemPosition() == 0 ? "F" : "G";
        String numIn = String.format(Locale.getDefault(), "%02d", spinMapNum.getSelectedItemPosition() + 1);

        customCourierMapView.updateDestination(blkIn, numIn);

        if (blkIn.equals("F")) {
            txMapGuideInstructions.setText(getString(R.string.map_route_f, numIn));
        } else {
            txMapGuideInstructions.setText(getString(R.string.map_route_g, numIn));
        }
    }

    @Override
    protected void onDestroy() {
        cctvHandler.removeCallbacks(cctvRunnable);
        super.onDestroy();
    }
}
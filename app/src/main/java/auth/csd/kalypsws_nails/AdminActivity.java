package auth.csd.kalypsws_nails;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private CalendarView adminCalendarView;
    private Button btnBlockTime, btnAdminLogout;
    private TextView tvDateStatus;
    private LinearLayout appointmentsLayout;
    private FirebaseFirestore db;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        db = FirebaseFirestore.getInstance();

        adminCalendarView = findViewById(R.id.adminCalendarView);
        btnBlockTime = findViewById(R.id.btnBlockTime);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        tvDateStatus = findViewById(R.id.tvDateStatus);
        appointmentsLayout = findViewById(R.id.appointmentsLayout);

        // Κλείδωμα παλαιότερων ημερομηνιών
        adminCalendarView.setMinDate(System.currentTimeMillis() - 1000);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date(adminCalendarView.getDate()));

        checkIfDateIsAvailable(selectedDate);

        adminCalendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = sdf.format(calendar.getTime());
            checkIfDateIsAvailable(selectedDate);
        });

        btnAdminLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnBlockTime.setOnClickListener(v -> saveBlockToFirebase());
    }

    private void checkIfDateIsAvailable(String dateStr) {
        btnBlockTime.setEnabled(false);
        tvDateStatus.setText("Αναζήτηση...");
        appointmentsLayout.removeAllViews();

        db.collection("appointments")
                .whereEqualTo("date", dateStr)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();

                        if (!docs.isEmpty()) {
                            boolean isBlockedByAdmin = false;
                            for (DocumentSnapshot doc : docs) {
                                if ("ADMIN_BLOCK".equals(doc.getString("userId"))) {
                                    isBlockedByAdmin = true;
                                    break;
                                }
                            }

                            if (isBlockedByAdmin) {
                                tvDateStatus.setText("🔒 ΗΜΕΡΑ ΔΕΣΜΕΥΜΕΝΗ (ADMIN)");
                                tvDateStatus.setTextColor(Color.parseColor("#FF4C4C"));
                            } else {
                                tvDateStatus.setText("📅 ΥΠΑΡΧΟΥΝ ΡΑΝΤΕΒΟΥ ΠΕΛΑΤΩΝ");
                                tvDateStatus.setTextColor(Color.parseColor("#FF66B2"));
                            }

                            Collections.sort(docs, (d1, d2) -> {
                                String t1 = d1.getString("time");
                                String t2 = d2.getString("time");
                                return (t1 != null && t2 != null) ? t1.compareTo(t2) : 0;
                            });

                            for (DocumentSnapshot doc : docs) {
                                createAppCard(doc);
                            }

                            btnBlockTime.setEnabled(false);
                            btnBlockTime.setAlpha(0.3f);
                        } else {
                            tvDateStatus.setText("✅ Η ΗΜΕΡΑ ΕΙΝΑΙ ΕΛΕΥΘΕΡΗ");
                            tvDateStatus.setTextColor(Color.parseColor("#4CFF4C"));
                            btnBlockTime.setEnabled(true);
                            btnBlockTime.setAlpha(1.0f);
                        }
                    }
                });
    }

    private void createAppCard(DocumentSnapshot doc) {
        String time = doc.getString("time");
        String service = doc.getString("service");
        String userId = doc.getString("userId");

        TextView tv = new TextView(this);
        tv.setTextSize(14);
        tv.setPadding(35, 35, 35, 35);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTypeface(null, Typeface.BOLD);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        tv.setLayoutParams(params);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(15f);
        gd.setColor(Color.parseColor("#151515"));

        if ("ADMIN_BLOCK".equals(userId)) {
            tv.setText("🚫 ΗΜΕΡΑ ΔΕΣΜΕΥΜΕΝΗ ΑΠΟ ΔΙΑΧΕΙΡΙΣΤΗ");
            tv.setTextColor(Color.WHITE);
            gd.setStroke(3, Color.parseColor("#FF4C4C"));
        } else {
            tv.setText("👤 ΠΕΛΑΤΗΣ: " + time + " | " + service);
            tv.setTextColor(Color.parseColor("#E8C6C6"));
            gd.setStroke(2, Color.parseColor("#FF66B2"));
        }

        tv.setBackground(gd);
        appointmentsLayout.addView(tv);
    }

    private void saveBlockToFirebase() {
        btnBlockTime.setEnabled(false);
        btnBlockTime.setText("Αποθήκευση...");

        Map<String, Object> block = new HashMap<>();
        block.put("userId", "ADMIN_BLOCK");
        block.put("service", "ΗΜΕΡΑ ΔΕΣΜΕΥΜΕΝΗ ΑΠΟ ΔΙΑΧΕΙΡΙΣΤΗ");
        block.put("date", selectedDate);
        block.put("time", "10:00");
        block.put("duration", 480);

        db.collection("appointments").add(block)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Η ημέρα δεσμεύτηκε!", Toast.LENGTH_SHORT).show();
                    btnBlockTime.setText("Δέσμευση Ολόκληρης Ημέρας");
                    checkIfDateIsAvailable(selectedDate);
                })
                .addOnFailureListener(e -> {
                    btnBlockTime.setEnabled(true);
                    btnBlockTime.setText("Δέσμευση Ολόκληρης Ημέρας");
                });
    }
}
package auth.csd.kalypsws_nails;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminActivity extends AppCompatActivity {

    private CalendarView adminCalendarView;
    private Button btnBlockTime, btnAdminLogout;
    private TextView tvDateStatus;
    private FirebaseFirestore db;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        EdgeToEdge.enable(this);

        db = FirebaseFirestore.getInstance();

        adminCalendarView = findViewById(R.id.adminCalendarView);
        btnBlockTime = findViewById(R.id.btnBlockTime);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        tvDateStatus = findViewById(R.id.tvDateStatus);

        // --- Κλείδωμα παλαιότερων ημερομηνιών ---
        adminCalendarView.setMinDate(System.currentTimeMillis() - 1000);

        // Αρχική ημερομηνία
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date(adminCalendarView.getDate()));

        // Έλεγχος για τη σημερινή μέρα με το που ανοίγει το activity
        checkIfDateIsAvailable(selectedDate);

        adminCalendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = sdf.format(calendar.getTime());

            // Οπτικός έλεγχος αμέσως μόλις αλλάξει η μέρα
            checkIfDateIsAvailable(selectedDate);
        });

        btnAdminLogout.setOnClickListener(v -> {
            Intent intent = new Intent(AdminActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnBlockTime.setOnClickListener(v -> saveBlockToFirebase());
    }

    private void checkIfDateIsAvailable(String dateStr) {
        // Κατά τη διάρκεια του ελέγχου, απενεργοποιούμε το κουμπί
        btnBlockTime.setEnabled(false);
        tvDateStatus.setText("Έλεγχος διαθεσιμότητας...");
        tvDateStatus.setTextColor(Color.GRAY);

        db.collection("appointments")
                .whereEqualTo("date", dateStr)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot documents = task.getResult();
                        if (documents != null && !documents.isEmpty()) {
                            // ΟΠΤΙΚΗ ΑΛΛΑΓΗ: Η μέρα έχει ραντεβού (ή έχει ήδη δεσμευτεί από τον Admin)
                            tvDateStatus.setText("❌ Η ΜΕΡΑ ΕΧΕΙ ΚΛΕΙΣΜΕΝΑ ΡΑΝΤΕΒΟΥ");
                            tvDateStatus.setTextColor(Color.parseColor("#FF4C4C")); // Κόκκινο
                            btnBlockTime.setEnabled(false);
                            btnBlockTime.setAlpha(0.3f); // Ημι-διαφανές
                        } else {
                            // ΟΠΤΙΚΗ ΑΛΛΑΓΗ: Η μέρα είναι ελεύθερη
                            tvDateStatus.setText("✅ Η ΜΕΡΑ ΕΙΝΑΙ ΕΛΕΥΘΕΡΗ");
                            tvDateStatus.setTextColor(Color.parseColor("#4CFF4C")); // Πράσινο
                            btnBlockTime.setEnabled(true);
                            btnBlockTime.setAlpha(1.0f);
                        }
                    } else {
                        tvDateStatus.setText("Σφάλμα σύνδεσης.");
                    }
                });
    }

    private void saveBlockToFirebase() {
        btnBlockTime.setEnabled(false);
        btnBlockTime.setText("Δέσμευση...");

        Map<String, Object> block = new HashMap<>();
        block.put("userId", "ADMIN_BLOCK");
        block.put("service", "ΚΛΕΙΣΤΟ (ΟΛΟΚΛΗΡΗ ΜΕΡΑ)");
        block.put("date", selectedDate);
        block.put("time", "10:00");
        block.put("duration", 480);

        db.collection("appointments").add(block)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AdminActivity.this, "Η μέρα κλείστηκε!", Toast.LENGTH_SHORT).show();
                    btnBlockTime.setText("Δέσμευση Ολόκληρης Μέρας");
                    // Επανέλεγχος για να αλλάξει το UI σε "Κόκκινο" εφόσον πλέον υπάρχει το block
                    checkIfDateIsAvailable(selectedDate);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminActivity.this, "Σφάλμα κατά τη δέσμευση.", Toast.LENGTH_SHORT).show();
                    btnBlockTime.setEnabled(true);
                    btnBlockTime.setText("Δέσμευση Ολόκληρης Μέρας");
                });
    }
}

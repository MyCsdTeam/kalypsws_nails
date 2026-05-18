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

/**
 * Κλάση διαχείρισης (Admin). Επιτρέπει στον διαχειριστή να βλέπει
 * τα ραντεβού ανά ημέρα και να δεσμεύει (μπλοκάρει) ολόκληρες ημέρες.
 */
public class AdminActivity extends AppCompatActivity {

    // Δήλωση στοιχείων διεπαφής (UI)
    private CalendarView adminCalendarView;
    private Button btnBlockTime, btnAdminLogout;
    private TextView tvDateStatus;
    private LinearLayout appointmentsLayout;

    // Σύνδεση με τη βάση δεδομένων
    private FirebaseFirestore db;
    private String selectedDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        // Αρχικοποίηση του στιγμιοτύπου του Firestore
        db = FirebaseFirestore.getInstance();

        // Διασύνδεση των μεταβλητών με τα αντίστοιχα View στο XML
        adminCalendarView = findViewById(R.id.adminCalendarView);
        btnBlockTime = findViewById(R.id.btnBlockTime);
        btnAdminLogout = findViewById(R.id.btnAdminLogout);
        tvDateStatus = findViewById(R.id.tvDateStatus);
        appointmentsLayout = findViewById(R.id.appointmentsLayout);

        // Αποτροπή επιλογής παρελθοντικών ημερομηνιών από το ημερολόγιο
        adminCalendarView.setMinDate(System.currentTimeMillis() - 1000);

        // Αρχικοποίηση της τρέχουσας επιλεγμένης ημερομηνίας με την τρέχουσα μέρα
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        selectedDate = sdf.format(new Date(adminCalendarView.getDate()));

        // Έλεγχος διαθεσιμότητας για την προεπιλεγμένη ημερομηνία κατά την εκκίνηση
        checkIfDateIsAvailable(selectedDate);

        // Ακροατής συμβάντων (Listener) για την αλλαγή ημερομηνίας στο ημερολόγιο
        adminCalendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            selectedDate = sdf.format(calendar.getTime());

            // Ενημέρωση της λίστας ραντεβού για τη νέα ημερομηνία
            checkIfDateIsAvailable(selectedDate);
        });

        // Λειτουργία αποσύνδεσης διαχειριστή και επιστροφή στην οθόνη σύνδεσης
        btnAdminLogout.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            // Εκκαθάριση της στοίβας (back stack) ώστε να μην μπορεί να επιστρέψει με το back button
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Δέσμευση της επιλεγμένης ημερομηνίας
        btnBlockTime.setOnClickListener(v -> saveBlockToFirebase());
    }

    /**
     * Ελέγχει τη βάση δεδομένων για ραντεβού τη συγκεκριμένη ημερομηνία
     * και ενημερώνει δυναμικά το UI.
     */
    private void checkIfDateIsAvailable(String dateStr) {
        // Αρχικοποίηση κατάστασης UI πριν την ολοκλήρωση του ερωτήματος
        btnBlockTime.setEnabled(false);
        tvDateStatus.setText("Αναζήτηση...");
        appointmentsLayout.removeAllViews();

        // Άντληση εγγράφων από τη συλλογή "appointments" για την επιλεγμένη ημερομηνία
        db.collection("appointments")
                .whereEqualTo("date", dateStr)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();

                        if (!docs.isEmpty()) {
                            // Έλεγχος αν η ημέρα έχει μπλοκαριστεί από τον ίδιο τον διαχειριστή
                            boolean isBlockedByAdmin = false;
                            for (DocumentSnapshot doc : docs) {
                                if ("ADMIN_BLOCK".equals(doc.getString("userId"))) {
                                    isBlockedByAdmin = true;
                                    break;
                                }
                            }

                            // Ενημέρωση της ετικέτας κατάστασης της ημέρας
                            if (isBlockedByAdmin) {
                                tvDateStatus.setText("🔒 ΗΜΕΡΑ ΔΕΣΜΕΥΜΕΝΗ (ADMIN)");
                                tvDateStatus.setTextColor(Color.parseColor("#FF4C4C"));
                            } else {
                                tvDateStatus.setText("📅 ΥΠΑΡΧΟΥΝ ΡΑΝΤΕΒΟΥ ΠΕΛΑΤΩΝ");
                                tvDateStatus.setTextColor(Color.parseColor("#FF66B2"));
                            }

                            // Ταξινόμηση των ραντεβού χρονολογικά (βάσει της ώρας)
                            Collections.sort(docs, (d1, d2) -> {
                                String t1 = d1.getString("time");
                                String t2 = d2.getString("time");
                                return (t1 != null && t2 != null) ? t1.compareTo(t2) : 0;
                            });

                            // Δημιουργία και προσθήκη καρτών ραντεβού στο UI για κάθε έγγραφο
                            for (DocumentSnapshot doc : docs) {
                                createAppCard(doc);
                            }

                            // Απενεργοποίηση του κουμπιού δέσμευσης εφόσον υπάρχουν ήδη εγγραφές
                            btnBlockTime.setEnabled(false);
                            btnBlockTime.setAlpha(0.3f);
                        } else {
                            // Καμία εγγραφή για την ημερομηνία, άρα είναι διαθέσιμη προς δέσμευση
                            tvDateStatus.setText("✅ Η ΗΜΕΡΑ ΕΙΝΑΙ ΕΛΕΥΘΕΡΗ");
                            tvDateStatus.setTextColor(Color.parseColor("#4CFF4C"));
                            btnBlockTime.setEnabled(true);
                            btnBlockTime.setAlpha(1.0f);
                        }
                    }
                });
    }

    /**
     * Δημιουργεί δυναμικά ένα TextView που αναπαριστά ένα ραντεβού
     * και το προσθέτει στο κύριο Layout της οθόνης.
     */
    private void createAppCard(DocumentSnapshot doc) {
        String time = doc.getString("time");
        String service = doc.getString("service");
        String userId = doc.getString("userId");

        // Αρχικοποίηση και παραμετροποίηση του TextView
        TextView tv = new TextView(this);
        tv.setTextSize(14);
        tv.setPadding(35, 35, 35, 35);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTypeface(null, Typeface.BOLD);

        // Ρύθμιση περιθωρίων (margins)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 12);
        tv.setLayoutParams(params);

        // Διαμόρφωση του φόντου (περίγραμμα και στρογγυλεμένες γωνίες)
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(15f);
        gd.setColor(Color.parseColor("#151515"));

        // Διαφοροποίηση της εμφάνισης ανάλογα με το αν είναι δέσμευση ή ραντεβού πελάτη
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

        // Προσθήκη του παραγόμενου View στον γονικό υποδοχέα
        appointmentsLayout.addView(tv);
    }

    /**
     * Εγγράφει μια εικονική κράτηση στη βάση δεδομένων που δρα ως δέσμευση (block)
     * για την επιλεγμένη ημερομηνία, ώστε να μην μπορούν να κλείσουν ραντεβού οι πελάτες.
     */
    private void saveBlockToFirebase() {
        btnBlockTime.setEnabled(false);
        btnBlockTime.setText("Αποθήκευση...");

        // Δομή των δεδομένων της δέσμευσης
        Map<String, Object> block = new HashMap<>();
        block.put("userId", "ADMIN_BLOCK");
        block.put("service", "ΗΜΕΡΑ ΔΕΣΜΕΥΜΕΝΗ ΑΠΟ ΔΙΑΧΕΙΡΙΣΤΗ");
        block.put("date", selectedDate);
        block.put("time", "10:00"); // Τυπική ώρα για να αποφευχθούν null exceptions σε συγκρίσεις
        block.put("duration", 480); // Αντιστοιχεί θεωρητικά σε πλήρες 8ωρο (σε λεπτά)

        // Αποστολή στο Firestore
        db.collection("appointments").add(block)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Η ημέρα δεσμεύτηκε!", Toast.LENGTH_SHORT).show();
                    btnBlockTime.setText("Δέσμευση Ολόκληρης Ημέρας");

                    // Ανανέωση της διεπαφής για να αποτυπωθεί η δέσμευση
                    checkIfDateIsAvailable(selectedDate);
                })
                .addOnFailureListener(e -> {
                    // Επαναφορά του κουμπιού σε περίπτωση αποτυχίας
                    btnBlockTime.setEnabled(true);
                    btnBlockTime.setText("Δέσμευση Ολόκληρης Ημέρας");
                });
    }
}
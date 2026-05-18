package auth.csd.kalypsws_nails;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Η κύρια οθόνη (Home) της εφαρμογής για τους εγγεγραμμένους χρήστες (πελάτες).
 * Διαχειρίζεται την προβολή του προφίλ, το κλείσιμο νέων ραντεβού,
 * την προβολή ιστορικού και την ακύρωση υφιστάμενων ραντεβού.
 */
public class HomeActivity extends AppCompatActivity {

    // Στοιχεία διεπαφής (UI)
    private TextView tvGreeting;
    private Button btnLogout, btnBookAppointment, btnMyAppointments;

    // Εργαλεία Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Αρχικοποίηση στοιχείων UI
        tvGreeting = findViewById(R.id.tvGreeting);
        btnLogout = findViewById(R.id.btnLogout);
        btnBookAppointment = findViewById(R.id.tvBookAppointment);
        btnMyAppointments = findViewById(R.id.btnMyAppointments);

        // Προσθήκη υπογράμμισης (underline) στα κουμπιά για καλύτερη οπτική ένδειξη (UX)
        if (btnBookAppointment instanceof Button) {
            btnBookAppointment.setPaintFlags(btnBookAppointment.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }
        btnMyAppointments.setPaintFlags(btnMyAppointments.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        // Αρχικοποίηση στιγμιοτύπων Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Έλεγχος ταυτοποίησης χρήστη και άντληση ονόματος από το Firestore για προσωποποιημένο χαιρετισμό
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            tvGreeting.setText("Hello, " + task.getResult().getString("username") + "!");
                        } else {
                            tvGreeting.setText("Hello User!");
                        }
                    });
        } else {
            // Ανακατεύθυνση στην οθόνη σύνδεσης αν η συνεδρία έχει λήξει
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        }

        // Λειτουργία αποσύνδεσης χρήστη
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Αντιστοίχιση ενεργειών στα βασικά κουμπιά
        btnBookAppointment.setOnClickListener(v -> showAppointmentDialog());
        btnMyAppointments.setOnClickListener(v -> showCancelAppointmentDialog());
    }

    /**
     * Εμφανίζει ένα δυναμικό παράθυρο διαλόγου με τα μελλοντικά ραντεβού του χρήστη.
     * Επιτρέπει την επιλογή και ακύρωση ενός ραντεβού, εφόσον απομένουν περισσότερες από 24 ώρες.
     */
    private void showCancelAppointmentDialog() {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Αναζήτηση ραντεβού...");
        loading.show();

        // Ερώτημα (Query) στη βάση για τα ραντεβού του τρέχοντος χρήστη
        db.collection("appointments")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loading.dismiss();

                    // Δυναμική κατασκευή του UI του AlertDialog μέσω κώδικα (programmatically)
                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
                    builder.setTitle("Τα Ραντεβού μου");

                    LinearLayout rootLayout = new LinearLayout(this);
                    rootLayout.setOrientation(LinearLayout.VERTICAL);
                    rootLayout.setPadding(40, 40, 40, 40);

                    // Προσθήκη ScrollView για περιπτώσεις πολλών ραντεβού
                    ScrollView scrollView = new ScrollView(this);
                    LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
                    scrollView.setLayoutParams(scrollParams);

                    LinearLayout itemsContainer = new LinearLayout(this);
                    itemsContainer.setOrientation(LinearLayout.VERTICAL);
                    scrollView.addView(itemsContainer);

                    rootLayout.addView(scrollView);

                    // Ρύθμιση του κουμπιού ακύρωσης (Αρχικά απενεργοποιημένο)
                    Button btnCancelAction = new Button(this);
                    btnCancelAction.setText("Ακύρωση Επιλεγμένου Ραντεβού");
                    btnCancelAction.setAllCaps(false);
                    btnCancelAction.setTextColor(Color.WHITE);
                    btnCancelAction.setBackgroundColor(Color.parseColor("#FF4C4C"));
                    btnCancelAction.setEnabled(false);
                    btnCancelAction.setAlpha(0.5f);
                    btnCancelAction.setPadding(20, 30, 20, 30);

                    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    btnParams.setMargins(0, 30, 0, 0);
                    btnCancelAction.setLayoutParams(btnParams);

                    rootLayout.addView(btnCancelAction);

                    // Πίνακες για διατήρηση της κατάστασης της επιλογής (λόγω lambda expressions)
                    final String[] selectedId = {null};
                    final String[] selectedInfo = {null};
                    final Date[] selectedDateObj = {null};
                    final String[] emailDetails = new String[3];
                    final View[] lastSelectedView = {null};

                    boolean hasFutureAppointments = false;
                    SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    long currentTime = System.currentTimeMillis();

                    // Επεξεργασία των αποτελεσμάτων του Firestore
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String service = doc.getString("service");
                        String docId = doc.getId();

                        try {
                            Date appDate = fullSdf.parse(date + " " + time);
                            // Φιλτράρισμα: Εμφάνιση μόνο των μελλοντικών ραντεβού
                            if (appDate != null && appDate.getTime() > currentTime) {
                                hasFutureAppointments = true;

                                // Δημιουργία UI κάρτας για κάθε ραντεβού
                                TextView tvApp = new TextView(this);
                                tvApp.setText(service + "\n" + date + " | " + time);
                                tvApp.setTextColor(Color.parseColor("#E8C6C6"));
                                tvApp.setPadding(40, 40, 40, 40);
                                tvApp.setTextSize(16);
                                tvApp.setGravity(Gravity.CENTER);

                                GradientDrawable normalBg = new GradientDrawable();
                                normalBg.setColor(Color.parseColor("#151515"));
                                normalBg.setCornerRadius(15f);
                                normalBg.setStroke(3, Color.parseColor("#E8C6C6"));

                                GradientDrawable selectedBg = new GradientDrawable();
                                selectedBg.setColor(Color.parseColor("#FF66B2"));
                                selectedBg.setCornerRadius(15f);

                                tvApp.setBackground(normalBg);

                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                                params.setMargins(0, 10, 0, 20);
                                tvApp.setLayoutParams(params);

                                // Λειτουργία επιλογής ραντεβού (Highlighting)
                                tvApp.setOnClickListener(v -> {
                                    if (lastSelectedView[0] != null) {
                                        lastSelectedView[0].setBackground(normalBg);
                                        ((TextView)lastSelectedView[0]).setTextColor(Color.parseColor("#E8C6C6"));
                                    }
                                    tvApp.setBackground(selectedBg);
                                    tvApp.setTextColor(Color.BLACK);
                                    lastSelectedView[0] = tvApp;

                                    selectedId[0] = docId;
                                    selectedInfo[0] = service + " στις " + date + " " + time;
                                    selectedDateObj[0] = appDate;
                                    emailDetails[0] = service; emailDetails[1] = date; emailDetails[2] = time;

                                    btnCancelAction.setEnabled(true);
                                    btnCancelAction.setAlpha(1.0f);
                                });

                                itemsContainer.addView(tvApp);
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                    }

                    // Διαχείριση περίπτωσης χωρίς μελλοντικά ραντεβού
                    if (!hasFutureAppointments) {
                        TextView tvNo = new TextView(this);
                        tvNo.setText("Δεν έχετε κανένα προσεχές ραντεβού.");
                        tvNo.setTextColor(Color.parseColor("#E8C6C6"));
                        tvNo.setTextSize(18);
                        tvNo.setGravity(Gravity.CENTER);
                        tvNo.setPadding(0, 50, 0, 50);
                        itemsContainer.addView(tvNo);
                        btnCancelAction.setVisibility(View.GONE);
                    }

                    builder.setView(rootLayout);
                    builder.setNegativeButton("Κλείσιμο", (d, w) -> d.dismiss());
                    AlertDialog mainDialog = builder.create();

                    // Λογική ελέγχου 24 ωρών και εκτέλεση διαγραφής
                    btnCancelAction.setOnClickListener(v -> {
                        long diffHours = (selectedDateObj[0].getTime() - System.currentTimeMillis()) / (60 * 60 * 1000);
                        if (diffHours < 24) {
                            Toast.makeText(this, "Αδυναμία ακύρωσης. Απομένουν λιγότερες από 24 ώρες!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Επιβεβαίωση διαγραφής
                        new AlertDialog.Builder(this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                                .setTitle("Επιβεβαίωση Ακύρωσης")
                                .setMessage("Είστε σίγουροι ότι θέλετε να ακυρώσετε το ραντεβού σας για:\n\n" + selectedInfo[0] + ";")
                                .setPositiveButton("Ναι, Ακύρωση", (d, w) -> {
                                    db.collection("appointments").document(selectedId[0]).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Το ραντεβού ακυρώθηκε επιτυχώς.", Toast.LENGTH_LONG).show();
                                                mainDialog.dismiss();
                                                FirebaseUser user = mAuth.getCurrentUser();

                                                // Αποστολή ενημερωτικού Email ακύρωσης
                                                if (user != null && user.getEmail() != null) {
                                                    String subject = "Ακύρωση Ραντεβού - Kalypsw's Nails 💅";
                                                    String body = "Γεια σου!\n\nΤο ραντεβού σου ακυρώθηκε επιτυχώς.\n\n" +
                                                            "Υπηρεσία: " + emailDetails[0] + "\n" +
                                                            "Ημερομηνία: " + emailDetails[1] + "\n" +
                                                            "Ώρα: " + emailDetails[2] + "\n\nΕλπίζουμε να σε ξαναδούμε σύντομα!\nKalypsw's Nails";
                                                    sendEmail(user.getEmail(), getString(R.string.sender_email),
                                                            getString(R.string.email_app_password), subject, body);
                                                }
                                            })
                                            .addOnFailureListener(e -> Toast.makeText(this, "Σφάλμα κατά τη διαγραφή.", Toast.LENGTH_SHORT).show());
                                })
                                .setNegativeButton("Πίσω", null).show();
                    });

                    mainDialog.show();
                })
                .addOnFailureListener(e -> {
                    loading.dismiss();
                    Toast.makeText(this, "Σφάλμα σύνδεσης.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Εμφανίζει το παράθυρο διαλόγου για τη δημιουργία νέου ραντεβού.
     * Περιλαμβάνει επιλογή υπηρεσίας, ημερομηνίας (CalendarView) και δυναμικό έλεγχο διαθέσιμων ωρών.
     */
    private void showAppointmentDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_appointment);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Spinner spinnerService = dialog.findViewById(R.id.spinnerService);
        CalendarView calendarView = dialog.findViewById(R.id.calendarView);
        TextView tvSelectTimeLabel = dialog.findViewById(R.id.tvSelectTimeLabel);
        Spinner spinnerHours = dialog.findViewById(R.id.spinnerHours);
        Button btnConfirmAppointment = dialog.findViewById(R.id.btnConfirmAppointment);
        Button btnEmergency = dialog.findViewById(R.id.btnEmergency);

        // Περιορισμός ημερολογίου μόνο σε μελλοντικές ημερομηνίες
        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String[] currentSelectedDate = {sdf.format(new Date(calendarView.getDate()))};

        // Λίστα διαθέσιμων υπηρεσιών
        String[] services = {"Επίλεξε Υπηρεσία...", "Gel Επιμήκυνση (2 ώρες)", "Ακρυλικό Επιμήκυνση (2 ώρες)", "Συντήρηση (1.5 ώρα)", "Ημιμόνιμο (1 ώρα)", "Σπασμένο Νύχι SOS (20 λεπτά)"};

        // Προσαρμοσμένος Adapter (Custom Styling) για το Spinner Υπηρεσιών
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, services) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(Color.parseColor("#151515"));
                TextView tv = (TextView) view;
                tv.setPadding(40, 40, 40, 40);
                tv.setTextSize(16);

                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.parseColor("#E8C6C6"));
                }
                return view;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextSize(16);
                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.parseColor("#FF66B2"));
                }
                return view;
            }
        };
        spinnerService.setAdapter(serviceAdapter);

        // Συντόμευση για άμεση επιλογή υπηρεσίας "SOS"
        btnEmergency.setOnClickListener(v -> spinnerService.setSelection(5));

        // Ακροατής επιλογής υπηρεσίας για δυναμική ενημέρωση των ωρών
        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAvailableHoursFromFirebase(currentSelectedDate[0], position, spinnerHours, tvSelectTimeLabel);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Ακροατής αλλαγής ημερομηνίας
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            currentSelectedDate[0] = sdf.format(calendar.getTime());
            loadAvailableHoursFromFirebase(currentSelectedDate[0], spinnerService.getSelectedItemPosition(), spinnerHours, tvSelectTimeLabel);
        });

        // Επιβεβαίωση και αποθήκευση του ραντεβού
        btnConfirmAppointment.setOnClickListener(v -> {
            if (spinnerService.getSelectedItemPosition() > 0 && spinnerHours.getVisibility() == View.VISIBLE && spinnerHours.getSelectedItemPosition() > 0) {
                String timeSelection = spinnerHours.getSelectedItem().toString();
                // Αποτροπή αποθήκευσης αν ο χρήστης καταφέρει να επιλέξει δεσμευμένη ώρα
                if (timeSelection.contains("Δεσμευμένο")) return;

                String service = spinnerService.getSelectedItem().toString();
                String cleanTime = timeSelection.split(" ")[0];
                int duration = 0;
                int pos = spinnerService.getSelectedItemPosition();

                // Υπολογισμός διάρκειας ραντεβού ανάλογα με την υπηρεσία
                if (pos == 1 || pos == 2) duration = 120;
                else if (pos == 3) duration = 90;
                else if (pos == 4) duration = 60;
                else if (pos == 5) duration = 20;

                // Δημιουργία Map για αποστολή στο Firestore
                Map<String, Object> appointment = new HashMap<>();
                appointment.put("userId", mAuth.getCurrentUser().getUid());
                appointment.put("service", service);
                appointment.put("date", currentSelectedDate[0]);
                appointment.put("time", cleanTime);
                appointment.put("duration", duration);

                // Δημιουργούμε final μεταβλητή για να μπορεί να διαβαστεί σωστά μέσα στο lambda
                final int finalDuration = duration;

                db.collection("appointments").add(appointment).addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Το ραντεβού έκλεισε επιτυχώς!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    FirebaseUser user = mAuth.getCurrentUser();

                    // Αποστολή ενημερωτικού Email επιβεβαίωσης
                    if (user != null && user.getEmail() != null) {
                        sendEmail(user.getEmail(), getString(R.string.sender_email), getString(R.string.email_app_password),
                                "Επιβεβαίωση Ραντεβού - Kalypsw's Nails 💅",
                                "Γεια σου!\n\nΤο ραντεβού σου επιβεβαιώθηκε με επιτυχία.\n\nΥπηρεσία: " + service + "\nΗμερομηνία: " + currentSelectedDate[0] + "\nΏρα: " + cleanTime + "\n\nΣε περιμένουμε!\nKalypsw's Nails");
                    }

                    // Ενσωμάτωση (Intent) για προσθήκη του ραντεβού στο τοπικό ημερολόγιο της συσκευής
                    new AlertDialog.Builder(HomeActivity.this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                            .setTitle("Προσθήκη στο Ημερολόγιο")
                            .setMessage("Θέλετε να προσθέσετε το ραντεβού στο ημερολόγιο του κινητού σας;")
                            .setPositiveButton("Ναι", (dialogInterface, i) -> {
                                try {
                                    SimpleDateFormat sdfCalendar = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                    Date dateObj = sdfCalendar.parse(currentSelectedDate[0] + " " + cleanTime);

                                    if (dateObj != null) {
                                        long startTime = dateObj.getTime();
                                        long endTime = startTime + (finalDuration * 60 * 1000L);

                                        Intent calendarIntent = new Intent(Intent.ACTION_INSERT)
                                                .setData(android.provider.CalendarContract.Events.CONTENT_URI)
                                                .putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
                                                .putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endTime)
                                                .putExtra(android.provider.CalendarContract.Events.TITLE, "Ραντεβού: " + service + " 💅")
                                                .putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "Το προγραμματισμένο σου ραντεβού για " + service)
                                                .putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, "Kalypsw's Nails Salon");

                                        startActivity(calendarIntent);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Toast.makeText(HomeActivity.this, "Δεν ήταν δυνατή η πρόσβαση στο ημερολόγιο", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Όχι", null)
                            .show();
                });
            } else {
                Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    /**
     * Αντλεί από τη βάση δεδομένων τα κλεισμένα ραντεβού για τη συγκεκριμένη ημέρα
     * και εντοπίζει τα κενά διαστήματα (διαθέσιμες ώρες).
     */
    private void loadAvailableHoursFromFirebase(String dateStr, int servicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        if (servicePos == 0) { tvSelectTimeLabel.setVisibility(View.GONE); spinnerHours.setVisibility(View.GONE); return; }

        tvSelectTimeLabel.setVisibility(View.VISIBLE);
        tvSelectTimeLabel.setText("Φόρτωση...");

        db.collection("appointments").whereEqualTo("date", dateStr).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<int[]> booked = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String time = document.getString("time");
                    Long dur = document.getLong("duration");
                    if (time != null) {
                        String[] p = time.split(":");
                        // Μετατροπή της ώρας σε λεπτά της ημέρας για ευκολότερο έλεγχο επικαλύψεων (overlap)
                        int start = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
                        booked.add(new int[]{start, start + (dur != null ? dur.intValue() : 60)});
                    }
                }
                populateHoursSpinner(booked, servicePos, spinnerHours, tvSelectTimeLabel);
            }
        });
    }

    /**
     * Υπολογίζει τις διαθέσιμες ώρες βάσει της διάρκειας της επιλεγμένης υπηρεσίας
     * και ενημερώνει το Spinner εμφάνισης των ωρών.
     */
    private void populateHoursSpinner(List<int[]> booked, int servicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        tvSelectTimeLabel.setText("3. Διαθέσιμες Ώρες:");
        spinnerHours.setVisibility(View.VISIBLE);
        List<String> hoursList = new ArrayList<>();
        hoursList.add("Επίλεξε Ώρα...");

        int duration = 60; // Προεπιλεγμένη διάρκεια
        if (servicePos == 1 || servicePos == 2) duration = 120;
        else if (servicePos == 3) duration = 90;
        else if (servicePos == 5) duration = 20;

        // Έλεγχος ωραρίου από τις 10:00 (600 λεπτά) έως τις 18:00 (1080 λεπτά) ανά 30 λεπτά
        for (int m = 600; m + duration <= 1080; m += 30) {
            boolean overlap = false;
            for (int[] b : booked) {
                // Έλεγχος εάν το διάστημα του νέου ραντεβού (m έως m+duration) συγκρούεται με υφιστάμενο
                if (m < b[1] && b[0] < (m + duration)) { overlap = true; break; }
            }
            String t = String.format(Locale.getDefault(), "%02d:%02d", m / 60, m % 60);
            hoursList.add(overlap ? t + " (Δεσμευμένο)" : t + " (Διαθέσιμο)");
        }

        // Προσαρμοσμένος Adapter για το Spinner των Ωρών
        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, hoursList) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) return false;
                // Απενεργοποίηση της δυνατότητας κλικ στα δεσμευμένα χρονικά διαστήματα
                return !getItem(position).contains("Δεσμευμένο");
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(Color.parseColor("#151515"));
                TextView tv = (TextView) view;
                tv.setPadding(40, 40, 40, 40);
                tv.setTextSize(16);

                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                    tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                } else if (getItem(position).contains("Δεσμευμένο")) {
                    tv.setTextColor(Color.parseColor("#FF4C4C")); // Κόκκινο χρώμα για τα μη διαθέσιμα
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Προσθήκη γραμμής διαγραφής
                } else {
                    tv.setTextColor(Color.parseColor("#4CFF4C")); // Πράσινο χρώμα για τα διαθέσιμα
                    tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                }
                return view;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextSize(16);
                if (position == 0) {
                    tv.setTextColor(Color.GRAY);
                } else {
                    tv.setTextColor(Color.parseColor("#FF66B2"));
                }
                return view;
            }
        };

        spinnerHours.setAdapter(hoursAdapter);
    }

    /**
     * Υπηρεσία αποστολής Email στο παρασκήνιο (Background Thread) χρησιμοποιώντας το JavaMail API.
     */
    private void sendEmail(String to, String sender, String pass, String sub, String body) {
        // Χρήση ExecutorService για αποφυγή μπλοκαρίσματος του κεντρικού UI Thread (NetworkOnMainThreadException)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() { return new PasswordAuthentication(sender, pass); }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(sender, "Kalypsw's Nails"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(sub);
                message.setText(body);

                Transport.send(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
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

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private Button btnLogout, btnBookAppointment, btnMyAppointments;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        tvGreeting = findViewById(R.id.tvGreeting);
        btnLogout = findViewById(R.id.btnLogout);
        btnBookAppointment = findViewById(R.id.tvBookAppointment);
        btnMyAppointments = findViewById(R.id.btnMyAppointments);

        if (btnBookAppointment instanceof Button) {
            btnBookAppointment.setPaintFlags(btnBookAppointment.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }
        btnMyAppointments.setPaintFlags(btnMyAppointments.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

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
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        }

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnBookAppointment.setOnClickListener(v -> showAppointmentDialog());
        btnMyAppointments.setOnClickListener(v -> showCancelAppointmentDialog());
    }

    private void showCancelAppointmentDialog() {
        ProgressDialog loading = new ProgressDialog(this);
        loading.setMessage("Αναζήτηση ραντεβού...");
        loading.show();

        db.collection("appointments")
                .whereEqualTo("userId", mAuth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loading.dismiss();

                    AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog);
                    builder.setTitle("Τα Ραντεβού μου");

                    LinearLayout rootLayout = new LinearLayout(this);
                    rootLayout.setOrientation(LinearLayout.VERTICAL);
                    rootLayout.setPadding(40, 40, 40, 40);

                    ScrollView scrollView = new ScrollView(this);
                    LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
                    scrollView.setLayoutParams(scrollParams);

                    LinearLayout itemsContainer = new LinearLayout(this);
                    itemsContainer.setOrientation(LinearLayout.VERTICAL);
                    scrollView.addView(itemsContainer);

                    rootLayout.addView(scrollView);

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

                    final String[] selectedId = {null};
                    final String[] selectedInfo = {null};
                    final Date[] selectedDateObj = {null};
                    final String[] emailDetails = new String[3];
                    final View[] lastSelectedView = {null};

                    boolean hasFutureAppointments = false;
                    SimpleDateFormat fullSdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    long currentTime = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String service = doc.getString("service");
                        String docId = doc.getId();

                        try {
                            Date appDate = fullSdf.parse(date + " " + time);
                            if (appDate != null && appDate.getTime() > currentTime) {
                                hasFutureAppointments = true;

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

                    btnCancelAction.setOnClickListener(v -> {
                        long diffHours = (selectedDateObj[0].getTime() - System.currentTimeMillis()) / (60 * 60 * 1000);
                        if (diffHours < 24) {
                            Toast.makeText(this, "Αδυναμία ακύρωσης. Απομένουν λιγότερες από 24 ώρες!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        new AlertDialog.Builder(this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                                .setTitle("Επιβεβαίωση Ακύρωσης")
                                .setMessage("Είστε σίγουροι ότι θέλετε να ακυρώσετε το ραντεβού σας για:\n\n" + selectedInfo[0] + ";")
                                .setPositiveButton("Ναι, Ακύρωση", (d, w) -> {
                                    db.collection("appointments").document(selectedId[0]).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(this, "Το ραντεβού ακυρώθηκε επιτυχώς.", Toast.LENGTH_LONG).show();
                                                mainDialog.dismiss();
                                                FirebaseUser user = mAuth.getCurrentUser();
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

        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String[] currentSelectedDate = {sdf.format(new Date(calendarView.getDate()))};

        String[] services = {"Επίλεξε Υπηρεσία...", "Gel Επιμήκυνση (2 ώρες)", "Ακρυλικό Επιμήκυνση (2 ώρες)", "Συντήρηση (1.5 ώρα)", "Ημιμόνιμο (1 ώρα)", "Σπασμένο Νύχι SOS (20 λεπτά)"};

        // --- Custom Styling για το Υπηρεσία Spinner ---
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, services) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(Color.parseColor("#151515")); // Σκούρο φόντο
                TextView tv = (TextView) view;
                tv.setPadding(40, 40, 40, 40); // Κενά γύρω γύρω
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
                    tv.setTextColor(Color.parseColor("#FF66B2")); // Έντονο ροζ όταν έχει επιλεγεί
                }
                return view;
            }
        };
        spinnerService.setAdapter(serviceAdapter);

        btnEmergency.setOnClickListener(v -> spinnerService.setSelection(5));

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAvailableHoursFromFirebase(currentSelectedDate[0], position, spinnerHours, tvSelectTimeLabel);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            currentSelectedDate[0] = sdf.format(calendar.getTime());
            loadAvailableHoursFromFirebase(currentSelectedDate[0], spinnerService.getSelectedItemPosition(), spinnerHours, tvSelectTimeLabel);
        });

        btnConfirmAppointment.setOnClickListener(v -> {
            if (spinnerService.getSelectedItemPosition() > 0 && spinnerHours.getVisibility() == View.VISIBLE && spinnerHours.getSelectedItemPosition() > 0) {
                String timeSelection = spinnerHours.getSelectedItem().toString();
                if (timeSelection.contains("Δεσμευμένο")) return;
                String service = spinnerService.getSelectedItem().toString();
                String cleanTime = timeSelection.split(" ")[0];
                int duration = 0;
                int pos = spinnerService.getSelectedItemPosition();
                if (pos == 1 || pos == 2) duration = 120; else if (pos == 3) duration = 90; else if (pos == 4) duration = 60; else if (pos == 5) duration = 20;

                Map<String, Object> appointment = new HashMap<>();
                appointment.put("userId", mAuth.getCurrentUser().getUid());
                appointment.put("service", service);
                appointment.put("date", currentSelectedDate[0]);
                appointment.put("time", cleanTime);
                appointment.put("duration", duration);

                db.collection("appointments").add(appointment).addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Το ραντεβού έκλεισε επιτυχώς!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null && user.getEmail() != null) {
                        sendEmail(user.getEmail(), getString(R.string.sender_email), getString(R.string.email_app_password),
                                "Επιβεβαίωση Ραντεβού - Kalypsw's Nails 💅",
                                "Γεια σου!\n\nΤο ραντεβού σου επιβεβαιώθηκε με επιτυχία.\n\nΥπηρεσία: " + service + "\nΗμερομηνία: " + currentSelectedDate[0] + "\nΏρα: " + cleanTime + "\n\nΣε περιμένουμε!\nKalypsw's Nails");
                    }
                });
            } else {
                Toast.makeText(this, "Συμπλήρωσε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void loadAvailableHoursFromFirebase(String dateStr, int servicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        if (servicePos == 0) { tvSelectTimeLabel.setVisibility(View.GONE); spinnerHours.setVisibility(View.GONE); return; }
        tvSelectTimeLabel.setVisibility(View.VISIBLE); tvSelectTimeLabel.setText("Φόρτωση...");
        db.collection("appointments").whereEqualTo("date", dateStr).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<int[]> booked = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    String time = document.getString("time");
                    Long dur = document.getLong("duration");
                    if (time != null) {
                        String[] p = time.split(":");
                        int start = Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
                        booked.add(new int[]{start, start + (dur != null ? dur.intValue() : 60)});
                    }
                }
                populateHoursSpinner(booked, servicePos, spinnerHours, tvSelectTimeLabel);
            }
        });
    }

    private void populateHoursSpinner(List<int[]> booked, int servicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        tvSelectTimeLabel.setText("3. Διαθέσιμες Ώρες:");
        spinnerHours.setVisibility(View.VISIBLE);
        List<String> hoursList = new ArrayList<>();
        hoursList.add("Επίλεξε Ώρα...");

        int duration = 60; // Default
        if (servicePos == 1 || servicePos == 2) duration = 120;
        else if (servicePos == 3) duration = 90;
        else if (servicePos == 5) duration = 20;

        for (int m = 600; m + duration <= 1080; m += 30) {
            boolean overlap = false;
            for (int[] b : booked) {
                if (m < b[1] && b[0] < (m + duration)) { overlap = true; break; }
            }
            String t = String.format(Locale.getDefault(), "%02d:%02d", m / 60, m % 60);
            hoursList.add(overlap ? t + " (Δεσμευμένο)" : t + " (Διαθέσιμο)");
        }

        // --- Custom Styling για το Ώρες Spinner ---
        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, hoursList) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) return false;
                return !getItem(position).contains("Δεσμευμένο"); // Απενεργοποιούμε το κλικ στα δεσμευμένα
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
                    tv.setTextColor(Color.parseColor("#FF4C4C")); // Κόκκινο
                    tv.setPaintFlags(tv.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG); // Γραμμή διαγραφής!
                } else {
                    tv.setTextColor(Color.parseColor("#4CFF4C")); // Πράσινο
                    tv.setPaintFlags(tv.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)); // Αφαίρεση γραμμής
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
                    tv.setTextColor(Color.parseColor("#FF66B2")); // Έντονο ροζ όταν έχει επιλεγεί
                }
                return view;
            }
        };

        spinnerHours.setAdapter(hoursAdapter);
    }

    private void sendEmail(String to, String sender, String pass, String sub, String body) {
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
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
}
package auth.csd.kalypsws_nails;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
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

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting;
    private Button btnLogout, btnBookAppointment;

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

        if (btnBookAppointment instanceof Button) {
            btnBookAppointment.setPaintFlags(btnBookAppointment.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }

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

        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        final String[] currentSelectedDate = {sdf.format(new Date(calendarView.getDate()))};

        String[] services = {
                "Επίλεξε Υπηρεσία...",
                "Gel Επιμήκυνση (2 ώρες)",
                "Ακρυλικό Επιμήκυνση (2 ώρες)",
                "Συντήρηση (1.5 ώρα)",
                "Ημιμόνιμο (1 ώρα)"
        };

        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, services) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(Color.parseColor("#1A1A1A"));
                TextView tv = (TextView) view;
                tv.setTextColor(Color.WHITE);
                tv.setTypeface(null, Typeface.ITALIC);
                tv.setPadding(40, 40, 40, 40);
                return view;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTextColor(Color.parseColor("#E8C6C6"));
                tv.setTypeface(null, Typeface.BOLD_ITALIC);
                return view;
            }
        };
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadAvailableHoursFromFirebase(currentSelectedDate[0], position, spinnerHours, tvSelectTimeLabel);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            currentSelectedDate[0] = sdf.format(calendar.getTime());

            int selectedServicePos = spinnerService.getSelectedItemPosition();
            if (selectedServicePos == 0) {
                Toast.makeText(HomeActivity.this, "Παρακαλώ επίλεξε υπηρεσία πρώτα!", Toast.LENGTH_SHORT).show();
            }
            loadAvailableHoursFromFirebase(currentSelectedDate[0], selectedServicePos, spinnerHours, tvSelectTimeLabel);
        });

        btnConfirmAppointment.setOnClickListener(v -> {
            if (spinnerService.getSelectedItemPosition() > 0 &&
                    spinnerHours.getVisibility() == View.VISIBLE &&
                    spinnerHours.getSelectedItemPosition() > 0) {

                String timeSelection = spinnerHours.getSelectedItem().toString();
                if (timeSelection.contains("Δεσμευμένο")) {
                    Toast.makeText(HomeActivity.this, "Αυτή η ώρα είναι κλεισμένη!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String service = spinnerService.getSelectedItem().toString();
                String cleanTime = timeSelection.split(" ")[0];

                int durationMinutes = 0;
                if (spinnerService.getSelectedItemPosition() == 1 || spinnerService.getSelectedItemPosition() == 2) durationMinutes = 120;
                else if (spinnerService.getSelectedItemPosition() == 3) durationMinutes = 90;
                else if (spinnerService.getSelectedItemPosition() == 4) durationMinutes = 60;

                btnConfirmAppointment.setEnabled(false);
                btnConfirmAppointment.setText("Αποθήκευση...");

                Map<String, Object> appointment = new HashMap<>();
                appointment.put("userId", mAuth.getCurrentUser().getUid());
                appointment.put("service", service);
                appointment.put("date", currentSelectedDate[0]);
                appointment.put("time", cleanTime);
                appointment.put("duration", durationMinutes); // Αποθηκεύουμε πλέον ΚΑΙ τη διάρκεια

                db.collection("appointments").add(appointment)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(HomeActivity.this, "Το ραντεβού έκλεισε επιτυχώς!", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            btnConfirmAppointment.setEnabled(true);
                            btnConfirmAppointment.setText("Επιβεβαίωση");
                            Toast.makeText(HomeActivity.this, "Σφάλμα: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } else {
                Toast.makeText(HomeActivity.this, "Συμπλήρωσε όλα τα πεδία (Υπηρεσία και Ώρα)!", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void loadAvailableHoursFromFirebase(String dateStr, int servicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        if (servicePos == 0) {
            tvSelectTimeLabel.setVisibility(View.GONE);
            spinnerHours.setVisibility(View.GONE);
            return;
        }

        tvSelectTimeLabel.setVisibility(View.VISIBLE);
        tvSelectTimeLabel.setText("Φόρτωση διαθεσιμότητας...");
        tvSelectTimeLabel.setTextColor(Color.parseColor("#E8C6C6"));
        spinnerHours.setVisibility(View.GONE);

        db.collection("appointments")
                .whereEqualTo("date", dateStr)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Λίστα που θα κρατάει [Έναρξη σε λεπτά, Λήξη σε λεπτά] για κάθε κλεισμένο ραντεβού
                        List<int[]> bookedIntervals = new ArrayList<>();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String time = document.getString("time");
                            Long durationLong = document.getLong("duration");
                            int duration = (durationLong != null) ? durationLong.intValue() : 0;

                            // Fallback σε περίπτωση που υπάρχουν παλιά test ραντεβού χωρίς πεδίο duration
                            if (duration == 0) {
                                String svc = document.getString("service");
                                if (svc != null && svc.contains("2 ώρες")) duration = 120;
                                else if (svc != null && svc.contains("1.5 ώρα")) duration = 90;
                                else if (svc != null && svc.contains("1 ώρα")) duration = 60;
                            }

                            if (time != null) {
                                String[] parts = time.split(":");
                                int h = Integer.parseInt(parts[0]);
                                int m = Integer.parseInt(parts[1]);
                                int startMins = h * 60 + m;

                                // ΠΡΟΣΘΗΚΗ ΤΟΥ 20ΛΕΠΤΟΥ ΚΕΝΟΥ
                                int endMins = startMins + duration + 20;
                                bookedIntervals.add(new int[]{startMins, endMins});
                            }
                        }
                        populateHoursSpinner(bookedIntervals, servicePos, spinnerHours, tvSelectTimeLabel);
                    } else {
                        tvSelectTimeLabel.setText("Σφάλμα φόρτωσης ωρών.");
                        tvSelectTimeLabel.setTextColor(Color.parseColor("#FF4C4C"));
                    }
                });
    }

    private void populateHoursSpinner(List<int[]> bookedIntervals, int servicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        int baseDuration = 0;
        if (servicePos == 1 || servicePos == 2) baseDuration = 120;
        else if (servicePos == 3) baseDuration = 90;
        else if (servicePos == 4) baseDuration = 60;

        // Ο συνολικός χρόνος που απαιτείται για το ΝΕΟ ραντεβού (Υπηρεσία + 20 λεπτά)
        int reqTotalMins = baseDuration + 20;

        tvSelectTimeLabel.setText("3. Διαθέσιμες Ώρες:");
        spinnerHours.setVisibility(View.VISIBLE);

        List<String> hoursList = new ArrayList<>();
        hoursList.add("Επίλεξε Ώρα...");

        int openTimeMins = 10 * 60; // 10:00
        int closeTimeMins = 18 * 60; // 18:00
        int stepMins = 30; // Δημιουργία επιλογών ανά 30 λεπτά για ευελιξία

        // Ελέγχουμε κάθε 30λεπτο αν χωράει το ραντεβού
        for (int startMins = openTimeMins; (startMins + reqTotalMins) <= closeTimeMins; startMins += stepMins) {
            int endMins = startMins + reqTotalMins;

            boolean isOverlapping = false;

            // Μαθηματικός έλεγχος: Το νέο ραντεβού "χτυπάει" πάνω σε κάποιο ήδη κλεισμένο;
            for (int[] booked : bookedIntervals) {
                int bStart = booked[0];
                int bEnd = booked[1];

                // Αν η αρχή του νέου ραντεβού είναι πριν το τέλος του παλιού
                // ΚΑΙ η αρχή του παλιού είναι πριν το τέλος του νέου -> Υπάρχει επικάλυψη!
                if (startMins < bEnd && bStart < endMins) {
                    isOverlapping = true;
                    break;
                }
            }

            int h = startMins / 60;
            int m = startMins % 60;
            String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", h, m);

            if (isOverlapping) {
                hoursList.add(timeFormatted + " (Δεσμευμένο)");
            } else {
                hoursList.add(timeFormatted + " (Διαθέσιμο)");
            }
        }

        boolean allBooked = true;
        for (int i = 1; i < hoursList.size(); i++) {
            if (!hoursList.get(i).contains("Δεσμευμένο")) {
                allBooked = false;
                break;
            }
        }

        if (allBooked && hoursList.size() > 1) {
            tvSelectTimeLabel.setText("❌ Δεν υπάρχει αρκετός χρόνος σήμερα!");
            tvSelectTimeLabel.setTextColor(Color.parseColor("#FF4C4C"));
            spinnerHours.setVisibility(View.GONE);
            return;
        }

        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, hoursList) {
            @Override
            public boolean isEnabled(int position) {
                if (position == 0) return false;
                return !getItem(position).contains("Δεσμευμένο");
            }

            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                view.setBackgroundColor(Color.parseColor("#1A1A1A"));
                TextView tv = (TextView) view;
                tv.setTypeface(null, Typeface.ITALIC);
                tv.setPadding(40, 40, 40, 40);

                if (position == 0) tv.setTextColor(Color.GRAY);
                else if (getItem(position).contains("Δεσμευμένο")) tv.setTextColor(Color.parseColor("#FF4C4C"));
                else tv.setTextColor(Color.parseColor("#4CFF4C"));

                return view;
            }

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view;
                tv.setTypeface(null, Typeface.ITALIC);

                if (position == 0) tv.setTextColor(Color.parseColor("#E8C6C6"));
                else if (getItem(position).contains("Δεσμευμένο")) tv.setTextColor(Color.parseColor("#FF4C4C"));
                else tv.setTextColor(Color.parseColor("#4CFF4C"));

                return view;
            }
        };

        hoursAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHours.setAdapter(hoursAdapter);
    }
}

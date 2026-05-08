package auth.csd.kalypsws_nails;

import android.app.Dialog;
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
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView tvBookAppointment = findViewById(R.id.tvBookAppointment);

        tvBookAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppointmentDialog();
            }
        });
    }

    private void showAppointmentDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_appointment);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        Spinner spinnerService = dialog.findViewById(R.id.spinnerService);
        CalendarView calendarView = dialog.findViewById(R.id.calendarView);
        TextView tvSelectTimeLabel = dialog.findViewById(R.id.tvSelectTimeLabel);
        Spinner spinnerHours = dialog.findViewById(R.id.spinnerHours);
        Button btnConfirmAppointment = dialog.findViewById(R.id.btnConfirmAppointment);

        String[] services = {
                "Επίλεξε Υπηρεσία...",
                "Gel Επιμήκυνση (2 ώρες)",
                "Ακρυλικό Επιμήκυνση (2 ώρες)",
                "Συντήρηση (1.5 ώρα)",
                "Ημιμόνιμο (1 ώρα)"
        };
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, services);
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);

        // 1. Τι γίνεται όταν αλλάζει ΥΠΗΡΕΣΙΑ ο χρήστης
        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Κάθε φορά που αλλάζει υπηρεσία, ανανεώνουμε δυναμικά τις ώρες
                updateAvailableHours(position, spinnerHours, tvSelectTimeLabel);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 2. Τι γίνεται όταν αλλάζει ΗΜΕΡΟΜΗΝΙΑ ο χρήστης
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                int selectedServicePos = spinnerService.getSelectedItemPosition();
                if (selectedServicePos == 0) {
                    Toast.makeText(HomeActivity.this, "Παρακαλώ επίλεξε υπηρεσία πρώτα!", Toast.LENGTH_SHORT).show();
                }
                // Ανανεώνουμε δυναμικά τις ώρες
                updateAvailableHours(selectedServicePos, spinnerHours, tvSelectTimeLabel);
            }
        });

        // 3. Λογική Κουμπιού Επιβεβαίωσης
        btnConfirmAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (spinnerService.getSelectedItemPosition() > 0 &&
                        spinnerHours.getVisibility() == View.VISIBLE &&
                        spinnerHours.getSelectedItemPosition() > 0) {

                    String service = spinnerService.getSelectedItem().toString();
                    String time = spinnerHours.getSelectedItem().toString();
                    Toast.makeText(HomeActivity.this, "Κράτηση: " + service + " στις " + time, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(HomeActivity.this, "Συμπλήρωσε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    // --- Η "ΕΞΥΠΝΗ" ΜΕΘΟΔΟΣ ΠΟΥ ΥΠΟΛΟΓΙΖΕΙ ΤΙΣ ΩΡΕΣ ΔΥΝΑΜΙΚΑ ---
    private void updateAvailableHours(int selectedServicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        // Αν δεν έχει επιλέξει υπηρεσία, κρύβουμε τις ώρες
        if (selectedServicePos == 0) {
            tvSelectTimeLabel.setVisibility(View.GONE);
            spinnerHours.setVisibility(View.GONE);
            return;
        }

        int durationMinutes = 0;
        if (selectedServicePos == 1 || selectedServicePos == 2) {
            durationMinutes = 120; // 2 ώρες
        } else if (selectedServicePos == 3) {
            durationMinutes = 90;  // 1.5 ώρα
        } else if (selectedServicePos == 4) {
            durationMinutes = 60;  // 1 ώρα
        }

        tvSelectTimeLabel.setVisibility(View.VISIBLE);
        spinnerHours.setVisibility(View.VISIBLE);

        List<String> hoursList = new ArrayList<>();
        hoursList.add("Επίλεξε Ώρα...");

        int openTimeMinutes = 10 * 60; // 10:00
        int closeTimeMinutes = 18 * 60; // 18:00

        // Δυναμικός υπολογισμός βάσει του durationMinutes της ΚΑΘΕ υπηρεσίας
        for (int startMins = openTimeMinutes; (startMins + durationMinutes) <= closeTimeMinutes; startMins += durationMinutes) {
            int h = startMins / 60;
            int m = startMins % 60;
            String timeFormatted = String.format("%02d:%02d", h, m);
            hoursList.add(timeFormatted);
        }

        ArrayAdapter<String> hoursAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, hoursList);
        hoursAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHours.setAdapter(hoursAdapter);
    }
}
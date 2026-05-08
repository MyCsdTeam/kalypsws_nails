package auth.csd.kalypsws_nails;

import android.app.Dialog;
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
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView tvBookAppointment = findViewById(R.id.tvBookAppointment);

        // Υπογράμμιση στο "Book your appointment" για να φαίνεται σαν link
        tvBookAppointment.setPaintFlags(tvBookAppointment.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

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
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        Spinner spinnerService = dialog.findViewById(R.id.spinnerService);
        CalendarView calendarView = dialog.findViewById(R.id.calendarView);
        TextView tvSelectTimeLabel = dialog.findViewById(R.id.tvSelectTimeLabel);
        Spinner spinnerHours = dialog.findViewById(R.id.spinnerHours);
        Button btnConfirmAppointment = dialog.findViewById(R.id.btnConfirmAppointment);

        calendarView.setMinDate(System.currentTimeMillis() - 1000);

        String[] services = {
                "Επίλεξε Υπηρεσία...",
                "Gel Επιμήκυνση (2 ώρες)",
                "Ακρυλικό Επιμήκυνση (2 ώρες)",
                "Συντήρηση (1.5 ώρα)",
                "Ημιμόνιμο (1 ώρα)"
        };

        // Custom Adapter για τις Υπηρεσίες (Διόρθωση χρωμάτων και φόντου)
        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, services) {
            @Override
            public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                // Σκούρο φόντο στη λίστα για να φαίνονται τα λευκά γράμματα
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
                tv.setTextColor(Color.parseColor("#E8C6C6")); // Απαλό ροζ στο κλειστό μενού
                tv.setTypeface(null, Typeface.BOLD_ITALIC);
                return view;
            }
        };
        serviceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(serviceAdapter);

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateAvailableHours(position, spinnerHours, tvSelectTimeLabel);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Έλεγχος Ημερομηνίας
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(@NonNull CalendarView view, int year, int month, int dayOfMonth) {
                int selectedServicePos = spinnerService.getSelectedItemPosition();

                // Παράδειγμα: Η 13η μέρα εμφανίζεται ως "Πλήρως Κλεισμένη"
                if (dayOfMonth == 13) {
                    tvSelectTimeLabel.setVisibility(View.VISIBLE);
                    tvSelectTimeLabel.setText("❌ Η μέρα είναι πλήρως κλεισμένη!");
                    tvSelectTimeLabel.setTextColor(Color.parseColor("#FF4C4C"));
                    spinnerHours.setVisibility(View.GONE);
                    return;
                } else {
                    tvSelectTimeLabel.setText("3. Διαθέσιμες Ώρες:");
                    tvSelectTimeLabel.setTextColor(Color.parseColor("#E8C6C6"));
                }

                if (selectedServicePos == 0) {
                    Toast.makeText(HomeActivity.this, "Παρακαλώ επίλεξε υπηρεσία πρώτα!", Toast.LENGTH_SHORT).show();
                }
                updateAvailableHours(selectedServicePos, spinnerHours, tvSelectTimeLabel);
            }
        });

        btnConfirmAppointment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    Toast.makeText(HomeActivity.this, "Επιτυχία: " + service + " στις " + cleanTime, Toast.LENGTH_LONG).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(HomeActivity.this, "Συμπλήρωσε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        dialog.show();
    }

    private void updateAvailableHours(int selectedServicePos, Spinner spinnerHours, TextView tvSelectTimeLabel) {
        if (tvSelectTimeLabel.getText().toString().contains("❌")) return;

        if (selectedServicePos == 0) {
            tvSelectTimeLabel.setVisibility(View.GONE);
            spinnerHours.setVisibility(View.GONE);
            return;
        }

        int durationMinutes = 0;
        if (selectedServicePos == 1 || selectedServicePos == 2) durationMinutes = 120;
        else if (selectedServicePos == 3) durationMinutes = 90;
        else if (selectedServicePos == 4) durationMinutes = 60;

        tvSelectTimeLabel.setVisibility(View.VISIBLE);
        spinnerHours.setVisibility(View.VISIBLE);

        List<String> hoursList = new ArrayList<>();
        hoursList.add("Επίλεξε Ώρα...");

        int openTimeMinutes = 10 * 60;
        int closeTimeMinutes = 18 * 60;

        for (int startMins = openTimeMinutes; (startMins + durationMinutes) <= closeTimeMinutes; startMins += durationMinutes) {
            int h = startMins / 60;
            int m = startMins % 60;
            String timeFormatted = String.format("%02d:%02d", h, m);

            // Παράδειγμα δεσμευμένων ωρών
            if (timeFormatted.equals("12:00") || timeFormatted.equals("14:30")) {
                hoursList.add(timeFormatted + " (Δεσμευμένο)");
            } else {
                hoursList.add(timeFormatted + " (Διαθέσιμο)");
            }
        }

        // Custom Adapter για τις Ώρες (Πράσινο/Κόκκινο και Σκούρο Φόντο)
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
package auth.csd.kalypsws_nails;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    private TextView tvGreeting; // Άλλαξε το όνομα εδώ
    private Button btnLogout, btnBookAppointment; // Προστέθηκε το κουμπί ραντεβού

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        tvGreeting = findViewById(R.id.tvGreeting);
        btnLogout = findViewById(R.id.btnLogout);
        btnBookAppointment = findViewById(R.id.tvBookAppointment); // Αρχικοποίηση του νέου κουμπιού

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();

            db.collection("users").document(uid).get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String username = document.getString("username");
                                tvGreeting.setText("Hello, " + username + "!");
                            } else {
                                tvGreeting.setText("Hello User!");
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "Σφάλμα βάσης δεδομένων", Toast.LENGTH_SHORT).show();
                            tvGreeting.setText("Hello User!");
                        }
                    });
        } else {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        }

        // Λειτουργία Logout
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Εδώ θα μπει η λειτουργία για το ραντεβού αργότερα!
        btnBookAppointment.setOnClickListener(v -> {
            Toast.makeText(HomeActivity.this, "Σύντομα κοντά σας...", Toast.LENGTH_SHORT).show();
            // TODO: Μετάβαση στο BookAppointmentActivity
        });
    }
}
package auth.csd.kalypsws_nails;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Οθόνη αυθεντικοποίησης (Login) της εφαρμογής.
 * Διαχειρίζεται την είσοδο των χρηστών μέσω Firebase Authentication
 * και διαχωρίζει τη δρομολόγηση μεταξύ απλών πελατών και του διαχειριστή (Admin).
 */
public class LoginActivity extends AppCompatActivity {

    // Στοιχεία διεπαφής χρήστη (UI)
    private Button btnBackLogin, btnLoginSubmit;
    private EditText etEmailLogin, etPasswordLogin;

    // Στιγμιότυπο για την επικοινωνία με την υπηρεσία ταυτοποίησης της Firebase
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Αρχικοποίηση του Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Διασύνδεση των μεταβλητών Java με τα αντίστοιχα στοιχεία του XML
        btnBackLogin = findViewById(R.id.btnBackLogin);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);

        // Λειτουργία επιστροφής στην προηγούμενη οθόνη (αρχική οθόνη υποδοχής)
        btnBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Λειτουργία υποβολής διαπιστευτηρίων
        btnLoginSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Άντληση και καθαρισμός (trim) των δεδομένων εισόδου από τον χρήστη
                String email = etEmailLogin.getText().toString().trim();
                String password = etPasswordLogin.getText().toString().trim();

                // Βασικός έλεγχος εγκυρότητας (Validation) για κενά πεδία
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Συμπληρώστε email και password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Ανάκτηση των κωδικών διαχειριστή από τα ασφαλή resources της εφαρμογής (strings.xml)
                String adminEmail = getString(R.string.admin_email);
                String adminPass = getString(R.string.admin_pass);

                // Έλεγχος αν ο χρήστης που επιχειρεί σύνδεση είναι ο διαχειριστής
                if (email.equals(adminEmail) && password.equals(adminPass)) {
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(LoginActivity.this, "Καλωσήρθες Admin!", Toast.LENGTH_SHORT).show();
                                        // Δρομολόγηση στο πάνελ ελέγχου του διαχειριστή
                                        Intent intent = new Intent(LoginActivity.this, AdminActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(LoginActivity.this, "Σφάλμα Admin: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                    return;
                }

                // Ροή εκτέλεσης για τους απλούς χρήστες (Πελάτες)
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    Toast.makeText(LoginActivity.this, "Σύνδεση επιτυχής!", Toast.LENGTH_SHORT).show();
                                    // Δρομολόγηση στην κύρια οθόνη κρατήσεων του πελάτη
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Εμφάνιση μηνύματος λάθους σε περίπτωση αποτυχίας (π.χ. λάθος κωδικός)
                                    Toast.makeText(LoginActivity.this, "Σφάλμα: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
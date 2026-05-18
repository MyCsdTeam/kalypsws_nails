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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Οθόνη εγγραφής (Sign Up) νέου χρήστη.
 * Διαχειρίζεται τη δημιουργία του λογαριασμού μέσω Firebase Authentication
 * και την παράλληλη δημιουργία του προφίλ χρήστη στο Cloud Firestore.
 */
public class SignupActivity extends AppCompatActivity {

    // Στοιχεία διεπαφής χρήστη (UI)
    private Button btnBackSignup, btnSignupSubmit;
    private EditText etUsernameSignup, etEmailSignup, etPasswordSignup, etConfirmPassword;

    // Στιγμιότυπα υπηρεσιών της Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // Αρχικοποίηση των υπηρεσιών ταυτοποίησης και βάσης δεδομένων
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Διασύνδεση των μεταβλητών κώδικα με τα στοιχεία του Layout (XML)
        btnBackSignup = findViewById(R.id.btnBackSignup);
        btnSignupSubmit = findViewById(R.id.btnSignupSubmit);
        etUsernameSignup = findViewById(R.id.etUsernameSignup);
        etEmailSignup = findViewById(R.id.etEmailSignup);
        etPasswordSignup = findViewById(R.id.etPasswordSignup);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Λειτουργία επιστροφής στην προηγούμενη οθόνη (ακύρωση εγγραφής)
        btnBackSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Λειτουργία υποβολής φόρμας εγγραφής
        btnSignupSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Άντληση και καθαρισμός δεδομένων (trimming) από τα πεδία εισαγωγής
                String username = etUsernameSignup.getText().toString().trim();
                String email = etEmailSignup.getText().toString().trim();
                String pass = etPasswordSignup.getText().toString().trim();
                String confPass = etConfirmPassword.getText().toString().trim();

                // Βασικός έλεγχος εγκυρότητας (Validation) για μη συμπληρωμένα πεδία
                if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Παρακαλώ συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Επιβεβαίωση ορθής πληκτρολόγησης κωδικού
                if (!pass.equals(confPass)) {
                    Toast.makeText(SignupActivity.this, "Τα passwords δεν ταιριάζουν!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Έλεγχος ελάχιστου απαιτούμενου μήκους κωδικού από την πολιτική της Firebase
                if (pass.length() < 6) {
                    Toast.makeText(SignupActivity.this, "Ο κωδικός πρέπει να είναι τουλάχιστον 6 χαρακτήρες!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Αίτημα δημιουργίας νέου χρήστη στο Firebase Authentication
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Ανάκτηση του μοναδικού αναγνωριστικού (UID) που δημιουργήθηκε για τον χρήστη
                                    String userId = mAuth.getCurrentUser().getUid();

                                    // Δομή δεδομένων για το προφίλ του χρήστη
                                    Map<String, Object> user = new HashMap<>();
                                    user.put("username", username);
                                    user.put("email", email);

                                    // Αποθήκευση του προφίλ στη συλλογή "users" του Firestore με κλειδί το UID
                                    db.collection("users").document(userId)
                                            .set(user)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(SignupActivity.this, "Η εγγραφή ολοκληρώθηκε! Παρακαλώ συνδεθείτε.", Toast.LENGTH_LONG).show();

                                                // Επιτυχής ροή: Ανακατεύθυνση στην οθόνη σύνδεσης
                                                // και εκκαθάριση της στοίβας (back stack) ώστε ο χρήστης να μην μπορεί να γυρίσει πίσω
                                                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);

                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Διαχείριση σφάλματος κατά την εγγραφή στη βάση δεδομένων
                                                Toast.makeText(SignupActivity.this, "Σφάλμα αποθήκευσης στη βάση: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    // Διαχείριση σφάλματος κατά τη δημιουργία λογαριασμού (π.χ. το email υπάρχει ήδη)
                                    Toast.makeText(SignupActivity.this, "Σφάλμα εγγραφής: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
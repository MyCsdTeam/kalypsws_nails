package auth.csd.kalypsws_nails;

import android.content.Intent; // Προστέθηκε για τη μετάβαση
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Προσθήκη των βιβλιοθηκών Firebase
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private Button btnBackSignup, btnSignupSubmit;
    private EditText etUsernameSignup, etEmailSignup, etPasswordSignup, etConfirmPassword;

    // Δήλωση των μεταβλητών Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // 1. Αρχικοποίηση Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 2. Σύνδεση με τα στοιχεία του XML
        btnBackSignup = findViewById(R.id.btnBackSignup);
        btnSignupSubmit = findViewById(R.id.btnSignupSubmit);
        etUsernameSignup = findViewById(R.id.etUsernameSignup);
        etEmailSignup = findViewById(R.id.etEmailSignup);
        etPasswordSignup = findViewById(R.id.etPasswordSignup);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Λειτουργία κουμπιού Back
        btnBackSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 3. Λειτουργία κουμπιού Sign Up (Εγγραφή) με σύνδεση Firebase
        btnSignupSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsernameSignup.getText().toString().trim();
                String email = etEmailSignup.getText().toString().trim();
                String pass = etPasswordSignup.getText().toString().trim();
                String confPass = etConfirmPassword.getText().toString().trim();

                // Έλεγχος αν τα πεδία είναι συμπληρωμένα
                if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Παρακαλώ συμπληρώστε όλα τα πεδία!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Έλεγχος αν ταιριάζουν οι κωδικοί
                if (!pass.equals(confPass)) {
                    Toast.makeText(SignupActivity.this, "Τα passwords δεν ταιριάζουν!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Έλεγχος μήκους κωδικού (το Firebase απαιτεί τουλάχιστον 6 χαρακτήρες)
                if (pass.length() < 6) {
                    Toast.makeText(SignupActivity.this, "Ο κωδικός πρέπει να είναι τουλάχιστον 6 χαρακτήρες!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // ΒΗΜΑ Α: Δημιουργία Λογαριασμού στο Firebase Authentication
                mAuth.createUserWithEmailAndPassword(email, pass)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Αν η εγγραφή πετύχει, παίρνουμε το μοναδικό ID (UID) του χρήστη
                                    String userId = mAuth.getCurrentUser().getUid();

                                    // ΒΗΜΑ Β: Αποθήκευση του Username στο Cloud Firestore
                                    Map<String, Object> user = new HashMap<>();
                                    user.put("username", username);
                                    user.put("email", email);

                                    db.collection("users").document(userId)
                                            .set(user)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(SignupActivity.this, "Η εγγραφή ολοκληρώθηκε! Παρακαλώ συνδεθείτε.", Toast.LENGTH_LONG).show();

                                                // ΑΛΛΑΓΗ: Αυτόματη μετάβαση στο LoginActivity
                                                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                                // Καθαρίζουμε το stack των οθονών
                                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                startActivity(intent);

                                                finish(); // Κλείνει το SignupActivity
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(SignupActivity.this, "Σφάλμα αποθήκευσης στη βάση: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    // Αν αποτύχει η εγγραφή στο Authentication (π.χ. το email υπάρχει ήδη)
                                    Toast.makeText(SignupActivity.this, "Σφάλμα εγγραφής: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
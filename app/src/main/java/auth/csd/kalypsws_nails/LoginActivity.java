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

// Βιβλιοθήκες Firebase
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private Button btnBackLogin, btnLoginSubmit;
    private EditText etEmailLogin, etPasswordLogin;
    private FirebaseAuth mAuth; // Δήλωση Firebase Auth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Αρχικοποίηση Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        btnBackLogin = findViewById(R.id.btnBackLogin);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        etEmailLogin = findViewById(R.id.etEmailLogin); // Ενημερωμένο ID
        etPasswordLogin = findViewById(R.id.etPasswordLogin);

        btnBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnLoginSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = etEmailLogin.getText().toString().trim();
                String password = etPasswordLogin.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Συμπληρώστε email και password!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Λογική Σύνδεσης Firebase
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Επιτυχία: Μετάβαση στο Home
                                    Toast.makeText(LoginActivity.this, "Σύνδεση επιτυχής!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    // Αποτυχία
                                    Toast.makeText(LoginActivity.this, "Σφάλμα: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
            }
        });
    }
}
package auth.csd.kalypsws_nails;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {

    private Button btnBackSignup, btnSignupSubmit;
    private EditText etUsernameSignup, etEmailSignup, etPasswordSignup, etConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        // 1. Βρίσκουμε τα στοιχεία από το αρχείο activity_signup.xml
        btnBackSignup = findViewById(R.id.btnBackSignup);
        btnSignupSubmit = findViewById(R.id.btnSignupSubmit);
        etUsernameSignup = findViewById(R.id.etUsernameSignup);
        etEmailSignup = findViewById(R.id.etEmailSignup);
        etPasswordSignup = findViewById(R.id.etPasswordSignup);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // 2. Λειτουργία κουμπιού Back (Επιστροφή)
        btnBackSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Κλείνει την οθόνη Signup και γυρνάει στην MainActivity
            }
        });

        // 3. Λειτουργία κουμπιού Sign Up (Εγγραφή)
        btnSignupSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pass = etPasswordSignup.getText().toString();
                String confPass = etConfirmPassword.getText().toString();

                // Έλεγχος αν ταιριάζουν οι κωδικοί
                if (pass.equals(confPass) && !pass.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Επιτυχής προσπάθεια εγγραφής!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SignupActivity.this, "Τα passwords δεν ταιριάζουν ή είναι κενά!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
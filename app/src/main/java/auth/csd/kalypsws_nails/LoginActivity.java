package auth.csd.kalypsws_nails;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private Button btnBackLogin, btnLoginSubmit;
    private EditText etUsernameLogin, etPasswordLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 1. Βρίσκουμε τα στοιχεία από το αρχείο activity_login.xml
        btnBackLogin = findViewById(R.id.btnBackLogin);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);
        etUsernameLogin = findViewById(R.id.etUsernameLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);

        // 2. Λειτουργία κουμπιού Back (Επιστροφή)
        btnBackLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Κλείνει την οθόνη Login και γυρνάει στην MainActivity
            }
        });

        // 3. Λειτουργία κουμπιού Login
        btnLoginSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUsernameLogin.getText().toString();
                String password = etPasswordLogin.getText().toString();

                // Προσωρινό μήνυμα για να δούμε ότι δουλεύει (αργότερα θα μπει η SQLite)
                Toast.makeText(LoginActivity.this, "Προσπάθεια σύνδεσης: " + username, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
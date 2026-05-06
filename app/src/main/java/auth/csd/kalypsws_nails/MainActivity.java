package auth.csd.kalypsws_nails;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button; // Μην ξεχάσεις αυτό το import
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Δήλωση των μεταβλητών για τα κουμπιά
    private Button btnLoginMain;
    private Button btnSignupMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Σύνδεση των μεταβλητών με τα κουμπιά του αρχείου activity_main.xml
        // ΠΡΟΣΟΧΗ: Αν έχεις βάλει διαφορετικά id στο xml, άλλαξέ τα εδώ
        btnLoginMain = findViewById(R.id.btnLogin);
        btnSignupMain = findViewById(R.id.btnSignUp);

        // 2. Λειτουργία (Λογική) για το κουμπί του Login
        btnLoginMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Δημιουργία Intent για μετάβαση στο LoginActivity
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        // 3. Λειτουργία (Λογική) για το κουμπί του Signup
        btnSignupMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Δημιουργία Intent για μετάβαση στο SignupActivity
                Intent signupIntent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(signupIntent);
            }
        });
    }
}
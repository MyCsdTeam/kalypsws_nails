package auth.csd.kalypsws_nails;

import android.content.Intent;
import android.os.Bundle;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Αρχική οθόνη (Entry Point) της εφαρμογής.
 * Παρέχει στον χρήστη τις βασικές επιλογές πλοήγησης για σύνδεση, εγγραφή
 * και ανακατεύθυνση στα μέσα κοινωνικής δικτύωσης του καταστήματος.
 */
public class MainActivity extends AppCompatActivity {

    // Στοιχεία διεπαφής χρήστη (UI)
    private Button btnLoginMain;
    private Button btnSignupMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Διαμόρφωση UI για εμφάνιση σε όλη την οθόνη (Edge-to-Edge)
        // και προσαρμογή των περιθωρίων βάσει των system bars της συσκευής
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Διασύνδεση των μεταβλητών κώδικα με τα αντίστοιχα στοιχεία διεπαφής στο XML
        btnLoginMain = findViewById(R.id.btnLogin);
        btnSignupMain = findViewById(R.id.btnSignUp);

        // Ορισμός ακροατή συμβάντων (Listener) για μετάβαση στην οθόνη ταυτοποίησης (Login)
        btnLoginMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent loginIntent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(loginIntent);
            }
        });

        // Ορισμός ακροατή συμβάντων (Listener) για μετάβαση στην οθόνη εγγραφής νέου χρήστη (Signup)
        btnSignupMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signupIntent = new Intent(MainActivity.this, SignupActivity.class);
                startActivity(signupIntent);
            }
        });
    }

    /**
     * Εξωτερική διασύνδεση.
     * Ανακατευθύνει τον χρήστη στο επαγγελματικό προφίλ του καταστήματος στο Instagram.
     * Προσπαθεί να ανοίξει την εγγενή εφαρμογή, διαφορετικά καταφεύγει σε χρήση browser.
     */
    public void openInstagramProfile(View view) {
        String username = "kalypsws_nails";

        // Δημιουργία Intent για προβολή (ACTION_VIEW) του συγκεκριμένου URI
        Uri uri = Uri.parse("http://instagram.com/_u/" + username);
        Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

        // Στόχευση του συγκεκριμένου πακέτου για να ανοίξει απευθείας η εφαρμογή του Instagram
        likeIng.setPackage("com.instagram.android");

        try {
            startActivity(likeIng);
        } catch (android.content.ActivityNotFoundException e) {
            // Fallback μηχανισμός: Αν δεν υπάρχει η εφαρμογή εγκατεστημένη, άνοιγμα μέσω web browser
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://instagram.com/" + username)));
        }
    }
}
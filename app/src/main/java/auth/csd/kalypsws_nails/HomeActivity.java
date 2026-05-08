package auth.csd.kalypsws_nails; // ΠΡΟΣΟΧΗ: Κράτα το δικό σου package name εδώ

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Αυτή η γραμμή συνδέει την Java με το XML παρουσιαστικό που έφτιαξες
        setContentView(R.layout.activity_home);
    }
}

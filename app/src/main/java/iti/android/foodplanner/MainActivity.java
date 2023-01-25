package iti.android.foodplanner;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.ArrayList;
import java.util.List;

import iti.android.foodplanner.data.authentication.AuthenticationFactory;
import iti.android.foodplanner.data.backup.BackupManager;
import iti.android.foodplanner.data.models.meal.MealsItem;
import iti.android.foodplanner.data.shared.SharedManager;
import iti.android.foodplanner.databinding.ActivityMainAppBinding;
import iti.android.foodplanner.ui.features.sign_in_with_google.SignUpOrLoginActivity;

public class MainActivity extends AppCompatActivity {

    private ActivityMainAppBinding binding;
    private NavController navController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainAppBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navigationUiSettings();

        BackupManager.getInstance(SharedManager.getInstance(this)).restoreData(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (!value.isEmpty()){
                    List<MealsItem> mealsItemList = new ArrayList<>();
                    for(DocumentSnapshot ds : value)   {
                        MealsItem mealsItem = ds.toObject(MealsItem.class);
                        mealsItemList.add(mealsItem);
                    }
                    Toast.makeText(MainActivity.this, "Hello OOO+: "+mealsItemList.size(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void navigationUiSettings() {
        BottomNavigationView navView = findViewById(R.id.nav_view);
        int[] pages = {R.id.navigation_home,R.id.navigation_favorite,R.id.navigation_category, R.id.navigation_plan,R.id.navigation_details,R.id.navigation_onboarding,R.id.navigation_search};

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(pages)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main_app);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.option_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.settings_option:
                break;
            case R.id.signout_option:
                logoutFromApp();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void logoutFromApp() {
        int autProvider = SharedManager.getInstance(this).getUser().getAuthProvider();
        AuthenticationFactory.authenticationManager(autProvider)
                .logout(this);
        startActivity(new Intent(this, SignUpOrLoginActivity.class));
        finish();
    }

    @Override
    public boolean onNavigateUp() {
        return navController.navigateUp() || super.onNavigateUp();
    }
}
package iti.android.foodplanner.ui.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import iti.android.foodplanner.R;
import iti.android.foodplanner.data.models.meal.MealPlan;
import iti.android.foodplanner.data.room.Week;
import iti.android.foodplanner.ui.features.category.CategoryFragmentDirections;
import iti.android.foodplanner.ui.features.home.HomeFragmentDirections;
import iti.android.foodplanner.ui.features.login.LoginActivity;

public class Utils {
    public static RecyclerView recyclerViewHandler(RecyclerView recyclerView, Context context){
        // Function to handle recyclerview and adjust its settings
        recyclerView.setHasFixedSize(true);
//        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        return recyclerView;
    }
    public static boolean isValidEmail(CharSequence email) {
        CharSequence target=email;
        Pattern pattern;
        Matcher matcher;
        pattern = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
        matcher = pattern.matcher(target);
        return (!TextUtils.isEmpty(target) && matcher.matches());
    }

    public static void loadImage(Context context, String path, ImageView imageView){
        Glide.with(context)
                .load(path)
                        .apply(new RequestOptions()
                        .override(400,300)
                        .placeholder(R.drawable.animated_vecotr)
                        .error(R.drawable.shippingback))
                .into(imageView);
    }
    public static void navigatorCategoryToSearchFragment(View view, int type, String searchQuerry){
        CategoryFragmentDirections.ActionNavigationCategoryToNavigationSearch action = CategoryFragmentDirections.actionNavigationCategoryToNavigationSearch(type);
        action.setSearch(searchQuerry);
        Navigation.findNavController(view).navigate(action);
    }

    public static void setAutoCompleteCash(Context context,String[] data, AutoCompleteTextView search) {
        // Auto Complete
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, data);
        search.setAdapter(adapter);
    }
    public static void navigatorHomeToSearchFragment(View view, int type,String searchQuerry){
        HomeFragmentDirections.ActionNavigationHomeToNavigationSearch action = HomeFragmentDirections.actionNavigationHomeToNavigationSearch(type);
        action.setSearch(searchQuerry);
        Navigation.findNavController(view).navigate(action);
    }

    public static boolean isValidPassword(CharSequence password) {

        Pattern pattern;
        Matcher matcher;

        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{4,}$";

        pattern = Pattern.compile(PASSWORD_PATTERN);
        matcher = pattern.matcher(password);

        return matcher.matches();

    }

    public static ProgressDialog loadingDialog(Context context){
       return ProgressDialog.show(context, "",
                "Loading. Please wait...", true);
    }

    public static Snackbar snakeMessage(Context context,View view,String message,boolean isGood,String btnTitle,View.OnClickListener clickListener){
        Snackbar snackbar = Snackbar.make(view,message,Snackbar.LENGTH_LONG);
        if (isGood){
            snackbar.setBackgroundTint(context.getColor(R.color.green));
        }else {
            snackbar.setBackgroundTint(context.getColor(R.color.red));
        }

        snackbar.setAction(btnTitle, clickListener);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);


        return snackbar;
    }



    public static Snackbar snakeMessage(Context context,View view,String message,boolean isGood){
        Snackbar snackbar = Snackbar.make(view,message,Snackbar.LENGTH_LONG);
        if (isGood){
            snackbar.setBackgroundTint(context.getColor(R.color.green));
        }else {
            snackbar.setBackgroundTint(context.getColor(R.color.red));
        }
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) view.getLayoutParams();
        params.gravity = Gravity.TOP;
        view.setLayoutParams(params);
        snackbar.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_SLIDE);


        return snackbar;
    }
}

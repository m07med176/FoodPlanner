package iti.android.foodplanner.data.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;

import iti.android.foodplanner.data.models.meal.MealPlan;
import iti.android.foodplanner.data.models.meal.MealsItem;


@Database(entities = {MealsItem.class , MealPlan.class}, version = 11,exportSchema = false)
public abstract class RoomDatabase extends androidx.room.RoomDatabase {
    public static final String DATABASE_FILE_NAME = "foodPlanner.db";
    private  static volatile RoomDatabase instance = null;
    // Entities
    public abstract FavoriteDAO FavoriteDAO();
    public abstract PlaneFoodDAO PlaneFoodDAO();

    public static synchronized RoomDatabase getInstance(Context context){
        if (instance == null)
            instance = Room
                    .databaseBuilder(context.getApplicationContext(), RoomDatabase.class,DATABASE_FILE_NAME)
                    .fallbackToDestructiveMigration()
                    .build();
        return instance;
    }
}

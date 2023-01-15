package iti.android.foodplanner.data.models;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class Ingredients{

	@SerializedName("meals")
	private List<MealsItem> meals;

	public void setMeals(List<MealsItem> meals){
		this.meals = meals;
	}

	public List<MealsItem> getMeals(){
		return meals;
	}

	@Override
 	public String toString(){
		return 
			"Ingredients{" + 
			"meals = '" + meals + '\'' + 
			"}";
		}
}
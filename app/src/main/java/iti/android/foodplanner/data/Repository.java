package iti.android.foodplanner.data;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import iti.android.foodplanner.data.backup.BackupManager;
import iti.android.foodplanner.data.models.User;
import iti.android.foodplanner.data.models.selections.Ingredient.Ingredient;
import iti.android.foodplanner.data.models.selections.area.Area;
import iti.android.foodplanner.data.models.selections.area.AreasList;
import iti.android.foodplanner.data.models.selections.category.Category;
import iti.android.foodplanner.data.models.selections.categoryFeed.CategoriesFeed;
import iti.android.foodplanner.data.models.selections.categoryFeed.CategoriesItem;
import iti.android.foodplanner.data.models.selections.category.CategoriesList;
import iti.android.foodplanner.data.models.selections.Ingredient.IngredientsList;
import iti.android.foodplanner.data.models.meal.MealPlan;
import iti.android.foodplanner.data.models.meal.MealsItem;
import iti.android.foodplanner.data.models.meal.MealsList;
import iti.android.foodplanner.data.network.ApiCalls;
import iti.android.foodplanner.data.network.Network;
import iti.android.foodplanner.data.room.RoomDatabase;
import iti.android.foodplanner.data.room.Week;
import iti.android.foodplanner.data.shared.SharedManager;

/**
 * Gather all functions from Network, Room Database And Shared Manager
 */
public class Repository {
    private static final String TAG = "Repository";

    public static final int DELETE_FAV = 1;
    public static final int DELETE_PLAN = 2;
    public static final int DELETE_PLAN_AND_FAV = 3;
    private final ApiCalls apiCalls;
    private final RoomDatabase roomDatabase;
    private final BackupManager backupManager;
    private final SharedManager sharedManager;
    public static Repository repository = null;
    public static Repository getInstance(Context context){
        if (repository==null)
            repository = new Repository(context);
        return repository;
    }

    private Repository(Context context) {
            apiCalls = Network.apiCalls;
            roomDatabase = RoomDatabase.getInstance(context);
            sharedManager = SharedManager.getInstance(context);
            backupManager = BackupManager.getInstance(sharedManager);
    }

    // region Shared
    public boolean isUser(){
        return sharedManager.isUser();
    }

    public void saveUser(User user){
        sharedManager.saveUser(user);
    }

    public User getUser(){
        return sharedManager.getUser();
    }

    public String[] getList(String type){
        return sharedManager.getList(type);
    }
    public boolean isFirstEntrance(){
        return sharedManager.isFirstEntrance();
    }


    public void saveEntrance(){
        sharedManager.saveEntrance();
    }


    // endregion Shared
    // region ROOM

    /**
     * this function is responsible for [restore all user data] <i>favorites</i> and <i>MealsPlane</i>
     * <br><b>Steps</b><hr>
     * <br> 1- call backup manager to restore all plane data from user
     * <br> 2- gather array list of objects
     * <br> 3- remove all current table for check [to prevent duplication]
     * <br> 4- if remove happened successfully insert all data to room database
     */
    public void restoreAllData(){
        backupManager.restoreDataPlane(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Gather all items from firebase
                List<MealPlan> mealPlans = new ArrayList<>();
                for(DocumentSnapshot ds : queryDocumentSnapshots)   {
                    MealPlan mealPlan = ds.toObject(MealPlan.class);
                    mealPlans.add(mealPlan);
                }
                Log.d(TAG, "restoreAllData: Gather all items from firebase");

                // remove all data from room [for sure] there is no items to prevent duplication
                roomDatabase.PlaneFoodDAO().removeAllTable()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                Log.d(TAG, "onSubscribe: Remove all db");
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: remove all data successfully");
                                // insert all data in room db
                                roomDatabase.PlaneFoodDAO()
                                        .insertAllTable(mealPlans)
                                        .subscribeOn(Schedulers.io())
                                        .subscribe();

                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG, "onError: error happened during removing "+e.getMessage());
                            }
                        });
            }
        });
        backupManager.restoreDataFavorite(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                // Gather all items from firebase
                List<MealsItem> mealsItemList = new ArrayList<>();
                for(DocumentSnapshot ds : queryDocumentSnapshots)   {
                    MealsItem mealsItem = ds.toObject(MealsItem.class);
                    mealsItemList.add(mealsItem);
                }


                // remove all data from room [for sure] there is no items to prevent duplication
                roomDatabase.FavoriteDAO().removeAllTable()
                        .subscribeOn(Schedulers.io())
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                Log.d(TAG, "onSubscribe:  Remove all db");

                            }

                            @Override
                            public void onComplete() {
                                // insert all data in room db
                                Log.d(TAG, "onComplete: remove all data successfully");

                                roomDatabase.FavoriteDAO()
                                        .insertAllTable(mealsItemList)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe();
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG, "onError: error happened during removing "+e.getMessage());
                            }
                        });


            }
        });
    }

    /**
     * this function responsible for delete all room database or specify any room according type
     * @param type
     */
    public void deleteAllTable(int type){
        CompletableObserver completableObserver = new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {

            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: TABLE HASE BEEN DELETED");
            }

            @Override
            public void onError(@NonNull Throwable e) {
                Log.d(TAG, "onComplete: TABLE HASE BEEN FAILED TO DELETE "+e.getMessage());
            }
        };
            switch (type){
                case DELETE_FAV:
                    roomDatabase.FavoriteDAO()
                            .removeAllTable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(completableObserver);
                    break;
                case DELETE_PLAN:
                    roomDatabase.PlaneFoodDAO().removeAllTable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(completableObserver);

                    break;
                case DELETE_PLAN_AND_FAV:
                    roomDatabase.FavoriteDAO().removeAllTable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(completableObserver);
                    roomDatabase.PlaneFoodDAO().removeAllTable()
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(completableObserver);
                    break;

            }
    }
    private void insertFavoriteToRoom(MealsItem mealsItem,DataFetch<Void> dataFetch){
        roomDatabase
                .FavoriteDAO()
                .insertFavoriteMeal(mealsItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onComplete() {
                        backupManager.saveFavorite(mealsItem);
                        dataFetch.onDataSuccessResponse(null);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }
    public void insertFavoriteMealDataBase(MealsItem mealsItem,DataFetch<Void> dataFetch){
        apiCalls.retrieveMealByID(mealsItem.getIdMeal())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<MealsList>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull MealsList mealsList) {
                        insertFavoriteToRoom(mealsList.getMeals().get(0),dataFetch);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

    }

    public void showFavouriteMealsDataBase(DataFetch<List<MealsItem>> dataFetch){
        roomDatabase
                .FavoriteDAO()
                .showFavouriteMeals()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<MealsItem>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull List<MealsItem> mealsItems) {
                        dataFetch.onDataSuccessResponse(mealsItems);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }
    public void deleteFavorite(MealsItem mealsItem,DataFetch<Void> dataFetch){
        roomDatabase.FavoriteDAO()
                .deleteFavouriteMeal(mealsItem)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onComplete() {
                        backupManager.deleteFavorite(mealsItem);
                        dataFetch.onDataSuccessResponse(null);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                            dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }

    public void insertPlaneMealDataBase(MealPlan mealPlan,DataFetch<Void> dataFetch){
        apiCalls.retrieveMealByID(mealPlan.getIdMeal())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<MealsList>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onSuccess(@NonNull MealsList mealsList) {
                        insertPlanMealRoom(mealPlan.migrateMealsToPlaneModel(mealsList.getMeals().get(0)),dataFetch);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });

    }

    private void insertPlanMealRoom(MealPlan mealPlan, DataFetch<Void> dataFetch){
        roomDatabase.PlaneFoodDAO().insertPlanMeal(mealPlan)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onComplete() {
                        dataFetch.onDataSuccessResponse(null);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                            dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }

    public void showMealPlan(DataFetch<List<MealPlan>> dataFetch){
        roomDatabase
                .PlaneFoodDAO()
                .showPlanMeals()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<MealPlan>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull List<MealPlan> mealPlans) {
                        dataFetch.onDataSuccessResponse(mealPlans);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }
    //git conflict keep both changes
    public void showPlanMealsByDay(Week dayName,DataFetch<List<MealPlan>> dataFetch)
    {
        roomDatabase.PlaneFoodDAO().showPlanMealsByDay(dayName)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<MealPlan>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull List<MealPlan> mealPlans) {
                        dataFetch.onDataSuccessResponse(mealPlans);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }
    public void deletePlanMeal(MealPlan mealPlan,DataFetch<Void> dataFetch){
        roomDatabase.PlaneFoodDAO()
                .deletePlanMeal(mealPlan)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onComplete() {
                            backupManager.deletePlane(mealPlan);
                            dataFetch.onDataSuccessResponse(null);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                            dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });
    }
    // endregion ROOM
    // region API

    /**
     * link: <a href="https://www.themealdb.com/api/json/v1/1/random.php">Single Random Meal</a>
     * @return List<MealsItem>
     */
    public void lookupSingleRandomMeal(DataFetch<List<MealsItem>> dataFetch){
        apiCalls.lookupSingleRandomMeal().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<MealsList>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                dataFetch.onDataLoading();
            }

            @Override
            public void onSuccess(@NonNull MealsList mealsList) {
                if (mealsList!=null && mealsList.getMeals()!=null)
                    dataFetch.onDataSuccessResponse(mealsList.getMeals());
                else
                    dataFetch.onDataSuccessResponse(new ArrayList<>());
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dataFetch.onDataFailedResponse(e.getMessage());
            }
        });
    };


    /**
     * link: <a href="https://www.themealdb.com/api/json/v1/1/list.php?i=list">List of Ingredients</a>
     * @return List<Ingredient>
     */
    public void ingredientsList(DataFetch<List<Ingredient>> dataFetch){
        apiCalls.ingredientsList().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<IngredientsList>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                dataFetch.onDataLoading();
            }

            @Override
            public void onSuccess(@NonNull IngredientsList ingredientsList) {
                if (ingredientsList!=null && ingredientsList.getMeals()!=null) {
                    List<String> arrayList = ingredientsList.getMeals().stream().map(category -> category.getStrIngredient()).collect(Collectors.toList());
                    sharedManager.saveList(SharedManager.INGREDIENTS, arrayList);
                    dataFetch.onDataSuccessResponse(ingredientsList.getMeals());
                }else {
                    dataFetch.onDataSuccessResponse(new ArrayList<>());
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                dataFetch.onDataFailedResponse(e.getMessage());
            }
        });
    };


    /**
     * link: <a href="https://www.themealdb.com/api/json/v1/1/list.php?c=list">List of Categories</a>
     * @return List<Category>
     */
    public void categoriesList(DataFetch<List<Category>> dataFetch){
        apiCalls.categoriesList().subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<CategoriesList>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull CategoriesList categoriesList) {
                        if (categoriesList!=null && categoriesList.getCategory()!=null) {
                            List<String> arrayList = categoriesList.getCategory().stream().map(category -> category.getStrCategory()).collect(Collectors.toList());
                            sharedManager.saveList(SharedManager.CATEGORIES, arrayList);
                            dataFetch.onDataSuccessResponse(categoriesList.getCategory());
                        }else{
                            dataFetch.onDataSuccessResponse(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });

    };

    /**
     * link: <a href="https://www.themealdb.com/api/json/v1/1/list.php?a=list">List of Areas</a>
     * @return List<Area>
     */
    public void areasList(DataFetch<List<Area>> dataFetch){
        apiCalls.areasList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<AreasList>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull AreasList areasList) {
                        if (areasList!=null && areasList.getAreas()!=null){
                            List<String> arrayList = areasList.getAreas().stream().map(category -> category.getStrArea()).collect(Collectors.toList());
                            sharedManager.saveList(SharedManager.AREAS,arrayList);
                            dataFetch.onDataSuccessResponse(areasList.getAreas());
                        }else {
                            dataFetch.onDataSuccessResponse(new ArrayList<>());
                        }

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                            dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });

    };


    /**
     * Link <a href="https://www.themealdb.com/api/json/v1/1/filter.php?c=Dessert"> Result of Short Meals</a>
     * @return List<ShortMeals>
     */

    public void retrieveFilterResults(
            String category,
            String ingredient,
            String area,
            DataFetch<List<MealsItem>> dataFetch
    ){
        apiCalls
                .retrieveFilterResults(category,ingredient,area)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<MealsList>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull MealsList mealsList) {

                        if (mealsList !=null && mealsList.getMeals() != null)  // sometime api return null
                            dataFetch.onDataSuccessResponse(mealsList.getMeals());
                        else
                            dataFetch.onDataSuccessResponse(new ArrayList<>());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });;

    };

    /**
     * Link <a href="https://www.themealdb.com/api/json/v1/1/categories.php">Categories List</a>
     * @return List<CategoriesItem>
     */
    public void retrieveCategoriesList(DataFetch<List<CategoriesItem>> dataFetch){

         apiCalls
                 .retrieveCategoriesList()
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(new SingleObserver<CategoriesFeed>() {
                     @Override
                     public void onSubscribe(@NonNull Disposable d) {
                         dataFetch.onDataLoading();
                     }

                     @Override
                     public void onSuccess(@NonNull CategoriesFeed categoriesFeed) {
                         if (categoriesFeed !=null && categoriesFeed.getCategories()!=null)
                             dataFetch.onDataSuccessResponse(categoriesFeed.getCategories());
                         else
                             dataFetch.onDataSuccessResponse(new ArrayList<>());
                     }

                     @Override
                     public void onError(@NonNull Throwable e) {
                            dataFetch.onDataFailedResponse(e.getMessage());
                     }
                 })
         ;
    };


    /**
     * Link <a href="https://www.themealdb.com/api/json/v1/1/search.php?s='Q'">Search By Name</a>
     * @return List<MealsItem>
     */
    public void searchMealsByName(String search,DataFetch<List<MealsItem>> dataFetch){
        apiCalls
                .searchMealsByName(search)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<MealsList>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dataFetch.onDataLoading();
                    }

                    @Override
                    public void onSuccess(@NonNull MealsList mealsList) {
                        if (mealsList!=null && mealsList.getMeals()!=null)
                            dataFetch.onDataSuccessResponse(mealsList.getMeals());
                        else
                            dataFetch.onDataSuccessResponse(new ArrayList<>());
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dataFetch.onDataFailedResponse(e.getMessage());
                    }
                });;
    };

    /**
     * Link <a href="https://www.themealdb.com/api/json/v1/1/lookup.php?i='525'">Retrieve Meals By ID</a>
     * @return List<MealsItem>
     */

    public void retrieveMealByID(String id,DataFetch<List<MealsItem>> dataFetch ){

         apiCalls.retrieveMealByID(id)
                 .subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread())
                 .subscribe(new SingleObserver<MealsList>() {
                     @Override
                     public void onSubscribe(@NonNull Disposable d) {
                         dataFetch.onDataLoading();
                     }

                     @Override
                     public void onSuccess(@NonNull MealsList mealsList) {
                         if (mealsList!=null && mealsList.getMeals()!=null)
                            dataFetch.onDataSuccessResponse(mealsList.getMeals());
                         else
                             dataFetch.onDataSuccessResponse(new ArrayList<>());
                     }

                     @Override
                     public void onError(@NonNull Throwable e) {
                         dataFetch.onDataFailedResponse(e.getMessage());
                     }
                 });
    };



    // endregion APIs
}

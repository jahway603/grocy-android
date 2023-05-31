/*
 * This file is part of Grocy Android.
 *
 * Grocy Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Grocy Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Grocy Android. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) 2020-2023 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;
import java.util.List;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.helper.DownloadHelper;
import xyz.zedler.patrick.grocy.model.Location;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.ProductGroup;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.Store;
import xyz.zedler.patrick.grocy.model.TaskCategory;
import xyz.zedler.patrick.grocy.repository.MasterDataOverviewRepository;
import xyz.zedler.patrick.grocy.web.NetworkQueue;

public class MasterDataOverviewViewModel extends BaseViewModel {

  private static final String TAG = MasterDataOverviewViewModel.class.getSimpleName();

  private final SharedPreferences sharedPrefs;
  private final DownloadHelper dlHelper;
  private final MasterDataOverviewRepository repository;

  private final MutableLiveData<Boolean> isLoadingLive;
  private final MutableLiveData<Boolean> offlineLive;

  private final MutableLiveData<List<Store>> storesLive;
  private final MutableLiveData<List<Location>> locationsLive;
  private final MutableLiveData<List<ProductGroup>> productGroupsLive;
  private final MutableLiveData<List<QuantityUnit>> quantityUnitsLive;
  private final MutableLiveData<List<Product>> productsLive;
  private final MutableLiveData<List<TaskCategory>> taskCategoriesLive;

  private NetworkQueue currentQueueLoading;

  public MasterDataOverviewViewModel(@NonNull Application application) {
    super(application);

    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

    isLoadingLive = new MutableLiveData<>(false);
    dlHelper = new DownloadHelper(getApplication(), TAG, isLoadingLive::setValue);
    repository = new MasterDataOverviewRepository(application);

    offlineLive = new MutableLiveData<>(false);
    storesLive = new MutableLiveData<>();
    locationsLive = new MutableLiveData<>();
    productGroupsLive = new MutableLiveData<>();
    quantityUnitsLive = new MutableLiveData<>();
    productsLive = new MutableLiveData<>();
    taskCategoriesLive = new MutableLiveData<>();
  }

  public void loadFromDatabase(boolean downloadAfterLoading) {
    repository.loadFromDatabase(data -> {
      this.storesLive.setValue(data.getStores());
      this.locationsLive.setValue(data.getLocations());
      this.productGroupsLive.setValue(data.getProductGroups());
      this.quantityUnitsLive.setValue(data.getQuantityUnits());
      this.productsLive.setValue(data.getProducts());
      this.taskCategoriesLive.setValue(data.getTaskCategories());
      if (downloadAfterLoading) {
        downloadData();
      }
    }, error -> onError(error, TAG));
  }

  public void downloadData(@Nullable String dbChangedTime, boolean skipOfflineCheck) {
    if (currentQueueLoading != null) {
      currentQueueLoading.reset(true);
      currentQueueLoading = null;
    }
    if (!skipOfflineCheck && isOffline()) { // skip downloading
      isLoadingLive.setValue(false);
      return;
    }
    if (dbChangedTime == null) {
      dlHelper.getTimeDbChanged(
          time -> downloadData(time, skipOfflineCheck),
          error -> onError(error, TAG)
      );
      return;
    }

    NetworkQueue queue = dlHelper.newQueue(() -> {
      if (isOffline()) {
        setOfflineLive(false);
      }
    }, error -> onError(error, TAG));
    queue.append(
        Store.updateStores(dlHelper, dbChangedTime, this.storesLive::setValue),
        Location.updateLocations(dlHelper, dbChangedTime, this.locationsLive::setValue),
        ProductGroup.updateProductGroups(dlHelper, dbChangedTime, this.productGroupsLive::setValue),
        QuantityUnit.updateQuantityUnits(dlHelper, dbChangedTime, this.quantityUnitsLive::setValue),
        Product.updateProducts(dlHelper, dbChangedTime, this.productsLive::setValue),
        TaskCategory.updateTaskCategories(dlHelper, dbChangedTime, this.taskCategoriesLive::setValue)
    );

    if (queue.isEmpty()) {
      return;
    }

    currentQueueLoading = queue;
    queue.start();
  }

  public void downloadData() {
    downloadData(null, false);
  }

  public void downloadDataForceUpdate() {
    SharedPreferences.Editor editPrefs = sharedPrefs.edit();
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_STORES, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_LOCATIONS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCT_GROUPS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_QUANTITY_UNITS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_PRODUCTS, null);
    editPrefs.putString(Constants.PREF.DB_LAST_TIME_TASK_CATEGORIES, null);
    editPrefs.apply();
    downloadData(null, true);
  }

  @NonNull
  public MutableLiveData<Boolean> getOfflineLive() {
    return offlineLive;
  }

  public Boolean isOffline() {
    return offlineLive.getValue();
  }

  public void setOfflineLive(boolean isOffline) {
    offlineLive.setValue(isOffline);
  }

  @NonNull
  public MutableLiveData<Boolean> getIsLoadingLive() {
    return isLoadingLive;
  }

  @NonNull
  public MutableLiveData<List<Store>> getStoresLive() {
    return storesLive;
  }

  public MutableLiveData<List<Location>> getLocationsLive() {
    return locationsLive;
  }

  public MutableLiveData<List<ProductGroup>> getProductGroupsLive() {
    return productGroupsLive;
  }

  public MutableLiveData<List<QuantityUnit>> getQuantityUnitsLive() {
    return quantityUnitsLive;
  }

  public MutableLiveData<List<Product>> getProductsLive() {
    return productsLive;
  }

  public MutableLiveData<List<TaskCategory>> getTaskCategoriesLive() {
    return taskCategoriesLive;
  }

  public void setCurrentQueueLoading(NetworkQueue queueLoading) {
    currentQueueLoading = queueLoading;
  }

  @Override
  protected void onCleared() {
    dlHelper.destroy();
    super.onCleared();
  }
}

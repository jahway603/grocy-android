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

package xyz.zedler.patrick.grocy.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import xyz.zedler.patrick.grocy.Constants;
import xyz.zedler.patrick.grocy.Constants.ACTION;
import xyz.zedler.patrick.grocy.Constants.ARGUMENT;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.activity.MainActivity;
import xyz.zedler.patrick.grocy.behavior.SystemBarBehavior;
import xyz.zedler.patrick.grocy.databinding.FragmentMasterProductBinding;
import xyz.zedler.patrick.grocy.helper.InfoFullscreenHelper;
import xyz.zedler.patrick.grocy.model.BottomSheetEvent;
import xyz.zedler.patrick.grocy.model.Event;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.SnackbarMessage;
import xyz.zedler.patrick.grocy.util.NumUtil;
import xyz.zedler.patrick.grocy.util.ResUtil;
import xyz.zedler.patrick.grocy.viewmodel.MasterProductViewModel;

public class MasterProductFragment extends BaseFragment {

  private final static String TAG = MasterProductFragment.class.getSimpleName();

  private MainActivity activity;
  private FragmentMasterProductBinding binding;
  private MasterProductViewModel viewModel;
  private InfoFullscreenHelper infoFullscreenHelper;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState
  ) {
    binding = FragmentMasterProductBinding.inflate(
        inflater, container, false
    );
    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
    activity = (MainActivity) requireActivity();
    MasterProductFragmentArgs args = MasterProductFragmentArgs
        .fromBundle(requireArguments());
    viewModel = new ViewModelProvider(this, new MasterProductViewModel
        .MasterProductViewModelFactory(activity.getApplication(), args)
    ).get(MasterProductViewModel.class);
    if (!viewModel.isActionEdit() && args.getProductName() != null) {
      // remove product name from arguments because it was filled
      // in the form during ViewModel creation
      setArguments(new MasterProductFragmentArgs.Builder(args).setProductName(null)
          .setProductId(null).build().toBundle());
    }
    binding.setActivity(activity);
    binding.setFormData(viewModel.getFormData());
    binding.setViewModel(viewModel);
    binding.setFragment(this);
    binding.setLifecycleOwner(getViewLifecycleOwner());

    SystemBarBehavior systemBarBehavior = new SystemBarBehavior(activity);
    systemBarBehavior.setAppBar(binding.appBar);
    systemBarBehavior.setContainer(binding.swipeMasterProductSimple);
    systemBarBehavior.setScroll(binding.scroll, binding.constraint);
    systemBarBehavior.setUp();
    activity.setSystemBarBehavior(systemBarBehavior);

    binding.toolbar.setNavigationOnClickListener(v -> activity.navUtil.navigateUp());

    binding.categoryOptional.setOnClickListener(
        v -> activity.navUtil.navigateFragment(MasterProductFragmentDirections
            .actionMasterProductFragmentToMasterProductCatOptionalFragment(viewModel.getAction())
            .setProduct(viewModel.getFilledProduct())
            .setForceSaveWithClose(viewModel.isForceSaveWithClose())));
    binding.categoryLocation.setOnClickListener(
        v -> activity.navUtil.navigateFragment(MasterProductFragmentDirections
            .actionMasterProductFragmentToMasterProductCatLocationFragment(viewModel.getAction())
            .setProduct(viewModel.getFilledProduct())
            .setForceSaveWithClose(viewModel.isForceSaveWithClose())));
    binding.categoryDueDate.setOnClickListener(
        v -> activity.navUtil.navigateFragment(MasterProductFragmentDirections
            .actionMasterProductFragmentToMasterProductCatDueDateFragment(viewModel.getAction())
            .setProduct(viewModel.getFilledProduct())
            .setForceSaveWithClose(viewModel.isForceSaveWithClose())));
    binding.categoryAmount.setOnClickListener(
        v -> activity.navUtil.navigateFragment(MasterProductFragmentDirections
            .actionMasterProductFragmentToMasterProductCatAmountFragment(viewModel.getAction())
            .setProduct(viewModel.getFilledProduct())
            .setForceSaveWithClose(viewModel.isForceSaveWithClose())));
    binding.categoryQuantityUnit.setOnClickListener(
        v -> activity.navUtil.navigateFragment(MasterProductFragmentDirections
            .actionMasterProductFragmentToMasterProductCatQuantityUnitFragment(viewModel.getAction())
            .setProduct(viewModel.getFilledProduct())
            .setForceSaveWithClose(viewModel.isForceSaveWithClose())));
    binding.categoryBarcodes.setOnClickListener(v -> {
      if (!viewModel.isActionEdit()) {
        activity.showSnackbar(R.string.msg_save_product_first, true);
        return;
      }
      activity.navUtil.navigateFragment(MasterProductFragmentDirections
          .actionMasterProductFragmentToMasterProductCatBarcodesFragment(viewModel.getAction())
          .setProduct(viewModel.getFilledProduct()));
    });
    binding.categoryQuConversions.setOnClickListener(v -> {
      if (!viewModel.isActionEdit()) {
        activity.showSnackbar(R.string.msg_save_product_first, true);
        return;
      }
      activity.navUtil.navigateFragment(MasterProductFragmentDirections
          .actionMasterProductFragmentToMasterProductCatConversionsFragment(viewModel.getAction())
          .setProduct(viewModel.getFilledProduct()));
    });

    Product product = (Product) getFromThisDestinationNow(Constants.ARGUMENT.PRODUCT);
    if (product != null) {
      viewModel.setCurrentProduct(product);
      removeForThisDestination(Constants.ARGUMENT.PRODUCT);
    }

    viewModel.getEventHandler().observeEvent(getViewLifecycleOwner(), event -> {
      if (event.getType() == Event.SNACKBAR_MESSAGE) {
        activity.showSnackbar(
            ((SnackbarMessage) event).getSnackbar(activity.binding.coordinatorMain)
        );
      } else if (event.getType() == Event.NAVIGATE_UP) {
        activity.navUtil.navigateUp();
      } else if (event.getType() == Event.SET_PRODUCT_ID) {
        int id = event.getBundle().getInt(Constants.ARGUMENT.PRODUCT_ID);
        setForPreviousDestination(Constants.ARGUMENT.PRODUCT_ID, id);
        if (NumUtil.isStringInt(args.getPendingProductId())) {
          setForPreviousDestination(
              ARGUMENT.PENDING_PRODUCT_ID,
              Integer.parseInt(args.getPendingProductId())
          );
        }
      } else if (event.getType() == Event.BOTTOM_SHEET) {
        BottomSheetEvent bottomSheetEvent = (BottomSheetEvent) event;
        activity.showBottomSheet(bottomSheetEvent.getBottomSheet(), event.getBundle());
      } else if (event.getType() == Event.FOCUS_INVALID_VIEWS) {
        if (binding.editTextName.getText() == null
            || binding.editTextName.getText().length() == 0) {
          activity.showKeyboard(binding.editTextName);
        }
      } else if (event.getType() == Event.TRANSACTION_SUCCESS) {
        if (args.getAction().equals(ACTION.CREATE) && viewModel.isActionEdit()) {
          activity.updateFab(
              R.drawable.ic_round_save,
              R.string.action_save_close,
              Constants.FAB.TAG.SAVE,
              savedInstanceState == null,
              () -> {
                if (!viewModel.getFormData().isNameValid()) {
                  clearInputFocus();
                  activity.showKeyboard(binding.editTextName);
                } else {
                  clearInputFocus();
                  viewModel.saveProduct(true);
                }
              }
          );
        }
      }
    });

    infoFullscreenHelper = new InfoFullscreenHelper(binding.container);
    viewModel.getInfoFullscreenLive().observe(
        getViewLifecycleOwner(),
        infoFullscreen -> infoFullscreenHelper.setInfo(infoFullscreen)
    );

    viewModel.getIsLoadingLive().observe(getViewLifecycleOwner(), isLoading -> {
      if (!isLoading) {
        viewModel.setCurrentQueueLoading(null);
      }
    });

    viewModel.getActionEditLive().observe(getViewLifecycleOwner(), isEdit -> activity.updateBottomAppBar(
        true,
        isEdit
            ? R.menu.menu_master_product_edit
            : R.menu.menu_master_product_create,
        menuItem -> {
          if (menuItem.getItemId() == R.id.action_delete) {
            viewModel.deleteProductSafely();
            return true;
          }
          if (menuItem.getItemId() == R.id.action_save) {
            viewModel.saveProduct(true);
            return true;
          }
          return false;
        }
    ));

    String action = (String) getFromThisDestinationNow(Constants.ARGUMENT.ACTION);
    if (action != null) {
      removeForThisDestination(Constants.ARGUMENT.ACTION);
      switch (action) {
        case ACTION.SAVE_CLOSE:
          new Handler().postDelayed(() -> viewModel.saveProduct(true), 500);
          break;
        case ACTION.SAVE_NOT_CLOSE:
          new Handler().postDelayed(() -> viewModel.saveProduct(false), 500);
          break;
        case ACTION.DELETE:
          new Handler().postDelayed(() -> viewModel.deleteProductSafely(), 500);
          break;
      }
    }

    viewModel.getFormData().getCatOptionalErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textCatOptional.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnBackground)
        )
    );
    viewModel.getFormData().getCatLocationErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textCatLocation.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnBackground)
        )
    );
    viewModel.getFormData().getCatDueDateErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textCatDueDate.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnBackground)
        )
    );
    viewModel.getFormData().getCatQuErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textCatQu.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnBackground)
        )
    );
    viewModel.getFormData().getCatAmountErrorLive().observe(
        getViewLifecycleOwner(), value -> binding.textCatAmount.setTextColor(
            ResUtil.getColorAttr(activity, value ? R.attr.colorError : R.attr.colorOnBackground)
        )
    );

    if (savedInstanceState == null) {
      viewModel.loadFromDatabase(true);
    }

    // UPDATE UI

    activity.getScrollBehavior().setNestedOverScrollFixEnabled(true);
    activity.getScrollBehavior().setUpScroll(
        binding.appBar, false, binding.scroll, false
    );
    activity.getScrollBehavior().setBottomBarVisibility(true);
    boolean showSaveWithCloseButton = viewModel.isActionEdit() || viewModel.isForceSaveWithClose();
    activity.updateFab(
        showSaveWithCloseButton ? R.drawable.ic_round_save : R.drawable.ic_round_save_as,
        showSaveWithCloseButton ? R.string.action_save : R.string.action_save_not_close,
        showSaveWithCloseButton ? Constants.FAB.TAG.SAVE : Constants.FAB.TAG.SAVE_NOT_CLOSE,
        savedInstanceState == null,
        () -> viewModel.saveProduct(showSaveWithCloseButton)
    );
  }

  public void clearInputFocus() {
    activity.hideKeyboard();
    binding.textInputName.clearFocus();
  }

  @Override
  public void deleteObject(int objectId) {
    viewModel.deleteProduct(objectId);
  }

  @Override
  public void updateConnectivity(boolean online) {
    if (!online == viewModel.isOffline()) {
      return;
    }
    viewModel.setOfflineLive(!online);
    if (online) {
      viewModel.downloadData();
    }
  }

  @NonNull
  @Override
  public String toString() {
    return TAG;
  }
}

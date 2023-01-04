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
 * Copyright (c) 2020-2022 by Patrick Zedler and Dominic Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;
import androidx.lifecycle.LiveData;
import java.util.HashMap;
import java.util.List;
import xyz.zedler.patrick.grocy.R;
import xyz.zedler.patrick.grocy.model.Product;
import xyz.zedler.patrick.grocy.model.QuantityUnit;
import xyz.zedler.patrick.grocy.model.QuantityUnitConversion;

public class QuantityUnitConversionUtil {

  public static HashMap<QuantityUnit, Double> getUnitFactors(
      Context context,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversion> unitConversions,
      Product product,
      boolean relativeToStockUnit
  ) {
    QuantityUnit relativeToUnit = relativeToStockUnit
        ? quantityUnitHashMap.get(product.getQuIdStockInt())
        : quantityUnitHashMap.get(product.getQuIdPurchaseInt());
    QuantityUnit stockUnit = quantityUnitHashMap.get(product.getQuIdStockInt());
    QuantityUnit purchaseUnit = quantityUnitHashMap.get(product.getQuIdPurchaseInt());

    if (relativeToUnit == null || stockUnit == null || purchaseUnit == null) {
      throw new IllegalArgumentException(context.getString(R.string.error_loading_qus));
    }

    HashMap<QuantityUnit, Double> unitFactors = new HashMap<>();
    unitFactors.put(relativeToUnit, (double) -1);
    if (relativeToStockUnit && !unitFactors.containsKey(purchaseUnit)) {
      unitFactors.put(purchaseUnit, product.getQuFactorPurchaseToStockDouble());
    } else if (!relativeToStockUnit && !unitFactors.containsKey(stockUnit)) {
      unitFactors.put(stockUnit, product.getQuFactorPurchaseToStockDouble());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      if (!NumUtil.isStringInt(conversion.getProductId())
          || product.getId() != conversion.getProductIdInt()) {
        continue;
      }
      // Only add product specific conversions
      // ("overriding" standard conversions which are added in the next step)
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || unitFactors.containsKey(unit)) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
    }
    for (QuantityUnitConversion conversion : unitConversions) {
      if (NumUtil.isStringInt(conversion.getProductId())
          || relativeToUnit.getId() != conversion.getFromQuId()) {
        continue;
      }
      // Only add standard unit conversions
      QuantityUnit unit = quantityUnitHashMap.get(conversion.getToQuId());
      if (unit == null || unitFactors.containsKey(unit)) {
        continue;
      }
      unitFactors.put(unit, conversion.getFactor());
    }
    return unitFactors;
  }

  public static HashMap<QuantityUnit, Double> getUnitFactors(
      Context context,
      HashMap<Integer, QuantityUnit> quantityUnitHashMap,
      List<QuantityUnitConversion> unitConversions,
      Product product
  ) {
    return getUnitFactors(context, quantityUnitHashMap, unitConversions, product, true);
  }

  public static String getAmountStock(
      Product product,
      QuantityUnit stock,
      QuantityUnit current,
      LiveData<String> amountLive,
      LiveData<HashMap<QuantityUnit, Double>> quantityUnitsFactorsLive,
      int maxDecimalPlacesAmount
  ) {
    if (!NumUtil.isStringDouble(amountLive.getValue())
        || quantityUnitsFactorsLive.getValue() == null
    ) {
      return null;
    }
    assert amountLive.getValue() != null;

    if (stock != null && current != null && stock.getId() != current.getId()) {
      HashMap<QuantityUnit, Double> hashMap = quantityUnitsFactorsLive.getValue();
      double amount = Double.parseDouble(amountLive.getValue());
      Object currentFactor = hashMap.get(current);
      if (currentFactor == null) {
        //amountHelperLive.setValue(null);
        return null;
      }
      double amountMultiplied;
      if (product != null && current.getId() == product.getQuIdPurchaseInt()) {
        amountMultiplied = amount * (double) currentFactor;
      } else {
        amountMultiplied = amount / (double) currentFactor;
      }
      return NumUtil.trimAmount(amountMultiplied, maxDecimalPlacesAmount);
    } else {
      return null;
    }
  }

  public static double getAmountRelativeToUnit(
      HashMap<QuantityUnit, Double> unitFactors,
      Product product,
      QuantityUnit quantityUnit,
      double inputAmount
  ) {
    if (quantityUnit == null || !unitFactors.containsKey(quantityUnit)) {
      return inputAmount;
    }
    Double factor = unitFactors.get(quantityUnit);
    assert factor != null;
    if (factor != -1 && quantityUnit.getId() == product.getQuIdPurchaseInt()) {
      return inputAmount / factor;
    } else if (factor != -1) {
      return inputAmount * factor;
    }
    return inputAmount;
  }
}

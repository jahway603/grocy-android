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

package xyz.zedler.patrick.grocy.model;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import xyz.zedler.patrick.grocy.R;

public class FilterChipLiveDataFields extends FilterChipLiveData {

  public final static String MULTI_SEPARATOR = "%0";
  public final static String VALUE_SEPARATOR = "%=";

  private final Application application;
  private final String prefKey;
  private final List<Field> fields;
  private final SharedPreferences sharedPrefs;

  public FilterChipLiveDataFields(
      Application application,
      String prefKey,
      Runnable clickListener,
      Field... fields
  ) {
    this.application = application;
    this.prefKey = prefKey;
    sharedPrefs = PreferenceManager.getDefaultSharedPreferences(application);
    this.fields = getFieldsFromFieldsWithDefault(
        sharedPrefs.getString(prefKey, null),
        fields
    );

    setText(application.getString(R.string.property_fields));
    setItems();
    if (clickListener != null) {
      setMenuItemClickListener(item -> {
        setValues(item.getItemId());
        setItems();
        emitValue();
        clickListener.run();
        return true;
      });
    }
  }

  public void setValues(int id) {
    if (id > fields.size()-1) return;
    Field field = fields.get(id);
    field.currentValue = !field.currentValue;
    sharedPrefs.edit().putString(prefKey, createMultiFieldStates()).apply();
  }

  private void setItems() {
    ArrayList<MenuItemData> menuItemDataList = new ArrayList<>();
    for (int id=0; id < fields.size(); id++) {
      Field field = fields.get(id);
      menuItemDataList.add(new MenuItemData(
          id,
          0,
          application.getString(field.textResId),
          field.currentValue
      ));
    }
    setMenuItemDataList(menuItemDataList);
    setMenuItemGroups(new MenuItemGroup(0, true, false));
    emitValue();
  }

  public List<String> getActiveFields() {
    return fields.stream()
        .filter(field -> field.currentValue)
        .map(field -> field.name)
        .collect(Collectors.toList());
  }

  public List<Field> getFieldsFromFieldsWithDefault(
      @Nullable String multiFieldStates,
      Field[] fields
  ) {
    HashMap<String, Boolean> savedStates = new HashMap<>();
    if (multiFieldStates != null && !multiFieldStates.isBlank()) {
      for (String fieldStatePair : multiFieldStates.split(MULTI_SEPARATOR)) {
        String[] pairArray = fieldStatePair.split(VALUE_SEPARATOR);
        if (pairArray.length != 2) continue;
        savedStates.put(pairArray[0], Boolean.parseBoolean(pairArray[1]));
      }
    }

    List<Field> fieldList = new ArrayList<>();
    for (Field field : fields) {
      if (savedStates.containsKey(field.name)) {
        field.currentValue = Boolean.TRUE.equals(savedStates.get(field.name));
      } else {
        field.currentValue = field.defaultValue;
      }
      fieldList.add(field);
    }
    return fieldList;
  }

  public String createMultiFieldStates() {
    if (fields.isEmpty()) return "";
    StringBuilder stringBuilder = new StringBuilder();
    for (Field field : fields) {
      stringBuilder.append(field.name);
      stringBuilder.append(VALUE_SEPARATOR);
      stringBuilder.append(field.currentValue ? "true" : "false");
      if (!fields.get(fields.size()-1).equals(field)) {
        stringBuilder.append(MULTI_SEPARATOR);
      }
    }
    return stringBuilder.toString();
  }

  public static class Field {
    public final String name;
    @StringRes public final int textResId;
    public final boolean defaultValue;
    public boolean currentValue;
    public Field(String name, @StringRes int textResId, boolean defaultValue) {
      this.name = name;
      this.textResId = textResId;
      this.defaultValue = defaultValue;
    }
  }
}
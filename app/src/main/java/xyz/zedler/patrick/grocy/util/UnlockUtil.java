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
 * Copyright (c) 2020-2024 by Patrick Zedler and Dominic Zedler
 * Copyright (c) 2024 by Patrick Zedler
 */

package xyz.zedler.patrick.grocy.util;

import android.content.Context;

public class UnlockUtil {

  public final static String PACKAGE = "xyz.zedler.patrick.grocy.unlock";

  public static boolean isKeyInstalled(Context context) {
    try {
      context.getPackageManager().getPackageInfo(PACKAGE, 0);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isPlayStoreInstalled(Context context){
    try {
      context.getPackageManager().getPackageInfo("com.android.vending", 0);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}

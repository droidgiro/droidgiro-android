/*
 * Copyright (C) 2011 aGiro authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.agiro.scanner.android;

import java.lang.StringBuffer;
import java.lang.String;
import java.lang.Character;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Iterator;
import java.util.ListIterator;
import android.util.Log;

/**
* Supposed to parse and map the string from the scanner.
*/
public final class StringDecoder {

  private static final String TAG = "StringDecoder:";

  public static String reverseString(String source) {
    int i, len = source.length();
    StringBuffer dest = new StringBuffer(len);

    for (i = (len - 1); i >= 0; i--)
    dest.append(source.charAt(i));
    return dest.toString();
  }

  /**
  * Parses the string and maps its digits
  */
  public static HashMap parseString(String str) {
    HashMap dataMap = new HashMap();
    StringBuffer strBuff = new StringBuffer();
    char c;
    int last = (str.length()) - 1;
    // if 4 last are #,digit,digit,# it's account num.
    // no idea what those digits mean though
    if (str.charAt(last) == (char)35 &&
      Character.isDigit(str.charAt(last-1)) &&
      Character.isDigit(str.charAt(last-2)) &&
      str.charAt(last-3) == (char)35) {
      // last digit series is account num
      for (int i = last-4; i >= 0; i--){
        c = str.charAt(i);
        if (Character.isDigit(c) && i != 0) {
          strBuff.append(c);
        } else {
          if (Character.isDigit(c) && i == 0) {
            strBuff.append(c);
          }
          //account num end
          String entry = strBuff.toString();
          entry = reverseString(entry);
          dataMap.put("account", new String(entry));
          strBuff.setLength(0);
          break;
        }
      }
    } else if (str.charAt(last) == (char)62) {
      //indicates amount
      for (int i = last-1; i >= 0; i--){
        c = str.charAt(i);
        if (Character.isDigit(c) && i != 0) {
          strBuff.append(c);
        } else {
          //account num end
          String entry = strBuff.toString();
          entry = reverseString(entry);
          dataMap.put("amount", new String(entry));
          strBuff.setLength(0);
          break;
        }
      }
    } else {
      //else, presumably the first digits are the ref number
        for (int i = 0; i <= last ; i++) {
          c = str.charAt(i);
          if (Character.isDigit(c)) {
            strBuff.append(c);
          } else {
            if (strBuff.length() == 0) {
              //first is (char)62 or 35
              continue;
            } else {
              String entry = strBuff.toString();
              dataMap.put("reference", new String(entry));
              strBuff.setLength(0);
              break;
            }
          }
        }
    }
    dataMap.put("debug", str);
    Set set = dataMap.entrySet();
    Iterator it = set.iterator();
    while(it.hasNext()){
       Map.Entry me = (Map.Entry)it.next();
       Log.d(TAG, me.getKey() + " : " + me.getValue() );
    }
    return dataMap;
  }

  /**
  * Check with the mod 10 checksum algorithm.
  */
  public static boolean isValidCC(String num) {
    final int[][] sumTable = {{0,1,2,3,4,5,6,7,8,9},{0,2,4,6,8,1,3,5,7,9}};
    int sum = 0, flip = 0;

    for (int i = num.length() - 1; i >= 0; i--)
      sum += sumTable[flip++ & 0x1][Character.digit(num.charAt(i), 10)];
    return sum % 10 == 0;
  }

}


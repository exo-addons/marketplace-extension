package org.exoplatform.addon.utils;

import com.ibm.icu.text.Transliterator;

public class AddonUtils {
  
  public static String cleanString(String str) {
    Transliterator accentsconverter = Transliterator.getInstance("Latin; NFD; [:Nonspacing Mark:] Remove; NFC;");
    str = accentsconverter.transliterate(str);
    // the character ? seems to not be changed to d by the transliterate
    // function
    StringBuffer cleanedStr = new StringBuffer(str.trim());
    // delete special character
    for (int i = 0; i < cleanedStr.length(); i++) {
      char c = cleanedStr.charAt(i);
      if (c == ' ') {
        if (i > 0 && cleanedStr.charAt(i - 1) == '-') {
          cleanedStr.deleteCharAt(i--);
        } else {
          c = '-';
          cleanedStr.setCharAt(i, c);
        }
        continue;
      }
      if (i >= 0 && !(Character.isLetterOrDigit(c) || c == '-')) {
        cleanedStr.deleteCharAt(i--);
        continue;
      }
      if (i >= 0 && c == '-' && cleanedStr.charAt(i - 1) == '-')
        cleanedStr.deleteCharAt(i--);
    }
    return cleanedStr.toString().toLowerCase();
  }
}

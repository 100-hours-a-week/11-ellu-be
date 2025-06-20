package com.ellu.looper.commons.util;

public class HangulUtil {

  private static final char[] CHOSEONG = {
      'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ'
  };

  public static String extractChoseong(String text) {
    StringBuilder result = new StringBuilder();

    for (char c : text.toCharArray()) {
      if (c >= 0xAC00 && c <= 0xD7A3) { // 한글 유니코드 범위
        int choseongIndex = (c - 0xAC00) / (21 * 28);
        result.append(CHOSEONG[choseongIndex]);
      } else {
        result.append(c);
      }
    }

    return result.toString();
  }

  public static boolean isChoseong(char c) {
    return c >= 'ㄱ' && c <= 'ㅎ';
  }

  public static boolean containsOnlyChoseong(String text) {
    return !text.isEmpty() && text.chars().allMatch(c -> isChoseong((char) c));
  }
}
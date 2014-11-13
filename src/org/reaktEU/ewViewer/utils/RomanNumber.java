/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.reaktEU.ewViewer.utils;

/**
 *
 * @author Stephan Herrnkind <herrnkind@gempa.de>
 */
public class RomanNumber {

    private static final int[] numbers = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] letters = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    public static String toString(int n) {
        if (n < 1 || n > 3999) {
            return Integer.toString(n);
        }
        String roman = "";
        for (int i = 0; i < numbers.length; i++) {
            while (n >= numbers[i]) {
                roman += letters[i];
                n -= numbers[i];
            }
        }
        return roman;
    }
}

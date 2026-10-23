package com.fulllock.app;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.*;

/** Pure Java parser; unknown layouts fail closed. Amounts are never concatenated. */
public final class JobParser {
    public record Job(String category, String origin, String destination, String type,
                      BigDecimal fare, BigDecimal tip, String title, String text) {
        // Overlay display uses commission on the total (fare + tip), independently of the selected filter formula.
        public BigDecimal overlayNet(int fee) {
            return fare.add(tip).multiply(BigDecimal.valueOf(100 - Math.max(0, Math.min(100, fee))))
                    .movePointLeft(2).setScale(2, java.math.RoundingMode.HALF_UP);
        }
        public BigDecimal price(int formula, int fee) {
            BigDecimal base = fare;
            if (formula >= 2) base = base.multiply(BigDecimal.valueOf(100 - fee)).movePointLeft(2);
            return formula % 2 == 1 ? base.add(tip) : base;
        }
    }
    private static final Pattern ROUTE = Pattern.compile("^\\s*\\[([^]]+)]\\s*(.+?)\\s*>\\s*(.+?)\\s*$");
    private static final Pattern MONEY = Pattern.compile("฿\\s*([0-9]+(?:,[0-9]{3})*(?:\\.[0-9]{1,2})?)(?![0-9.,])");
    private static final Pattern JOB_START = Pattern.compile("^\\s*(?:ขนส่งทันที|งานล่วงหน้า)\\s+฿");
    public static Job notificationJob(String title,String text,boolean ongoing,boolean summary) {
        if(ongoing || summary || text==null || !JOB_START.matcher(text).find())return null;
        return parse(title,text);
    }
    public static Job parse(String title, String text) {
        if (title == null || text == null) return null;
        Matcher route = ROUTE.matcher(title);
        if (!route.matches()) return null;
        boolean now = text.contains("ขนส่งทันที"), later = text.contains("งานล่วงหน้า");
        if (now == later) return null;
        List<BigDecimal> amounts = new ArrayList<>();
        Matcher money = MONEY.matcher(text);
        int firstEnd = -1, secondStart = -1;
        while (money.find()) {
            if (amounts.isEmpty()) firstEnd = money.end();
            else if (amounts.size() == 1) secondStart = money.start();
            amounts.add(new BigDecimal(money.group(1).replace(",", "")));
        }
        if (amounts.isEmpty() || amounts.size() > 2) return null;
        // User-confirmed format: base fare + tip. Fees apply to the base only.
        if (amounts.size() == 2 && !text.substring(firstEnd, secondStart).trim().equals("+")) return null;
        if (amounts.size() == 1 && text.contains("ทิป")) return null;
        return new Job(route.group(1).trim(), route.group(2).trim(), route.group(3).trim(),
                now ? "ขนส่งทันที" : "งานล่วงหน้า", amounts.get(0), amounts.size()==2 ? amounts.get(1) : BigDecimal.ZERO, title, text);
    }
    public static boolean areaMatches(String area, String list) {
        return Arrays.stream(list.split(",")).map(String::trim).filter(s -> !s.isEmpty()).anyMatch(area::contains);
    }
}

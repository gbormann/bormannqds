package com.bormannqds.mds.lib.referencedata;

import com.bormannqds.lib.dataaccess.referencedata.RollScheduleInterface;

import java.time.LocalDate;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created by bormanng on 23/03/2015.
 */
public class RollSchedule implements RollScheduleInterface {
    public RollSchedule(final String baseSymbol) {
        this.baseSymbol = baseSymbol;
    }

    public void add(LocalDate rolldate, String expiryCode) {
        rollMap.put(rolldate, expiryCode);
    }

    @Override
    public String getFrontMonthSymbolOn(final LocalDate date) {
        return baseSymbol + rollMap.floorEntry(date).getValue();
    }

    @Override
    public String getFrontMonthSymbolOn(final String dateStr) {
        return getFrontMonthSymbolOn(LocalDate.parse(dateStr));
    }

    @Override
    public String getNthFrontMonthSymbolOn(final LocalDate date, int rank) {
        if (rank < 2)
            return getFrontMonthSymbolOn(date);
        int ngen = rank - 1; // ngen > 0 per definition

        NavigableMap<LocalDate, String> relevantRolls = rollMap.tailMap(date, false);
        // first entry of relevantRolls is now successor of front month
        for (Map.Entry<LocalDate, String> entry : relevantRolls.entrySet()) { // ascending key iteration
            if (--ngen == 0)
                return baseSymbol + entry.getValue();
        }

        // not enough entries
        return null;
    }

    @Override
    public String getNthFrontMonthSymbolOn(final String dateStr, int rank) {
        return getNthFrontMonthSymbolOn(LocalDate.parse(dateStr), rank);
    }

//private:
    private final String baseSymbol;
    private final NavigableMap<LocalDate, String> rollMap = new TreeMap<>();
}

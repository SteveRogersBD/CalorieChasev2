package com.example.caloriechase.data;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * Type converters for Room database to handle complex data types
 */
public class Converters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static String fromActivityType(ActivityType activityType) {
        return activityType == null ? null : activityType.name();
    }

    @TypeConverter
    public static ActivityType toActivityType(String activityType) {
        return activityType == null ? null : ActivityType.valueOf(activityType);
    }

    @TypeConverter
    public static String fromTreasureType(TreasureType treasureType) {
        return treasureType == null ? null : treasureType.name();
    }

    @TypeConverter
    public static TreasureType toTreasureType(String treasureType) {
        return treasureType == null ? null : TreasureType.valueOf(treasureType);
    }

    @TypeConverter
    public static String fromStringSet(Set<String> stringSet) {
        return stringSet == null ? null : gson.toJson(stringSet);
    }

    @TypeConverter
    public static Set<String> toStringSet(String stringSetString) {
        if (stringSetString == null) return null;
        Type setType = new TypeToken<Set<String>>(){}.getType();
        return gson.fromJson(stringSetString, setType);
    }

    @TypeConverter
    public static String fromLocationUpdateList(List<LocationUpdate> locationUpdates) {
        return locationUpdates == null ? null : gson.toJson(locationUpdates);
    }

    @TypeConverter
    public static List<LocationUpdate> toLocationUpdateList(String locationUpdatesString) {
        if (locationUpdatesString == null) return null;
        Type listType = new TypeToken<List<LocationUpdate>>(){}.getType();
        return gson.fromJson(locationUpdatesString, listType);
    }

    @TypeConverter
    public static String fromDoubleArray(double[] doubleArray) {
        return doubleArray == null ? null : gson.toJson(doubleArray);
    }

    @TypeConverter
    public static double[] toDoubleArray(String doubleArrayString) {
        if (doubleArrayString == null) return null;
        return gson.fromJson(doubleArrayString, double[].class);
    }
}
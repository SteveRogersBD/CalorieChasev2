package com.example.caloriechase.api;

import java.util.ArrayList;

public class DirectionsResponse {
    
    public static class Bounds {
        public Northeast northeast;
        public Southwest southwest;
    }
    
    public static class Distance {
        public String text;
        public int value;
    }
    
    public static class Duration {
        public String text;
        public int value;
    }
    
    public static class EndLocation {
        public double lat;
        public double lng;
    }
    
    public static class GeocodedWaypoint {
        public String geocoder_status;
        public String place_id;
        public ArrayList<String> types;
    }
    
    public static class Leg {
        public Distance distance;
        public Duration duration;
        public String end_address;
        public EndLocation end_location;
        public String start_address;
        public StartLocation start_location;
        public ArrayList<Step> steps;
        public ArrayList<Object> traffic_speed_entry;
        public ArrayList<Object> via_waypoint;
    }
    
    public static class Northeast {
        public double lat;
        public double lng;
    }
    
    public static class OverviewPolyline {
        public String points;
    }
    
    public static class Polyline {
        public String points;
    }
    
    public static class Root {
        public ArrayList<GeocodedWaypoint> geocoded_waypoints;
        public ArrayList<Route> routes;
        public String status;
    }
    
    public static class Route {
        public Bounds bounds;
        public String copyrights;
        public ArrayList<Leg> legs;
        public OverviewPolyline overview_polyline;
        public String summary;
        public ArrayList<Object> warnings;
        public ArrayList<Object> waypoint_order;
    }
    
    public static class Southwest {
        public double lat;
        public double lng;
    }
    
    public static class StartLocation {
        public double lat;
        public double lng;
    }
    
    public static class Step {
        public Distance distance;
        public Duration duration;
        public EndLocation end_location;
        public String html_instructions;
        public Polyline polyline;
        public StartLocation start_location;
        public String travel_mode;
        public String maneuver;
    }
}

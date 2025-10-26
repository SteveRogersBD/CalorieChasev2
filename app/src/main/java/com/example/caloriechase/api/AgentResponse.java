package com.example.caloriechase.api;

import java.util.ArrayList;

public class AgentResponse {
    public class Content{
        public ArrayList<Json> json;
    }

    public class Json{
        public String name;
        public double lat;
        public double lng;
        public String address;
    }

    public class Root{
        public String status;
        public ArrayList<Content> content;
    }


}

package com.hortonworks.simpleyarnapp;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.net.URI;

public class MyApp {

    private static final String uri = "hdfs://hadoop1/user1/";

    private static final Configuration config = new Configuration();


    public static void createDirectory(String directory) throws IOException {

        /* Get FileSystem object for given uri */
        FileSystem fs = FileSystem.get(URI.create(uri), config);
        boolean isCreated = fs.mkdirs(new Path(directory));

        System.out.println("Directory path " + directory);

        if (isCreated) {
            System.out.println("Directory created");
        } else {
            System.out.println("Directory creation failed");
        }
    }


    public static void main(String[] args) {

        String id = args[1];

        try {
            createDirectory(id);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        int n = 0;
        while( n < 10){
            System.out.println("SLEEPING");
            try {
                Thread.sleep(1000);
            }
            catch (Exception e){

            }
            ++n;
        }
    }
}

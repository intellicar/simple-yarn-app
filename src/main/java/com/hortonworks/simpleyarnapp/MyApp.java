package com.hortonworks.simpleyarnapp;

public class MyApp {


    public static void main(String[] args) {

        int n = 0;
        while( n < 60){
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

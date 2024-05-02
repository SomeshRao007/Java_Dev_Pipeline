package com.mycompany.app;

/**
 * Welcome to My Web Page !
 */
public class App
{

    private final String message = "Welcome to My Web Page !";

    public App() {}

    public static void main(String[] args) {
        System.out.println(new App().getMessage());
    }

    private final String getMessage() {
        return message;
    }

}

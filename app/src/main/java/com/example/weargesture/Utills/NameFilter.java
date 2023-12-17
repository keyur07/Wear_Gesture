package com.example.weargesture.Utills;

// A utility class for filtering and manipulating gesture names
public class NameFilter {
    // Original name of the gesture
    String originalName;

    // Filtered name after processing
    String filteredName;

    // Method used for filtering
    String method;

    // Execution name derived from the original name
    String executionName;

    // Constructor to initialize the NameFilter object
    public NameFilter(String NameOriginal) {
        this.originalName = NameOriginal;

        // Check if the originalName contains "##" to split it
        if (originalName.indexOf("##") != -1) {
            String[] split = originalName.split("##");
            filteredName = split[0];
            method = split[1];
            executionName = split[2];
        } else {
            filteredName = NameOriginal;
            method = "none";
            executionName = NameOriginal;
        }
    }

    // Get the filtered name based on the filtering method
    public String getFilteredName() {
        if (method.equals("mapp")) {
            // Mobile phone prefix for 'mapp' method
            String mobilePhonePrefix = "\uD83D\uDCF1";
            return mobilePhonePrefix + filteredName;
        } else if (method.equals("tasker")) {
            // Tasker prefix for 'tasker' method
            String taskerPrefix = "⚡";
            return taskerPrefix + filteredName;
        }

        // Return the original filtered name if method is not recognized
        return filteredName;
    }

    // Get the filtering method
    public String getMethod() {
        return method;
    }

    // Get the execution name (packName) derived from the original name
    public String getPackName() {
        return executionName;
    }

    // Get the original name of the gesture
    public String getOriginalName() {
        return originalName;
    }

    // Change the filtered name with a new name
    public String changeFilteredName(String newName) {
        return newName + "##" + method + "##" + executionName;
    }
}

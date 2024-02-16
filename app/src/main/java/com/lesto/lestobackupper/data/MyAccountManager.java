package com.lesto.lestobackupper.data;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.os.Bundle;

public class MyAccountManager {
    // Add a new account
    public void addAccount(Context context, String accountType, String username, String password) {
        AccountManager accountManager = AccountManager.get(context);

        // Create a new account
        Account account = new Account(username, accountType);

        // Add the account to the account manager
        boolean accountAdded = accountManager.addAccountExplicitly(account, password, null);

        if (accountAdded) {
            // Account added successfully
            // You can perform additional tasks here, such as syncing data
        } else {
            // Failed to add the account
            // Handle the error accordingly
        }
    }

    // Get an authentication token for an existing account
    public String getAuthToken(Context context, String accountType, String authTokenType) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(accountType);

        if (accounts.length > 0) {
            Account account = accounts[0]; // Use the first account for simplicity

            // Get an authentication token for the account
            Bundle options = null; // Optional bundle for additional options
            String authToken = null;
            try {
                authToken = accountManager.blockingGetAuthToken(account, authTokenType, true);
            } catch (Exception e) {
                // Error occurred while retrieving the authentication token
                // Handle the error accordingly
            }

            return authToken;
        } else {
            // No accounts of the specified type found
            // Handle the situation accordingly
            return null;
        }
    }

    // Remove an existing account
    public void removeAccount(Context context, String accountType) {
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(accountType);

        for (Account account : accounts) {
            // Remove the account from the account manager
            accountManager.removeAccount(account, null, null);
        }
    }
}
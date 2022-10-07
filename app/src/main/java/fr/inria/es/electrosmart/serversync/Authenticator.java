/*
 * BSD 3-Clause License
 *
 *       Copyright (c) 2014-2022, Arnaud Legout (arnaudlegout), centre Inria de
 *       l'Université Côte d'Azur, France. Contact: arnaud.legout@inria.fr
 *       All rights reserved.
 *
 *       Redistribution and use in source and binary forms, with or without
 *       modification, are permitted provided that the following conditions are met:
 *
 *       1. Redistributions of source code must retain the above copyright notice, this
 *       list of conditions and the following disclaimer.
 *
 *       2. Redistributions in binary form must reproduce the above copyright notice,
 *       this list of conditions and the following disclaimer in the documentation
 *       and/or other materials provided with the distribution.
 *
 *       3. Neither the name of the copyright holder nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 *       THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *       AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *       IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *       DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 *       FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *       DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *       SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *       CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *       OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *       OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package fr.inria.es.electrosmart.serversync;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.content.Context;
import android.os.Bundle;

/**
 * Standard implementation of the Authenticator which does nothing but implements the required
 * functions for the SyncAdapter framework to work.
 */
class Authenticator extends AbstractAccountAuthenticator {

    Authenticator(Context context) {
        super(context);
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse r, String s) {
        throw new UnsupportedOperationException();  // Editing properties is not supported
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse r, String s, String s2, String[] strings, Bundle bundle) {
        return null;    // Don't add additional accounts
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse r, Account account, Bundle bundle) {
        return null;    // Ignore attempts to confirm credentials
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse r, Account account, String s, Bundle bundle) {
        throw new UnsupportedOperationException();  // Getting an authentication token is not supported
    }

    @Override
    public String getAuthTokenLabel(String s) {
        throw new UnsupportedOperationException();  // Getting a label for the auth token is not supported
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse r, Account account, String s, Bundle bundle) {
        throw new UnsupportedOperationException();  // Updating user credentials is not supported
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse r, Account account, String[] strings) {
        throw new UnsupportedOperationException();  // Checking features for the account is not supported
    }

}
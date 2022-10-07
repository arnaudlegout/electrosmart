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

package fr.inria.es.electrosmart;

import android.util.Log;

/**
 * A class that represents a user profile containing
 * - name
 * - email
 * - sex
 * - age
 * - user segment
 */
public class UserProfile {
    private static final String TAG = "UserProfile";
    private String mName;
    private String mEmail;
    private int mSex;
    private int mAge;
    private int mSegment;

    public UserProfile() {
        mName = Const.PROFILE_NAME_UNKNOWN;
        mEmail = Const.PROFILE_EMAIL_UNKNOWN;
        mSex = Const.PROFILE_SEX_UNKNOWN;
        mAge = Const.PROFILE_AGE_UNKNOWN;
        mSegment = Const.PROFILE_SEGMENT_UNKNOWN;
    }

    /**
     * Constructor to create a UserProfile object
     *
     * @param name    the name of the profile
     * @param email   the email of the profile
     * @param sex     the sex of the profile
     * @param age     the age of the profile
     * @param segment the user segment of the profile
     */
    public UserProfile(String name, String email, int sex, int age, int segment) {
        Log.d(TAG, "UserProfile: create a new user profile");
        mName = name;
        mEmail = email;
        mSex = sex;
        mAge = age;
        mSegment = segment;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setEmail(String email) {
        mEmail = email;
    }

    public void setSex(int sex) {
        mSex = sex;
    }

    public void setAge(int age) {
        mAge = age;
    }

    public void setSegment(int segment) {
        mSegment = segment;
    }

    public String getName() {
        return mName;
    }

    public String getEmail() {
        return mEmail;
    }

    public int getSex() {
        return mSex;
    }

    public int getAge() {
        return mAge;
    }

    public int getSegment() {
        return mSegment;
    }
}
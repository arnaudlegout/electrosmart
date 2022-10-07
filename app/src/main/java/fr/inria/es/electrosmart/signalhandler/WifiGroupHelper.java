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

package fr.inria.es.electrosmart.signalhandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import fr.inria.es.electrosmart.signalproperties.BaseProperty;
import fr.inria.es.electrosmart.signalproperties.WifiGroupProperty;
import fr.inria.es.electrosmart.signalproperties.WifiProperty;

public class WifiGroupHelper {

    private static final String TAG = "WifiGroupHelper";

    /**
     * take as input an ArrayList of raw wifi signals and compute for
     * all Wifi Signals a grouping of all signals belonging to the same physical antenna. We use the
     * following heuristics:
     * - to group Wifi signals
     * - we group all signals with the same 5 first digit in the MAC address + the same frequency.
     * - to display the name for the group (by priority)
     * - do not display Hidden SSID if there are alternatives
     * - display the first SSID of the group (when sorted by alphabetical order) to do not
     * flicker the name with the change in the dBm values
     *
     * @param wifisignals: a arraylist containing the raw wifi signals obtained from one measurement
     * @return a hashmap containing as key a fake signal representing the characteristics of the
     * antenna (so this signal does not correspond to a real measured signal, but to a computed
     * signal that contains as dBm the highest value of the group and as name a name selected
     * with the heuristic given above. The value of the hashmap is the arraylist of all signals
     * belonging to the group.
     */
    public static ConcurrentHashMap<BaseProperty, List<BaseProperty>> groupWifi(List<BaseProperty> wifisignals) {
        ConcurrentHashMap<UniqueWifiAntennaKey, List<BaseProperty>> tmpWifiGroups = new ConcurrentHashMap<>();

        // 1) go through all signals to create tmpWifiGroups that contains as key a WifiKey object and
        //    as values all signals matching this wifiKey (that is belonging to the same antenna)
        UniqueWifiAntennaKey wifiKey;

        // test that wifisignals contains at least one signal
        if (wifisignals != null && wifisignals.size() > 0) {
            // go through all Wifi signals
            for (BaseProperty signal : wifisignals) {
                // convert the signal to a wifiKey in order to test whether it is already in a wifiGroup
                wifiKey = UniqueWifiAntennaKey.basePropertyToWifiGroupId(signal);
                if (tmpWifiGroups.containsKey(wifiKey) && tmpWifiGroups.get(wifiKey) != null) {
                    // here we add all signals of the group, but the first one (that is added
                    // in the else clause).
                    if (tmpWifiGroups.get(wifiKey).get(0).bssid.substring(0, 14).trim().equals(signal.bssid.substring(0, 14).trim())) {
                        signal.starredBssid = signal.bssid.substring(0, 14).trim() + ":*";
                    } else if (tmpWifiGroups.get(wifiKey).get(0).bssid.substring(3, 17).trim().equals(signal.bssid.substring(3, 17).trim())) {
                        signal.starredBssid = "*:" + signal.bssid.substring(3, 17).trim();
                    }
                    tmpWifiGroups.get(wifiKey).add(signal);
                } else {
                    /*
                    here we add the first signal of the group

                    Note that we do not create a starredBssid for the first signal of the group.
                    There is no strong reason not to do so (that is, we could have set it without
                    making the code buggy). Among the reasons not to set it
                    - it makes the code simpler
                    - a signal in a group with a single signal has no defined starredBssid,
                    we could have set it, but it would not have been used (to display
                    the starredBssid we make sure the group of at least of size 2)
                    */
                    List<BaseProperty> tmp = new ArrayList<>();
                    tmp.add(signal);
                    tmpWifiGroups.put(wifiKey, tmp);
                }
            }
        }

        // 2) sort the wifi groups per dBm values, and create a hashmap with key a
        //    forged WifiGroupProperty with the maximum dBm of the group and ssid the most
        //    relevant one, and a starred bssid, and with value the arraylist of all the signals
        //    of the group
        ConcurrentHashMap<BaseProperty, List<BaseProperty>> wifiGroups = new ConcurrentHashMap<>();
        List<BaseProperty> wifiGroup;
        WifiGroupProperty topWifi;
        for (UniqueWifiAntennaKey wifiId : tmpWifiGroups.keySet()) {
            wifiGroup = tmpWifiGroups.get(wifiId);

            if (wifiGroup != null && wifiGroup.size() > 0) {
                topWifi = new WifiGroupProperty((WifiProperty) wifiGroup.get(0));

                /*
                The starredBssid for a group of at least 2 signals will be the starredBssid
                of the second signal of the group (because the first one has no starredBssid set,
                see the comment above for an explanation). In case there is a single signal in
                the group, we must still set a starredBssid because it is used to compare
                WifiGroupProperty objects. In that case, we set it to the bssid of the first
                (and only one) signal of the group.
                */
                if (wifiGroup.size() > 1) {
                    topWifi.setStarredBssid(wifiGroup.get(1).starredBssid);
                } else {
                    // in case there is a single signal in the group, topWifi is this signal,
                    // so we set its starredBssid to its bssid.
                    topWifi.setStarredBssid(topWifi.bssid);
                }

                // set the right SSID for the topWifi
                // we manage here the case of hidden SSIDs
                for (int i = 0; i < wifiGroup.size(); i++) {
                    BaseProperty tmp = wifiGroup.get(i);
                    // test whether the SSID is hidden, skip it if this is the case
                    //TODO: implement a method making a more clever processing, e.g., skip all generic names such as FreeWifi (be careful that some signal contains only generic names)
                    if (tmp.ssid != null && !tmp.ssid.isEmpty()) {
                        topWifi.setSsid(tmp.ssid);
                        break;
                    }
                    if (i == wifiGroup.size() - 1) {
                        topWifi.setSsid(tmp.ssid);
                        //we are at the end of the for loop, no need to make an extra test in for
                        // This is an optimization, but I am not sure it makes sense
                        break;
                    }
                }
                wifiGroups.put(topWifi, wifiGroup);
            }
        }
        return wifiGroups;
    }

    // TODO: improve the identification of unique antenna to group wifi signals. See below
    // I assume that if signals have an overlap of 5bytes in the MAC address (so only different
    // by the 1st or last byte, they belong to the same physical antenna. This is only an
    // approximation as some antennas might share the last bytes (this will happen in professional
    // environment when AP are bought together). A better strategy would be to group signals
    // only if they appeared in the same atomic measurement. Here I can try to come up with a
    // better heuristic. Note that the current solution is satisfactory in most cases.

    /**
     * This class is used to define a unique wiki key in a hashmap  to define a list of wifi signals
     * that belongs to the same antenna.
     * <p/>
     * For now, we consider a wifi signal to belong to the same antenna if it has the same
     * frequency and truncated (to the 5 first or 5 last bytes) bssid.
     */
    private static class UniqueWifiAntennaKey {
        private int frequency;
        private String bssid;
        //the ssid is purely informative and cannot be used to compare different UniqueWifiAntennaKey
        private String ssid;

        UniqueWifiAntennaKey(int frequency, String bssid, String ssid) {
            this.frequency = frequency;
            this.bssid = bssid;
            this.ssid = ssid;
        }

        static UniqueWifiAntennaKey basePropertyToWifiGroupId(BaseProperty es) {
            return new UniqueWifiAntennaKey(es.freq, es.bssid, es.ssid);
        }

        public void setSsid(String ssid) {
            this.ssid = ssid;
        }

        /*
         * This methods returns true if both signals belong to the same Wifi group (that is the same
         * physical antenna) false otherwise
         */
        @Override
        public boolean equals(Object o) {
            if (o == null)
                return false;
            if (!(o instanceof UniqueWifiAntennaKey))
                return false;
            UniqueWifiAntennaKey other = (UniqueWifiAntennaKey) o;
            //implement here other policy to find that a wifi signal belongs to the same group
            //For now we consider that is bssid match the 5 first or five last bytes and frequency
            //are the same, they belong to the same group
            boolean matchBssid = false;
            if (this.bssid != null && other.bssid != null) {
                if ((this.bssid.substring(0, 14).trim().equals(other.bssid.substring(0, 14).trim())) ||
                        (this.bssid.substring(3, 17).trim().equals(other.bssid.substring(3, 17).trim()))) {
                    matchBssid = true;
                }
            }

            /*
             * IMPORTANT WARNING: the behavior of the ConcurrentHashMap.get() has changed between Java 6 and
             * Java 7. This might have an important consequence has Android below (and including) API 16 have
             * been compiled with Java 6,  and API strictly above 16 are compiled with Java 7.
             *
             * In Java 6, the get method compares with the equals method the object passed to get() with
             * all the object with the same hash value. If ever two objects are equals (with the equals()
             * method), the values corresponding to the object stored in the hashmap is returned.
             *
             * In Java 7, this behavior has been optimized. The get() method first access to all the objects
             * with the same hash value as the object passed to get (similar to java 6), but test before
             * using the equals method if the two objects have the same address (that is, they are the same
             * object). If this is the case, the equal() method is not called.
             *
             * Therefore, we need the test below to set the matchBssid to true if the bssid are null
             * to have a consistent behavior between Java 6 and Java 7.
             *
             * In Java 6, without this test, if I
             * get a key from a hashmap M with keySet() for instance and then test if this key
             * is in the hashmap with containsKey(), the test will be false (in Java 6) if the
             * bssid is null, which is incorrect. With the code below, the test will be true, which
             * is the correct behavior.
             *
             * Note that with Java 7, the test was always true, because testing is an object returns
             * by keySet() is in the hashmap M will always be true, because the get() method (used
             * by containsKey()) will test first the equality on the object address and will never
             * call this equals method.
             */
            if (this.bssid == null && other.bssid == null) {
                matchBssid = true;
            }

            return (this.frequency == other.frequency) && matchBssid;
        }

        /*
         The hash code only points to a certain "area" (or list, bucket etc) internally.
         Since different key objects could potentially have the same hash code, the hash code
         itself is no guarantee that the right key is found. The hashtable then iterates this
         area (all keys with the same hash code) and uses the key's equals() method to find
         the right key. Once the right key is found, the object stored for that key is returned.
          */
        @Override
        public int hashCode() {
            int prime = 31;
            int result = 17;
            result = prime * result + (bssid == null ? 0 : bssid.substring(3, 14).trim().hashCode());
            result = prime * result + frequency;
            return result;
        }

        @Override
        public String toString() {
            return "UniqueWifiAntennaKey:"
                    + " bssid=" + bssid
                    + " frequency=" + frequency
                    + " ssid=" + ssid;
        }
    }
}

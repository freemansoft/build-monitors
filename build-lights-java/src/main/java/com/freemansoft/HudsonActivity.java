package com.freemansoft;

/*
 * Copyright 2011 FreemanSoft Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

/**
 * An enum that represents each of the legal Hudson/CC curent activities
 * 
 * @author Joe Freeman
 */
public enum HudsonActivity {

    /**
     * 
     */
    SLEEPING("Sleeping"),
    /**
     * 
     */
    BUILDING("Building"),
    /**
     * 
     */
    PENDING("Pending"),

    /**
     * 
     */
    CHECKING_MODIFICATIONS("CheckingModifications"),

    /**
     * 
     */
    UNKNOWN("Unknown");

    /**
     * string we get back in XML
     */
    private String hudsonStatusString;

    /**
     * constructor
     * 
     * @param hudsonActivityString
     */
    private HudsonActivity(final String hudsonActivityString) {
        this.hudsonStatusString = hudsonActivityString;
    }

    /**
     * getter
     * 
     * @return
     */
    public String getCcXmlActivityString() {
        return hudsonStatusString;
    }

    /**
     * 
     * @param candidateString
     * @return
     */
    public static HudsonActivity getActivityForString(final String candidateString) {
        for (final HudsonActivity activity : values()) {
            if (activity.getCcXmlActivityString().equals(candidateString)) {
                return activity;
            }
        }
        return UNKNOWN;
    }
}

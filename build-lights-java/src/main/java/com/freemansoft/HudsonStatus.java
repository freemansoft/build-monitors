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
 * The last run status values we get from cc.xml
 * 
 * @author Joe Freeman
 */
public enum HudsonStatus {

    /**
     * 
     */
    SUCCESS("Success"),
    /**
     * 
     */
    FAILURE("Failure"),

    /**
     * 
     */
    EXCEPTION("Exception"),

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
     * @param hudsonStatusString
     */
    private HudsonStatus(final String hudsonStatusString) {
        this.hudsonStatusString = hudsonStatusString;
    }

    /**
     * getter
     * 
     * @return
     */
    public String getCcXmlStatusString() {
        return hudsonStatusString;
    }

    /**
     * 
     * @param candidateString
     * @return
     */
    public static HudsonStatus getStatusForString(final String candidateString) {
        for (final HudsonStatus status : values()) {
            if (status.getCcXmlStatusString().equals(candidateString)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}

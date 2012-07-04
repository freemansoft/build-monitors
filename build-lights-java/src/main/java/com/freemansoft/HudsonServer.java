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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.sun.org.apache.xml.internal.dtm.ref.DTMNodeList;

/**
 * Proxy for the hudson server. This proxy will poll the server in a background loop and park the
 * results in a buffer than can be picked up by other theads.
 * 
 * @author Joe Freeman
 */
public class HudsonServer extends TimerTask {

    /** logger */
    private static Logger LOG = Logger.getLogger(HudsonServer.class);
    /** path to the hudson server */
    private String url = null;
    /** interval in seconds */
    private int updateInterval = 180;

    /** xpath ready xml */
    private Document xmlDocument;
    /** compilation factory related */
    private XPath xPath;

    /**
     * the thing that lets us poll the server without writing Thread code
     */
    Timer timer = null;

    /**
     * constructor
     * 
     * @param url
     * @param updateInterval in seconds
     */
    public HudsonServer(final String url, final int updateInterval) {
        if (url == null) {
            throw new IllegalArgumentException();
        }
        if (updateInterval <= 0) {
            throw new IllegalArgumentException();
        }
        this.url = url;
        this.updateInterval = updateInterval;
    }

    /**
     * ugly method that fills instance variables but does nothing with it
     */
    private synchronized void fetchDocument() {
        try {
            final InputStream is = new URL(url).openStream();
            LOG.debug("fetched document " + url + ". remote final server sent " + is.available());
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            final DocumentBuilder db = dbf.newDocumentBuilder();
            xmlDocument = db.parse(is);
            is.close();
            xPath = XPathFactory.newInstance().newXPath();
        } catch (final SAXException ex) {
            clearDocsBecauseOfException("Unexpected: ", ex);
        } catch (final ParserConfigurationException ex) {
            clearDocsBecauseOfException("Unexpected: ", ex);
        } catch (final FileNotFoundException ex) {
            clearDocsBecauseOfException("Unable to find server info ", ex);
        } catch (final IOException ex) {
            clearDocsBecauseOfException("Unable to open server info ", ex);
        }
    }

    private void clearDocsBecauseOfException(final String messagePrefix, final Exception ex) {
        LOG.error(messagePrefix + " for " + url, ex);
        xmlDocument = null;
        xPath = null;
    }

    /**
     * extracts an element from the xmlDocument
     * 
     * @param projectName
     * @param returnType
     * @return status of the found project
     */
    public synchronized HudsonProjectStatus extractElement(final String projectName) {
        // LOG.debug("Number of top level nodes: " + xmlDocument.getChildNodes().getLength());
        // LOG.debug("Number of project nodes: "
        // + xmlDocument.getElementsByTagName("Project").getLength());
        LOG.debug("Requested nodes extraction under path " + projectName);
        if (xmlDocument == null || xPath == null) {
            return new HudsonProjectStatus(projectName, HudsonStatus.UNKNOWN,
                HudsonActivity.UNKNOWN);
        }
        try {
            // first see if the node is even there
            final XPathExpression wholeNodeExpression =
                xPath.compile("//Projects/Project[@name='" + projectName + "']");
            final DTMNodeList wholeNode =
                (DTMNodeList) wholeNodeExpression.evaluate(xmlDocument, XPathConstants.NODESET);
            if (wholeNode == null || wholeNode.getLength() == 0) {
                LOG.error("No project status for " + projectName);
                return new HudsonProjectStatus(projectName, HudsonStatus.UNKNOWN,
                    HudsonActivity.UNKNOWN);
            } else {

                // I'm lazy so let xpath parse it
                final XPathExpression lastBuildStatusExpression =
                    xPath.compile("//Projects/Project[@name='" + projectName
                        + "']/@lastBuildStatus");
                final String lastBuildStatus =
                    (String) lastBuildStatusExpression.evaluate(xmlDocument, XPathConstants.STRING);

                // I'm lazy so let xpath parse it
                final XPathExpression activityExpression =
                    xPath.compile("//Projects/Project[@name='" + projectName + "']/@activity");
                final String activity =
                    (String) activityExpression.evaluate(xmlDocument, XPathConstants.STRING);

                final HudsonProjectStatus projectStatus =
                    new HudsonProjectStatus(projectName, HudsonStatus
                        .getStatusForString(lastBuildStatus), HudsonActivity
                        .getActivityForString(activity));
                LOG.debug("Extracted: " + projectStatus);

                return projectStatus;
            }
        } catch (final XPathExpressionException ex) {
            LOG.error("Couldn't parse unexpected xml ", ex);
            return new HudsonProjectStatus(projectName, HudsonStatus.UNKNOWN,
                HudsonActivity.UNKNOWN);
        }
    }

    /**
     * we don't start the polling on creation because this lets us have more control
     */
    public void startPollingServer() {
        // get initial snapshot
        fetchDocument();
        // start a watcher thread that fetches document on regular basis
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer(true);
        timer.scheduleAtFixedRate(this, updateInterval * 1000, updateInterval * 1000);
    }

    /**
     * stop our thread
     */
    public void stopPollingServer() {
        LOG.debug("stopping polling.");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * fetches the data form the server
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void run() {
        fetchDocument();
    }
}

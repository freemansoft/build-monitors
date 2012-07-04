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

import gnu.io.SerialPort;

import java.lang.reflect.InvocationTargetException;

import org.apache.log4j.Logger;

/**
 * simple rxtx program to enumerate ports run with -
 * <p>
 * -Djava.library.path=c:/dev/build-lights/lib
 * <p>
 * Sample Program Arguments if running from inside Eclipse
 * <ul>
 * <li>LED Cube pointed at public hudson server with 4 builds<br>
 * COM9 115200 com.freemansoft.LEDCube http://ci.jenkins-ci.org/cc.xml
 * plugins_virtualbox,plugins_vmware,plugins_chucknorris,plugins_clover</li>
 * <li>Ambient orb point at public Hudson server with one build<br>
 * COM4 19200 com.freemansoft.AmbOrb http://ci.jenkins-ci.org/cc.xml plugins_cvs-tag</li>
 * </ul>
 * 
 * @author Joe Freeman
 */
public class BuildLights {

    private static Logger LOG = Logger.getLogger(SerialDevice.class);

    private static final int SERVER_POLLING_INTERVAL = 60;

    private HudsonServer ciServer;
    private SerialDevice communicationChannel;
    private BuildLightDevice buildLight;

    /** configuration parameter */
    private String rawArgProjectNames = "plugins_twitter";
    /** configuration parameter */
    private String projectNames[] = {};
    /** configuration parameter */
    private String serverUri = "http://ci.jenkins-ci.org/cc.xml";
    /** configuration parameter */
    private String port = "COM9";
    /** configuration parameter */
    private int dataRate = 112500;
    /** configuration parameter */
    private String deviceClassName = "com.freemansoft.LEDCube";

    /**
     * command line arguments
     * <ul>
     * <li>serial port name</li>
     * <li>data rate</li>
     * <li>LED device class name</li>
     * <li>hudson server url</li>
     * <li>hudson project name (comma separated list for devices supporting more than one light</li>
     * </ul>
     * 
     * @param args port server url project name
     */
    public static void main(final String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                System.out.println("Exited!");
            }
        });

        final BuildLights master = new BuildLights(args);
        master.run();

        LogUtil.logWithThreadName(LOG, "main is done", true);
        System.exit(0);
    }

    /**
     * constructs an object for our utility methods so they all don't have to be static
     * 
     * @param args
     */
    public BuildLights(final String[] args) {
        if (args.length < 5) {
            LOG.error("must run with 5 args port,data_rate,device_class,hudson_server,project");
            System.exit(-1);
        }
        port = args[0];
        dataRate = Integer.valueOf(args[1]).intValue();
        deviceClassName = args[2];
        serverUri = args[3];
        rawArgProjectNames = args[4];
        projectNames = rawArgProjectNames.split(",");
        if (projectNames.length == 0) {
            throw new IllegalArgumentException("No project names specified");
        }
    }

    private void run() {
        communicationChannel =
            new SerialDevice(port, dataRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        if (communicationChannel != null) {
            buildLight = createDevice(deviceClassName, communicationChannel);
            if (projectNames.length > buildLight.bldGetNumLights()) {
                throw new IllegalArgumentException("Device supports "
                    + buildLight.bldGetNumLights() + " but " + projectNames.length
                    + " were requested");
            }
            ciServer = new HudsonServer(serverUri, SERVER_POLLING_INTERVAL);
            ciServer.startPollingServer();
            loop();
            ciServer.stopPollingServer();
            communicationChannel.close();
        } else {
            LogUtil.logWithThreadName(LOG, "port " + port + " not found.", true);
        }

    }

    /**
     * instantiates a device from the class name and passes the attachedChannel in as a constructor
     * argument. This would be a lot easier with Spring.
     * 
     * @param deviceClassName
     * @param attachedChannel
     * @return
     */
    private BuildLightDevice createDevice(final String deviceClassName,
                    final SerialDevice attachedChannel) {
        Class deviceClass;
        BuildLightDevice blinkenDevice;
        try {
            deviceClass = BuildLights.class.getClassLoader().loadClass(deviceClassName);
            blinkenDevice =
                (BuildLightDevice) deviceClass.getConstructor(SerialDevice.class).newInstance(
                    attachedChannel);
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        } catch (final IllegalArgumentException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        } catch (final SecurityException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        } catch (final InstantiationException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        } catch (final IllegalAccessException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        } catch (final InvocationTargetException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        } catch (final NoSuchMethodException e) {
            throw new IllegalArgumentException("Can't build light device class " + deviceClassName,
                e);
        }
        return blinkenDevice;
    }

    /**
     * the main program's processing loop
     */
    private void loop() {
        try {
            while (true) {
                for (int i = 0; i < projectNames.length; i++) {
                    final HudsonProjectStatus status = ciServer.extractElement(projectNames[i]);
                    LOG.debug("About to tell device about status " + status);
                    switch (status.getStatus()) {
                        case SUCCESS:
                            if (status.getActivity() == HudsonActivity.BUILDING) {
                                buildLight.bldBlink(i, 0, 0, buildLight.bldGetMaxColor());
                            } else {
                                buildLight.bldSolid(i, 0, 0, buildLight.bldGetMaxColor());
                            }
                            break;
                        case FAILURE:
                            if (status.getActivity() == HudsonActivity.BUILDING) {
                                buildLight.bldBlink(i, buildLight.bldGetMaxColor(), 0, 0);
                            } else {
                                buildLight.bldSolid(i, buildLight.bldGetMaxColor(), 0, 0);
                            }
                            break;
                        case EXCEPTION:
                        case UNKNOWN:
                        default:
                            buildLight.bldBlink(i, buildLight.bldGetMaxColor(), buildLight
                                .bldGetMaxColor(), 0);
                            break;
                    }
                }
                // 1/4 of hudson server polling rate so we're never more than 1/4 interval behind
                Thread.sleep(SERVER_POLLING_INTERVAL / 4 * 1000);
            }
        } catch (final InterruptedException e) {
            // assume we're done
        }
    }
}

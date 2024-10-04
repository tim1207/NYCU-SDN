/*
 * Copyright 2024-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nycu.winlab.bridge;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.onlab.util.Tools.get;

import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import org.onosproject.core.CoreService;
import org.onosproject.core.ApplicationId;

import org.onosproject.cfg.ComponentConfigService;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true,
           service = {SomeInterface.class},
           property = {
               "someProperty=Some Default String Value",
           })
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger("LearningBridge");

    /** Some configurable property. */
    private String someProperty;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /* For registering the application */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    /* For handling the packet */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PacketService packetService;

    /* For installing the flow rule */
    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    /* Variables */
    private ApplicationId appId;
    private LearningBridgeProcessor processor = new LearningBridgeProcessor();
    private Map<DeviceId, Map<MacAddress, PortNumber>> bridgeTable = new HashMap<>();
    private int flowPriority = 30; // Default value
    private int flowTimeout = 30;  // Default value

    @Activate
    protected void activate() {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("nycu.winlab.bridge");
        packetService.addProcessor(processor, PacketProcessor.director(2));
        // Request for IPv4 packets
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.requestPackets(selector.build(), PacketPriority.LOWEST, appId);

        log.info("Started {}", appId.id());
    }

    @Deactivate
    protected void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        packetService.removeProcessor(processor);
        // Cancel the request for IPv4 packets
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();

        selector.matchEthType(Ethernet.TYPE_IPV4);
        packetService.cancelPackets(selector.build(), PacketPriority.LOWEST, appId);

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        if (context != null) {
            someProperty = get(properties, "someProperty");
        }
    }

    @Override
    public void someMethod() {
        log.info("Invoked");
    }

    /* Send out the packet from the specified port */
    private void packetOut(PacketContext context, PortNumber portNumber) {
        context.treatmentBuilder().setOutput(portNumber);
        context.send();
    }

    /* Broadcast the packet */
    private void flood(PacketContext context) {
        packetOut(context, PortNumber.FLOOD);
    }

    /* Install Flow Rule */
    private void installRule(PacketContext context, PortNumber dstProt) {
        InboundPacket packet = context.inPacket();
        Ethernet ethPacket = packet.parsed();
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment treatment = context.treatmentBuilder().setOutput(dstProt).build();

        // Match Src and Dst MAC Address
        selectorBuilder.matchEthDst(ethPacket.getDestinationMAC());
        selectorBuilder.matchEthSrc(ethPacket.getSourceMAC());

        // Create Flow Rule
        ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())          // Build the selector
                .withTreatment(treatment)                       // Setup the treatment
                .withPriority(flowPriority)                     // Setup the priority of flow
                .withFlag(ForwardingObjective.Flag.VERSATILE)   // Matches two or more header fields.
                .fromApp(appId)                                 // Specify from which application
                .makeTemporary(flowTimeout)                     // Set timeout
                .add();                                         // Build the flow rule
        // log.info("Flow Rule {}", forwardingObjective);

        // Install the flow rule on the specified switch
        flowObjectiveService.forward(packet.receivedFrom().deviceId(), forwardingObjective);

        // After install the flow rule, use packet-out message to send packet
        packetOut(context, dstProt);
    }

    /* Handle the packets coming from switchs */
    private class LearningBridgeProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            // log.info("LearningBridgeProcessor Handle Packet.");

            if (context.isHandled()) {
                //log.info("Packet has been handled, skip it...");
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethPacket = pkt.parsed();

            if (ethPacket == null) {
                log.error("Packet type is not ethernet");
                return;
            }

            if (ethPacket.getEtherType() == Ethernet.TYPE_LLDP || ethPacket.getEtherType() == Ethernet.TYPE_BSN) {
                log.info("Ignore LLDP or BDDP packet");
                return;
            }

            MacAddress srcMac = ethPacket.getSourceMAC();
            MacAddress dstMac = ethPacket.getDestinationMAC();
            DeviceId receivedID = pkt.receivedFrom().deviceId();

            if (bridgeTable.get(receivedID) == null) {
                bridgeTable.put(receivedID, new HashMap<>());
            }
            if (bridgeTable.get(receivedID).get(srcMac) == null) {
                Map<MacAddress, PortNumber> switchMap = bridgeTable.get(receivedID);
                switchMap.put(srcMac, context.inPacket().receivedFrom().port());
                bridgeTable.put(receivedID, switchMap);
                log.info("Add an entry to the port table of `{}`. MAC address: `{}` => Port: `{}`.",
                        receivedID, srcMac, context.inPacket().receivedFrom().port());
            }

            PortNumber dstPort = bridgeTable.get(receivedID).get(dstMac);

            if (dstPort == null) {
                // Table Miss, Flood it
                log.info("MAC address `{}` is missed on `{}`. Flood the packet.", dstMac, receivedID);
                flood(context);
            } else {
                // Table Hit, Install rule
                log.info("MAC address `{}` is matched on `{}`. Install a flow rule.", dstMac, receivedID);
                installRule(context, dstPort);
            }
        }
    }
}

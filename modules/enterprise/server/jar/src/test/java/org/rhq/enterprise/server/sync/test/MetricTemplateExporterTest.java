/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.sync.test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.rhq.core.clientapi.descriptor.plugin.PluginDescriptor;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.MeasurementDefinitionCriteria;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.DisplayType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.plugin.Plugin;
import org.rhq.core.domain.resource.ResourceCategory;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.sync.ExporterMessages;
import org.rhq.core.domain.util.PageControl;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.measurement.MeasurementDefinitionManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginManagerLocal;
import org.rhq.enterprise.server.resource.metadata.PluginStats;
import org.rhq.enterprise.server.sync.ExportingInputStream;
import org.rhq.enterprise.server.sync.MetricTemplateSynchronizer;
import org.rhq.enterprise.server.sync.Synchronizer;
import org.rhq.enterprise.server.sync.exporters.Exporter;
import org.rhq.enterprise.server.sync.exporters.MetricTemplateExporter;
import org.rhq.enterprise.server.sync.importers.MetricTemplateImporter;
import org.rhq.enterprise.server.sync.validators.ConsistencyValidator;

/**
 * 
 *
 * @author Lukas Krejci
 */
@Test
public class MetricTemplateExporterTest {

    private static final Log LOG = LogFactory.getLog(MetricTemplateExporterTest.class);

    private MeasurementDefinitionManagerLocal measurementDefManagerStub = new MeasurementDefinitionManagerLocal() {

        @Override
        public void removeMeasurementDefinition(MeasurementDefinition def) {
        }

        @Override
        public MeasurementDefinition getMeasurementDefinition(Subject subject, int definitionId) {
            return null;
        }

        @Override
        public List<MeasurementDefinition> findMeasurementDefinitionsByResourceType(Subject user, int resourceTypeId,
            DataType dataType, DisplayType displayType) {
            return null;
        }

        @Override
        public List<MeasurementDefinition> findMeasurementDefinitionsByIds(Subject subject,
            Integer[] measurementDefinitionIds) {
            return null;
        }

        @Override
        public PageList<MeasurementDefinition> findMeasurementDefinitionsByCriteria(Subject subject,
            MeasurementDefinitionCriteria criteria) {
            List<MeasurementDefinition> ret = new ArrayList<MeasurementDefinition>();

            ResourceType rt = new ResourceType("fakeType", "fakePlugin", ResourceCategory.PLATFORM, null);

            ret.add(new MeasurementDefinition(rt, "m1"));
            ret.add(new MeasurementDefinition(rt, "m2"));
            ret.add(new MeasurementDefinition(rt, "m3"));

            return new PageList<MeasurementDefinition>(ret, PageControl.getUnlimitedInstance());
        }
    };

    private PluginManagerLocal pluginManagerStub = new PluginManagerLocal() {

        @Override
        public void setPluginEnabledFlag(Subject subject, int pluginId, boolean enabled) throws Exception {
        }

        @Override
        public boolean registerPluginTypes(Plugin newPlugin, PluginDescriptor pluginDescriptor, boolean newOrUpdated,
            boolean forceUpdate) throws Exception {
            return false;
        }

        @Override
        public void registerPlugin(Subject subject, Plugin plugin, PluginDescriptor metadata, File pluginFile,
            boolean forceUpdate) throws Exception {
        }

        @Override
        public void purgePlugins(List<Plugin> plugins) {
        }

        @Override
        public void markPluginsForPurge(Subject subject, List<Integer> pluginIds) throws Exception {
        }

        @Override
        public boolean isReadyForPurge(Plugin plugin) {
            return false;
        }

        @Override
        public boolean installPluginJar(Subject subject, Plugin newPlugin, PluginDescriptor pluginDescriptor,
            File pluginFile) throws Exception {
            return false;
        }

        @Override
        public List<Plugin> getPluginsByResourceTypeAndCategory(String resourceTypeName,
            ResourceCategory resourceCategory) {
            return null;
        }

        @Override
        public List<Plugin> getPlugins() {
            return null;
        }

        @Override
        public List<PluginStats> getPluginStats(List<Integer> pluginIds) {
            return null;
        }

        @Override
        public Plugin getPlugin(String name) {
            return null;
        }

        @Override
        public List<Plugin> getInstalledPlugins() {
            Plugin p = new Plugin("fakePlugin", null, "12345");
            p.setVersion("1.0.0.test");
            return Collections.singletonList(p);
        }

        @Override
        public List<Plugin> getAllPluginsById(List<Integer> pluginIds) {
            return null;
        }

        @Override
        public List<Plugin> findPluginsMarkedForPurge() {
            return null;
        }

        @Override
        public List<Plugin> findAllDeletedPlugins() {
            return null;
        }

        @Override
        public void enablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        }

        @Override
        public void disablePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        }

        @Override
        public void deletePlugins(Subject subject, List<Integer> pluginIds) throws Exception {
        }
    };

    public void testCanExport() throws Exception {
        MetricTemplateSynchronizer exporter = new MetricTemplateSynchronizer(measurementDefManagerStub, pluginManagerStub);

        Set<Synchronizer<?, ?>> exporters = new HashSet<Synchronizer<?, ?>>();
        exporters.add(exporter);

        InputStream eis = new ExportingInputStream(exporters, new HashMap<String, ExporterMessages>(), 65536, false);

        //        String exportContents = readAll(new InputStreamReader(eis, "UTF-8"));
        //
        //        LOG.warn("Export contents:\n" + exportContents);
        //
        //        eis = new ByteArrayInputStream(exportContents.getBytes("UTF-8"));

        //        <?xml version="1.0" ?>
        //        <configuration-export>
        //            <validator class="org.rhq.enterprise.server.sync.validators.DeployedAgentPluginsValidator">
        //                <plugin name="fakePlugin" hash="12345" version="1.0.0.test"></plugin>
        //            </validator>
        //            <entities id="org.rhq.enterprise.server.sync.exporters.MetricTemplatesExporter">
        //                <entity>
        //                    <data>
        //                        <metricTemplate referencedEntityId="0" enabled="false" defaultInterval="0" metricName="m1" resourceTypePlugin="fakePlugin" resourceTypeName="fakeType"></metricTemplate>
        //                    </data>
        //                </entity>
        //                <entity>
        //                    <data>
        //                        <metricTemplate referencedEntityId="0" enabled="false" defaultInterval="0" metricName="m2" resourceTypePlugin="fakePlugin" resourceTypeName="fakeType"></metricTemplate>
        //                    </data>
        //                </entity>
        //                <entity>
        //                    <data>
        //                        <metricTemplate referencedEntityId="0" enabled="false" defaultInterval="0" metricName="m3" resourceTypePlugin="fakePlugin" resourceTypeName="fakeType"></metricTemplate>
        //                    </data>
        //                </entity>
        //            </entities>
        //        </configuration-export>        

        DocumentBuilder bld = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document doc = bld.parse(eis);

        Element root = doc.getDocumentElement();

        List<Node> validators = getDirectChildrenByTagName(root, ExportingInputStream.VALIDATOR_ELEMENT);
        Set<ConsistencyValidator> declaredValidators = exporter.getRequiredValidators();
        assertEquals(validators.size(), declaredValidators.size(), "Unexpected number of validators in the export xml");

        for (Node v : validators) {
            Element validator = (Element) v;

            String cls = validator.getAttribute(ExportingInputStream.CLASS_ATTRIBUTE);

            boolean found = false;
            for (ConsistencyValidator dv : declaredValidators) {
                if (cls.equals(dv.getClass().getName())) {
                    found = true;
                    break;
                }
            }

            assertTrue(found, "The metric template exporter doesn't seem to declare a validator with class: " + cls
                + ", but one such appeared in the export");
        }

        Element entities = (Element) getFirstDirectChildByTagName(root, ExportingInputStream.ENTITIES_EXPORT_ELEMENT);
        
        assertEquals(entities.getAttribute(ExportingInputStream.ID_ATTRIBUTE), MetricTemplateSynchronizer.class.getName(), "Unexpected id of the entities element.");
        
        NodeList metricTemplates = entities.getElementsByTagName("metricTemplate");
        
        assertEquals(metricTemplates.getLength(), 3, "Unexpected number of exported metric templates.");
        
        for(int i = 0; i < metricTemplates.getLength(); ++i) {
            Element m = (Element) metricTemplates.item(i);
            String expectedName = "m" + (i + 1);
            
            assertEquals(m.getAttribute("referencedEntityId"), "0", "Unexpected referencedEntityId value");
            assertEquals(m.getAttribute("enabled"), "false", "Unexpected enabled value");
            assertEquals(m.getAttribute("defaultInterval"), "0", "Unexpected defaultInterval value");
            assertEquals(m.getAttribute("metricName"), expectedName, "Unexpected metricName value");
            assertEquals(m.getAttribute("resourceTypePlugin"), "fakePlugin", "Unexpected resourceTypePlugin value");
            assertEquals(m.getAttribute("resourceTypeName"), "fakeType", "Unexpected resourceTypeName value");
        }
    }

    private static String readAll(Reader rdr) throws IOException {
        try {
            StringBuilder bld = new StringBuilder();
            int c;
            while ((c = rdr.read()) != -1) {
                bld.append((char) c);
            }

            return bld.toString();
        } finally {
            rdr.close();
        }
    }

    private static Node getFirstDirectChildByTagName(Node node, String tagName) {
        for (int i = 0; i < node.getChildNodes().getLength(); ++i) {
            Node n = node.getChildNodes().item(i);
            if (n.getNodeName().equals(tagName)) {
                return n;
            }
        }

        return null;
    }

    private static List<Node> getDirectChildrenByTagName(Node node, String tagName) {
        List<Node> ret = new ArrayList<Node>();
        for (int i = 0; i < node.getChildNodes().getLength(); ++i) {
            Node n = node.getChildNodes().item(i);
            if (n.getNodeName().equals(tagName)) {
                ret.add(n);
            }
        }

        return ret;
    }
}
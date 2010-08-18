/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.inventory.groups;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Criteria;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.SelectionStyle;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;

import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.gwt.ResourceGroupGWTServiceAsync;
import org.rhq.enterprise.gui.coregui.client.inventory.groups.wizard.GroupCreateWizard;
import org.rhq.enterprise.gui.coregui.client.util.message.Message;
import org.rhq.enterprise.gui.coregui.client.util.message.Message.Severity;

/**
 * @author Greg Hinkle
 */
public class ResourceGroupListView extends Table {

    private static final String DEFAULT_TITLE = "Resource Groups";

    public ResourceGroupListView() {
        this(DEFAULT_TITLE);
    }

    public ResourceGroupListView(String title) {
        super(title);
        setWidth100();
        setHeight100();
    }

    public ResourceGroupListView(Criteria criteria, String title) {
        super(title, criteria);
        setWidth100();
        setHeight100();
    }

    @Override
    protected void onInit() {
        super.onInit();

        // setHeaderIcon("?_24.png");

        final ResourceGroupsDataSource datasource = ResourceGroupsDataSource.getInstance();
        setDataSource(datasource);

        getListGrid().setSelectionType(SelectionStyle.SIMPLE);
        //table.getListGrid().setSelectionAppearance(SelectionAppearance.CHECKBOX);
        getListGrid().setResizeFieldsInRealTime(true);

        ListGridField idField = new ListGridField("id", "Id", 55);
        idField.setType(ListGridFieldType.INTEGER);
        ListGridField nameField = new ListGridField("name", "Name", 250);
        nameField.setCellFormatter(new CellFormatter() {
            public String format(Object o, ListGridRecord listGridRecord, int i, int i1) {
                return "<a href=\"#ResourceGroup/" + listGridRecord.getAttribute("id") + "\">" + o + "</a>";
            }
        });

        ListGridField descriptionField = new ListGridField("description", "Description");
        ListGridField typeNameField = new ListGridField("typeName", "Type", 130);
        ListGridField pluginNameField = new ListGridField("pluginName", "Plugin", 100);
        ListGridField categoryField = new ListGridField("category", "Category", 60);

        ListGridField availabilityField = new ListGridField("currentAvailability", "Availability", 55);

        availabilityField.setAlign(Alignment.CENTER);
        getListGrid().setFields(idField, nameField, descriptionField, typeNameField, pluginNameField, categoryField,
            availabilityField);

        addTableAction("Delete", Table.SelectionEnablement.ANY, "Delete the selected resource groups?",
            new TableAction() {
                public void executeAction(ListGridRecord[] selections) {
                    int[] groupIds = new int[selections.length];
                    int index = 0;
                    for (ListGridRecord selection : selections) {
                        groupIds[index++] = selection.getAttributeAsInt("id");
                    }
                    ResourceGroupGWTServiceAsync resourceGroupManager = GWTServiceLookup.getResourceGroupService();

                    resourceGroupManager.deleteResourceGroups(groupIds, new AsyncCallback<Void>() {
                        public void onFailure(Throwable caught) {
                            CoreGUI.getErrorHandler().handleError("Failed to delete selected resource groups", caught);
                        }

                        public void onSuccess(Void result) {
                            CoreGUI.getMessageCenter().notify(
                                new Message("Deleted selected resource groups", Severity.Info));

                            ResourceGroupListView.this.refresh();
                        }
                    });
                }
            });

        addTableAction("New", new TableAction() {
            public void executeAction(ListGridRecord[] selection) {
                new GroupCreateWizard(ResourceGroupListView.this).startBundleWizard();
            }
        });
    }

}